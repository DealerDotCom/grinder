// Copyright (C) 2000 Paco Gomez
// Copyright (C) 2000, 2001, 2002, 2003, 2004 Philip Aston
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

import java.io.File;

import net.grinder.common.GrinderException;
import net.grinder.communication.Message;
import net.grinder.console.common.Resources;
import net.grinder.console.communication.ConsoleCommunication;
import net.grinder.console.communication.ProcessControl;
import net.grinder.console.messages.RegisterStatisticsViewMessage;
import net.grinder.console.messages.RegisterTestsMessage;
import net.grinder.console.messages.ReportStatisticsMessage;
import net.grinder.console.messages.ReportStatusMessage;
import net.grinder.console.model.ConsoleProperties;
import net.grinder.console.model.Model;
import net.grinder.console.swingui.ConsoleUI;
import net.grinder.engine.messages.DistributeFilesMessage;
import net.grinder.engine.messages.ResetGrinderMessage;
import net.grinder.engine.messages.StartGrinderMessage;
import net.grinder.engine.messages.StopGrinderMessage;
import net.grinder.statistics.StatisticsView;
import net.grinder.util.FileContents;


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

    String homeDirectory = System.getProperty("user.home");

    if (homeDirectory == null) {
      // Some platforms do not have user home directories.
      homeDirectory = System.getProperty("java.home");
    }

    final Resources resources =
      new Resources("net.grinder.console.swingui.resources.Console");

    final ConsoleProperties properties =
      new ConsoleProperties(resources,
                            new File(homeDirectory, ".grinder_console"));

    m_model = new Model(properties, resources);

    final ProcessControl processControl = new ProcessControl() {
        public void startWorkerProcesses() {
          m_model.getProcessStatusSet().processEvent();
          m_communication.send(new StartGrinderMessage(null));
        }

        public void resetWorkerProcesses() {
          m_model.getProcessStatusSet().processEvent();
          m_communication.send(new ResetGrinderMessage());
        }

        public void stopWorkerProcesses() {
          m_model.getProcessStatusSet().processEvent();
          m_communication.send(new StopGrinderMessage());
        }

        public void distributeFiles(FileContents[] files) {
          m_communication.send(new DistributeFilesMessage(files));
        }
      };

    m_userInterface = new ConsoleUI(m_model, processControl);

    m_communication =
      new ConsoleCommunication(resources,
                               properties,
                               m_userInterface.getErrorHandler());
  }

  /**
   * Console message event loop. Dispatches communication messages
   * appropriately to the console model.
   *
   * @exception GrinderException If an error occurs.
   */
  public void run() throws GrinderException {

    while (true) {
      final Message message = m_communication.waitForMessage();

      if (message instanceof RegisterTestsMessage) {
        m_model.registerTests(((RegisterTestsMessage)message).getTests());
      }

      if (message instanceof ReportStatisticsMessage) {
        m_model.addTestReport(
          ((ReportStatisticsMessage)message).getStatisticsDelta());
      }

      if (message instanceof RegisterStatisticsViewMessage) {
        final StatisticsView statisticsView =
          ((RegisterStatisticsViewMessage)message).getStatisticsView();

        m_model.registerStatisticsViews(statisticsView, statisticsView);
      }

      if (message instanceof ReportStatusMessage) {
        m_model.getProcessStatusSet().addStatusReport(
          (ReportStatusMessage)message);
      }
    }
  }
}
