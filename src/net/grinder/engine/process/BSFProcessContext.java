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

package net.grinder.engine.process;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import com.ibm.bsf.BSFException;
import com.ibm.bsf.BSFManager;

import net.grinder.common.AbstractTestSemantics;
import net.grinder.common.GrinderException;
import net.grinder.common.GrinderProperties;
import net.grinder.common.Logger;
import net.grinder.common.Test;
import net.grinder.engine.EngineException;
import net.grinder.script.InvokeableTest;
import net.grinder.script.ScriptContext;
import net.grinder.script.ScriptException;
import net.grinder.script.TestResult;
import net.grinder.util.Sleeper;


/**
 * Wrap up the context information necessary to invoke a BSF script.
 *
 * Package scope.
 * 
 * @author Philip Aston
 * @version $Revision$
 */
class BSFProcessContext
{
    private final String m_script;
    private final String m_language;

    public BSFProcessContext(File scriptFile) throws EngineException
    {
	try {
	    final char[] data = new char[(int)scriptFile.length()];

	    final FileReader reader = new FileReader(scriptFile);
	    reader.read(data);
	    reader.close();

	    m_script = new String(data);
	}
	catch (IOException e) {
	    throw new EngineException("Could not read script file", e);
	}

	try {
	    final String language =
		BSFManager.getLangFromFilename(scriptFile.getPath());
	    new BSFManager().loadScriptingEngine(language);

	    m_language = language;
	}
	catch (BSFException e) {
	    throw new EngineException("BSF exception", e);
	}
    }

    class BSFThreadContext
    {
	private final ThreadContext m_threadContext;

	// Pretty sure BSFManager isn't thread safe, instantiate a new
	// instance for each thread.
	private final BSFManager m_bsfManager = new BSFManager();

	public BSFThreadContext(ThreadContext threadContext)
	    throws EngineException
	{
	    m_threadContext = threadContext;

	    try {
		m_bsfManager.declareBean("grinder", new BSFScriptContext(),
					 ScriptContext.class);
	    }
	    catch (BSFException e) {
		throw new EngineException("BSF exception", e);
	    }
	}

	public void run() throws EngineException
	{
	    try {
		m_bsfManager.exec(m_language, "Grinder", 0, 0, m_script);
	    }
	    catch (BSFException e) {
		throw new EngineException(
		    "Exception whilst invoking script", e);
	    }
	}

	private class BSFScriptContext implements ScriptContext
	{
	    private InvokeableTest[] m_tests;

	    public BSFScriptContext()
	    {
		recalculateTests();
	    }

	    private synchronized InvokeableTest[] recalculateTests()
	    {
		final Collection testDataSet =
		    ProcessContext.getInstance().getTestRegistry().getTests();

		if (m_tests == null || m_tests.length != testDataSet.size()) {
		    m_tests = new InvokeableTest[testDataSet.size()];

		    final Iterator iterator = testDataSet.iterator();
		    int i = 0;
	    
		    while (iterator.hasNext()) {
			final TestData testData = (TestData)iterator.next();
			m_tests[i++] = new BSFInvokeableTest(testData);
		    }
		}

		return m_tests;
	    }

	    public String getGrinderID()
	    {
		return ProcessContext.getInstance().getGrinderID();
	    }

	    public int getThreadID()
	    {
		return m_threadContext.getThreadID();
	    }

	    public Logger getLogger()
	    {
		return m_threadContext;
	    }

	    public synchronized InvokeableTest[] getTests()
	    {
		return recalculateTests();
	    }

	    public InvokeableTest registerTest(Test test)
		throws ScriptException
	    {
		final TestRegistry testRegistry =
		    ProcessContext.getInstance().getTestRegistry();

		try {
		    return
			new BSFInvokeableTest(testRegistry.registerTest(test));
		}
		catch (GrinderException e) {
		    throw new ScriptException("Exception registering test", e);
		}
	    }
	}
	
	private class BSFInvokeableTest
	    extends AbstractTestSemantics implements InvokeableTest
	{
	    private final TestData m_testData;

	    BSFInvokeableTest(TestData testData)
	    {
		m_testData = testData;
	    }

	    public final int getNumber()
	    {
		return m_testData.getTest().getNumber();
	    }

	    public final String getDescription()
	    {
		return m_testData.getTest().getDescription();
	    }

	    public final GrinderProperties getParameters()
	    {
		return m_testData.getTest().getParameters();
	    }

	    public TestResult invoke()
		throws net.grinder.script.AbortRunException
	    {
		try {
		    return m_threadContext.invokeTest(m_testData);
		}
		catch (AbortRunException e) {
		    throw new net.grinder.script.AbortRunException("Aborted",
								   e);
		}
		catch (Sleeper.ShutdownException e) {
		    throw new net.grinder.script.AbortRunException("Shut down",
								   e);
		}
	    }
	}
    }
}
