// Copyright (C) 2000 Paco Gomez
// Copyright (C) 2000, 2001, 2002 Philip Aston
// All rights reserved.
//
// This file is part of The Grinder software distribution. Refer to
// the file LICENSE which is part of The Grinder distribution for
// licensing details. The Grinder distribution is available on the
// Internet at http://grinder.sourceforge.net/
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
// "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
// LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
// FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
// REGENTS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
// INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
// (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
// SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
// HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
// STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
// ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
// OF THE POSSIBILITY OF SUCH DAMAGE.

package net.grinder.plugin.junit;

import java.lang.reflect.Method;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

import junit.framework.AssertionFailedError;
import junit.framework.TestCase;
import junit.framework.TestResult;
import junit.framework.TestSuite;

import net.grinder.common.GrinderException;
import net.grinder.common.GrinderProperties;
import net.grinder.common.Test;
import net.grinder.common.TestImplementation;
import net.grinder.plugininterface.GrinderPlugin;
import net.grinder.plugininterface.PluginException;
import net.grinder.plugininterface.PluginProcessContext;
import net.grinder.plugininterface.PluginThreadContext;
import net.grinder.plugininterface.ThreadCallbacks;


/**
 * Grinder Plugin thats wraps a JUnit test suite.
 * 
 * @author Philip Aston
 * @version $Revision$
 */
public class JUnitPlugin implements GrinderPlugin
{
    private PluginProcessContext m_processContext;
    private junit.framework.Test m_testSuite;
    private boolean m_logStackTraces;

    /**
     * This method is executed when the process starts. It is only
     * executed once.
     */
    public void initialize(PluginProcessContext processContext,
			   Set testsFromPropertiesFile)
	throws PluginException
    {
	m_processContext = processContext;
	
	final GrinderProperties parameters =
	    processContext.getPluginParameters();

	m_logStackTraces = parameters.getBoolean("logStackTraces", false);

	try {
	    final String testSuiteName =
		parameters.getMandatoryProperty("testSuite");

	    // Logic cribbed from junit.runner.BaseTestRunner.
	    final Class testSuiteClass = Class.forName(testSuiteName);

	    try {
		final Method suiteMethod =
		    testSuiteClass.getMethod("suite", new Class[0]);

		m_testSuite =(junit.framework.Test)
		    suiteMethod.invoke(null, new Class[0]);
	    }
	    catch(Exception e) {
		// Try to extract a test suite automatically
		m_testSuite = new TestSuite(testSuiteClass);
	    }
	}
	catch (Exception e) {
	    throw new PluginException("Error instantiating test suite", e);
	}

	try {
	    processContext.registerTests(getTests());
	}
	catch (GrinderException e) {
	    throw new PluginException("Failed to register tests", e);
	}	
    }

    private Set getTests()
    {
	final Set tests = new HashSet();

	// Flatten JUnit TestSuite.
	getTests(m_testSuite, tests);
	
	return tests;
    }

    private void getTests(junit.framework.Test test, Set tests)
    {
	// Really hacky switch on type, but no obvious other way of
	// doing this with the JUnit API.
	if (test instanceof TestSuite)  {
	    final Enumeration jUnitTestEnumeration =
		((TestSuite)test).tests();

	    while (jUnitTestEnumeration.hasMoreElements()) {
		junit.framework.Test jUnitTest =
		    (junit.framework.Test)jUnitTestEnumeration.nextElement();

		getTests(jUnitTest, tests); // Recurse.
	    }
	}
	else if (test instanceof TestCase) {
	    tests.add(new TestWrapper((TestCase)test));
	}
	else {
	    m_processContext.error("Unknown Test: " + test);
	}
    }

    /**
     * This method is called to create a handler for each thread.
     */
    public ThreadCallbacks createThreadCallbackHandler()
	throws PluginException
    {
	return new JUnitThreadCallbacks();
    }

    private class JUnitThreadCallbacks implements ThreadCallbacks
    {
	private PluginThreadContext m_context = null;
	private final TestListener m_testListener = new TestListener();
	private final TestResult m_testResult = new TestResult();

	public JUnitThreadCallbacks()
	{
	    m_testResult.addListener(m_testListener);
	}

	public void initialize(PluginThreadContext pluginThreadContext)
	{
	    m_context = pluginThreadContext;
	}

	public void beginRun() throws PluginException
	{
	}

	public boolean doTest(Test testDefinition) throws PluginException
	{
	    m_context.output("performing test");

	    final TestWrapper testWrapper = (TestWrapper)testDefinition;
	    final TestCase testCase = testWrapper.getJUnitTestCase();

	    m_context.startTimer();

	    try {
		testCase.run(m_testResult);
	    }
	    finally {
		m_context.stopTimer();
	    }

	    if (m_testResult.shouldStop()) {
		m_context.abortRun();
	    }

	    return m_testListener.getResult();
	}
		
	public void endRun() throws PluginException
	{
	}

	private class TestListener implements junit.framework.TestListener
	{
	    private boolean m_result = false;

	    public void addError(junit.framework.Test test, Throwable t) 
	    {
		m_context.error("error: " + t);

		if (m_logStackTraces) {
		    t.printStackTrace();
		}
		
		m_result = false;
	    }

	    public void addFailure(junit.framework.Test test,
				   AssertionFailedError failure) 
	    {
		m_context.error("failure: " + failure);

		if (m_logStackTraces) {
		    failure.printStackTrace();
		}
		
		m_result = false;
	    }

	    public void startTest(junit.framework.Test test) 
	    {
		m_result = true;	// Success.
	    }

	    public void endTest(junit.framework.Test test) 
	    {
	    }

	    boolean getResult()
	    {
		return m_result;
	    }
	}
    }
}

class TestWrapper extends TestImplementation
{
    private static int s_nextTestNumber = 0;

    private final transient TestCase m_testCase;

    public TestWrapper(TestCase jUnitTest)
    {
	super(s_nextTestNumber++, jUnitTest.toString(), null);

	m_testCase = jUnitTest;
    }

    public TestCase getJUnitTestCase()
    {
	return m_testCase;
    }
}
