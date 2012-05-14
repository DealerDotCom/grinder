// Copyright (C) 2008 - 2012 Philip Aston
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
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
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

import net.grinder.communication.Message;
import net.grinder.communication.MessageDispatchRegistry;
import net.grinder.communication.MessageDispatchRegistry.Handler;
import net.grinder.console.ConsoleFoundation.UI;
import net.grinder.console.client.ConsoleConnection;
import net.grinder.console.client.ConsoleConnectionException;
import net.grinder.console.client.ConsoleConnectionFactory;
import net.grinder.console.common.ErrorHandler;
import net.grinder.console.common.Resources;
import net.grinder.console.common.StubResources;
import net.grinder.console.communication.ConsoleCommunication;
import net.grinder.console.communication.DistributionControl;
import net.grinder.console.communication.ProcessControl;
import net.grinder.console.communication.server.DispatchClientCommands;
import net.grinder.console.distribution.FileDistribution;
import net.grinder.console.model.ConsoleProperties;
import net.grinder.console.model.SampleModel;
import net.grinder.console.model.SampleModelViews;
import net.grinder.messages.console.RegisterExpressionViewMessage;
import net.grinder.messages.console.RegisterTestsMessage;
import net.grinder.messages.console.ReportStatisticsMessage;
import net.grinder.statistics.ExpressionView;
import net.grinder.statistics.StatisticsServices;
import net.grinder.statistics.StatisticsServicesImplementation;
import net.grinder.statistics.TestStatisticsMap;
import net.grinder.testutility.AbstractJUnit4FileTestCase;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;


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
  @Mock private Logger m_logger;

  @Captor private ArgumentCaptor<Handler<Message>> m_handlerCaptor;

  @Before public void setUp() {
    MockitoAnnotations.initMocks(this);

    when(m_consoleCommunication.getMessageDispatchRegistry())
      .thenReturn(m_messageDispatchRegistry);
  }

  private final Resources m_resources =
    new StubResources<Object>(new HashMap<String, Object>() {{
    }});

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

    connectToConsole("localhost", 6372);

    foundation.shutdown();

    verifyNoMoreInteractions(m_logger);
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

    verifyNoMoreInteractions(m_logger);

    final Thread runConsole = new Thread(new Runnable() {
      public void run() { foundation.run(); }
    });

    runConsole.start();

    connectToConsole(hostName, port);

    foundation.shutdown();

    runConsole.join();

    verifyNoMoreInteractions(m_logger);
  }

  private static void connectToConsole(String hostName, int port)
      throws Exception {

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
}
