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
		return
		    ProcessContext.getInstance().getTestRegistry().getTests();
	    }
	}
    }
}
