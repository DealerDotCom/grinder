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

import com.ibm.bsf.BSFManager;

import net.grinder.common.GrinderProperties;
import net.grinder.plugininterface.PluginException;
import net.grinder.plugininterface.ThreadCallbacks;
import net.grinder.util.Sleeper;


/**
 * The class executed by each thread.
 * 
 * @author Paco Gomez
 * @author Philip Aston
 * @version $Revision$
 */
class GrinderThread implements java.lang.Runnable
{
    /**
     * m_numberOfThreads is incremented in constructor rather than in
     * run to avoid pathological race conditions. Hence it really
     * means "the number of GrinderThread's that have been created but
     * not run to completion"
     **/
    private static int m_numberOfThreads = 0;

    private static Random m_random = new Random();

    private final GrinderProcess m_grinderProcess;
    private final ThreadContext m_context;
    private final BSFFacade m_bsfFacade;

    private final long m_initialSleepTime;

    private final int m_numberOfRuns;

    /**
     * This is a member so that ThreadContext can generate context
     * sensitive log messages.
     **/
    private int m_currentRun = -1;

    /**
     * The constructor.
     */        
    public GrinderThread(GrinderProcess grinderProcess,
			 ThreadContext threadContext,
			 BSFFacade bsfFacade)
    {
	m_grinderProcess = grinderProcess;
	m_context = threadContext;
	m_bsfFacade = bsfFacade;

	m_context.setGrinderThread(this);

	// Should really wrap all of this in a configuration class.
	final GrinderProperties properties = m_context.getProperties();

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
	m_currentRun = -1;

	try {
	    final ThreadCallbacks threadCallbackHandler =
		m_context.getThreadCallbackHandler();

	    try {
		threadCallbackHandler.initialize(m_context);
	    }
	    catch (PluginException e) {
		m_context.logError("Plug-in initialize() threw " + e);
		e.printStackTrace(m_context.getErrorLogWriter());
		return;
	    }
	    
	    m_context.logMessage("Initialized " +
				 threadCallbackHandler.getClass().getName());

	    m_context.getSleeper().sleepFlat(m_initialSleepTime);

	    if (m_numberOfRuns == 0) {
		m_context.logMessage("About to run forever");
	    }
	    else {
		m_context.logMessage("About to do " + m_numberOfRuns +
				     " runs");
	    }

	    RUN_LOOP:
	    for (m_currentRun = 0;
		 (m_numberOfRuns == 0 || m_currentRun < m_numberOfRuns);
		 m_currentRun++)
	    {
		try {
		    threadCallbackHandler.beginRun();
		}
		catch (PluginException e) {
		    m_context.logError(
			"Aborting run - plug-in beginRun() threw " + e);
		    e.printStackTrace(m_context.getErrorLogWriter());
		    continue RUN_LOOP; // .. or should we abort the thread?
		}

		if (m_bsfFacade != null) {
		    m_bsfFacade.run();
		}
		else {
		    final Iterator testIterator =
			m_context.getTestRegistry().getTests().iterator();

		    TEST_LOOP:
		    while (testIterator.hasNext()) {
			final TestData testData =
			    (TestData)testIterator.next();

			m_context.invokeTest(testData);
		    }
		}

		try {
		    threadCallbackHandler.endRun();
		}
		catch (PluginException e) {
		    m_context.logError("Plugin endRun() threw: " + e);
		    e.printStackTrace(m_context.getErrorLogWriter());
		}
	    }

	    final int numberOfRuns = m_currentRun;
	    m_currentRun = -1;

	    m_context.logMessage("Finished " + numberOfRuns + " runs");
	}
	catch (AbortRunException e) {
	    m_context.logError("Aborting run");
	    e.printStackTrace(m_context.getErrorLogWriter());
	    
	}
	catch (Sleeper.ShutdownException e) {
	    m_currentRun = -1;
	    m_context.logMessage("Shutdown");
	}
	catch(Exception e) {
	    m_context.logError(" threw an exception:" + e);
	    e.printStackTrace(m_context.getErrorLogWriter());
	}
	finally {
	    decrementThreadCount();
	}
	
	synchronized(m_grinderProcess) {
	    m_grinderProcess.notifyAll();
	}
    }

    /**
     * Package scope.
     */
    int getCurrentRun() 
    {
	return m_currentRun;
    }

    private static synchronized void incrementThreadCount() 
    {
	m_numberOfThreads++;
    }

    private static synchronized void decrementThreadCount() 
    {
	m_numberOfThreads--;
    }

    public static int numberOfUncompletedThreads()
    {
	return m_numberOfThreads;
    }

    public static synchronized void shutdown()
    {
	// We rely on everyone picking this up next time they sleep.
	Sleeper.shutdown();
    }
}
