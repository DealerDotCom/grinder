// Copyright (C) 2000 Paco Gomez
// Copyright (C) 2000, 2001, 2002, 2003, 2004 Philip Aston
// Copyright (C) 2004 Bertrand Ave
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

package net.grinder.engine.agent;

import java.io.File;
import java.io.PrintWriter;
import java.util.Timer;
import java.util.TimerTask;

import net.grinder.common.Logger;
import net.grinder.common.GrinderBuild;
import net.grinder.common.GrinderException;
import net.grinder.common.GrinderProperties;
import net.grinder.communication.ClientReceiver;
import net.grinder.communication.CommunicationDefaults;
import net.grinder.communication.CommunicationException;
import net.grinder.communication.ConnectionType;
import net.grinder.communication.Connector;
import net.grinder.communication.FanOutStreamSender;
import net.grinder.communication.InitialiseGrinderMessage;
import net.grinder.communication.MessagePump;
import net.grinder.communication.Receiver;
import net.grinder.communication.Sender;
import net.grinder.communication.TeeSender;
import net.grinder.engine.common.ConsoleListener;


/**
 * This is the entry point of The Grinder agent process.
 *
 * @author Paco Gomez
 * @author Philip Aston
 * @author Bertrand Ave
 * @version $Revision$
 */
public final class Agent {

  private final File m_alternateFile;

  /**
   * Constructor.
   */
  public Agent() {
    this(null);
  }

  /**
   * Constructor.
   *
   * @param alternateFile Alternative properties file.
   */
  public Agent(File alternateFile) {
    m_alternateFile = alternateFile;
  }

  /**
   * Run the Grinder agent process.
   *
   * @throws GrinderException If an error occurs.
   * @throws InterruptedException If the calling thread is
   * interrupted whilst waiting.
   */
  public void run() throws GrinderException, InterruptedException {

    final Timer timer = new Timer(true);
    final Logger logger = new AgentLogger(new PrintWriter(System.out),
                                          new PrintWriter(System.err));

    boolean startImmediately = false;

    while (true) {
      logger.output("The Grinder version " + GrinderBuild.getVersionString());

      final GrinderProperties properties =
        new GrinderProperties(m_alternateFile);

      final Object eventSynchronisation = new Object();
      final FanOutStreamSender fanOutStreamSender = new FanOutStreamSender(3);
      final ConsoleListener consoleListener =
        new ConsoleListener(eventSynchronisation, logger);

      Receiver receiver = null;

      if (properties.getBoolean("grinder.useConsole", true)) {
        final Connector connector =
          new Connector(
            properties.getProperty("grinder.consoleHost",
                                   CommunicationDefaults.CONSOLE_HOST),
            properties.getInt("grinder.consolePort",
                              CommunicationDefaults.CONSOLE_PORT),
            ConnectionType.CONTROL);

        try {
          receiver = ClientReceiver.connect(connector);

          // Ordering of the TeeSender is important so the child
          // processes get the signals before our console listener.
          final Sender sender =
            new TeeSender(fanOutStreamSender, consoleListener.getSender());

          new MessagePump(receiver, sender, 1);
        }
        catch (CommunicationException e) {
          logger.error(
            e.getMessage() + ", proceeding without the console; set " +
            "grinder.useConsole=false to disable this warning.");
        }
      }

      final ProcessLauncher processLauncher =
        new ProcessLauncher(logger, properties, m_alternateFile,
                            fanOutStreamSender,
                            new InitialiseGrinderMessage(receiver != null),
                            eventSynchronisation);

      if (!startImmediately && receiver != null) {
        logger.output("waiting for console signal");
        consoleListener.waitForMessage();
      }

      if (receiver == null ||
          startImmediately ||
          consoleListener.received(ConsoleListener.START)) {

        final int processIncrement =
          properties.getInt("grinder.processIncrement", 0);

        if (processIncrement > 0) {
          final boolean moreProcessesToStart =
            processLauncher.startSomeProcesses(
              properties.getInt("grinder.initialProcesses", processIncrement));

          if (moreProcessesToStart) {
            final int incrementInterval =
              properties.getInt("grinder.processIncrementInterval", 60000);

            final RampUpTimerTask rampUpTimerTask =
              new RampUpTimerTask(processLauncher, processIncrement);

            timer.scheduleAtFixedRate(
              rampUpTimerTask, incrementInterval, incrementInterval);
          }
        }
        else {
          processLauncher.startAllProcesses();
        }

        // Wait for a termination event.
        synchronized (eventSynchronisation) {
          final long maximumShutdownTime = 20000;
          long consoleSignalTime = -1;

          while (!processLauncher.allFinished()) {

            if (consoleListener.checkForMessage(ConsoleListener.ANY ^
                                                ConsoleListener.START)) {
              processLauncher.dontStartAnyMore();
              consoleSignalTime = System.currentTimeMillis();
            }

            if (consoleSignalTime >= 0 &&
                System.currentTimeMillis() - consoleSignalTime >
                maximumShutdownTime) {

              logger.output("forcibly terminating unresponsive processes");
              processLauncher.destroy();
            }

            eventSynchronisation.wait(maximumShutdownTime);
          }
        }
      }

      if (receiver == null) {
        break;
      }
      else {
        if (!consoleListener.received(ConsoleListener.ANY)) {
          // We've got here naturally, without a console signal.
          logger.output("finished, waiting for console signal");

          consoleListener.waitForMessage();
        }

        receiver.shutdown();

        if (consoleListener.received(ConsoleListener.START)) {
          startImmediately = true;
        }
        else if (consoleListener.received(ConsoleListener.STOP |
                                          ConsoleListener.SHUTDOWN)) {
          break;
        }
        else {
          // ConsoleListener.RESET or natural death.
          startImmediately = false;
        }
      }
    }

    logger.output("finished");
  }

  private class RampUpTimerTask extends TimerTask {

    private final ProcessLauncher m_processLauncher;
    private final int m_processIncrement;
    private boolean m_rampUpFinished = false;

    public RampUpTimerTask(ProcessLauncher processLauncher,
                           int processIncrement) {
      m_processLauncher = processLauncher;
      m_processIncrement = processIncrement;
    }

    public void run () {
      final boolean moreProcessesToStart =
        m_processLauncher.startSomeProcesses(m_processIncrement);

      if (!moreProcessesToStart) {
        super.cancel();

        synchronized (this) {
          m_rampUpFinished = true;
          notifyAll();
        }
      }
    }
  }
}
