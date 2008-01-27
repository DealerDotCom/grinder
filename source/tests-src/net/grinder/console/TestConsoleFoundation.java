// Copyright (C) 2008 Philip Aston
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

import java.io.File;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.HashMap;
import java.util.Timer;

import net.grinder.common.Logger;
import net.grinder.common.LoggerStubFactory;
import net.grinder.console.ConsoleFoundation.UI;
import net.grinder.console.client.ConsoleConnection;
import net.grinder.console.client.ConsoleConnectionFactory;
import net.grinder.console.common.ErrorHandler;
import net.grinder.console.common.Resources;
import net.grinder.console.common.StubResources;
import net.grinder.console.communication.ConsoleCommunication;
import net.grinder.console.communication.DistributionControl;
import net.grinder.console.communication.ProcessControl;
import net.grinder.console.distribution.FileDistribution;
import net.grinder.console.model.ConsoleProperties;
import net.grinder.console.model.Model;
import net.grinder.statistics.StatisticsServices;
import net.grinder.statistics.StatisticsServicesImplementation;
import net.grinder.testutility.AbstractFileTestCase;


/**
 * Unit tests for {@link ConsoleFoundation}.
 *
 * @author Philip Aston
 * @version $Revision:$
 */
public class TestConsoleFoundation extends AbstractFileTestCase {

  private static TestConsoleFoundation s_instance;

  private final Resources m_resources =
    new StubResources(new HashMap() {{

    }});

  private final LoggerStubFactory m_loggerStubFactory = new LoggerStubFactory();
  private final Logger m_logger = m_loggerStubFactory.getLogger();

  public void testConstruction() throws Exception {

    final ConsoleFoundation foundation =
      new ConsoleFoundation(m_resources, m_logger);

    s_instance = this;

    final MyUI ui = (MyUI)foundation.createUI(MyUI.class);
    assertNotNull(ui);

    final Thread runConsole = new Thread(new Runnable() {
      public void run() { foundation.run(); }
    });

    runConsole.start();

    foundation.shutdown();

    m_loggerStubFactory.assertNoMoreCalls();
  }

  public void XtestSimpleRun() throws Exception {

    final Timer timer = new Timer(true);

    final ConsoleProperties consoleProperties =
      new ConsoleProperties(m_resources, new File(getDirectory(), "props"));

    final ConsoleFoundation foundation =
      new ConsoleFoundation(m_resources, m_logger, timer, consoleProperties);

    m_loggerStubFactory.assertNoMoreCalls();

    final Thread runConsole = new Thread(new Runnable() {
      public void run() { foundation.run(); }
    });

    // Figure out a free local port.

    final ServerSocket serverSocket =
      new ServerSocket(0, 50, InetAddress.getLocalHost());
    final String hostName = serverSocket.getInetAddress().getHostName();
    final int port = serverSocket.getLocalPort();
    serverSocket.close();
    consoleProperties.setConsoleHost(hostName);
    consoleProperties.setConsolePort(port);

    runConsole.start();

    final ConsoleConnectionFactory ccf = new ConsoleConnectionFactory();
    final ConsoleConnection client = ccf.connect(hostName, port);

    assertEquals(0, client.getNumberOfAgents());
    client.close();

    foundation.shutdown();

    runConsole.join();
  }

  public static class MyUI implements UI {
    public MyUI(Logger logger,
                Resources resources,
                ConsoleProperties properties,
                StatisticsServices statisticsServices,
                Model model,
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
