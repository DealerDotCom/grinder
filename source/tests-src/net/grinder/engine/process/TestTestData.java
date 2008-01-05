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

import junit.framework.TestCase;

import net.grinder.common.Test;
import net.grinder.common.StubTest;
import net.grinder.engine.common.EngineException;
import net.grinder.engine.process.DispatchContext.DispatchStateException;
import net.grinder.engine.process.ScriptEngine.Dispatcher;
import net.grinder.engine.process.ScriptEngine.Dispatcher.Callable;
import net.grinder.script.InvalidContextException;
import net.grinder.script.Statistics.StatisticsForTest;
import net.grinder.statistics.StatisticsIndexMap;
import net.grinder.statistics.StatisticsServicesImplementation;
import net.grinder.statistics.StatisticsSet;
import net.grinder.statistics.StatisticsSetFactory;
import net.grinder.testutility.AssertUtilities;
import net.grinder.testutility.RandomStubFactory;
import net.grinder.testutility.Time;
import net.grinder.util.StandardTimeAuthority;


/**
 * Unit test case for <code>TestData</code>.
 *
 * @author Philip Aston
 * @version $Revision$
 */
public class TestTestData extends TestCase {

  private static final StatisticsIndexMap.LongSampleIndex s_timedTestsIndex;
  private static final StatisticsIndexMap.LongIndex s_untimedTestsIndex;

  static {
    final StatisticsIndexMap indexMap =
      StatisticsServicesImplementation.getInstance().getStatisticsIndexMap();

    s_timedTestsIndex= indexMap.getLongSampleIndex("timedTests");
    s_untimedTestsIndex= indexMap.getLongIndex("untimedTests");
  }


  private final StatisticsSetFactory m_statisticsSetFactory =
    StatisticsServicesImplementation.getInstance().getStatisticsSetFactory();

  private final RandomStubFactory m_scriptEngineStubFactory =
    new RandomStubFactory(ScriptEngine.class);
  private final Instrumenter m_scriptEngine =
    (Instrumenter)m_scriptEngineStubFactory.getStub();

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
    m_testStatisticsHelperStubFactory.setResult("getSuccess", Boolean.TRUE);

    // Calling report() is the only way to reset the dispatcher.
    dispatchContext.report();

    m_testStatisticsHelperStubFactory.assertSuccess(
      "recordTest", StatisticsSet.class, Long.class);

    m_testStatisticsHelperStubFactory.assertSuccess(
      "getSuccess", StatisticsSet.class);

    m_testStatisticsHelperStubFactory.assertNoMoreCalls();

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

    m_testStatisticsHelperStubFactory.assertSuccess(
      "getSuccess", StatisticsSet.class);

    m_testStatisticsHelperStubFactory.assertNoMoreCalls();

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

    // 3b. Unhappy case with pause timer left running.
    dispatchContext.report();
    m_testStatisticsHelperStubFactory.resetCallHistory();

    Callable evilCallable = new Callable() {
      public Object call() {
        dispatchContext.getPauseTimer().start();
        return callable.call();
      }};

      try {
        testData.dispatch(evilCallable);
        fail("Expected RuntimeException");
      }
      catch (RuntimeException e) {
        assertSame(problem, e);
      }

    // The dispatcher's statistics (not the test statistics) are
    // marked bad.
    final StatisticsSet dispatcherStatistics2 =
      (StatisticsSet)
      m_testStatisticsHelperStubFactory.assertSuccess(
      "setSuccess", StatisticsSet.class, Boolean.class).getParameters()[0];
    assertNotSame(statistics, dispatcherStatistics2);
    m_testStatisticsHelperStubFactory.assertNoMoreCalls();

    // 4. Assertion failures.
    try {
      testData.dispatch(callable);
      fail("Expected DispatchStateException");
    }
    catch (DispatchStateException e) {
    }

    // 5. Lets test the reporting with an error.
    m_testStatisticsHelperStubFactory.setResult("getSuccess", Boolean.FALSE);

    dispatchContext.report();

    m_testStatisticsHelperStubFactory.assertSuccess(
      "recordTest", StatisticsSet.class, Long.class);

    m_testStatisticsHelperStubFactory.assertSuccess(
      "getSuccess", dispatcherStatistics);

    m_testStatisticsHelperStubFactory.assertSuccess(
      "incrementErrors", statistics);

    m_testStatisticsHelperStubFactory.assertNoMoreCalls();
  }

  public void testDispatchContext() throws Exception {
    final Test test1 = new StubTest(1, "test1");

    // We need a real helper here, not a stub.
    final TestStatisticsHelper testStatisticsHelper =
      new TestStatisticsHelperImplementation(
        StatisticsServicesImplementation.getInstance().getStatisticsIndexMap());

    final TestData testData =
      new TestData(m_threadContextLocator,
                   m_statisticsSetFactory,
                   testStatisticsHelper,
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

    assertSame(test1, dispatchContext.getTest());

    final StatisticsForTest dispatchStatisticsForTest =
      dispatchContext.getStatisticsForTest();
    assertSame(dispatchStatisticsForTest,
      dispatchContext.getStatisticsForTest());
    assertEquals(test1, dispatchStatisticsForTest.getTest());
    assertNotNull(dispatchStatisticsForTest);

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
      public boolean isRunning() {
        return false;
      }
    });

    // Call will take much less than a second, so we get 0.
    assertEquals(0, dispatchContext.getElapsedTime());

    assertEquals(0, dispatchStatisticsForTest.getLong("untimedTests"));

    // Its easier for the test to update the statistics by hand.
    dispatchStatisticsForTest.setLong("untimedTests", 2);

    dispatchContext.report();

    try {
      dispatchContext.report();
      fail("Expected DispatchStateException");
    }
    catch (DispatchStateException e) {
    }

    // report() will have updated the statistics with a single,
    // successful, timed test.
    assertEquals(1, statistics.getCount(s_timedTestsIndex));
    assertEquals(0, statistics.getValue(s_untimedTestsIndex));

    assertEquals(-1, dispatchContext.getElapsedTime());
    assertNull(dispatchContext.getStatisticsForTest());

    try {
      dispatchStatisticsForTest.setLong("untimedTests", 2);
      fail("Expected InvalidContextException");
    }
    catch (InvalidContextException e) {
    }

    final Callable longerCallable = new Callable() {
      public Object call() {
        assertTrue(dispatchContext.getElapsedTime() < 20);
        try {
          Thread.sleep(50);
        }
        catch (InterruptedException e) {
          fail(e.getMessage());
        }

        assertTrue(dispatchContext.getElapsedTime() >=
                   50 - Time.J2SE_TIME_ACCURACY_MILLIS);
        return null;
      }
    };

    testData.dispatch(longerCallable);

    final long elapsedTime2 = dispatchContext.getElapsedTime();
    assertTrue(elapsedTime2 >= 50 - Time.J2SE_TIME_ACCURACY_MILLIS);
    assertTrue(elapsedTime2 <= 200); // Pause timer was reset after last call.

    assertFalse(statistics.isComposite());
    dispatchContext.setHasNestedContexts();
    assertTrue(statistics.isComposite());
  }

  public void testDispatchForBug1593169() throws Exception {
    final TestData testData =
      new TestData(m_threadContextLocator,
                   m_statisticsSetFactory,
                   m_testStatisticsHelper,
                   m_timeAuthority,
                   m_scriptEngine,
                   new StubTest(1, "test1"));


    final RandomStubFactory callableStubFactory =
      new RandomStubFactory(Callable.class);
    final Callable callable = (Callable)callableStubFactory.getStub();

    m_threadContextLocator.set(m_threadContext);

    final ShutdownException se = new ShutdownException("Bang");
    m_threadContextStubFactory.setThrows("pushDispatchContext", se);

    try {
      testData.dispatch(callable);
      fail("Expected ShutdownException");
    }
    catch (ShutdownException e) {
    }

    m_threadContextStubFactory.assertSuccess("getDispatchResultReporter");
    m_threadContextStubFactory.assertException(
      "pushDispatchContext",
      new Class[] { DispatchContext.class, },
      se);
    m_threadContextStubFactory.assertNoMoreCalls();
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