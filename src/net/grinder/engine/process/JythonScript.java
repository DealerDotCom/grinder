// Copyright (C) 2001, 2002 Philip Aston
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

package net.grinder.engine.process;

import java.io.File;
import java.util.Properties;

import org.python.core.PyException;
import org.python.core.PyObject;
import org.python.core.PyString;
import org.python.core.PySystemState;
import org.python.util.PythonInterpreter;

import net.grinder.common.GrinderProperties;
import net.grinder.common.Logger;
import net.grinder.common.Test;
import net.grinder.engine.EngineException;
import net.grinder.script.InvokeableTest;
import net.grinder.script.ScriptContext;
import net.grinder.script.ScriptException;
import net.grinder.script.TestResult;


/**
 * Wrap up the context information necessary to invoke a Jython script.
 *
 * Package scope.
 * 
 * @author Philip Aston
 * @version $Revision$
 */
class JythonScript
{
    private static final String TEST_CASE_CALLABLE_NAME = "TestCase";

    private final PySystemState m_systemState;
    private final PythonInterpreter m_interpreter;
    private final PyObject m_testCaseFactory;
    private final JythonScriptContext m_scriptContext;

    public JythonScript(File scriptFile) throws EngineException
    {
	//Properties properties = new Properties();
	
	//properties.put("python.home", "d:/jython/jython-2.1");

	//        PythonInterpreter.initialize(properties, null, new String[0]);
	PySystemState.initialize();

	m_systemState = new PySystemState();
	m_interpreter = new PythonInterpreter(null, m_systemState);
	
	m_scriptContext = new JythonScriptContext();
	m_interpreter.set("grinder", m_scriptContext);

	final String parentPath = scriptFile.getParent();

	m_systemState.path.insert(0, new PyString(parentPath != null ?
						  parentPath: ""));

        try {
	    // Run the test script, script does global set up here.
            m_interpreter.execfile(scriptFile.getPath());
        }
	catch (PyException e) {
            throw new JythonScriptExecutionException(
		"initialising test case", e);
        }

	// Find the callable that acts as a factory for test case instances.
	m_testCaseFactory = m_interpreter.get(TEST_CASE_CALLABLE_NAME);

	if (m_testCaseFactory == null || !m_testCaseFactory.isCallable()) {
	    throw new EngineException(
		"There is no callable (class or function) named '" +
		TEST_CASE_CALLABLE_NAME + "' in " + scriptFile);
	}
    }

    class JythonRunnable
    {
	private final PyObject m_testCase;

	public JythonRunnable() throws EngineException
	{
	    try {
		// Script does per-thread initialisation here and
		// returns a callable object.
		m_testCase = m_testCaseFactory.__call__();
	    }
	    catch (PyException e) {
		throw new JythonScriptExecutionException(
		    "creating per-thread test case object", e);
	    }	    

            if (!m_testCase.isCallable()) {
                throw new EngineException(
		    "The result of '" + TEST_CASE_CALLABLE_NAME +
		    "()' is not callable");
	    }
	}

	public void run() throws EngineException
	{
	    try {
		m_testCase.__call__();
	    }
	    catch (PyException e) {
		throw new JythonScriptExecutionException("invoking script", e);
	    }
	}
    }

    private class JythonScriptContext implements ScriptContext
    {
	public String getGrinderID()
	{
	    return ProcessContext.getInstance().getGrinderID();
	}

	public int getThreadID()
	{
	    final ThreadContext threadContext =
		ThreadContext.getThreadInstance();

	    if (threadContext != null) {
		return threadContext.getThreadID();
	    }

	    return -1;
	}

	public Logger getLogger()
	{
	    final ThreadContext threadContext =
		ThreadContext.getThreadInstance();

	    if (threadContext != null) {
		return threadContext;
	    }

	    return ProcessContext.getInstance();
	}
	
	public synchronized InvokeableTest[] getTests()
	{
	    return ProcessContext.getInstance().getTestRegistry().getTests();
	}
    }
}
