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

import java.io.PrintWriter;

import net.grinder.common.Test;
import net.grinder.plugininterface.PluginException;
import net.grinder.plugininterface.PluginThreadContext;
import net.grinder.plugininterface.ThreadCallbacks;
import net.grinder.statistics.CommonStatisticsViews;
import net.grinder.statistics.ExpressionView;
import net.grinder.statistics.StatisticExpression;
import net.grinder.statistics.TestStatistics;
import net.grinder.statistics.TestStatisticsFactory;


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
    private final TestStatistics m_currentTestStatistics =
	TestStatisticsFactory.getInstance().create();
    private final ExpressionView[] m_detailExpressionViews =
	CommonStatisticsViews.getDetailStatisticsView().getExpressionViews();

    private boolean m_aborted;
    private boolean m_abortedCycle;
    private boolean m_errorOccurred;
    private long m_startTime;
    private long m_elapsedTime;

    private StringBuffer m_scratchBuffer = new StringBuffer();

    public ThreadContext(ProcessContext processContext, int threadID)
    {
	super(processContext, Integer.toString(threadID));

	m_threadID = threadID;

	reset();
    }

    /** Package scope */
    void setGrinderThread(GrinderThread grinderThread) 
    {
	m_grinderThread = grinderThread;
    }
    
    public void reset()
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

    void invokeTest(ThreadCallbacks threadCallbacks, TestData testData)
    {
	final Test test = testData.getTest();
	m_currentTestStatistics.reset();

	try {
	    final boolean success;

	    startTimer();

	    try {
		success = threadCallbacks.doTest(test);
	    }
	    finally {
		stopTimer();
	    }

	    if (getAborted()) {
		m_currentTestStatistics.addError();
		logError("Plug-in aborted thread");
	    }
	    else if (getAbortedCycle()) {
		m_currentTestStatistics.addError();
		logError("Plug-in aborted cycle");
	    }
	    else {
		final long time = getElapsedTime();

		if (success) {
		    if (getRecordTime()) {
			m_currentTestStatistics.addTransaction(time);
		    }
		    else {
			m_currentTestStatistics.addTransaction();
		    }
		}
		else {
		    m_currentTestStatistics.addError();
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

		    for (int i=0; i<m_detailExpressionViews.length; ++i) {
			m_scratchBuffer.append(", ");

			final StatisticExpression expression =
			    m_detailExpressionViews[i].getExpression();

			if (expression.isDouble()) {
			    m_scratchBuffer.append(
				expression.getDoubleValue(
				    m_currentTestStatistics));
			}
			else {
			    m_scratchBuffer.append(
				expression.getLongValue(
				    m_currentTestStatistics));
			}
		    }

		    dataWriter.println(m_scratchBuffer);
		}
	    }
	}
	catch (PluginException e) {
	    m_currentTestStatistics.addError();
	    logError("Aborting cycle - plug-in threw " + e);
	    e.printStackTrace(getErrorLogWriter());
	    abortCycle();
	}
	finally {
	    testData.getStatistics().add(m_currentTestStatistics);
	}
    }

    public long getStartTime()
    {
	return m_startTime;
    }

    public TestStatistics getCurrentTestStatistics()
    {
	return m_currentTestStatistics;
    }
}

