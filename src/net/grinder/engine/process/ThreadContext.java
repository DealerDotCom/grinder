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

import net.grinder.common.FilenameFactory;
import net.grinder.common.GrinderException;
import net.grinder.common.GrinderProperties;
import net.grinder.common.Test;
import net.grinder.engine.EngineException;
import net.grinder.plugininterface.PluginException;
import net.grinder.plugininterface.PluginThreadContext;
import net.grinder.plugininterface.ThreadCallbacks;
import net.grinder.statistics.CommonStatisticsViews;
import net.grinder.statistics.ExpressionView;
import net.grinder.statistics.StatisticExpression;
import net.grinder.statistics.TestStatistics;
import net.grinder.statistics.TestStatisticsFactory;
import net.grinder.util.Sleeper;


/**
 * Package scope.
 *
 * @author Philip Aston
 * @version $Revision$
 **/
final class ThreadContext implements PluginThreadContext
{
    private final static ThreadLocal s_threadInstance = new ThreadLocal();

    private final ThreadCallbacks m_threadCallbackHandler;
    private final ThreadLogger m_threadLogger;
    private final PrintWriter m_dataWriter;
    private final FilenameFactory m_filenameFactory;
    private final boolean m_recordTime;
    private final long m_defaultSleepTime;
    private final Sleeper m_sleeper;
    private final TestResult m_testResult = new TestResult();

    private boolean m_abortedRun;

    private final TestStatistics m_currentTestStatistics =
	TestStatisticsFactory.getInstance().create();
    private final ExpressionView[] m_detailExpressionViews =
	CommonStatisticsViews.getDetailStatisticsView().getExpressionViews();

    private long m_startTime;
    private long m_elapsedTime;

    private StringBuffer m_scratchBuffer = new StringBuffer();

    public ThreadContext(ProcessContext processContext, int threadID,
			 ThreadCallbacks threadCallbackHandler)
	throws EngineException
    {
	m_threadCallbackHandler = threadCallbackHandler;

	final LoggerImplementation loggerImplementation =
	    processContext.getLoggerImplementation();

	m_threadLogger = loggerImplementation.createThreadLogger(threadID);
	m_dataWriter = loggerImplementation.getDataWriter();

	m_filenameFactory =
	    loggerImplementation.getFilenameFactory().
	    createSubContextFilenameFactory(Integer.toString(threadID));

	m_recordTime = processContext.getRecordTime();

	final GrinderProperties properties = processContext.getProperties();

	m_defaultSleepTime = properties.getLong("grinder.thread.sleepTime", 0);

	m_sleeper = new Sleeper(
	    properties.getDouble("grinder.thread.sleepTimeFactor", 1.0d),
	    properties.getDouble("grinder.thread.sleepTimeVariation", 0.2d),
	    m_threadLogger);
    }

    public final void setThreadInstance()
    {
	s_threadInstance.set(this);
    }

    public final static ThreadContext getThreadInstance()
    {
	return (ThreadContext)s_threadInstance.get();
    }
    
    public final boolean getAbortedRun() {
	return m_abortedRun;
    } 

    public final long getElapsedTime() {
	return m_elapsedTime;
    }

    public final int getThreadID()
    {
	return m_threadLogger.getThreadID();
    }

    public final int getCurrentRunNumber()
    {
	return m_threadLogger.getCurrentRunNumber();
    }

    public final void abortRun()
    {
	m_testResult.abortRun();
    }

    public void startTimer()
    {
	// This is to make it more likely that the timed section has a
	// "clear run".
	Thread.yield();
	m_startTime = System.currentTimeMillis();
	m_elapsedTime = -1;
    }

    public final void stopTimer()
    {
	if (m_elapsedTime < 0) // Not already stopped.
	{
	    m_elapsedTime = System.currentTimeMillis() - m_startTime;
	}
    }

    public final void logMessage(String message)
    {
	m_threadLogger.logMessage(message);
    }

    public final void logMessage(String message, int where)
    {
	m_threadLogger.logMessage(message, where);
    }

    public final void logError(String message)
    {
	m_threadLogger.logError(message);
    }
    
    public final void logError(String message, int where)
    {
	m_threadLogger.logError(message, where);
    }

    public final PrintWriter getOutputLogWriter()
    {
	return m_threadLogger.getOutputLogWriter();
    }

    public final PrintWriter getErrorLogWriter()
    {
	return m_threadLogger.getErrorLogWriter();
    }

    public String createFilename(String prefix)
    {
	return m_filenameFactory.createFilename(prefix);
    }

    public String createFilename(String prefix, String suffix)
    {
	return m_filenameFactory.createFilename(prefix, suffix);
    }

    final ThreadLogger getThreadLogger()
    {
	return m_threadLogger;
    }

    final ThreadCallbacks getThreadCallbackHandler()
    {
	return m_threadCallbackHandler;
    }

    final TestResult invokeTest(TestData testData)
	throws AbortRunException, Sleeper.ShutdownException
    {
	m_testResult.reset();
	m_currentTestStatistics.reset();

	final Test test = testData.getTest();
	m_threadLogger.setCurrentTestNumber(test.getNumber());
	
	m_sleeper.sleepNormal(
	    test.getParameters().getLong("sleepTime", m_defaultSleepTime));

	try {
	    startTimer();

	    try {
		m_testResult.setSuccess(m_threadCallbackHandler.doTest(test));
	    }
	    finally {
		stopTimer();
	    }

	    if (m_testResult.getAbortedRun()) {
		m_currentTestStatistics.addError();
		throw new AbortRunException("Plugin aborted run");
	    }
	    else {
		final long time = getElapsedTime();

		if (m_testResult.isSuccessful()) {
		    if (m_recordTime) {
			m_currentTestStatistics.addTransaction(time);
		    }
		    else {
			m_currentTestStatistics.addTransaction();
		    }
		}
		else {
		    m_currentTestStatistics.addError();
		    m_threadLogger.logError("Plug-in reported an error");
		}

		if (m_dataWriter != null) {
		    m_scratchBuffer.setLength(0);
		    m_scratchBuffer.append(getThreadID());
		    m_scratchBuffer.append(", ");
		    m_scratchBuffer.append(getCurrentRunNumber());
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

		    m_dataWriter.println(m_scratchBuffer);
		}
	    }
	}
	catch (PluginException e) {
	    m_currentTestStatistics.addError();
	    throw new AbortRunException("Plugin threw exception", e);
	}
	finally {
	    m_threadLogger.setCurrentTestNumber(-1);
	    testData.getStatistics().add(m_currentTestStatistics);
	}

	return m_testResult;
    }

    final Sleeper getSleeper()
    {
	return m_sleeper;
    }
    
    private final static class TestResult
	implements net.grinder.script.TestResult
    {
	private boolean m_successful;
	private boolean m_abortRun;

	public final boolean isSuccessful()
	{
	    return m_successful;
	}

	final void setSuccess(boolean b)
	{
	    m_successful = b;
	}

	final void reset()
	{
	    m_abortRun = false;
	    m_successful = false;
	}

	final void abortRun()
	{
	    m_abortRun = true;
	}

	final boolean getAbortedRun()
	{
	    return m_abortRun;
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

