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

package net.grinder.engine.process;

import java.util.Iterator;

import net.grinder.common.GrinderProperties;
import net.grinder.engine.EngineException;
import net.grinder.plugininterface.PluginException;
import net.grinder.plugininterface.PluginThreadCallbacks;
import net.grinder.util.Sleeper;


/**
 * The class executed by each thread.
 * 
 * @author Paco Gomez
 * @author Philip Aston
 * @version $Revision$
 **/
class GrinderThread implements java.lang.Runnable
{
    /**
     * m_numberOfThreads is incremented in constructor
     * rather than in run to avoid pathological race conditions. Hence
     * it really means "the number of GrinderThread's that have been
     * created but not run to completion"
     **/

    private static short m_numberOfThreads = 0;

    private final Monitor m_notifyOnCompletion;
    private final ProcessContext m_processContext;
    private final ThreadContext m_context;
    private final JythonScript.JythonRunnable m_jythonRunnable;

    private final long m_initialSleepTime;
    private final int m_numberOfRuns;

    /**
     * The constructor.
     */        
    public GrinderThread(Monitor notifyOnCompletion,
			 ProcessContext processContext,
			 JythonScript jythonScript,
			 int threadID)
	throws EngineException
    {
	m_notifyOnCompletion = notifyOnCompletion;

	m_processContext = processContext;
	m_context = new ThreadContext(processContext, threadID);

	m_jythonRunnable = jythonScript.new JythonRunnable();

	final GrinderProperties properties = processContext.getProperties();

	m_initialSleepTime = properties.getLong("grinder.initialSleepTime", 0);

	m_numberOfRuns = properties.getInt("grinder.runs", 1);

	incrementThreadCount();	// See m_numberOfThreads javadoc.
    }
    
    /**
     * The thread's main loop.
     */     
    public void run()
    {
	m_context.setThreadInstance();

	final ThreadLogger logger = m_context.getThreadLogger();
	logger.setCurrentRunNumber(-1);

	try {
	    m_context.getSleeper().sleepFlat(m_initialSleepTime);

	    if (m_numberOfRuns == 0) {
		logger.output("about to run forever");
	    }
	    else {
		logger.output("about to do " + m_numberOfRuns + " run" +
			      (m_numberOfRuns == 1 ? "" : "s"));
	    }

	    int currentRun;	    

	    RUN_LOOP:
	    for (currentRun = 0;
		 (m_numberOfRuns == 0 || currentRun < m_numberOfRuns);
		 currentRun++)
	    {
		logger.setCurrentRunNumber(currentRun);

		m_beginRunPluginThreadCaller.run();

		// What exceptionhandling here?
		m_jythonRunnable.run();

		m_endRunPluginThreadCaller.run();
	    }

	    logger.setCurrentRunNumber(-1);

	    logger.output("finished " + currentRun + " run" +
			  (currentRun == 1 ? "" : "s"));
	}
	catch (Sleeper.ShutdownException e) {
	    logger.output("shutdown");
	}
	catch(Exception e) {
	    logger.error(" threw an exception:" + e);
	    e.printStackTrace(logger.getErrorLogWriter());
	}
	finally {
	    logger.setCurrentRunNumber(-1);
	    decrementThreadCount();
	}
	
	synchronized (m_notifyOnCompletion) {
	    m_notifyOnCompletion.notifyAll();
	}
    }

    private static synchronized void incrementThreadCount() 
    {
	m_numberOfThreads++;
    }

    private static synchronized void decrementThreadCount() 
    {
	m_numberOfThreads--;
    }

    public static short getNumberOfThreads()
    {
	return m_numberOfThreads;
    }

    public static synchronized void shutdown()
    {
	// We rely on everyone picking this up next time they sleep.
	Sleeper.shutdownAllCurrentSleepers();
    }

    private abstract class PluginThreadCaller
    {
	public void run() throws EngineException
	{
	    final Iterator iterator =
		m_processContext.getPluginRegistry().
		getPluginThreadCallbacksList(m_context).iterator();

	    while (iterator.hasNext()) {
		final PluginThreadCallbacks pluginThreadCallbacks =
		    (PluginThreadCallbacks)iterator.next();
		
		doOne(pluginThreadCallbacks);
	    }
	}

	protected abstract void doOne(
	    PluginThreadCallbacks pluginThreadCallbacks)
	    throws EngineException;
    }

    private final PluginThreadCaller m_beginRunPluginThreadCaller =
	new PluginThreadCaller() {
	    protected void doOne(PluginThreadCallbacks pluginThreadCallbacks)
		throws EngineException {
		try {
		    pluginThreadCallbacks.beginRun();
		}
		catch (PluginException e) {
		    final ThreadLogger logger = m_context.getThreadLogger();

		    logger.error(
			"Aborting thread - " +
			pluginThreadCallbacks.getClass().getName() +
			".beginRun() threw " + e);
		    e.printStackTrace(logger.getErrorLogWriter());

		    throw new EngineException("Thread could not begin run", e);
		}
	    }
	};

    private final PluginThreadCaller m_endRunPluginThreadCaller =
	new PluginThreadCaller() {
	    protected void doOne(PluginThreadCallbacks pluginThreadCallbacks)
		throws EngineException {
		try {
		    pluginThreadCallbacks.endRun();
		}
		catch (PluginException e) {
		    final ThreadLogger logger = m_context.getThreadLogger();

		    logger.error(
			"Aborting thread - " +
			pluginThreadCallbacks.getClass().getName() +
			".endRun() threw " + e);
		    e.printStackTrace(logger.getErrorLogWriter());

		    throw new EngineException("Thread could not end run", e);
		}
	    }
	};
}
