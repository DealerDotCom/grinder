// Copyright (C) 2000 Paco Gomez
// Copyright (C) 2000, 2001, 2002, 2003, 2004, 2005 Philip Aston
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
import net.grinder.communication.ClientSender;
import net.grinder.communication.CommunicationDefaults;
import net.grinder.communication.CommunicationException;
import net.grinder.communication.ConnectionType;
import net.grinder.communication.Connector;
import net.grinder.communication.FanOutStreamSender;
import net.grinder.communication.MessagePump;
import net.grinder.communication.Sender;
import net.grinder.communication.TeeSender;
import net.grinder.console.messages.AgentProcessStatusMessage;
import net.grinder.engine.common.ConsoleListener;
import net.grinder.engine.common.EngineException;
import net.grinder.engine.messages.StartGrinderMessage;
import net.grinder.util.JVM;
import net.grinder.util.SimpleLogger;


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
  private final Logger m_logger;
  private final Timer m_timer;

  /**
   * Constructor.
   *
   * @throws GrinderException If an error occurs.
   */
  public Agent() throws GrinderException {
    this(null);
  }

  /**
   * Constructor.
   *
   * @param alternateFile Alternative properties file.
   * @throws GrinderException If an error occurs.
   */
  public Agent(File alternateFile) throws GrinderException {

    m_alternateFile = alternateFile;
    m_timer = new Timer(true);
    m_logger = new SimpleLogger("agent",
                                new PrintWriter(System.out),
                                new PrintWriter(System.err));

    if (!JVM.getInstance().haveRequisites(m_logger)) {
      return;
    }
  }

  /**
   * Run the Grinder agent process.
   *
   * @throws GrinderException If an error occurs.
   * @throws InterruptedException If the calling thread is
   * interrupted whilst waiting.
   */
  public void run() throws GrinderException, InterruptedException {

    StartGrinderMessage nextStartMessage = null;

    // We use one file store throughout an agent's life, but can't
    // initialise it until we've read the properties.
    FileStore fileStore = null;

    ConsoleCommunication consoleCommunication = null;
    Connector lastConnector = null;
    final FanOutStreamSender fanOutStreamSender = new FanOutStreamSender(3);
    final Object eventSynchronisation = new Object();
    final ConsoleListener consoleListener =
      new ConsoleListener(eventSynchronisation, m_logger);

    while (true) {
      m_logger.output(GrinderBuild.getName());

      final GrinderProperties properties =
        new GrinderProperties(m_alternateFile);

      final WorkerProcessCommandLine workerCommandLine =
        new WorkerProcessCommandLine(
          properties, System.getProperties(), m_alternateFile);

      m_logger.output("Worker process command line: " + workerCommandLine);

      final String agentID =
        properties.getProperty("grinder.hostID", getHostName());

      if (properties.getBoolean("grinder.useConsole", true)) {
        final Connector connector =
          new Connector(
            properties.getProperty("grinder.consoleHost",
                                   CommunicationDefaults.CONSOLE_HOST),
            properties.getInt("grinder.consolePort",
                              CommunicationDefaults.CONSOLE_PORT),
            ConnectionType.AGENT);

        if (!connector.equals(lastConnector)) {
          // We only reconnect if the connection details have changed.
          if (consoleCommunication != null) {
            consoleCommunication.shutdown();
            consoleCommunication = null;
          }

          try {
            consoleCommunication =
              new ConsoleCommunication(connector, agentID);
            lastConnector = connector;

            if (fileStore == null) {
              // Only create the file store if we connected.
              fileStore =
                new FileStore(new File("./" + agentID + "-file-store"),
                              m_logger);
            }

            // Ordering of the TeeSender is important so the child
            // processes get the stop and reset signals before our
            // console listener.
            // TODO - generalise the listener stuff in the console and
            // use it to subscribe all three senders.
            // The fanOutStreamSender can be subscribed above.
            final Sender workerSender =
              fileStore.getSender(new TeeSender(fanOutStreamSender,
                                                consoleListener.getSender()));

            new MessagePump(consoleCommunication.getReceiver(),
                            workerSender, 1);
          }
          catch (CommunicationException e) {
            m_logger.error(
              e.getMessage() + ", proceeding without the console; set " +
              "grinder.useConsole=false to disable this warning.");
          }
        }

        if (consoleCommunication != null && nextStartMessage == null) {
          m_logger.output("waiting for console signal");
          consoleListener.waitForMessage();
        }
      }

      // final to ensure we only take one path through the following.
      final File scriptFile;
      File scriptDirectory = null;

      if (consoleCommunication == null ||
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
          m_logger.error("Files have not been distributed from the console");
          scriptFile = null;
        }
        else if (scriptFromConsole != null) {
          final File fileStoreDirectory = fileStore.getDirectory().getAsFile();

          final File absoluteFile =
            new File(fileStoreDirectory, scriptFromConsole.getPath());

          if (absoluteFile.canRead()) {
            scriptFile = absoluteFile;
            scriptDirectory = fileStoreDirectory;
          }
          else {
            m_logger.error("The script file '" + scriptFromConsole +
                         "' requested by the console does not exist " +
                         "or is not readable.",
                         Logger.LOG | Logger.TERMINAL);

            scriptFile = null;
          }
        }
        else {
          final File scriptFromProperties =
            new File(properties.getProperty("grinder.script", "grinder.py"));

          if (scriptFromProperties.canRead()) {
            scriptFile = scriptFromProperties;
            scriptDirectory =
              scriptFromProperties.getAbsoluteFile().getParentFile();
          }
          else {
            m_logger.error("The script file '" + scriptFromProperties +
                         "' does not exist or is not readable. " +
                         "Check grinder.properties.",
                         Logger.LOG | Logger.TERMINAL);

            scriptFile = null;
          }
        }
      }
      else {
        scriptFile = null;
      }

      if (scriptFile != null) {
        final ProcessFactory workerProcessFactory =
          new WorkerProcessFactory(workerCommandLine,
                                   fanOutStreamSender,
                                   agentID,
                                   consoleCommunication != null,
                                   scriptFile,
                                   scriptDirectory);

        final ProcessLauncher processLauncher =
          new ProcessLauncher(properties.getInt("grinder.processes", 1),
                              workerProcessFactory, eventSynchronisation,
                              m_logger);

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

            m_timer.scheduleAtFixedRate(
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

              m_logger.output("forcibly terminating unresponsive processes");
              processLauncher.destroyAllProcesses();
            }

            eventSynchronisation.wait(maximumShutdownTime);
          }
        }
      }

      if (consoleCommunication == null) {
        break;
      }
      else {
        // Ignore any pending start messages.
        consoleListener.discardMessages(ConsoleListener.START);

        if (!consoleListener.received(ConsoleListener.ANY)) {
          // We've got here naturally, without a console signal.
          m_logger.output("finished, waiting for console signal");
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

    if (consoleCommunication != null) {
      consoleCommunication.shutdown();
    }

    m_logger.output("finished");
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

  private final class ConsoleCommunication {
    private final ClientReceiver m_receiver;
    private final ClientSender m_sender;
    private final String m_agentID;

    public ConsoleCommunication(Connector connector, String agentID)
      throws CommunicationException {
      m_receiver = ClientReceiver.connect(connector);
      m_sender = ClientSender.connect(m_receiver);
      m_agentID = agentID;

      m_sender.send(
        new AgentProcessStatusMessage(
          m_agentID,
          AgentProcessStatusMessage.STATE_STARTED));
    }

    public ClientReceiver getReceiver() {
      return m_receiver;
    }

    public void shutdown() throws CommunicationException {
      m_sender.send(
        new AgentProcessStatusMessage(
          m_agentID,
          AgentProcessStatusMessage.STATE_FINISHED));
      m_receiver.shutdown();
      m_sender.shutdown();
    }
  }
}
