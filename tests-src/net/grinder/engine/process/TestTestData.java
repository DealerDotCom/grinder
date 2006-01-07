// Copyright (C) 2000, 2001, 2002, 2003, 2004, 2005 Philip Aston
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
import net.grinder.engine.process.ScriptEngine.Dispatcher;
import net.grinder.statistics.StatisticsServicesImplementation;
import net.grinder.statistics.StatisticsSetFactory;
import net.grinder.testutility.CallData;
import net.grinder.testutility.RandomStubFactory;


/**
 * Unit test case for <code>TestData</code>.
 *
 * @author Philip Aston
 * @version $Revision$
 */
public class TestTestData extends TestCase {

  public void testTestData() throws Exception {
    final ThreadContextLocator threadContextLocator =
      new StubThreadContextLocator();

    final Test test1 = new StubTest(99, "Some stuff");

    final StatisticsSetFactory statisticsSetFactory =
      StatisticsServicesImplementation.getInstance().getStatisticsSetFactory();

    final TestData testData1 =
      new TestData(
        null, threadContextLocator, statisticsSetFactory.create(), test1);
    assertEquals(test1, testData1.getTest());
    assertNotNull(testData1.getStatisticsSet());

    final Test test2 = new StubTest(-33, "");

    final TestData testData2 =
      new TestData(
        null, threadContextLocator, statisticsSetFactory.create(), test2);
    assertEquals(test2, testData2.getTest());
    assertNotNull(testData2.getStatisticsSet());
  }

  public void testDispatch() throws Exception {

    final ThreadContextLocator threadContextLocator =
      new StubThreadContextLocator();

    final StatisticsSetFactory statisticsSetFactory =
      StatisticsServicesImplementation.getInstance().getStatisticsSetFactory();

    final Test test = new StubTest(2, "A description");
    final TestData testData =
      new TestData(null, threadContextLocator, statisticsSetFactory.create(), test);

    final Dispatcher.Callable callable =
      (Dispatcher.Callable)
      (new RandomStubFactory(Dispatcher.Callable.class)).getStub();

    try {
      testData.dispatch(callable);
      fail("Expected EngineException");
    }
    catch (EngineException e) {
    }

    final RandomStubFactory threadContextStubFactory =
      new RandomStubFactory(ThreadContext.class);

    threadContextLocator.set(
      (ThreadContext)threadContextStubFactory.getStub());

    final Object o = testData.dispatch(callable);

    final CallData callData =
      threadContextStubFactory.assertSuccess("invokeTest", testData, callable);
    assertEquals(o, callData.getResult());

    threadContextStubFactory.assertNoMoreCalls();
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

  public void testCreateProxy() throws Exception {
    final StatisticsSetFactory statisticsSetFactory =
      StatisticsServicesImplementation.getInstance().getStatisticsSetFactory();

    final RandomStubFactory scriptEngineStubFactory =
      new RandomStubFactory(ScriptEngine.class);
    final ScriptEngine scriptEngine =
      (ScriptEngine)scriptEngineStubFactory.getStub();

    final TestData testData =
      new TestData(scriptEngine, null, statisticsSetFactory.create(), null);

    final Object original = new Object();

    testData.createProxy(original);

    scriptEngineStubFactory.assertSuccess(
      "createInstrumentedProxy", null, testData, original);
    scriptEngineStubFactory.assertNoMoreCalls();
  }
}