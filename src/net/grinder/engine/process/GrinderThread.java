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
	    bsfProcessContext != null ?
	    bsfProcessContext.new BSFThreadContext(m_context) : null;

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
