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

import net.grinder.common.GrinderException;
import net.grinder.common.Logger;
import net.grinder.engine.EngineException;
import net.grinder.script.ScriptContext;


/**
 * @author Philip Aston
 * @version $Revision$
 */
class ScriptContextImplementation implements ScriptContext
{
    private final ProcessContext m_processContext;

    public ScriptContextImplementation(ProcessContext processContext)
    {
	m_processContext = processContext;
    }
    
    public String getGrinderID()
    {
	return m_processContext.getGrinderID();
    }

    public int getThreadID()
    {
	final ThreadContext threadContext = ThreadContext.getThreadInstance();

	if (threadContext != null) {
	    return threadContext.getThreadID();
	}

	return -1;
    }

    public int getRunNumber()
    {
	final ThreadContext threadContext =
	    ThreadContext.getThreadInstance();

	if (threadContext != null) {
	    return threadContext.getCurrentRunNumber();
	}

	return -1;
    }

    public Logger getLogger()
    {
	final ThreadContext threadContext = ThreadContext.getThreadInstance();

	if (threadContext != null) {
	    return threadContext;
	}

	return m_processContext.getLogger();
    }

    public void sleep(long meanTime) throws GrinderException
    {
	final ThreadContext threadContext = ThreadContext.getThreadInstance();

	if (threadContext == null) {
	    throw new EngineException(
		"sleep is currently only supported for worker threads");
	}

	threadContext.getSleeper().sleepNormal(meanTime);
    }

    public void sleep(long meanTime, long sigma) throws GrinderException
    {
	final ThreadContext threadContext = ThreadContext.getThreadInstance();

	if (threadContext == null) {
	    throw new EngineException(
		"sleep is currently only supported for worker threads");
	}

	threadContext.getSleeper().sleepNormal(meanTime, sigma);
    }
}
