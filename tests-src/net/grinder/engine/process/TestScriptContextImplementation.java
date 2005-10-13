// Copyright (C) 2004 Philip Aston
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

import java.util.Arrays;

import net.grinder.common.GrinderProperties;
import net.grinder.common.FilenameFactory;
import net.grinder.common.Logger;
import net.grinder.common.WorkerIdentity;
import net.grinder.communication.QueuedSender;
import net.grinder.console.messages.RegisterStatisticsViewMessage;
import net.grinder.engine.agent.PublicAgentIdentityImplementation;
import net.grinder.plugininterface.PluginThreadContext;
import net.grinder.script.InvalidContextException;
import net.grinder.script.SSLControl;
import net.grinder.script.Statistics;
import net.grinder.statistics.ExpressionView;
import net.grinder.statistics.StatisticsServicesImplementation;
import net.grinder.statistics.StatisticsView;
import net.grinder.testutility.CallData;
import net.grinder.testutility.RandomStubFactory;
import net.grinder.testutility.Time;
import net.grinder.util.Sleeper;


/**
 * Unit test case for <code>ScriptContextImplementation</code>.
 *
 * @author Philip Aston
 * @version $Revision$
 */
public class TestScriptContextImplementation extends TestCase {

  public TestScriptContextImplementation(String name) {
    super(name);
  }

  public void testConstructorAndGetters() throws Exception {

    final GrinderProperties properties = new GrinderProperties();

    final RandomStubFactory queuedSenderStubFactory =
      new RandomStubFactory(QueuedSender.class);
    final QueuedSender queuedSender =
      (QueuedSender)queuedSenderStubFactory.getStub();

    final RandomStubFactory loggerStubFactory =
      new RandomStubFactory(Logger.class);
    final Logger logger = (Logger)loggerStubFactory.getStub();

    final RandomStubFactory filenameFactoryStubFactory =
      new RandomStubFactory(FilenameFactory.class);

    final FilenameFactory filenameFactory =
      (FilenameFactory)filenameFactoryStubFactory.getStub();

    final int threadID = 99;
    final int runNumber = 3;
    final ThreadContextLocator threadContextLocator =
      new StubThreadContextLocator();
    final ThreadContextStubFactory threadContextStubFactory =
      new ThreadContextStubFactory();
    threadContextLocator.set(threadContextStubFactory.getThreadContext());

    final PluginThreadContextStubFactory pluginContextStubFactory =
      new PluginThreadContextStubFactory(threadID, runNumber);
    threadContextStubFactory.setPluginThreadContext(
      pluginContextStubFactory.getPluginThreadContext());

    final RandomStubFactory statisticsStubFactory =
      new RandomStubFactory(Statistics.class);
    final Statistics statistics = (Statistics)statisticsStubFactory.getStub();
    threadContextStubFactory.setScriptStatistics(statistics);

    final Sleeper sleeper = new Sleeper(1, 0, logger);

    final RandomStubFactory sslControlStubFactory =
      new RandomStubFactory(SSLControl.class);
    final SSLControl sslControl = (SSLControl)sslControlStubFactory.getStub();

    final PublicAgentIdentityImplementation agentIdentity =
      new PublicAgentIdentityImplementation("Agent");
    final WorkerIdentity workerIdentity =
      agentIdentity.createWorkerIdentity();

    final ScriptContextImplementation scriptContext =
      new ScriptContextImplementation(
        workerIdentity, threadContextLocator, properties, queuedSender, logger,
        filenameFactory, sleeper, sslControl,
        StatisticsServicesImplementation.getInstance());

    assertEquals(workerIdentity.getName(), scriptContext.getProcessName());
    assertEquals(threadID, scriptContext.getThreadID());
    assertEquals(runNumber, scriptContext.getRunNumber());
    assertEquals(logger, scriptContext.getLogger());
    assertEquals(filenameFactory, scriptContext.getFilenameFactory());
    assertEquals(properties, scriptContext.getProperties());
    assertEquals(statistics, scriptContext.getStatistics());
    assertEquals(sslControl, scriptContext.getSSLControl());

    threadContextLocator.set(null);
    assertEquals(-1, scriptContext.getThreadID());
    assertEquals(-1, scriptContext.getRunNumber());

    try {
      scriptContext.getStatistics();
      fail("Expected InvalidContextException");
    }
    catch (InvalidContextException e) {
    }
  }

  public void testSleep() throws Exception {

    final RandomStubFactory loggerStubFactory =
      new RandomStubFactory(Logger.class);
    final Logger logger = (Logger)loggerStubFactory.getStub();

    final Sleeper sleeper = new Sleeper(1, 0, logger);

    final ScriptContextImplementation scriptContext =
      new ScriptContextImplementation(null, null, null, null, null, null,
                                      sleeper, null, null);

    assertTrue(
      new Time(50, 70) {
        public void doIt() throws Exception  { scriptContext.sleep(50); }
      }.run());

    assertTrue(
      new Time(40, 70) {
        public void doIt() throws Exception  { scriptContext.sleep(50, 5); }
      }.run());
  }

  public void testRegisterStatisticsViews() throws Exception {

    final RandomStubFactory queuedSenderStubFactory =
      new RandomStubFactory(QueuedSender.class);
    final QueuedSender queuedSender =
      (QueuedSender)queuedSenderStubFactory.getStub();

    final ThreadContextLocator threadContextLocator =
      new StubThreadContextLocator();
    final ThreadContextStubFactory threadContextStubFactory =
      new ThreadContextStubFactory();
    threadContextLocator.set(threadContextStubFactory.getThreadContext());

    final ScriptContextImplementation scriptContext =
      new ScriptContextImplementation(
        null, threadContextLocator, null,
        queuedSender, null, null, null, null,
        StatisticsServicesImplementation.getInstance());

    final ExpressionView expressionView =
      new ExpressionView("display", "resource key", "errors");
    final StatisticsView statisticsView = new StatisticsView();
    statisticsView.add(expressionView);
    scriptContext.registerSummaryStatisticsView(statisticsView);

    final CallData callData = queuedSenderStubFactory.getCallData();
    assertEquals("queue", callData.getMethodName());
    assertEquals(null, callData.getResult());
    final Object[] parameters = callData.getParameters();
    assertEquals(1, parameters.length);
    final RegisterStatisticsViewMessage message =
      (RegisterStatisticsViewMessage)parameters[0];
    assertEquals(statisticsView, message.getStatisticsView());
    queuedSenderStubFactory.assertNoMoreCalls();

    final StatisticsView summaryStatisticsView =
      StatisticsServicesImplementation.getInstance().getSummaryStatisticsView();

    final ExpressionView[] summaryExpressionViews =
      summaryStatisticsView.getExpressionViews();
    assertTrue(Arrays.asList(summaryExpressionViews).contains(expressionView));

    try {
      scriptContext.registerDetailStatisticsView(statisticsView);
      fail("Expected InvalidContextException");
    }
    catch (InvalidContextException e) {
    }

    threadContextLocator.set(null);

    scriptContext.registerDetailStatisticsView(statisticsView);

    final StatisticsView detailStatisticsView =
      StatisticsServicesImplementation.getInstance().getDetailStatisticsView();

    final ExpressionView[] detailExpressionViews =
      detailStatisticsView.getExpressionViews();
    assertTrue(Arrays.asList(detailExpressionViews).contains(expressionView));
  }

  /**
   * Must be public so that override_ methods can be called
   * externally.
   */
  public static class ThreadContextStubFactory extends RandomStubFactory {

    private PluginThreadContext m_pluginThreadContext;
    private Statistics m_scriptStatistics;

    ThreadContextStubFactory() {
      super(ThreadContext.class);
    }

    final ThreadContext getThreadContext() {
      return (ThreadContext)getStub();
    }

    public final PluginThreadContext override_getPluginThreadContext(
      Object proxy) {
      return m_pluginThreadContext;
    }

    final void setPluginThreadContext(
      PluginThreadContext pluginThreadContext) {
      m_pluginThreadContext = pluginThreadContext;
    }

    public final Statistics override_getScriptStatistics(Object proxy) {
      return m_scriptStatistics;
    }

    final void setScriptStatistics(Statistics scriptStatistics) {
      m_scriptStatistics = scriptStatistics;
    }
  }

  public static class PluginThreadContextStubFactory
    extends RandomStubFactory {

    private final int m_threadID;
    private final int m_runNumber;

    public PluginThreadContextStubFactory(int threadID, int runNumber) {
      super(PluginThreadContext.class);
      m_threadID = threadID;
      m_runNumber = runNumber;
    }

    public final PluginThreadContext getPluginThreadContext() {
      return (PluginThreadContext)getStub();
    }

    public int override_getThreadID(Object proxy) {
      return m_threadID;
    }

    public int override_getRunNumber(Object proxy) {
      return m_runNumber;
    }
  }
}
