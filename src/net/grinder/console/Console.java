// Copyright (C) 2000 Paco Gomez
// Copyright (C) 2000, 2001, 2002, 2003 Philip Aston
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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import net.grinder.common.GrinderException;
import net.grinder.communication.Message;
import net.grinder.communication.RegisterStatisticsViewMessage;
import net.grinder.communication.RegisterTestsMessage;
import net.grinder.communication.ReportStatisticsMessage;
import net.grinder.communication.ReportStatusMessage;
import net.grinder.console.common.Resources;
import net.grinder.console.model.ConsoleProperties;
import net.grinder.console.model.Model;
import net.grinder.console.model.ProcessStatusSet;
import net.grinder.console.swingui.ConsoleUI;
import net.grinder.statistics.StatisticsView;


/**
 * This is the entry point of The Grinder Console.
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

    final ConsoleProperties properties =
      new ConsoleProperties(new File(homeDirectory, ".grinder_console"));

    final Resources resources =
      new Resources("net.grinder.console.swingui.resources.Console");

    m_model = new Model(properties, resources);

    final ProcessStatusSet processStatusSet = m_model.getProcessStatusSet();

    final ActionListener startHandler =
      new ActionListener() {
        public void actionPerformed(ActionEvent event) {
          try {
            processStatusSet.processEvent();
            m_communication.sendStartMessage();
          }
          catch (GrinderException e) {
            System.err.println("Could not send start message: " + e);
            e.printStackTrace();
          }
        }
      };

    final ActionListener resetHandler =
      new ActionListener() {
        public void actionPerformed(ActionEvent event) {
          try {
            processStatusSet.processEvent();
            m_communication.sendResetMessage();
          }
          catch (GrinderException e) {
            System.err.println("Could not send reset message: " + e);
            e.printStackTrace();
          }
        }
      };

    final ActionListener stopHandler =
      new ActionListener() {
        public void actionPerformed(ActionEvent event) {
          try {
            processStatusSet.processEvent();
            m_communication.sendStopMessage();
          }
          catch (GrinderException e) {
            System.err.println("Could not send stop message: " + e);
            e.printStackTrace();
          }
        }
      };

    m_userInterface =
      new ConsoleUI(m_model, startHandler, resetHandler, stopHandler);

    m_communication = new ConsoleCommunication(properties, m_userInterface);
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
          message.getSenderUniqueID(), (ReportStatusMessage)message);
      }
    }
  }
}
