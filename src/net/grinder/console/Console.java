// Copyright (C) 2000 Paco Gomez
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

package net.grinder.console;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.Timer;
import java.util.TimerTask;

import net.grinder.common.GrinderException;
import net.grinder.communication.Message;
import net.grinder.communication.HandlerChainSender.MessageHandler;
import net.grinder.console.common.Resources;
import net.grinder.console.common.ResourcesImplementation;
import net.grinder.console.communication.ConsoleCommunication;
import net.grinder.console.communication.ConsoleCommunicationImplementation;
import net.grinder.console.communication.ProcessStatus;
import net.grinder.console.distribution.FileDistribution;
import net.grinder.console.distribution.FileDistributionImplementation;
import net.grinder.console.messages.RegisterStatisticsViewMessage;
import net.grinder.console.messages.RegisterTestsMessage;
import net.grinder.console.messages.ReportStatisticsMessage;
import net.grinder.console.model.ConsoleProperties;
import net.grinder.console.model.Model;
import net.grinder.console.swingui.ConsoleUI;
import net.grinder.statistics.StatisticsServicesImplementation;
import net.grinder.statistics.StatisticsView;


/**
 * This is the entry point of The Grinder console.
 *
 * @author Paco Gomez
 * @author Philip Aston
 * @version $Revision$
 **/
public class Console {

  private final ConsoleCommunication m_communication;
  private final Model m_model;
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

    m_model =
      new Model(properties, resources,
                StatisticsServicesImplementation.getInstance());

    final Timer timer = new Timer(true);

    m_communication =
      new ConsoleCommunicationImplementation(resources, properties, timer);

    final FileDistribution fileDistribution =
      new FileDistributionImplementation(
        m_communication.getDistributionControl());

    timer.schedule(new TimerTask() {
        public void run() {
          fileDistribution.scanDistributionFiles(
            properties.getDistributionDirectory());
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
      new ConsoleUI(m_model,
                    m_communication.getProcessControl(),
                    fileDistribution);

    m_communication.setErrorHandler(m_userInterface.getErrorHandler());

    m_communication.addMessageHandler(
      new MessageHandler() {
        public boolean process(Message message) {
          if (message instanceof RegisterTestsMessage) {
            m_model.registerTests(((RegisterTestsMessage)message).getTests());
            return true;
          }

          if (message instanceof ReportStatisticsMessage) {
            m_model.addTestReport(
              ((ReportStatisticsMessage)message).getStatisticsDelta());
            return true;
          }

          if (message instanceof RegisterStatisticsViewMessage) {
            final StatisticsView statisticsView =
              ((RegisterStatisticsViewMessage)message).getStatisticsView();

            m_model.registerStatisticsViews(statisticsView, statisticsView);
            return true;
          }

          return false;
        }

        public void shutdown() {
        }
      });
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
