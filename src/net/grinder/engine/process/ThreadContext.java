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

import net.grinder.common.FilenameFactory;
import net.grinder.common.GrinderException;
import net.grinder.common.GrinderProperties;
import net.grinder.common.Test;
import net.grinder.engine.EngineException;
import net.grinder.plugininterface.PluginException;
import net.grinder.plugininterface.PluginThreadContext;
import net.grinder.plugininterface.ThreadCallbacks;
import net.grinder.statistics.TestStatistics;
import net.grinder.util.Sleeper;


/**
 * Package scope.
 *
 * @author Philip Aston
 * @version $Revision$
 **/
final class ThreadContext implements PluginThreadContext
{
    private final ThreadCallbacks m_threadCallbackHandler;
    private final ThreadLogger m_threadLogger;
    private final PrintWriter m_dataWriter;
    private final FilenameFactory m_filenameFactory;
    private final boolean m_recordTime;
    private final long m_defaultSleepTime;
    private final Sleeper m_sleeper;
    private final TestResult m_testResult = new TestResult();

    private boolean m_abortedRun;
    private long m_startTime;
    private long m_elapsedTime;
    private TestStatistics m_currentTestStatistics;

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

	final Test test = testData.getTest();
	m_currentTestStatistics = testData.getStatistics();
	
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

		    if (m_recordTime) {
			m_scratchBuffer.append(", ");
			m_scratchBuffer.append(time);
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

