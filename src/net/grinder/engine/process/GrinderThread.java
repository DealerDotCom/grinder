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

    private static Random m_random = new Random();

    private final ThreadCallbacks m_threadCallbacks;
    private final ThreadContextImplementation m_context;
    private final Map m_tests;
    private final PrintWriter m_dataPrintWriter;

    private long m_defaultSleepTime;
    private double m_sleepTimeVariation;
    private double m_sleepTimeFactor;
    private long m_beginCycleSleepTime;

    private int m_numberOfCycles;

    /** This is a member so that ThreadContextImplementation can
     * generate context sensitive log messages. */
    private int m_currentCycle = -1;

    /** This is a member so that ThreadContextImplementation can
     * generate context sensitive log messages. */
    private TestData m_currentTestData = null;

    /**
     * The constructor.
     */        
    public GrinderThread(ThreadCallbacks threadCallbacks,
			 ThreadContextImplementation threadContext,
			 PrintWriter dataPrintWriter, Map tests)
    {
	m_threadCallbacks = threadCallbacks;
	m_context = threadContext;
	m_dataPrintWriter = dataPrintWriter;
	m_tests = tests;

	m_context.setGrinderThread(this);

	// Should really wrap all of this in a configuration class.
	final GrinderProperties properties = GrinderProperties.getProperties();
	
	m_defaultSleepTime =
	    properties.getLong("grinder.thread.sleepTime", 0);
	m_sleepTimeFactor =
	    properties.getDouble("grinder.thread.sleepTimeFactor", 1.0d);
	m_sleepTimeVariation =
	    properties.getDouble("grinder.thread.sleepTimeVariation", 0.2d);
	m_beginCycleSleepTime =
	    properties.getLong("grinder.thread.beginCycleSleepTime", 0);

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
		e.printStackTrace();
		return;
	    }
	    
	    m_context.logMessage("Initialized " +
				 m_threadCallbacks.getClass().getName());
	    m_context.logMessage("About to run " + m_numberOfCycles +
				 " cycles");

	    CYCLE_LOOP:
	    for (m_currentCycle=0; m_currentCycle<m_numberOfCycles;
		 m_currentCycle++)
	    {
		// Random initial wait
		sleep(m_beginCycleSleepTime);

		try {
		    m_threadCallbacks.beginCycle();
		}
		catch (PluginException e) {
		    m_context.logError(
			"Aborting cycle - plug-in beginCycle() threw " + e);
		    e.printStackTrace();
		    continue CYCLE_LOOP;
		}
		
		final Iterator testIterator = m_tests.entrySet().iterator();

		TEST_LOOP:
		while (testIterator.hasNext()) {
		    final Map.Entry entry = (Map.Entry)testIterator.next();
		    final Integer testNumber = (Integer)entry.getKey();
		    m_currentTestData = (TestData)entry.getValue();

		    m_context.reset();

		    final long sleepTime = m_currentTestData.getSleepTime();
		    sleep(sleepTime >= 0 ? sleepTime : m_defaultSleepTime);

		    final TestStatistics statistics =
			m_currentTestData.getStatistics();

		    boolean success = false;
			
		    m_context.startTimer();

		    try {
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
			statistics.addAbortion();
			m_context.logError(
			    "Aborting cycle - plug-in threw " + e);
			e.printStackTrace();
			continue CYCLE_LOOP;
		    }

		    if (m_context.getAborted()) {
			statistics.addAbortion();
			m_context.logError("Plug-in aborted thread");
			break CYCLE_LOOP;
		    }

		    if (m_context.getAbortedCycle()) {
			statistics.addAbortion();
			m_context.logError("Plug-in aborted cycle");
			continue CYCLE_LOOP;
		    }

		    final long time = m_context.getElapsedTime();

		    if (success) {
			statistics.addTransaction(time);
		    }
		    else {
			statistics.addError();
			m_context.logError("Plug-in reported an error");
		    }

		    if (m_dataPrintWriter != null) {
			m_dataPrintWriter.println(
			    m_context.getThreadID() + ", " +
			    m_currentCycle + ", " + testNumber + ", " + time);
		    }
		}

		m_currentTestData = null;

		try {
		    m_threadCallbacks.endCycle();
		}
		catch (PluginException e) {
		    m_context.logError("Plugin endCycle() threw: " + e);
		    e.printStackTrace();
		}
	    }

	    m_currentCycle = -1;

	    m_context.logMessage("Finished " + m_numberOfCycles + " cycles");
	}
	catch(Exception e) {
	    m_context.logError(" threw an exception:" + e);
	    e.printStackTrace(System.err);
	}
	finally {
	    decrementThreadCount();
	}
    }

    /**
     * Sleep for a time based on the meanTime parameter.
     *
     * The actual time is taken from a pseudo normal distribution.
     * Approximately 99.75% of times will be within (100*
     * m_sleepTimeVariation) percent of the meanTime.
     */
    private void sleep(long meanTime) throws InterruptedException
    {
	if (meanTime > 0) {
	    meanTime = (long)(meanTime * m_sleepTimeFactor);

	    final long sleepTime;

	    if (m_sleepTimeVariation > 0) {
		final double sigma = (meanTime * m_sleepTimeVariation)/3.0;

		sleepTime = meanTime + (long)(m_random.nextGaussian() * sigma);
	    }
	    else {
		sleepTime = meanTime;
	    }

	    if (sleepTime > 0) {
		m_context.logMessage("Sleeping for " + sleepTime +
					   " ms");
		Thread.sleep(sleepTime);
	    }
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

    private String formatMessage(String message) 
    {
	final StringBuffer buffer = new StringBuffer();
	
	buffer.append("(thread ");
	buffer.append(m_context.getThreadID());

	if (m_currentCycle >= 0) {
	    buffer.append(" cycle " + m_currentCycle);
	}
	
	if (m_currentTestData != null) {
	    buffer.append(" test " +
			  m_currentTestData.getTest().getTestNumber());
	}

	buffer.append(") ");
	buffer.append(message);

	return buffer.toString();
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
}
