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
// REGENTS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
// INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
// (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
// SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
// HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
// STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
// ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
// OF THE POSSIBILITY OF SUCH DAMAGE.

package net.grinder.engine.process;

import junit.framework.TestCase;

import net.grinder.common.Test;
import net.grinder.common.StubTest;
import net.grinder.engine.common.EngineException;
import net.grinder.engine.process.DispatchContext.DispatchStateException;
import net.grinder.engine.process.ScriptEngine.Dispatcher;
import net.grinder.engine.process.ScriptEngine.Dispatcher.Callable;
import net.grinder.statistics.ImmutableStatisticsSet;
import net.grinder.statistics.StatisticsIndexMap;
import net.grinder.statistics.StatisticsServicesImplementation;
import net.grinder.statistics.StatisticsSet;
import net.grinder.statistics.StatisticsSetFactory;
import net.grinder.testutility.AssertUtilities;
import net.grinder.testutility.RandomStubFactory;
import net.grinder.util.StandardTimeAuthority;


/**
 * Unit test case for <code>TestData</code>.
 *
 * @author Philip Aston
 * @version $Revision$
 */
public class TestTestData extends TestCase {

  private static final StatisticsIndexMap.LongSampleIndex s_timedTestsIndex;

  static {
    final StatisticsIndexMap indexMap =
      StatisticsServicesImplementation.getInstance().getStatisticsIndexMap();

    s_timedTestsIndex= indexMap.getLongSampleIndex("timedTests");
  }


  private final StatisticsSetFactory m_statisticsSetFactory =
    StatisticsServicesImplementation.getInstance().getStatisticsSetFactory();

  private final RandomStubFactory m_scriptEngineStubFactory =
    new RandomStubFactory(ScriptEngine.class);
  private final ScriptEngine m_scriptEngine =
    (ScriptEngine)m_scriptEngineStubFactory.getStub();

  private final RandomStubFactory m_testStatisticsHelperStubFactory =
    new RandomStubFactory(TestStatisticsHelper.class);
  private final TestStatisticsHelper m_testStatisticsHelper =
    (TestStatisticsHelper)m_testStatisticsHelperStubFactory.getStub();

  private final StubThreadContextLocator m_threadContextLocator =
    new StubThreadContextLocator();
  private final RandomStubFactory m_threadContextStubFactory =
    new RandomStubFactory(ThreadContext.class);
  private final ThreadContext m_threadContext =
    (ThreadContext)m_threadContextStubFactory.getStub();

  private final StandardTimeAuthority m_timeAuthority =
    new StandardTimeAuthority();

  public void testCreateProxy() throws Exception {
    final TestData testData =
      new TestData(null, m_statisticsSetFactory, null,
                   m_timeAuthority, m_scriptEngine, null);

    final Object original = new Object();

    testData.createProxy(original);

    m_scriptEngineStubFactory.assertSuccess(
      "createInstrumentedProxy", null, testData, original);
    m_scriptEngineStubFactory.assertNoMoreCalls();
  }

  public void testDispatch() throws Exception {
    final Test test1 = new StubTest(1, "test1");

    final TestData testData =
      new TestData(m_threadContextLocator,
                   m_statisticsSetFactory,
                   m_testStatisticsHelper,
                   m_timeAuthority,
                   m_scriptEngine,
                   test1);

    assertSame(test1, testData.getTest());
    final StatisticsSet statistics = testData.getTestStatistics();
    assertNotNull(statistics);

    final RandomStubFactory callableStubFactory =
      new RandomStubFactory(Callable.class);
    final Callable callable = (Callable)callableStubFactory.getStub();

    // 1. Happy case.
    try {
      testData.dispatch(callable);
      fail("Expected EngineException");
    }
    catch (EngineException e) {
      AssertUtilities.assertContains(e.getMessage(), "Only Worker Threads");
    }

    callableStubFactory.assertNoMoreCalls();

    m_threadContextLocator.set(m_threadContext);

    testData.dispatch(callable);

    callableStubFactory.assertSuccess("call");
    callableStubFactory.assertNoMoreCalls();

    m_threadContextStubFactory.assertSuccess("getDispatchResultReporter");
    final DispatchContext dispatchContext =
      (DispatchContext) m_threadContextStubFactory.assertSuccess(
      "pushDispatchContext", DispatchContext.class).getParameters()[0];
    m_threadContextStubFactory.assertSuccess("popDispatchContext");
    m_threadContextStubFactory.assertNoMoreCalls();

    // Test statistics not updated until we report.
    m_testStatisticsHelperStubFactory.assertNoMoreCalls();

    // Calling report() is the only way to reset the dispatcher.
    dispatchContext.report();

    m_testStatisticsHelperStubFactory.assertSuccess(
      "recordTest", StatisticsSet.class, Long.class);

    // 2. Nested case.
    final Callable outer = new Callable() {

      public Object call() {
        // No call to getDispatchResultReporter as dispatcher is reused.
        m_threadContextStubFactory.assertSuccess(
          "pushDispatchContext", DispatchContext.class);

        try {
          testData.dispatch(callable);
        }
        catch (EngineException e) {
          fail(e.getMessage());
        }
        return null;
      }};

    testData.dispatch(outer);

    m_threadContextStubFactory.assertSuccess("popDispatchContext");
    m_threadContextStubFactory.assertNoMoreCalls();

    // Test statistics not updated until we report.
    m_testStatisticsHelperStubFactory.assertNoMoreCalls();

    dispatchContext.report();

    m_testStatisticsHelperStubFactory.assertSuccess(
      "recordTest", StatisticsSet.class, Long.class);

    // 3. Unhappy case.
    final RuntimeException problem = new RuntimeException();
    callableStubFactory.setThrows("call", problem);

    try {
      testData.dispatch(callable);
      fail("Expected RuntimeException");
    }
    catch (RuntimeException e) {
      assertSame(problem, e);
    }

    // The dispatcher's statistics (not the test statistics) are
    // marked bad.
    final StatisticsSet dispatcherStatistics =
      (StatisticsSet)
      m_testStatisticsHelperStubFactory.assertSuccess(
      "setSuccess", StatisticsSet.class, Boolean.class).getParameters()[0];
    assertNotSame(statistics, dispatcherStatistics);
    m_testStatisticsHelperStubFactory.assertNoMoreCalls();

    // 4. Assertion failures.
    try {
      testData.dispatch(callable);
      fail("Expected DispatchStateException");
    }
    catch (DispatchStateException e) {
    }
  }

  public void testDispatchContext() throws Exception {
    final Test test1 = new StubTest(1, "test1");

    final TestData testData =
      new TestData(m_threadContextLocator,
                   m_statisticsSetFactory,
                   m_testStatisticsHelper,
                   m_timeAuthority,
                   m_scriptEngine,
                   test1);

    assertSame(test1, testData.getTest());
    final StatisticsSet statistics = testData.getTestStatistics();
    assertNotNull(statistics);

    final RandomStubFactory callableStubFactory =
      new RandomStubFactory(Callable.class);
    final Callable callable = (Callable)callableStubFactory.getStub();

    m_threadContextLocator.set(m_threadContext);

    final long beforeTime = System.currentTimeMillis();

    testData.dispatch(callable);

    callableStubFactory.assertSuccess("call");
    callableStubFactory.assertNoMoreCalls();

    m_threadContextStubFactory.assertSuccess("getDispatchResultReporter");
    final DispatchContext dispatchContext =
      (DispatchContext)
      m_threadContextStubFactory.assertSuccess(
        "pushDispatchContext", DispatchContext.class).getParameters()[0];
    m_threadContextStubFactory.assertSuccess("popDispatchContext");
    m_threadContextStubFactory.assertNoMoreCalls();

    // Test statistics not updated until we report.
    assertEquals(0, statistics.getCount(s_timedTestsIndex));
    m_testStatisticsHelperStubFactory.assertNoMoreCalls();

    assertSame(test1, dispatchContext.getTest());

    final StatisticsSet dispatchStatistics = dispatchContext.getStatistics();
    assertNotSame(statistics, dispatchStatistics);
    assertNotNull(dispatchStatistics);

    assertNotNull(dispatchContext.getPauseTimer());

    final long elapsedTime = dispatchContext.getElapsedTime();
    assertTrue(elapsedTime >= 0);
    assertTrue(elapsedTime <= System.currentTimeMillis() - beforeTime);

    dispatchContext.getPauseTimer().add(new StopWatch(){

      public void start() { }
      public void stop() { }
      public void reset() { }
      public void add(StopWatch watch) { }

      public long getTime() throws StopWatchRunningException {
        return 1000;
      }
    });

    // Call will take much less than a second, so we get 0.
    assertEquals(0, dispatchContext.getElapsedTime());

    // We're using a stubbed test statistics helper, and its easier
    // for the test to update the statistics by hand.
    dispatchStatistics.addSample(s_timedTestsIndex, 0);

    final ImmutableStatisticsSet reportedStatistics = dispatchContext.report();
    m_testStatisticsHelperStubFactory.assertSuccess(
      "recordTest", dispatchStatistics, new Long(0));
    assertEquals(1, reportedStatistics.getCount(s_timedTestsIndex));

    try {
      dispatchContext.report();
      fail("Expected DispatchStateException");
    }
    catch (DispatchStateException e) {
    }

    assertEquals(1, statistics.getCount(s_timedTestsIndex));

    m_testStatisticsHelperStubFactory.assertNoMoreCalls();

    assertEquals(-1, dispatchContext.getElapsedTime());
    assertNull(dispatchContext.getStatistics());

    final Callable longerCallable = new Callable() {
      public Object call() {
        assertTrue(dispatchContext.getElapsedTime() < 20);
        try {
          Thread.sleep(50);
        }
        catch (InterruptedException e) {
          fail(e.getMessage());
        }
        assertTrue(dispatchContext.getElapsedTime() >= 50);
        return null;
      }
    };

    testData.dispatch(longerCallable);

    final long elapsedTime2 = dispatchContext.getElapsedTime();
    assertTrue(elapsedTime2 >= 50);
    assertTrue(elapsedTime2 <= 200); // Pause timer was reset after last call.
  }

  /**
   * Creates dynamic ThreadContext stubs which implement invokeTest by
   * delegating directly to the callable. Must be public so
   * override_ methods can be invoked.
   */
  public static class ThreadContextStubFactory extends RandomStubFactory {
    private final TestData m_expectedTestData;

    public ThreadContextStubFactory(TestData expectedTestData) {
      super(ThreadContext.class);
      m_expectedTestData = expectedTestData;
    }

    public Object override_invokeTest(Object proxy,
                                      TestData testData,
                                      Dispatcher.Callable callable) {
      assertSame(m_expectedTestData, testData);
      return callable.call();
    }

    public ThreadContext getThreadContext() {
      return (ThreadContext)getStub();
    }
  }
}