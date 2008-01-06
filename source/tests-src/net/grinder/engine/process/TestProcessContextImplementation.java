// Copyright (C) 2007 Philip Aston
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

import net.grinder.common.GrinderProperties;
import net.grinder.common.Logger;
import net.grinder.common.StubTest;
import net.grinder.common.ThreadLifeCycleListener;
import net.grinder.common.WorkerIdentity;
import net.grinder.common.WorkerProcessReport;
import net.grinder.communication.QueuedSender;
import net.grinder.console.messages.ReportStatisticsMessage;
import net.grinder.console.messages.WorkerProcessReportMessage;
import net.grinder.statistics.StatisticsServices;
import net.grinder.statistics.StatisticsServicesTestFactory;
import net.grinder.statistics.StatisticsSet;
import net.grinder.statistics.TestStatisticsMap;
import net.grinder.testutility.RandomStubFactory;
import junit.framework.TestCase;


/**
 * Unit tests for {@link TestProcessContextImplementation}.
 *
 * @author Philip Aston
 * @version $Revision:$
 */
public class TestProcessContextImplementation extends TestCase {

  private final RandomStubFactory m_threadContextStubFactory =
    new RandomStubFactory(ThreadContext.class);
  private final ThreadContext m_threadContext =
    (ThreadContext)m_threadContextStubFactory.getStub();

  private final RandomStubFactory m_loggerStubFactory =
    new RandomStubFactory(Logger.class);
  private final Logger m_logger =
    (Logger)m_loggerStubFactory.getStub();

  private final RandomStubFactory m_queuedSenderStubFactory =
    new RandomStubFactory(QueuedSender.class);
  private final QueuedSender m_queuedSender =
    (QueuedSender)m_queuedSenderStubFactory.getStub();

  private final RandomStubFactory m_workerIdentityStubFactory =
    new RandomStubFactory(WorkerIdentity.class);
  private final WorkerIdentity m_workerIdentity =
    (WorkerIdentity)m_workerIdentityStubFactory.getStub();

  public void testAccessors() throws Exception {
    final GrinderProperties properties = new GrinderProperties();

    final StatisticsServices statisticsServices =
      StatisticsServicesTestFactory.createTestInstance();

    final ProcessContext processContext =
      new ProcessContextImplementation(
        m_workerIdentity,
        properties,
        m_logger,
        null,
        m_queuedSender,
        statisticsServices);

    assertSame(statisticsServices, processContext.getStatisticsServices());
    assertSame(m_logger, processContext.getProcessLogger());
    assertSame(m_queuedSender, processContext.getConsoleSender());
    assertSame(properties, processContext.getProperties());

    assertNotNull(processContext.getSleeper());
    assertNotNull(processContext.getTestRegistry());
    assertNotNull(processContext.getScriptContext());

    m_workerIdentityStubFactory.setResult("getName", "test");
    assertEquals("test", processContext.getScriptContext().getProcessName());

    assertEquals(0, processContext.getExecutionStartTime());
    final long t1 = System.currentTimeMillis();
    processContext.setExecutionStartTime();
    final long t2 = System.currentTimeMillis();
    assertTrue(t1 <= processContext.getExecutionStartTime());
    assertTrue(processContext.getExecutionStartTime() <= t2);

    processContext.checkIfShutdown();
    processContext.shutdown();

    try {
      processContext.checkIfShutdown();
      fail("Expected ShutdownException");
    }
    catch (ShutdownException e) {
    }
  }

  public void testThreadContextLocator() throws Exception {
    final ProcessContext processContext =
      new ProcessContextImplementation(
        null,
        new GrinderProperties(),
        null,
        null,
        null,
        StatisticsServicesTestFactory.createTestInstance());

    final ThreadContextLocator threadContextLocator =
      processContext.getThreadContextLocator();
    assertNotNull(threadContextLocator);

    threadContextLocator.set(m_threadContext);
    assertSame(m_threadContext, threadContextLocator.get());

    final Thread t1 = new Thread() {
      public void run() {
        assertNull(threadContextLocator.get());
        threadContextLocator.set(m_threadContext);
        assertSame(m_threadContext, threadContextLocator.get());
        threadContextLocator.set(null);
        assertNull(threadContextLocator.get());
      }
    };
    t1.start();
    t1.join();

    assertSame(m_threadContext, threadContextLocator.get());
  }

  public void testProcessEvents() throws Exception {
    final ProcessContext processContext =
      new ProcessContextImplementation(
        m_workerIdentity,
        new GrinderProperties(),
        m_logger,
        null,
        m_queuedSender,
        StatisticsServicesTestFactory.createTestInstance());

    processContext.fireThreadCreatedEvent(m_threadContext);

    m_threadContextStubFactory.assertSuccess(
      "registerThreadLifeCycleListener", ThreadLifeCycleListener.class);
    m_threadContextStubFactory.assertNoMoreCalls();
  }

  public void testMessageFactories() throws Exception {
    final StatisticsServices statisticsServices =
      StatisticsServicesTestFactory.createTestInstance();

    final GrinderProperties grinderProperties = new GrinderProperties();

    final ProcessContext processContext =
      new ProcessContextImplementation(
        m_workerIdentity,
        grinderProperties,
        m_logger,
        null,
        m_queuedSender,
        statisticsServices);

    final WorkerProcessReportMessage message1 =
      processContext.createStatusMessage(
        WorkerProcessReport.STATE_RUNNING, (short)5, (short)10);

    final WorkerProcessReportMessage message2 =
      processContext.createStatusMessage(
        WorkerProcessReport.STATE_RUNNING, (short)5, (short)10);

    assertNotSame(message1, message2);

    assertEquals(WorkerProcessReport.STATE_RUNNING, message1.getState());
    assertEquals(5, message1.getNumberOfRunningThreads());
    assertEquals(10, message1.getMaximumNumberOfThreads());

    assertSame(m_workerIdentity, message1.getWorkerIdentity());

    final TestStatisticsMap testStatisticsMap = new TestStatisticsMap();
    final StatisticsSet statistics =
      statisticsServices.getStatisticsSetFactory().create();
    final TestStatisticsHelper testStatisticsHelper =
      new TestStatisticsHelperImplementation(
        statisticsServices.getStatisticsIndexMap());

    testStatisticsHelper.recordTest(statistics, 123);
    testStatisticsMap.put(new StubTest(1, ""), statistics);
    final ReportStatisticsMessage message3 =
      processContext.createReportStatisticsMessage(testStatisticsMap);

    final TestStatisticsMap statisticsDelta = message3.getStatisticsDelta();
    assertSame(testStatisticsMap, statisticsDelta);
    assertEquals(123, testStatisticsHelper.getTestTime(
      statisticsDelta.nonCompositeStatisticsTotals()));

    grinderProperties.setBoolean("grinder.reportTimesToConsole", false);

    final ProcessContext processContext2 =
      new ProcessContextImplementation(
        m_workerIdentity,
        grinderProperties,
        m_logger,
        null,
        m_queuedSender,
        statisticsServices);

    final ReportStatisticsMessage message4 =
      processContext2.createReportStatisticsMessage(testStatisticsMap);

    final TestStatisticsMap statisticsDelta2 = message4.getStatisticsDelta();
    assertEquals(0, testStatisticsHelper.getTestTime(
      statisticsDelta2.nonCompositeStatisticsTotals()));
  }
}