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
import java.util.Map;
import java.util.Random;

import net.grinder.common.GrinderProperties;
import net.grinder.common.Logger;
import net.grinder.engine.EngineException;
import net.grinder.plugininterface.PluginException;
import net.grinder.plugininterface.ThreadCallbacks;
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

    private static Random m_random = new Random();

    private final Monitor m_notifyOnCompletion;
    private final ThreadContext m_context;
    private final BSFProcessContext.BSFThreadContext m_bsfThreadContext;
    private final int m_threadID;
    private final TestRegistry m_testRegistry;

    private final long m_initialSleepTime;

    private final int m_numberOfRuns;

    /**
     * The constructor.
     */        
    public GrinderThread(Monitor notifyOnCompletion,
			 ProcessContext processContext,
			 BSFProcessContext bsfProcessContext, int threadID, 
			 ThreadCallbacks threadCallbacks)
	throws EngineException
    {
	m_notifyOnCompletion = notifyOnCompletion;

	m_context =
	    new ThreadContext(processContext, threadID, threadCallbacks);

	m_bsfThreadContext =
	    bsfProcessContext.new BSFThreadContext(m_context);

	m_threadID = threadID;
	m_testRegistry = processContext.getTestRegistry();

	final GrinderProperties properties = processContext.getProperties();

	m_initialSleepTime =
	    properties.getLong("grinder.thread.initialSleepTime", 0);

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
	    final ThreadCallbacks threadCallbackHandler =
		m_context.getThreadCallbackHandler();

	    try {
		threadCallbackHandler.initialize(m_context);
	    }
	    catch (PluginException e) {
		logger.logError("Plug-in initialize() threw " + e);
		e.printStackTrace(logger.getErrorLogWriter());
		return;
	    }
	    
	    logger.logMessage("Initialized " +
				threadCallbackHandler.getClass().getName());

	    m_context.getSleeper().sleepFlat(m_initialSleepTime);

	    if (m_numberOfRuns == 0) {
		logger.logMessage("About to run forever");
	    }
	    else {
		logger.logMessage("About to do " + m_numberOfRuns +
				    " runs");
	    }

	    int currentRun;	    

	    RUN_LOOP:
	    for (currentRun = 0;
		 (m_numberOfRuns == 0 || currentRun < m_numberOfRuns);
		 currentRun++)
	    {
		logger.setCurrentRunNumber(currentRun);

		try {
		    threadCallbackHandler.beginRun();
		}
		catch (PluginException e) {
		    logger.logError(
			"Aborting run - plug-in beginRun() threw " + e);
		    e.printStackTrace(logger.getErrorLogWriter());
		    continue RUN_LOOP; // .. or should we abort the thread?
		}

		m_bsfThreadContext.run();

		try {
		    threadCallbackHandler.endRun();
		}
		catch (PluginException e) {
		    logger.logError("Plugin endRun() threw: " + e);
		    e.printStackTrace(logger.getErrorLogWriter());
		}
	    }

	    logger.setCurrentRunNumber(-1);

	    logger.logMessage("Finished " + currentRun + " runs");
	}
	//	catch (AbortRunException e) {
	//	    logger.logError("Aborting run");
	//	    e.printStackTrace(logger.getErrorLogWriter());
	//	}
	catch (Sleeper.ShutdownException e) {
	    logger.logMessage("Shutdown");
	}
	catch(Exception e) {
	    logger.logError(" threw an exception:" + e);
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
}
