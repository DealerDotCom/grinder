// The Grinder
// Copyright (C) 2001  Paco Gomez
// Copyright (C) 2001  Philip Aston

// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.

// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.

// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.

package net.grinder.plugin.junit;

import java.lang.reflect.Method;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

import junit.framework.AssertionFailedError;
import junit.framework.TestCase;
import junit.framework.TestResult;
import junit.framework.TestSuite;

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
    }

    /**
     * Returns a Set of Tests. Returns null if the tests are to be
     * defined in the properties file.
     */
    public Set getTests()
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
	    m_processContext.logError("Unknown Test: " + test);
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

	public void beginCycle() throws PluginException
	{
	}

	public boolean doTest(Test testDefinition) throws PluginException
	{
	    m_context.logMessage("performing test");

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
		m_context.abortCycle();
	    }

	    return m_testListener.getResult();
	}
		
	public void endCycle() throws PluginException
	{
	}

	private class TestListener implements junit.framework.TestListener
	{
	    private boolean m_result = false;

	    public void addError(junit.framework.Test test, Throwable t) 
	    {
		m_context.logError("error: " + t);

		if (m_logStackTraces) {
		    t.printStackTrace();
		}
		
		m_result = false;
	    }

	    public void addFailure(junit.framework.Test test,
				   AssertionFailedError failure) 
	    {
		m_context.logError("failure: " + failure);

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
