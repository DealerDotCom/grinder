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

import java.io.PrintWriter;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;

import net.grinder.plugininterface.PluginException;
import net.grinder.plugininterface.ThreadCallbacks;
import net.grinder.statistics.StatisticsImplementation;
import net.grinder.util.GrinderProperties;


/**
 * The class executed by each thread.
 * The total number of threads per JVM is specified in the property "grinder.threads".
 * This class is responsible for instantiating an object of the class specified in the
 * property "grinder.cycleClass". It also invokes the methods specified in the
 * property "grinder.cycleMethods". It records the time spent in each method invocation.
 * The elapsed time is printed out in the "dat" file and stored in the shared space 
 * of the GrinderProcess object, shared by all threads.
 * 
 * @author Paco Gomez
 * @author Philip Aston
 * @version $Revision$
 */
class GrinderThread implements java.lang.Runnable
{
    /** m_numberOfThreads is incremented in constructor rather than in
     * run to avoid pathological race conditions. Hence it really
     * means "the number of GrinderThread's that have been created but
     * not run to completion" */
    private static int m_numberOfThreads = 0;
    private static boolean s_shutdown = false;

    private static Random m_random = new Random();

    private final GrinderProcess m_grinderProcess;
    private final ThreadCallbacks m_threadCallbacks;
    private final ThreadContextImplementation m_context;
    private final Map m_testSet;
    private final PrintWriter m_dataPrintWriter;

    private final long m_defaultSleepTime;
    private final double m_sleepTimeVariation;
    private final double m_sleepTimeFactor;
    private final long m_initialSleepTime;

    private final int m_numberOfCycles;

    private final boolean m_recordTime;

    /** This is a member so that ThreadContextImplementation can
     * generate context sensitive log messages. */
    private int m_currentCycle = -1;

    /** This is a member so that ThreadContextImplementation can
     * generate context sensitive log messages. */
    private TestData m_currentTestData = null;

    private StringBuffer m_scratchBuffer = new StringBuffer();

    /**
     * The constructor.
     */        
    public GrinderThread(GrinderProcess grinderProcess,
			 ThreadCallbacks threadCallbacks,
			 ThreadContextImplementation threadContext,
			 PrintWriter dataPrintWriter, boolean recordTime,
			 Map tests)
    {
	m_grinderProcess = grinderProcess;
	m_threadCallbacks = threadCallbacks;
	m_context = threadContext;
	m_dataPrintWriter = dataPrintWriter;
	m_recordTime = recordTime;
	m_testSet = tests;

	m_context.setGrinderThread(this);

	// Should really wrap all of this in a configuration class.
	final GrinderProperties properties = GrinderProperties.getProperties();
	
	m_defaultSleepTime =
	    properties.getLong("grinder.thread.sleepTime", 0);
	m_sleepTimeFactor =
	    properties.getDouble("grinder.thread.sleepTimeFactor", 1.0d);
	m_sleepTimeVariation =
	    properties.getDouble("grinder.thread.sleepTimeVariation", 0.2d);
	m_initialSleepTime =
	    properties.getLong("grinder.thread.initialSleepTime", 0);

	m_numberOfCycles = properties.getInt("grinder.cycles", 1);

	incrementThreadCount();	// See m_numberOfThreads javadoc.
    }
    
    /**
     * The thread's main loop.
     */     
    public void run()
    {
	m_currentCycle = -1;
	m_currentTestData = null;

	try{
	    try {
		m_threadCallbacks.initialize(m_context);
	    }
	    catch (PluginException e) {
		m_context.logError("Plug-in initialize() threw " + e);
		e.printStackTrace(m_context.getErrorLogWriter());
		return;
	    }
	    
	    m_context.logMessage("Initialized " +
				 m_threadCallbacks.getClass().getName());

	    sleepFlat(m_initialSleepTime);

	    if (m_numberOfCycles == 0) {
		m_context.logMessage("About to run forever");
	    }
	    else {
		m_context.logMessage("About to run " + m_numberOfCycles +
				     " cycles");
	    }

	    CYCLE_LOOP:
	    for (m_currentCycle=0;
		 (m_numberOfCycles == 0 || m_currentCycle < m_numberOfCycles)
		     && !s_shutdown;
		 m_currentCycle++)
	    {
		try {
		    m_threadCallbacks.beginCycle();
		}
		catch (PluginException e) {
		    m_context.logError(
			"Aborting cycle - plug-in beginCycle() threw " + e);
		    e.printStackTrace(m_context.getErrorLogWriter());
		    continue CYCLE_LOOP;
		}
		
		final Iterator testIterator = m_testSet.values().iterator();

		TEST_LOOP:
		while (testIterator.hasNext()) {
		    m_currentTestData = (TestData)testIterator.next();

		    m_context.reset();

		    final long sleepTime = m_currentTestData.getSleepTime();
		    sleepNormal(
			sleepTime >= 0 ? sleepTime : m_defaultSleepTime);

		    final StatisticsImplementation statistics =
			m_currentTestData.getStatistics();

		    boolean success = false;
			
		    try {
			m_context.startTimer();

			try {
			    success =
				m_threadCallbacks.doTest(
				    m_currentTestData.getTest());
			}
			finally {
			    m_context.stopTimer();		
			}
		    }
		    catch (PluginException e) {
			statistics.addError();
			m_context.logError(
			    "Aborting cycle - plug-in threw " + e);
			e.printStackTrace(m_context.getErrorLogWriter());
			continue CYCLE_LOOP;
		    }

		    if (m_context.getAborted()) {
			statistics.addError();
			m_context.logError("Plug-in aborted thread");
			break CYCLE_LOOP;
		    }

		    if (m_context.getAbortedCycle()) {
			statistics.addError();
			m_context.logError("Plug-in aborted cycle");
			continue CYCLE_LOOP;
		    }

		    final long time = m_context.getElapsedTime();

		    if (success) {
			if (m_recordTime) {
			    statistics.addTransaction(time);
			}
			else {
			    statistics.addTransaction();
			}
		    }
		    else {
			statistics.addError();
			m_context.logError("Plug-in reported an error");
		    }

		    if (m_dataPrintWriter != null) {
			m_scratchBuffer.setLength(0);
			final StringBuffer logLine = m_scratchBuffer;

			logLine.append(m_context.getThreadID());
			logLine.append(", ");
			logLine.append(m_currentCycle);
			logLine.append(", " );
			logLine.append(m_currentTestData.
				       getTest().getTestNumber());

			if (m_recordTime) {
			    logLine.append(", ");
			    logLine.append(time);
			}

			m_dataPrintWriter.println(logLine);
		    }
		}

		m_currentTestData = null;

		try {
		    m_threadCallbacks.endCycle();
		}
		catch (PluginException e) {
		    m_context.logError("Plugin endCycle() threw: " + e);
		    e.printStackTrace(m_context.getErrorLogWriter());
		}
	    }

	    m_currentCycle = -1;

	    m_context.logMessage("Finished " + m_numberOfCycles + " cycles");
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
     * Sleep for a time based on the meanTime parameter.
     *
     * The actual time is taken from a pseudo normal distribution.
     * Approximately 99.75% of times will be within (100*
     * m_sleepTimeVariation) percent of the meanTime.
     */
    private void sleepNormal(long meanTime) throws InterruptedException
    {
	if (meanTime > 0) {
	    if (m_sleepTimeVariation > 0) {
		final double sigma = (meanTime * m_sleepTimeVariation)/3.0;

		doSleep(meanTime + (long)(m_random.nextGaussian() * sigma));
	    }
	    else {
		doSleep(meanTime);
	    }
	}
    }

    /**
     * Sleep for a time based on the maxTime parameter.
     *
     * The actual time is taken from a pseudo random flat distribution
     * between 0 and maxTime.
     */
    private void sleepFlat(long maxTime) throws InterruptedException
    {
	if (maxTime > 0) {
	    doSleep(Math.abs(m_random.nextLong()) % maxTime);
	}
    }

    private void doSleep(long time) throws InterruptedException
    {
	if (time > 0) {
	    time = (long)(time * m_sleepTimeFactor);

	    m_context.logMessage("Sleeping for " + time + " ms");
	    Thread.sleep(time);
	}
    }

    /**
     * Package scope.
     */
    int getCurrentCycle() 
    {
	return m_currentCycle;
    }

    /**
     * Package scope.
     */
    TestData getCurrentTestData() 
    {
	return m_currentTestData;
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

    public static void shutdown()
    {
	s_shutdown = true;
    }
}
