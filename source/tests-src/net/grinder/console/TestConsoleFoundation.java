// Copyright (C) 2008 - 2011 Philip Aston
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

package net.grinder.console;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.io.File;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Pattern;

import net.grinder.common.Logger;
import net.grinder.common.LoggerStubFactory;
import net.grinder.common.processidentity.ProcessReport;
import net.grinder.common.processidentity.WorkerIdentity;
import net.grinder.common.processidentity.WorkerProcessReport;
import net.grinder.communication.CommunicationException;
import net.grinder.communication.Message;
import net.grinder.communication.MessageDispatchRegistry;
import net.grinder.communication.MessageDispatchRegistry.Handler;
import net.grinder.console.ConsoleFoundation.ConsoleBarrierGroups;
import net.grinder.console.ConsoleFoundation.UI;
import net.grinder.console.client.ConsoleConnection;
import net.grinder.console.client.ConsoleConnectionException;
import net.grinder.console.client.ConsoleConnectionFactory;
import net.grinder.console.common.ErrorHandler;
import net.grinder.console.common.Resources;
import net.grinder.console.common.StubResources;
import net.grinder.console.common.processidentity.StubWorkerProcessReport;
import net.grinder.console.communication.ConsoleCommunication;
import net.grinder.console.communication.DistributionControl;
import net.grinder.console.communication.ProcessControl;
import net.grinder.console.communication.StubProcessReports;
import net.grinder.console.communication.ProcessControl.Listener;
import net.grinder.console.communication.ProcessControl.ProcessReports;
import net.grinder.console.communication.server.DispatchClientCommands;
import net.grinder.console.distribution.FileDistribution;
import net.grinder.console.model.ConsoleProperties;
import net.grinder.console.model.SampleModel;
import net.grinder.console.model.SampleModelViews;
import net.grinder.engine.agent.StubAgentIdentity;
import net.grinder.messages.console.RegisterExpressionViewMessage;
import net.grinder.messages.console.RegisterTestsMessage;
import net.grinder.messages.console.ReportStatisticsMessage;
import net.grinder.messages.console.WorkerAddress;
import net.grinder.statistics.ExpressionView;
import net.grinder.statistics.StatisticsServices;
import net.grinder.statistics.StatisticsServicesImplementation;
import net.grinder.statistics.TestStatisticsMap;
import net.grinder.synchronisation.BarrierGroup;
import net.grinder.synchronisation.BarrierGroups;
import net.grinder.synchronisation.messages.AddBarrierMessage;
import net.grinder.synchronisation.messages.AddWaiterMessage;
import net.grinder.synchronisation.messages.BarrierIdentity;
import net.grinder.synchronisation.messages.CancelWaiterMessage;
import net.grinder.synchronisation.messages.OpenBarrierMessage;
import net.grinder.synchronisation.messages.RemoveBarriersMessage;
import net.grinder.testutility.AbstractJUnit4FileTestCase;
import net.grinder.testutility.StubTimer;
import net.grinder.util.Directory;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;


/**
 * Unit tests for {@link ConsoleFoundation}.
 *
 * @author Philip Aston
 */
public class TestConsoleFoundation extends AbstractJUnit4FileTestCase {

  private static TestConsoleFoundation s_instance;

  private static void setInstance(TestConsoleFoundation instance) {
    s_instance = instance;
  }

  @Mock private MessageDispatchRegistry m_messageDispatchRegistry;
  @Mock private ConsoleCommunication m_consoleCommunication;
  @Mock private ProcessControl m_processControl;
  @Mock private BarrierGroups m_barrierGroups;
  @Mock private BarrierIdentity m_barrierIdentity;

  @Captor private ArgumentCaptor<Message> m_messageCaptor;
  @Captor private ArgumentCaptor<Handler<Message>> m_handlerCaptor;
  @Captor private ArgumentCaptor<Listener> m_processStatusListenerCaptor;

  @Before public void setUp() {
    MockitoAnnotations.initMocks(this);

    when(m_consoleCommunication.getMessageDispatchRegistry())
      .thenReturn(m_messageDispatchRegistry);
  }

  private final Resources m_resources =
    new StubResources<Object>(new HashMap<String, Object>() {{
    }});

  private final LoggerStubFactory m_loggerStubFactory = new LoggerStubFactory();
  private final Logger m_logger = m_loggerStubFactory.getLogger();

  @Test public void testConstruction() throws Exception {

    final ConsoleFoundation foundation =
      new ConsoleFoundation(m_resources, m_logger);

    setInstance(this);

    final MyUI ui = (MyUI)foundation.createUI(MyUI.class);
    assertNotNull(ui);

    final Thread runConsole = new Thread(new Runnable() {
      public void run() {
        try {
          foundation.run();
        }
        catch (Exception e) {
          // Ignore.
        }
      }
    });

    runConsole.start();

    foundation.shutdown();

    m_loggerStubFactory.assertNoMoreCalls();
  }

  @Test public void testSimpleRun() throws Exception {

    final Timer timer = new Timer(true);

    final ConsoleProperties consoleProperties =
      new ConsoleProperties(m_resources, new File(getDirectory(), "props"));

    // Figure out a free local port.

    final ServerSocket serverSocket =
      new ServerSocket(0, 50, InetAddress.getLocalHost());
    final String hostName = serverSocket.getInetAddress().getHostName();
    final int port = serverSocket.getLocalPort();
    serverSocket.close();

    consoleProperties.setConsoleHost(hostName);
    consoleProperties.setConsolePort(port);

    final ConsoleFoundation foundation =
      new ConsoleFoundation(m_resources, m_logger, timer, consoleProperties);

    m_loggerStubFactory.assertNoMoreCalls();

    final Thread runConsole = new Thread(new Runnable() {
      public void run() { foundation.run(); }
    });

    runConsole.start();

    final ConsoleConnectionFactory ccf = new ConsoleConnectionFactory();

    final int retries = 3;

    for (int i = 0; i < retries; ++i) {
      try {
    	final ConsoleConnection client = ccf.connect(hostName, port);
    	assertEquals(0, client.getNumberOfAgents());
    	client.close();
      }
      catch (ConsoleConnectionException e) {
    	if (i == retries - 1) {
    	  throw e;
    	}

    	Thread.sleep(50);
      }
    }

    foundation.shutdown();

    runConsole.join();
  }

  @Test public void testWireFileDistribution() throws Exception {

    final FileDistribution fileDistribution = mock(FileDistribution.class);

    final ConsoleProperties consoleProperties =
      new ConsoleProperties(m_resources, new File(getDirectory(), "props"));

    final StubTimer timer = new StubTimer();

    new ConsoleFoundation.WireFileDistribution(fileDistribution,
                                               consoleProperties,
                                               timer);

    assertEquals(6000, timer.getLastDelay());
    assertEquals(6000, timer.getLastPeriod());

    final TimerTask scanFileTask = timer.getLastScheduledTimerTask();
    scanFileTask.run();
    verify(fileDistribution).scanDistributionFiles();

    consoleProperties.setDistributionFileFilterExpression(".*");

    final ArgumentCaptor<Pattern> patternCaptor =
      ArgumentCaptor.forClass(Pattern.class);

    verify(fileDistribution).setFileFilterPattern(patternCaptor.capture());
    assertEquals(".*", patternCaptor.getValue().pattern());

    final ArgumentCaptor<Directory> directoryCaptor =
      ArgumentCaptor.forClass(Directory.class);

    final Directory directory = new Directory(new File(getDirectory(), "foo"));
    consoleProperties.setAndSaveDistributionDirectory(directory);

    verify(fileDistribution).setDirectory(directoryCaptor.capture());
    assertSame(directory, directoryCaptor.getValue());

    consoleProperties.setConsolePort(999);

    verifyNoMoreInteractions(fileDistribution);
  }

  @Test public void testWireMessageDispatch() throws Exception {

    final SampleModel sampleModel = mock(SampleModel.class);

    final SampleModelViews sampleModelViews = mock(SampleModelViews.class);

    final DispatchClientCommands dispatchClientCommands =
      new DispatchClientCommands(null, null, null);

    new ConsoleFoundation.WireMessageDispatch(m_consoleCommunication,
                                              sampleModel,
                                              sampleModelViews,
                                              dispatchClientCommands);

    verify(m_messageDispatchRegistry).set(eq(RegisterTestsMessage.class),
                                          m_handlerCaptor.capture());

    final Collection<net.grinder.common.Test> tests = Collections.emptySet();
    m_handlerCaptor.getValue().handle(new RegisterTestsMessage(tests));

    verify(sampleModel).registerTests(tests);

    verify(m_messageDispatchRegistry).set(eq(ReportStatisticsMessage.class),
                                          m_handlerCaptor.capture());

    final TestStatisticsMap delta = new TestStatisticsMap();
    m_handlerCaptor.getValue().handle(new ReportStatisticsMessage(delta));

    verify(sampleModel).addTestReport(delta);

    verify(m_messageDispatchRegistry).set(
      eq(RegisterExpressionViewMessage.class), m_handlerCaptor.capture());

    final ExpressionView expressionView =
      StatisticsServicesImplementation.getInstance()
      .getStatisticExpressionFactory().createExpressionView(
        "blah", "userLong0", false);
    m_handlerCaptor.getValue().handle(
      new RegisterExpressionViewMessage(expressionView));

    verify(sampleModelViews).registerStatisticExpression(expressionView);

    verifyNoMoreInteractions(sampleModel,
                             sampleModelViews);
  }

  public static class MyUI implements UI {
    public MyUI(Logger logger,
                Resources resources,
                ConsoleProperties properties,
                StatisticsServices statisticsServices,
                SampleModel model,
                ConsoleCommunication consoleCommunication,
                DistributionControl distributionControl,
                FileDistribution fileDistribution,
                ProcessControl processControl) {
      assertSame(s_instance.m_logger, logger);
      assertSame(s_instance.m_resources, resources);
      assertNotNull(properties);
      assertSame(StatisticsServicesImplementation.getInstance(),
                 statisticsServices);
      assertNotNull(model);
      assertNotNull(consoleCommunication);
      assertNotNull(distributionControl);
      assertNotNull(fileDistribution);
      assertNotNull(processControl);
    }

    public ErrorHandler getErrorHandler() {
      return null;
    }
  }

  @Test public void testBarrierMessageHandlers() throws Exception {

    final BarrierGroup barrierGroup = mock(BarrierGroup.class);

    when(m_barrierGroups.getGroup("hello")).thenReturn(barrierGroup);

    new ConsoleFoundation.WireDistributedBarriers(m_consoleCommunication,
                                                  m_barrierGroups,
                                                  m_processControl);

    // Add barrier.
    verify(m_messageDispatchRegistry).set(eq(AddBarrierMessage.class),
                                          m_handlerCaptor.capture());

    m_handlerCaptor.getValue().handle(new AddBarrierMessage("hello"));

    verify(barrierGroup).addBarrier();

    // Add waiter.
    verify(m_messageDispatchRegistry).set(eq(AddWaiterMessage.class),
                                          m_handlerCaptor.capture());

    m_handlerCaptor.getValue().handle(new AddWaiterMessage("hello",
                                                           m_barrierIdentity));
    verify(barrierGroup).addWaiter(m_barrierIdentity);

    // Cancel waiter.
    verify(m_messageDispatchRegistry).set(eq(CancelWaiterMessage.class),
                                          m_handlerCaptor.capture());

    m_handlerCaptor.getValue().handle(
      new CancelWaiterMessage("hello", m_barrierIdentity));

    verify(barrierGroup).cancelWaiter(m_barrierIdentity);

    // Remove barriers.
    verify(m_messageDispatchRegistry).set(eq(RemoveBarriersMessage.class),
                                          m_handlerCaptor.capture());

    m_handlerCaptor.getValue().handle(new RemoveBarriersMessage("hello", 1));

    verify(barrierGroup).removeBarriers(1);
    verifyNoMoreInteractions(barrierGroup);
  }

  @Test public void testBarriersCleanUp() throws Exception {
    final BarrierGroup group1 = mock(BarrierGroup.class);
    final BarrierGroup group2 = mock(BarrierGroup.class);

    when(m_barrierGroups.getGroup("g1")).thenReturn(group1);
    when(m_barrierGroups.getGroup("g2")).thenReturn(group2);

    new ConsoleFoundation.WireDistributedBarriers(m_consoleCommunication,
                                                  m_barrierGroups,
                                                  m_processControl);

    verify(m_messageDispatchRegistry).set(eq(AddBarrierMessage.class),
                                          m_handlerCaptor.capture());

    final Handler<Message> addBarrierHandler = m_handlerCaptor.getValue();

    verify(m_messageDispatchRegistry).set(eq(AddWaiterMessage.class),
                                          m_handlerCaptor.capture());

    final Handler<Message> addWaiterHandler = m_handlerCaptor.getValue();


    // Create a couple of barrier groups.
    final StubAgentIdentity agent = new StubAgentIdentity("agent");
    final WorkerIdentity worker1 = agent.createWorkerIdentity();
    final WorkerIdentity worker2 = agent.createWorkerIdentity();

    final AddBarrierMessage message1 = new AddBarrierMessage("g1");
    message1.setAddress(new WorkerAddress(worker1));
    addBarrierHandler.handle(message1);
    addBarrierHandler.handle(message1);

    final AddBarrierMessage message2 = new AddBarrierMessage("g2");
    message2.setAddress(new WorkerAddress(worker2));
    addBarrierHandler.handle(message2);

    final AddBarrierMessage message3 = new AddBarrierMessage("g1");
    message3.setAddress(new WorkerAddress(worker2));
    addBarrierHandler.handle(message3);

    final AddWaiterMessage message4 = new AddWaiterMessage("g1",
                                                           m_barrierIdentity);
    message4.setAddress(new WorkerAddress(worker2));
    addWaiterHandler.handle(message4);

    verify(group1, times(3)).addBarrier();
    verify(group1).addWaiter(m_barrierIdentity);

    verify(group2).addBarrier();
    verifyNoMoreInteractions(group1, group2);

    verify(m_processControl).addProcessStatusListener(
      m_processStatusListenerCaptor.capture());

    final Listener listener = m_processStatusListenerCaptor.getValue();

    // Worker 1 has gone away.
    listener.update(new ProcessReports[] {
      new StubProcessReports(null,
        new WorkerProcessReport[] {
          new StubWorkerProcessReport(worker2,
                                      ProcessReport.STATE_RUNNING, 1, 1)
        }),
    });

    verify(group1).removeBarriers(2);
    verifyNoMoreInteractions(group1);

    // All workers have gone away.
    listener.update(new ProcessReports[0]);

    verify(group1).cancelWaiter(m_barrierIdentity);
    verify(group1).removeBarriers(1);

    verify(group2).removeBarriers(1);

    verifyNoMoreInteractions(group1, group2);
  }

  @Test public void testBarriersCleanUpAssertion() throws Exception {

    final CommunicationException exception = new CommunicationException("");

    final BarrierGroup group1 = mock(BarrierGroup.class);
    doThrow(exception).when(group1).removeBarriers(1);

    when(m_barrierGroups.getGroup("g1")).thenReturn(group1);

    new ConsoleFoundation.WireDistributedBarriers(m_consoleCommunication,
                                                  m_barrierGroups,
                                                  m_processControl);

    verify(m_messageDispatchRegistry).set(eq(AddBarrierMessage.class),
                                          m_handlerCaptor.capture());

    final Handler<Message> addBarrierHandler = m_handlerCaptor.getValue();

    verify(m_messageDispatchRegistry).set(eq(AddWaiterMessage.class),
                                          m_handlerCaptor.capture());


    final AddBarrierMessage message1 = new AddBarrierMessage("g1");
    addBarrierHandler.handle(message1);

    verify(m_processControl)
      .addProcessStatusListener(m_processStatusListenerCaptor.capture());

    final Listener listener = m_processStatusListenerCaptor.getValue();

    try {
      listener.update(new ProcessReports[0]);
      fail("Expected AssertionError");
    }
    catch (AssertionError e) {
      assertSame(exception, e.getCause());
    }
  }

  @Test public void testConsoleBarrierGroups() throws Exception {

    final ConsoleBarrierGroups barrierGroups =
      new ConsoleBarrierGroups(m_consoleCommunication);

    final BarrierGroup bg = barrierGroups.createBarrierGroup("foo");

    bg.addBarrier();
    verifyNoMoreInteractions(m_consoleCommunication);

    bg.addWaiter(mock(BarrierIdentity.class));
    verify(m_consoleCommunication).sendToAgents(m_messageCaptor.capture());

    assertEquals("foo",
                 ((OpenBarrierMessage)m_messageCaptor.getValue()).getName());
  }
}
