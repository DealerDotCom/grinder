// Copyright (C) 2000 Paco Gomez
// Copyright (C) 2000, 2001, 2002, 2003 Philip Aston
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

import net.grinder.common.FilenameFactory;
import net.grinder.common.GrinderProperties;
import net.grinder.common.Logger;
import net.grinder.engine.EngineException;
import net.grinder.plugininterface.PluginThreadContext;
import net.grinder.script.Statistics;
import net.grinder.util.Sleeper;


/**
 * Package scope.
 *
 * @author Philip Aston
 * @version $Revision$
 **/
final class ThreadContext implements PluginThreadContext {

  private static final ThreadLocal s_threadInstance = new ThreadLocal();

  private final ThreadLogger m_threadLogger;
  private final FilenameFactory m_filenameFactory;
  private final Sleeper m_sleeper;

  private final ScriptStatisticsImplementation m_scriptStatistics;

  private long m_startTime;
  private long m_elapsedTime;

  public ThreadContext(ProcessContext processContext, int threadID)
    throws EngineException {

    final LoggerImplementation loggerImplementation =
      processContext.getLoggerImplementation();

    m_threadLogger = loggerImplementation.createThreadLogger(threadID);

    m_filenameFactory =
      loggerImplementation.getFilenameFactory().
      createSubContextFilenameFactory(Integer.toString(threadID));

    m_scriptStatistics =
      new ScriptStatisticsImplementation(this,
					 loggerImplementation.getDataWriter(),
					 processContext.getRecordTime());

    final GrinderProperties properties = processContext.getProperties();

    m_sleeper =
      new Sleeper(properties.getDouble("grinder.sleepTimeFactor", 1.0d),
		  properties.getDouble("grinder.sleepTimeVariation", 0.2d),
		  m_threadLogger);
  }

  final void setThreadInstance() {
    s_threadInstance.set(this);
  }

  static final ThreadContext getThreadInstance() {
    return (ThreadContext)s_threadInstance.get();
  }

  public final Logger getLogger() {
    return m_threadLogger;
  }

  public final FilenameFactory getFilenameFactory() {
    return m_filenameFactory;
  }

  public final long getElapsedTime() {
    return m_elapsedTime;
  }

  public final int getThreadID() {
    return m_threadLogger.getThreadID();
  }

  public final int getRunNumber() {
    return m_threadLogger.getCurrentRunNumber();
  }

  public final void startTimer() {
    // This is to make it more likely that the timed section has a
    // "clear run".
    Thread.yield();
    m_startTime = System.currentTimeMillis();
    m_elapsedTime = -1;
  }

  public final void stopTimer() {
    if (m_elapsedTime < 0) { // Not already stopped.
      m_elapsedTime = System.currentTimeMillis() - m_startTime;
    }
  }

  final ThreadLogger getThreadLogger() {
    return m_threadLogger;
  }

  /**
   * This could be factored out to a separate "TestInvoker" class.
   * However, the sensible owner for a TestInvoker would be
   * ThreadContext, so keep it here for now. Also, all the
   * startTimer/stopTimer/getElapsedTime interface is part of the
   * PluginThreadContext interface.
   */
  final Object invokeTest(TestData testData, TestData.Invokeable invokeable)
    throws JythonScriptExecutionException {

    if (m_threadLogger.getCurrentTestNumber() != -1) {
      // Originally we threw a ReentrantInvocationException here.
      // However, this caused problems when wrapping Jython objects
      // that call themselves; in our scheme the wrapper shares a
      // dictionary so self = self we recurse up our own.
      return invokeable.call();
    }

    m_threadLogger.setCurrentTestNumber(testData.getTest().getNumber());

    m_scriptStatistics.beginTest(testData);

    try {
      startTimer();

      final Object testResult;

      try {
	testResult = invokeable.call();
      }
      finally {
	stopTimer();
      }

      final long time = getElapsedTime();

      m_scriptStatistics.setSuccessNoChecks();
      m_scriptStatistics.setTimeNoChecks(time);

      return testResult;
    }
    catch (org.python.core.PyException e) {
      m_scriptStatistics.setErrorNoChecks();

      // We don't log the exception. If the script doesn't handle the
      // exception it will be logged when the run is aborted,
      // otherwise we assume the script writer knows what they're
      // doing.
      throw e;
    }
    finally {
      m_scriptStatistics.endTest();
      m_threadLogger.setCurrentTestNumber(-1);
    }
  }

  final void endRun() {
    m_scriptStatistics.endRun();
  }

  final Sleeper getSleeper() {
    return m_sleeper;
  }
    
  public final long getStartTime() {
    return m_startTime;
  }

  final Statistics getScriptStatistics() {
    return m_scriptStatistics;
  }
}

