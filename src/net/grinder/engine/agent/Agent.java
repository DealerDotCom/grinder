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
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Timer;
import java.util.TimerTask;

import net.grinder.common.GrinderBuild;
import net.grinder.common.GrinderException;
import net.grinder.common.GrinderProperties;
import net.grinder.common.Logger;
import net.grinder.communication.ClientReceiver;
import net.grinder.communication.CommunicationDefaults;
import net.grinder.communication.CommunicationException;
import net.grinder.communication.ConnectionType;
import net.grinder.communication.Connector;
import net.grinder.communication.FanOutStreamSender;
import net.grinder.communication.MessagePump;
import net.grinder.communication.Receiver;
import net.grinder.communication.Sender;
import net.grinder.communication.TeeSender;
import net.grinder.engine.common.ConsoleListener;
import net.grinder.engine.common.EngineException;
import net.grinder.engine.messages.InitialiseGrinderMessage;
import net.grinder.engine.messages.StartGrinderMessage;
import net.grinder.util.JVM;


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
    final Logger logger = new AgentLogger(new PrintWriter(System.out),
                                          new PrintWriter(System.err));

    if (!JVM.getInstance().haveRequisites(logger)) {
      return;
    }

    final Timer timer = new Timer(true);
    StartGrinderMessage nextStartMessage = null;

    // We use one file store throughout an agent's life, but can't
    // initialise it until we've read the properties.
    FileStore fileStore = null;

    Receiver receiver = null;
    Connector lastConnector = null;
    final FanOutStreamSender fanOutStreamSender = new FanOutStreamSender(3);
    final Object eventSynchronisation = new Object();
    final ConsoleListener consoleListener =
      new ConsoleListener(eventSynchronisation, logger);

    while (true) {
      logger.output(GrinderBuild.getName());

      final GrinderProperties properties =
        new GrinderProperties(m_alternateFile);

      final WorkerProcessCommandLine workerCommandLine =
        new WorkerProcessCommandLine(
          properties, System.getProperties(), m_alternateFile);

      logger.output("Worker process command line: " + workerCommandLine);

      final String hostID =
        properties.getProperty("grinder.hostID", getHostName());

      if (properties.getBoolean("grinder.useConsole", true)) {
        final Connector connector =
          new Connector(
            properties.getProperty("grinder.consoleHost",
                                   CommunicationDefaults.CONSOLE_HOST),
            properties.getInt("grinder.consolePort",
                              CommunicationDefaults.CONSOLE_PORT),
            ConnectionType.CONTROL);

        if (!connector.equals(lastConnector)) {
          // We only reconnect if the connection details have changed.
          // This is important as the console currently uses
          // connections to track whether it needs to transmit the
          // entire file distribution.
          if (receiver != null) {
            receiver.shutdown();
            receiver = null;
          }

          try {
            receiver = ClientReceiver.connect(connector);
            lastConnector = connector;

            if (fileStore == null) {
              // Only create the file store if we connected.
              fileStore =
                new FileStore(new File("./" + hostID + "-file-store"), logger);
            }

            // Ordering of the TeeSender is important so the child
            // processes get the stop and reset signals before our
            // console listener.
            final Sender sender =
              fileStore.getSender(new TeeSender(fanOutStreamSender,
                                                consoleListener.getSender()));

            new MessagePump(receiver, sender, 1);
          }
          catch (CommunicationException e) {
            logger.error(
              e.getMessage() + ", proceeding without the console; set " +
              "grinder.useConsole=false to disable this warning.");
          }
        }

        if (receiver != null && nextStartMessage == null) {
          logger.output("waiting for console signal");
          consoleListener.waitForMessage();
        }
      }

      final InitialiseGrinderMessage initialiseMessage;

      if (receiver == null ||
          nextStartMessage != null ||
          consoleListener.received(ConsoleListener.START)) {

        File scriptFromConsole = null;

        if (nextStartMessage != null) {
          scriptFromConsole = nextStartMessage.getScriptFile();
        }
        else {
          final StartGrinderMessage lastStartMessage =
            consoleListener.getLastStartGrinderMessage();

          if (lastStartMessage != null) {
            scriptFromConsole = lastStartMessage.getScriptFile();
          }
        }

        if (scriptFromConsole != null && fileStore.getDirectory() == null) {
          logger.error("Files have not been distributed from the console");
          initialiseMessage = null;
        }
        else if (scriptFromConsole != null) {
          final File fileStoreDirectory = fileStore.getDirectory().getAsFile();

          final File absoluteFile =
            new File(fileStoreDirectory, scriptFromConsole.getPath());

          if (absoluteFile.canRead()) {
            initialiseMessage =
              new InitialiseGrinderMessage(
              true, absoluteFile, fileStoreDirectory);
          }
          else {
            logger.error("The script file '" + scriptFromConsole +
                         "' requested by the console does not exist " +
                         "or is not readable.",
                         Logger.LOG | Logger.TERMINAL);

            initialiseMessage = null;
          }
        }
        else {
          final File scriptFromProperties =
            new File(properties.getProperty("grinder.script", "grinder.py"));

          if (scriptFromProperties.canRead()) {
            initialiseMessage =
              new InitialiseGrinderMessage(
                receiver != null,
                scriptFromProperties,
                scriptFromProperties.getAbsoluteFile().getParentFile());
          }
          else {
            logger.error("The script file '" + scriptFromProperties +
                         "' does not exist or is not readable. " +
                         "Check grinder.properties.",
                         Logger.LOG | Logger.TERMINAL);

            initialiseMessage = null;
          }
        }
      }
      else {
        initialiseMessage = null;
      }

      if (initialiseMessage != null) {
        final ProcessFactory workerProcessFactory =
          new WorkerProcessFactory(workerCommandLine, hostID,
                                   fanOutStreamSender, initialiseMessage);

        final ProcessLauncher processLauncher =
          new ProcessLauncher(properties.getInt("grinder.processes", 1),
                              workerProcessFactory, eventSynchronisation,
                              logger);

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
              processLauncher.destroyAllProcesses();
            }

            eventSynchronisation.wait(maximumShutdownTime);
          }
        }
      }

      if (receiver == null) {
        break;
      }
      else {
        // Ignore any pending start messages.
        consoleListener.discardMessages(ConsoleListener.START);

        if (!consoleListener.received(ConsoleListener.ANY)) {
          // We've got here naturally, without a console signal.
          logger.output("finished, waiting for console signal");

          consoleListener.waitForMessage();
        }

        if (consoleListener.received(ConsoleListener.START)) {
          nextStartMessage = consoleListener.getLastStartGrinderMessage();
        }
        else if (consoleListener.received(ConsoleListener.STOP |
                                          ConsoleListener.SHUTDOWN)) {
          break;
        }
        else {
          // ConsoleListener.RESET or natural death.
          nextStartMessage = null;
        }
      }
    }

    if (receiver != null) {
      receiver.shutdown();
    }

    logger.output("finished");
  }

  private static String getHostName() {
    try {
      return InetAddress.getLocalHost().getHostName();
    }
    catch (UnknownHostException e) {
      return "UNNAMED HOST";
    }
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

    public void run() {
      try {
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
      catch (EngineException e) {
        // Really an assertion. Can't use logger because its not thread-safe.
        System.err.println("Failed to start processes");
        e.printStackTrace();
      }
    }
  }
}
