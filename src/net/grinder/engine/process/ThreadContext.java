// The Grinder
// Copyright (C) 2000, 2001  Paco Gomez
// Copyright (C) 2000, 2001  Philip Aston

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

import net.grinder.common.GrinderProperties;
import net.grinder.common.Test;
import net.grinder.plugininterface.PluginException;
import net.grinder.plugininterface.PluginThreadContext;
import net.grinder.plugininterface.ThreadCallbacks;
import net.grinder.statistics.StatisticsImplementation;
import net.grinder.util.Sleeper;


/**
 * Package scope.
 *
 * @author Philip Aston
 * @version $Revision$
 */
class ThreadContext extends ProcessContext implements PluginThreadContext
{
    private GrinderThread m_grinderThread = null;
    private final int m_threadID;
    private final Sleeper m_sleeper;
    private final ThreadCallbacks m_threadCallbackHandler;

    private boolean m_aborted;
    private boolean m_abortedCycle;
    private boolean m_errorOccurred;
    private long m_startTime;
    private long m_elapsedTime;

    private StringBuffer m_scratchBuffer = new StringBuffer();

    public ThreadContext(ProcessContext processContext, int threadID,
			 ThreadCallbacks threadCallbackHandler)
    {
	super(processContext, Integer.toString(threadID));

	m_threadID = threadID;
	m_threadCallbackHandler = threadCallbackHandler;

	final GrinderProperties properties = getProperties();

	m_sleeper = new Sleeper(
	    properties.getDouble("grinder.thread.sleepTimeFactor", 1.0d),
	    properties.getDouble("grinder.thread.sleepTimeVariation", 0.2d),
	    this);

	reset();
    }

    /** Package scope */
    void setGrinderThread(GrinderThread grinderThread) 
    {
	m_grinderThread = grinderThread;
    }
    
    private void reset()
    {
	m_aborted = false;
	m_abortedCycle = false;
	m_errorOccurred = false;
    }

    public boolean getAbortedCycle() {
	return m_abortedCycle;
    } 

    public boolean getAborted() {
	return m_aborted;
    }

    public long getElapsedTime() {
	return m_elapsedTime;
    }

    ThreadCallbacks getThreadCallbackHandler()
    {
	return m_threadCallbackHandler;
    }

    /*
     * Implementation of PluginThreadContext follows
     */

    public int getCurrentCycleID()
    {
	return m_grinderThread.getCurrentCycle();
    }

    public int getThreadID()
    {
	return m_threadID;
    }

    public void abort()
    {
	m_aborted = true;
    }

    public void abortCycle()
    {
	m_abortedCycle = true;
    }

    public void startTimer()
    {
	// This is to make it more likely that the timed section has a
	// "clear run".
	Thread.yield();
	m_startTime = System.currentTimeMillis();
	m_elapsedTime = -1;
    }

    public void stopTimer()
    {
	if (m_elapsedTime < 0) // Not already stopped.
	{
	    m_elapsedTime = System.currentTimeMillis() - m_startTime;
	}
    }

    protected void appendMessageContext(StringBuffer buffer) 
    {
	buffer.append("(thread ");
	buffer.append(getThreadID());

	final int currentCycle = getCurrentCycleID();
	final TestData currentTestData = m_grinderThread.getCurrentTestData();

	if (currentCycle >= 0) {
	    buffer.append(" cycle " + currentCycle);
	}
	
	if (currentTestData != null) {
	    buffer.append(" test " + currentTestData.getTest().getNumber());
	}

	buffer.append(") ");
    }

    void invokeTest(TestData testData)
	throws Sleeper.ShutdownException
    {
	reset();

	final Test test = testData.getTest();
	final StatisticsImplementation statistics = testData.getStatistics();

	m_sleeper.sleepNormal(testData.getSleepTime());

	try {
	    final boolean success;

	    startTimer();

	    try {
		success = m_threadCallbackHandler.doTest(test);
	    }
	    finally {
		stopTimer();
	    }

	    if (getAborted()) {
		statistics.addError();
		logError("Plug-in aborted thread");
	    }
	    else if (getAbortedCycle()) {
		statistics.addError();
		logError("Plug-in aborted cycle");
	    }
	    else {
		final long time = getElapsedTime();
		final boolean recordTime = getRecordTime();

		if (success) {
		    if (recordTime) {
			statistics.addTransaction(time);
		    }
		    else {
			statistics.addTransaction();
		    }
		}
		else {
		    statistics.addError();
		    logError("Plug-in reported an error");
		}

		final PrintWriter dataWriter = getDataWriter();

		if (dataWriter != null) {
		    m_scratchBuffer.setLength(0);
		    m_scratchBuffer.append(getThreadID());
		    m_scratchBuffer.append(", ");
		    m_scratchBuffer.append(getCurrentCycleID());
		    m_scratchBuffer.append(", " );
		    m_scratchBuffer.append(test.getNumber());

		    if (recordTime) {
			m_scratchBuffer.append(", ");
			m_scratchBuffer.append(time);
		    }

		    dataWriter.println(m_scratchBuffer);
		}
	    }
	}
	catch (PluginException e) {
	    statistics.addError();
	    logError("Aborting cycle - plug-in threw " + e);
	    e.printStackTrace(getErrorLogWriter());
	    abortCycle();
	}
    }

    Sleeper getSleeper()
    {
	return m_sleeper;
    }
}

