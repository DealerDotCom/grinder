// Copyright (C) 2000 Paco Gomez
// Copyright (C) 2000-2007 Philip Aston
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
import net.grinder.communication.MessageDispatchSender;
import net.grinder.communication.MessagePump;
import net.grinder.communication.TeeSender;
import net.grinder.console.messages.AgentProcessReportMessage;
import net.grinder.engine.common.ConsoleListener;
import net.grinder.engine.common.EngineException;
import net.grinder.engine.common.ScriptLocation;
import net.grinder.engine.messages.StartGrinderMessage;
import net.grinder.util.Directory;
import net.grinder.util.JVM;
import net.grinder.util.thread.Monitor;


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
  private final Monitor m_eventSynchronisation = new Monitor();
  private final AgentIdentityImplementation m_agentIdentity;
  private final ConsoleListener m_consoleListener;
  private final FanOutStreamSender m_fanOutStreamSender =
    new FanOutStreamSender(3);
  private ConsoleCommunication m_consoleCommunication = null;

  /**
   * We use an most one file store throughout an agent's life, but can't
   * initialise it until we've read the properties and connected to the console.
   */
  private FileStore m_fileStore;

  /**
   * Constructor.
   *
   * @param logger Logger.
   * @param alternateFile Alternative properties file, or <code>null</code>.
   * @throws GrinderException If an error occurs.
   */
  public Agent(Logger logger, File alternateFile) throws GrinderException {

    m_alternateFile = alternateFile;
    m_timer = new Timer(true);
    m_logger = logger;

    m_consoleListener = new ConsoleListener(m_eventSynchronisation, m_logger);
    m_agentIdentity = new AgentIdentityImplementation(getHostName());

    if (!JVM.getInstance().haveRequisites(m_logger)) {
      return;
    }
  }

  /**
   * Run the Grinder agent process.
   *
   * @throws GrinderException If an error occurs.
   */
  public void run() throws GrinderException {

    StartGrinderMessage nextStartMessage = null;
    Connector lastConnector = null;

    while (true) {
      m_logger.output(GrinderBuild.getName());

      final GrinderProperties properties =
        new GrinderProperties(m_alternateFile);

      final String newHostName =
        properties.getProperty("grinder.hostID", getHostName());

      if (!newHostName.equals(m_agentIdentity.getName())) {
        m_agentIdentity.setName(newHostName);
      }

      if (properties.getBoolean("grinder.useConsole", true)) {
        final Connector connector = new Connector(
            properties.getProperty("grinder.consoleHost",
                                   CommunicationDefaults.CONSOLE_HOST),
            properties.getInt("grinder.consolePort",
                              CommunicationDefaults.CONSOLE_PORT),
            ConnectionType.AGENT);

        if (!connector.equals(lastConnector)) {
          // We only reconnect if the connection details have changed.
          if (m_consoleCommunication != null) {
            m_consoleCommunication.shutdown();
          }

          try {
            m_consoleCommunication = new ConsoleCommunication(connector);
            lastConnector = connector;
          }
          catch (CommunicationException e) {
            m_logger.error(
              e.getMessage() + ", proceeding without the console; set " +
              "grinder.useConsole=false to disable this warning.");
            m_consoleCommunication = null;
          }
        }

        if (m_consoleCommunication != null && nextStartMessage == null) {
          m_logger.output("waiting for console signal");
          m_consoleListener.waitForMessage();
        }
      }
      else {
        if (m_consoleCommunication != null) {
          m_consoleCommunication.shutdown();
        }
        m_consoleCommunication = null;
      }

      // final to ensure we only take one path through the following.
      final File scriptFile;
      File scriptDirectory = null;

      if (m_consoleCommunication == null ||
          nextStartMessage != null ||
          m_consoleListener.received(ConsoleListener.START)) {

        File scriptFromConsole = null;

        if (nextStartMessage != null) {
          scriptFromConsole = nextStartMessage.getScriptFile();
        }
        else {
          final StartGrinderMessage lastStartMessage =
            m_consoleListener.getLastStartGrinderMessage();

          if (lastStartMessage != null) {
            scriptFromConsole = lastStartMessage.getScriptFile();
          }
        }

        if (scriptFromConsole != null) {
          final Directory fileStoreDirectory = m_fileStore.getDirectory();

          final File absoluteFile =
            fileStoreDirectory.getFile(scriptFromConsole.getPath());

          if (absoluteFile.canRead()) {
            scriptFile = absoluteFile;
            scriptDirectory = fileStoreDirectory.getFile();
          }
          else {
            m_logger.error("The script file '" + scriptFromConsole +
                         "' requested by the console does not exist " +
                         "or is not readable.");

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
                           "Check grinder.properties.");

            scriptFile = null;
          }
        }
      }
      else {
        scriptFile = null;
      }

      if (scriptFile != null) {
        final boolean singleProcess =
          properties.getBoolean("grinder.debug.singleprocess", false);
        final String jvmArguments =
          properties.getProperty("grinder.jvm.arguments");

        final WorkerFactory workerFactory;

        if (!singleProcess) {
          final WorkerProcessCommandLine workerCommandLine =
            new WorkerProcessCommandLine(properties,
                                         System.getProperties(),
                                         jvmArguments);

          m_logger.output("Worker process command line: " + workerCommandLine);

          workerFactory =
            new ProcessWorkerFactory(
              workerCommandLine, m_agentIdentity, m_fanOutStreamSender,
              m_consoleCommunication != null,
              new ScriptLocation(scriptDirectory, scriptFile),
              properties);
        }
        else {
          m_logger.output("DEBUG MODE: Spawning threads rather than processes");

          if (jvmArguments != null) {
            m_logger.output("WARNING grinder.jvm.arguments (" + jvmArguments +
                            ") ignored in single process mode");
          }

          workerFactory =
            new DebugThreadWorkerFactory(
              m_agentIdentity, m_fanOutStreamSender,
              m_consoleCommunication != null,
              new ScriptLocation(scriptDirectory, scriptFile),
              properties);
        }

        final WorkerLauncher workerLauncher =
          new WorkerLauncher(properties.getInt("grinder.processes", 1),
                             workerFactory, m_eventSynchronisation, m_logger);

        final int processIncrement =
          properties.getInt("grinder.processIncrement", 0);

        if (processIncrement > 0) {
          final boolean moreProcessesToStart =
            workerLauncher.startSomeWorkers(
              properties.getInt("grinder.initialProcesses", processIncrement));

          if (moreProcessesToStart) {
            final int incrementInterval =
              properties.getInt("grinder.processIncrementInterval", 60000);

            final RampUpTimerTask rampUpTimerTask =
              new RampUpTimerTask(workerLauncher, processIncrement);

            m_timer.scheduleAtFixedRate(
              rampUpTimerTask, incrementInterval, incrementInterval);
          }
        }
        else {
          workerLauncher.startAllWorkers();
        }

        // Wait for a termination event.
        synchronized (m_eventSynchronisation) {
          final long maximumShutdownTime = 20000;
          long consoleSignalTime = -1;

          while (!workerLauncher.allFinished()) {
            if (consoleSignalTime == -1 &&
                m_consoleListener.checkForMessage(ConsoleListener.ANY ^
                                                  ConsoleListener.START)) {
              workerLauncher.dontStartAnyMore();
              consoleSignalTime = System.currentTimeMillis();
            }

            if (consoleSignalTime >= 0 &&
                System.currentTimeMillis() - consoleSignalTime >
                maximumShutdownTime) {

              m_logger.output("forcibly terminating unresponsive processes");
              workerLauncher.destroyAllWorkers();
            }

            m_eventSynchronisation.waitNoInterrruptException(
              maximumShutdownTime);
          }
        }

        workerLauncher.shutdown();
      }

      if (m_consoleCommunication == null) {
        break;
      }
      else {
        // Ignore any pending start messages.
        m_consoleListener.discardMessages(ConsoleListener.START);

        if (!m_consoleListener.received(ConsoleListener.ANY)) {
          // We've got here naturally, without a console signal.
          m_logger.output("finished, waiting for console signal");
          m_consoleListener.waitForMessage();
        }

        if (m_consoleListener.received(ConsoleListener.START)) {
          nextStartMessage = m_consoleListener.getLastStartGrinderMessage();
        }
        else if (m_consoleListener.received(ConsoleListener.STOP |
                                            ConsoleListener.SHUTDOWN)) {
          break;
        }
        else {
          // ConsoleListener.RESET or natural death.
          nextStartMessage = null;
        }
      }
    }
  }

  /**
   * Clean up resources.
   */
  public void shutdown() {
    m_timer.cancel();

    if (m_consoleCommunication != null) {
      m_consoleCommunication.shutdown();
    }

    m_fanOutStreamSender.shutdown();

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

  private static class RampUpTimerTask extends TimerTask {

    private final WorkerLauncher m_processLauncher;
    private final int m_processIncrement;

    public RampUpTimerTask(WorkerLauncher processLauncher,
                           int processIncrement) {
      m_processLauncher = processLauncher;
      m_processIncrement = processIncrement;
    }

    public void run() {
      try {
        final boolean moreProcessesToStart =
          m_processLauncher.startSomeWorkers(m_processIncrement);

        if (!moreProcessesToStart) {
          super.cancel();
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
    private final TimerTask m_reportRunningTask;

    public ConsoleCommunication(Connector connector)
        throws CommunicationException, FileStore.FileStoreException {

      m_receiver = ClientReceiver.connect(connector);
      m_sender = ClientSender.connect(m_receiver);

      m_sender.send(
        new AgentProcessReportMessage(
          m_agentIdentity,
          AgentProcessReportMessage.STATE_STARTED));

      if (m_fileStore == null) {
        // Only create the file store if we connected.
        m_fileStore =
          new FileStore(
            new File("./" + m_agentIdentity.getName() + "-file-store"),
            m_logger);
      }

      final MessageDispatchSender fileStoreMessageDispatcher =
        new MessageDispatchSender();
      m_fileStore.registerMessageHandlers(fileStoreMessageDispatcher);

      final MessageDispatchSender messageDispatcher =
        new MessageDispatchSender();
      m_consoleListener.registerMessageHandlers(messageDispatcher);

      // Everything that the file store doesn't handle is tee'd to the
      // worker processes and our message handlers.
      fileStoreMessageDispatcher.addFallback(
        new TeeSender(messageDispatcher, m_fanOutStreamSender));

      new MessagePump(m_receiver, fileStoreMessageDispatcher, 1);

      m_reportRunningTask = new TimerTask() {
        public void run() {
          try {
            m_sender.send(
              new AgentProcessReportMessage(
                m_agentIdentity,
                AgentProcessReportMessage.STATE_RUNNING));
          }
          catch (CommunicationException e) {
            cancel();
          }
        }
      };

      m_timer.schedule(m_reportRunningTask, 1000, 1000);
    }

    public void shutdown() {
      m_reportRunningTask.cancel();

      try {
        m_sender.send(
          new AgentProcessReportMessage(
            m_agentIdentity,
            AgentProcessReportMessage.STATE_FINISHED));
      }
      catch (CommunicationException e) {
        // Ignore - peer has probably shut down.
      }
      finally {
        m_receiver.shutdown();
        m_sender.shutdown();
      }
    }
  }
}
