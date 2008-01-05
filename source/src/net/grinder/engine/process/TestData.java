// Copyright (C) 2000 Paco Gomez
// Copyright (C) 2000 - 2006 Philip Aston
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
// COPYRIGHT HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
// INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
// (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
// SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
// HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
// STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
// ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
// OF THE POSSIBILITY OF SUCH DAMAGE.

package net.grinder.engine.process;

import net.grinder.common.Test;
import net.grinder.common.UncheckedGrinderException;
import net.grinder.engine.common.EngineException;
import net.grinder.engine.process.DispatchContext.DispatchStateException;
import net.grinder.script.NotWrappableTypeException;
import net.grinder.script.Statistics.StatisticsForTest;
import net.grinder.statistics.StatisticsSet;
import net.grinder.statistics.StatisticsSetFactory;
import net.grinder.util.TimeAuthority;


/**
 * Represents an individual test. Holds configuration information and
 * the tests statistics.
 *
 * Package scope.
 *
 * @author Philip Aston
 * @version $Revision$
 */
final class TestData
  implements TestRegistry.RegisteredTest, ScriptEngine.Dispatcher {

  private final StatisticsSetFactory m_statisticsSetFactory;
  private final TestStatisticsHelper m_testStatisticsHelper;
  private final TimeAuthority m_timeAuthority;
  private final Instrumenter m_instrumenter;
  private final ThreadContextLocator m_threadContextLocator;
  private final Test m_test;

  /**
   * Cumulative statistics for our test that haven't yet been set to
   * the console.
   */
  private final StatisticsSet m_testStatistics;

  private final DispatcherHolderThreadLocal m_dispatcherHolderThreadLocal =
    new DispatcherHolderThreadLocal();

  TestData(ThreadContextLocator threadContextLocator,
           StatisticsSetFactory statisticsSetFactory,
           TestStatisticsHelper testStatisticsHelper,
           TimeAuthority timeAuthority,
           Instrumenter instrumenter,
           Test testDefinition) {
    m_statisticsSetFactory = statisticsSetFactory;
    m_testStatisticsHelper = testStatisticsHelper;
    m_timeAuthority = timeAuthority;
    m_instrumenter = instrumenter;
    m_threadContextLocator = threadContextLocator;
    m_test = testDefinition;
    m_testStatistics = m_statisticsSetFactory.create();
  }

  Test getTest() {
    return m_test;
  }

  StatisticsSet getTestStatistics() {
    return m_testStatistics;
  }

  /**
   * Create a proxy object that wraps an target object for this test.
   *
   * @param o Object to wrap.
   * @return The proxy.
   * @throws NotWrappableTypeException If the target could not be wrapped.
   */
  public Object createProxy(Object o) throws NotWrappableTypeException {
    return m_instrumenter.createInstrumentedProxy(getTest(), this, o);
  }

  public Object dispatch(Callable callable) throws EngineException {
    final DispatcherHolder dispatcherHolder =
      m_dispatcherHolderThreadLocal.getDispatcherHolder();

    return dispatcherHolder.dispatch(callable);
  }

  /**
   * Thread local storage which keeps a {@link DispatcherHolder} for each worker
   * thread that has ever used this test.
   */
  private final class DispatcherHolderThreadLocal {
    private ThreadLocal m_threadLocal = new ThreadLocal() {
      public Object initialValue() {
        final ThreadContext threadContext = m_threadContextLocator.get();

        if (threadContext == null) {
          throw new UncheckedException("Only Worker Threads can invoke tests");
        }

        final Dispatcher dispatcher =
          new Dispatcher(threadContext.getDispatchResultReporter(),
                         new StopWatchImplementation(m_timeAuthority));

        return new DispatcherHolder(threadContext, dispatcher);
      }
    };

    public DispatcherHolder getDispatcherHolder() throws EngineException {
      try {
        return (DispatcherHolder)m_threadLocal.get();
      }
      catch (UncheckedException e) {
        throw new EngineException(e.getMessage());
      }
    }
  }

  /**
   * Caches a single Dispatcher for a particular worker thread. Used by
   * {@link TestData#dispatch}.
   *
   * <p>
   * Only allowing a single Dispatcher prevents nested invocations for the same
   * test/thread from being recorded multiple times. This makes life simpler for
   * the user, and for the script engine instrumentation.
   * </p>
   */
  private static final class DispatcherHolder {
    private final ThreadContext m_threadContext;
    private Dispatcher m_dispatcher;

    public DispatcherHolder(ThreadContext threadContext,
                            Dispatcher dispatcher) {
      m_threadContext = threadContext;
      m_dispatcher = dispatcher;
    }

    public Object dispatch(Callable callable)
      throws DispatchStateException, ShutdownException {

      if (m_dispatcher != null) {
        final Dispatcher dispatcher = m_dispatcher;

        m_threadContext.pushDispatchContext(dispatcher);
        m_dispatcher = null;

        try {
          return dispatcher.dispatch(callable);
        }
        finally {
          m_threadContext.popDispatchContext();
          m_dispatcher = dispatcher;
        }
      }
      else {
        // Already in a dispatch.
        return callable.call();
      }
    }
  }

  /**
   * Three states:
   * <ul>
   * <li><em>initialised</em> Start time is -1. Dispatch time is -1.
   * m_statisticsForTest is null.</li>
   * <li><em>dispatching</em> Start time is valid. Dispatch time is -1.
   * m_statisticsForTest is not null.</li>
   * <li><em>complete</em> Ready to report. Start time is valid. Dispatch
   * time is valid. m_statisticsForTest is null.</li>
   * </ul>
   *
   * {@link ThreadContextImplementation#getDispatchContext()} takes care to only
   * return references to Dispatchers that are <em>dispatching</em> or
   * <em>complete</em>.
   */
  private final class Dispatcher implements DispatchContext {
    private final DispatchResultReporter m_resultReporter;
    private final StopWatch m_pauseTimer;

    private long m_startTime = -1;
    private long m_dispatchTime = -1;
    private StatisticsForTestImplementation m_statisticsForTest;

    public Dispatcher(DispatchResultReporter resultReporter,
                      StopWatch pauseTimer) {

      m_resultReporter = resultReporter;
      m_pauseTimer = pauseTimer;
    }

    public Object dispatch(Callable callable) throws DispatchStateException {
      if (m_startTime != -1 || m_dispatchTime != -1) {
        throw new DispatchStateException("Last statistics were not reported");
      }

      m_pauseTimer.reset();

      m_statisticsForTest = new StatisticsForTestImplementation(
        this,
        m_testStatisticsHelper,
        m_statisticsSetFactory.create());

      try {
        // Make it more likely that the timed section has a "clear run".
        Thread.yield();

        m_startTime = m_timeAuthority.getTimeInMilliseconds();

        try {
          return callable.call();
        }
        finally {
          m_dispatchTime =
            m_timeAuthority.getTimeInMilliseconds() - m_startTime;
        }
      }
      catch (RuntimeException e) {

        if (m_pauseTimer.isRunning()) {
          m_pauseTimer.stop();
        }

        // Always mark as an error if the test threw an exception.
        m_testStatisticsHelper.setSuccess(
          m_statisticsForTest.getStatistics(), false);

        // We don't log the exception. If the script doesn't handle the
        // exception it will be logged when the run is aborted,
        // otherwise we assume the script writer knows what they're
        // doing.
        throw e;
      }
    }

    public void report() throws DispatchStateException {
      if (m_dispatchTime < 0) {
        throw new DispatchStateException("No statistics to report");
      }

      final StatisticsSet statistics =  m_statisticsForTest.getStatistics();

      m_testStatisticsHelper.recordTest(statistics, getElapsedTime());

      m_resultReporter.report(getTest(), m_startTime, statistics);

      if (m_testStatisticsHelper.getSuccess(statistics)) {
        getTestStatistics().add(statistics);
      }
      else {
        // If an error, we consider other information to be unreliable,
        // so do not aggregate it.
        m_testStatisticsHelper.incrementErrors(getTestStatistics());
      }

      // Disassociate ourselves from m_statisticsForTest;
      m_statisticsForTest.freeze();
      m_statisticsForTest = null;

      m_startTime = -1;
      m_dispatchTime = -1;
    }

    public Test getTest() {
      return TestData.this.getTest();
    }

    public StopWatch getPauseTimer() {
      return m_pauseTimer;
    }

    public long getElapsedTime() {
      if (m_startTime == -1) {
        return -1;
      }

      final long unadjustedTime;

      if (m_dispatchTime == -1) {
        unadjustedTime = m_timeAuthority.getTimeInMilliseconds() - m_startTime;
      }
      else {
        unadjustedTime = m_dispatchTime;
      }

      return Math.max(unadjustedTime - m_pauseTimer.getTime(), 0);
    }

    public StatisticsForTest getStatisticsForTest() {
      return m_statisticsForTest;
    }

    public void setHasNestedContexts() {
      m_testStatistics.setIsComposite();
    }
  }

  private static final class UncheckedException
    extends UncheckedGrinderException {
    public UncheckedException(String message) {
      super(message);
    }
  }
}
