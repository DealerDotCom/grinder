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
// REGENTS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
// INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
// (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
// SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
// HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
// STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
// ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
// OF THE POSSIBILITY OF SUCH DAMAGE.

package net.grinder.console;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.Timer;
import java.util.TimerTask;

import net.grinder.common.GrinderException;
import net.grinder.communication.Message;
import net.grinder.communication.MessageDispatchRegistry;
import net.grinder.communication.MessageDispatchRegistry.AbstractHandler;
import net.grinder.console.common.Resources;
import net.grinder.console.common.ResourcesImplementation;
import net.grinder.console.communication.ConsoleCommunication;
import net.grinder.console.communication.ConsoleCommunicationImplementation;
import net.grinder.console.communication.ProcessStatus;
import net.grinder.console.communication.server.DispatchClientCommands;
import net.grinder.console.distribution.FileDistribution;
import net.grinder.console.distribution.FileDistributionImplementation;
import net.grinder.console.messages.RegisterExpressionViewMessage;
import net.grinder.console.messages.RegisterTestsMessage;
import net.grinder.console.messages.ReportStatisticsMessage;
import net.grinder.console.model.ConsoleProperties;
import net.grinder.console.model.Model;
import net.grinder.console.swingui.ConsoleUI;
import net.grinder.statistics.StatisticsServicesImplementation;


/**
 * This is the entry point of The Grinder console.
 *
 * @author Paco Gomez
 * @author Philip Aston
 * @version $Revision$
 **/
public class Console {

  private final ConsoleCommunication m_communication;
  private final ConsoleUI m_userInterface;

  /**
   * Constructor.
   *
   * @exception GrinderException If an error occurs.
   */
  public Console() throws GrinderException {

    // Some platforms do not have user home directories.
    final String homeDirectory =
      System.getProperty("user.home", System.getProperty("java.home"));

    final Resources resources =
      new ResourcesImplementation(
        "net.grinder.console.swingui.resources.Console");

    final ConsoleProperties properties =
      new ConsoleProperties(resources,
                            new File(homeDirectory, ".grinder_console"));

    final Model model =
      new Model(properties, StatisticsServicesImplementation.getInstance());

    final Timer timer = new Timer(true);

    m_communication =
      new ConsoleCommunicationImplementation(resources, properties, timer, 500);

    final FileDistribution fileDistribution =
      new FileDistributionImplementation(
        m_communication.getDistributionControl());

    timer.schedule(new TimerTask() {
        public void run() {
          fileDistribution.scanDistributionFiles(
            properties.getDistributionDirectory(),
            properties.getDistributionFileFilterPattern());
        }
      },
      properties.getScanDistributionFilesPeriod(),
      properties.getScanDistributionFilesPeriod());

    m_communication.getProcessControl().addProcessStatusListener(
      new ProcessStatus.Listener() {
        public void update(ProcessStatus.ProcessReports[] processStatuses,
                           boolean newAgent) {
          if (newAgent) {
            fileDistribution.getAgentCacheState().setOutOfDate();
          }
        }
      });

    properties.addPropertyChangeListener(
      new PropertyChangeListener() {
        public void propertyChange(PropertyChangeEvent e) {
          final String propertyName = e.getPropertyName();

          if (propertyName.equals(
                ConsoleProperties.DISTRIBUTION_DIRECTORY_PROPERTY) ||
              propertyName.equals(
                ConsoleProperties.
                DISTRIBUTION_FILE_FILTER_EXPRESSION_PROPERTY)) {
            fileDistribution.getAgentCacheState().setOutOfDate();
          }
        }
      });

    m_userInterface =
      new ConsoleUI(model,
                    m_communication.getProcessControl(),
                    fileDistribution,
                    resources);

    m_communication.setErrorHandler(m_userInterface.getErrorHandler());

    final MessageDispatchRegistry messageDispatchRegistry =
      m_communication.getMessageDispatchRegistry();

    messageDispatchRegistry.set(
      RegisterTestsMessage.class,
      new AbstractHandler() {
        public void send(Message message) {
          model.registerTests(((RegisterTestsMessage)message).getTests());
        }
      });

    messageDispatchRegistry.set(
      ReportStatisticsMessage.class,
      new AbstractHandler() {
        public void send(Message message) {
          model.addTestReport(
            ((ReportStatisticsMessage)message).getStatisticsDelta());
        }
      });

    messageDispatchRegistry.set(
      RegisterExpressionViewMessage.class,
      new AbstractHandler() {
        public void send(Message message) {
          model.registerStatisticExpression(
            ((RegisterExpressionViewMessage)message).getExpressionView());
        }
      });

    final DispatchClientCommands dispatchClientCommands =
      new DispatchClientCommands(model);
    dispatchClientCommands.registerMessageHandlers(messageDispatchRegistry);
  }

  /**
   * Console message event loop. Dispatches communication messages
   * appropriately.
   */
  public void run() {
    while (true) {
      m_communication.processOneMessage();
    }
  }
}
