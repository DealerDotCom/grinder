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
import net.grinder.plugininterface.PluginException;
import net.grinder.plugininterface.ThreadCallbacks;


/**
 * The class encapsulating the control for each thread.
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

    private static boolean s_shutdown = false;

    private static Random m_random = new Random();

    private final GrinderProcess m_grinderProcess;
    private final ThreadCallbacks m_threadCallbacks;
    private final ThreadContext m_context;
    private final Map m_testSet;

    private final long m_defaultSleepTime;
    private final double m_sleepTimeVariation;
    private final double m_sleepTimeFactor;
    private final long m_initialSleepTime;

    private final int m_numberOfCycles;

    /**
     * This is a member so that ThreadContext can generate context
     * sensitive log messages.
     **/
    private int m_currentCycle = -1;

    /**
     * This is a member so that ThreadContextImplementation can
     * generate context sensitive log messages.
     **/
    private TestData m_currentTestData = null;

    /**
     * The constructor.
     */        
    public GrinderThread(GrinderProcess grinderProcess,
			 ThreadCallbacks threadCallbacks,
			 ThreadContext threadContext,
			 Map tests)
    {
	m_grinderProcess = grinderProcess;
	m_threadCallbacks = threadCallbacks;
	m_context = threadContext;
	m_testSet = tests;

	m_context.setGrinderThread(this);

	// Should really wrap all of this in a configuration class.
	final GrinderProperties properties = m_context.getProperties();
	
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

	try {
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

	    if (!s_shutdown) {
		if (m_numberOfCycles == 0) {
		    m_context.logMessage("About to run forever");
		}
		else {
		    m_context.logMessage("About to run " + m_numberOfCycles +
					 " cycles");
		}
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
		    sleepNormal(sleepTime >= 0 ?
				sleepTime : m_defaultSleepTime);

		    if (s_shutdown) {
			break CYCLE_LOOP;
		    }

		    m_context.invokeTest(m_threadCallbacks, m_currentTestData);

		    if (m_context.getAborted()) {
			break CYCLE_LOOP;
		    }
		    else if (m_context.getAbortedCycle()) {
			continue CYCLE_LOOP;
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

	    final int numberOfCycles = m_currentCycle;
	    m_currentCycle = -1;

	    m_context.logMessage("Finished " + numberOfCycles + " cycles");
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
    private void sleepNormal(long meanTime)
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
    private void sleepFlat(long maxTime)
    {
	if (maxTime > 0) {
	    doSleep(Math.abs(m_random.nextLong()) % maxTime);
	}
    }

    private void doSleep(long time)
    {
	if (time > 0) {
	    time = (long)(time * m_sleepTimeFactor);

	    m_context.logMessage("Sleeping for " + time + " ms");

	    long currentTime = System.currentTimeMillis();
	    final long wakeUpTime = currentTime + time;

	    while (currentTime < wakeUpTime && !s_shutdown) {
		try {
		    synchronized(GrinderThread.class) {
			GrinderThread.class.wait(wakeUpTime - currentTime);
		    }
		    break;
		}
		catch (InterruptedException e) {
		    currentTime = System.currentTimeMillis();
		}
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
	s_shutdown = true;
	GrinderThread.class.notifyAll();
    }
}
