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
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.Timer;
import java.util.TimerTask;

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
import net.grinder.communication.Message;
import net.grinder.communication.MessagePump;
import net.grinder.communication.Receiver;
import net.grinder.communication.StreamSender;
import net.grinder.engine.common.EngineException;
import net.grinder.engine.process.GrinderProcess;


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

    final String version = GrinderBuild.getVersionString();
    System.out.println("The Grinder version " + version);

    boolean startImmediately = false;

    while (true) {
      final GrinderProperties properties =
        new GrinderProperties(m_alternateFile);

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
        }
        catch (CommunicationException e) {
          System.out.println(
            e.getMessage() + ", proceeding without the console; set " +
            "grinder.useConsole=false to disable this warning.");
        }
      }

      final FanOutStreamSender fanOutStreamSender = new FanOutStreamSender(3);

      final boolean haveConsole = receiver != null;

      final MessagePump messagePump =
        haveConsole ? new MessagePump(receiver, fanOutStreamSender, 1) : null;

      final ProcessStarter processStarter =
        new ProcessStarter(properties, fanOutStreamSender, haveConsole);

      // If we're using the console, wait for a console start signal here.

      final int processIncrement =
        properties.getInt("grinder.processIncrement", 0);

      if (processIncrement > 0) {
        final boolean moreProcessesToStart =
          processStarter.startSomeProcesses(
            properties.getInt("grinder.initialProcesses", processIncrement));

        if (moreProcessesToStart) {
          final int incrementInterval =
            properties.getInt("grinder.processIncrementInterval", 60000);

          final Timer timer = new Timer(true);
          final RampUpTimerTask rampUpTimerTask =
            new RampUpTimerTask(processStarter, processIncrement);

          timer.scheduleAtFixedRate(
            rampUpTimerTask, incrementInterval, incrementInterval);

          rampUpTimerTask.waitUntilFinished();
          timer.cancel();
        }
      }
      else {
        processStarter.startAllProcesses();
      }

      final int combinedExitStatus = processStarter.waitForProcessesToFinish();

      // If we're using the console, wait for a stop or reset signal here.

      if (messagePump != null) {
        messagePump.shutdown();
      }

      if (combinedExitStatus == GrinderProcess.EXIT_START_SIGNAL) {
        startImmediately = true;
      }
      else if (combinedExitStatus == GrinderProcess.EXIT_RESET_SIGNAL) {
        startImmediately = false;
      }
      else {
        break;
      }
    }

    System.out.println("The Grinder version " + version + " finished");
  }

  private class ProcessStarter {

    private final FanOutStreamSender m_fanOutStreamSender;
    private final Message m_initialiseMessage;

    private final String[] m_commandArray;
    private final int m_grinderIDIndex;
    private final String m_hostIDString;

    private final ChildProcess[] m_processes;
    private int m_nextProcessIndex = 0;

    public ProcessStarter(GrinderProperties properties,
                          FanOutStreamSender fanOutStreamSender,
                          boolean haveConsole) {

      m_fanOutStreamSender = fanOutStreamSender;
      m_initialiseMessage =
        new InitialiseGrinderMessage(false, haveConsole, haveConsole);

      m_processes =
        new ChildProcess[properties.getInt("grinder.processes", 1)];

      final List command = new ArrayList();
      command.add(properties.getProperty("grinder.jvm", "java"));

      final String jvmArguments =
        properties.getProperty("grinder.jvm.arguments");

      if (jvmArguments != null) {
        // Really should allow whitespace to be escaped/quoted.
        final StringTokenizer tokenizer = new StringTokenizer(jvmArguments);

        while (tokenizer.hasMoreTokens()) {
          command.add(tokenizer.nextToken());
        }
      }

      // Pass through any "grinder" system properties.
      final Iterator systemProperties =
        System.getProperties().entrySet().iterator();

      while (systemProperties.hasNext()) {
        final Map.Entry entry = (Map.Entry)systemProperties.next();
        final String key = (String)entry.getKey();
        final String value = (String)entry.getValue();

        if (key.startsWith("grinder.")) {
          command.add("-D" + key + "=" + value);
        }
      }

      final String additionalClasspath =
        properties.getProperty("grinder.jvm.classpath", null);

      final String classpath =
        (additionalClasspath != null ?
         additionalClasspath + File.pathSeparatorChar : "") +
        System.getProperty("java.class.path");

      if (classpath.length() > 0) {
        command.add("-classpath");
        command.add(classpath);
      }

      command.add(GrinderProcess.class.getName());

      m_hostIDString = properties.getProperty("grinder.hostID", getHostName());

      m_grinderIDIndex = command.size();
      command.add("");    // Place holder for grinder ID.

      if (m_alternateFile != null) {
        command.add(m_alternateFile.getPath());
      }

      m_commandArray = (String[])command.toArray(new String[0]);
    }

    public void startAllProcesses() {
      startSomeProcesses(m_processes.length - m_nextProcessIndex);
    }

    public boolean startSomeProcesses(int numberOfProcesses) {

      final int numberToStart =
        Math.min(numberOfProcesses, m_processes.length - m_nextProcessIndex);

      for (int i = 0; i < numberToStart; ++i) {
        final int processIndex = m_nextProcessIndex;

        final String grinderID = m_hostIDString + "-" + processIndex;
        m_commandArray[m_grinderIDIndex] = grinderID;

        try {
          m_processes[processIndex] =
            new ChildProcess(m_commandArray, System.out, System.err);

          final OutputStream processStdin =
            m_processes[processIndex].getStdinStream();

          new StreamSender(processStdin).send(m_initialiseMessage);

          m_fanOutStreamSender.add(processStdin);
        }
        catch (EngineException e) {
          System.err.println("Failed to create process");
          e.printStackTrace(System.err);
          return false;
        }
        catch (CommunicationException e) {
          System.err.println("Failed to communicate with process");
          e.printStackTrace(System.err);
          return false;
        }

        ++m_nextProcessIndex;

        final StringBuffer buffer =
          new StringBuffer(m_commandArray.length * 10);
        buffer.append("Worker processes (");
        buffer.append(grinderID);
        buffer.append(") started with command line:");

        for (int j = 0; j < m_commandArray.length; ++j) {
          buffer.append(" ");
          buffer.append(m_commandArray[j]);
        }

        System.out.println(buffer.toString());
      }

      return m_processes.length > m_nextProcessIndex;
    }

    public int waitForProcessesToFinish()
      throws EngineException, InterruptedException {

      int combinedExitStatus = 0;

      for (int i = 0; i < m_nextProcessIndex; i++) {
        final int exitStatus = m_processes[i].waitFor();

        if (exitStatus > 0) { // Not an error
          if (combinedExitStatus == 0) {
            combinedExitStatus = exitStatus;
          }
          else if (combinedExitStatus != exitStatus) {
            System.err.println(
              "WARNING, worker processes disagree on exit status");
          }
        }
      }

      return combinedExitStatus;
    }

    private String getHostName() {
      try {
        return InetAddress.getLocalHost().getHostName();
      }
      catch (UnknownHostException e) {
        return "UNNAMED HOST";
      }
    }
  }

  private class RampUpTimerTask extends TimerTask {

    private final ProcessStarter m_processStarter;
    private final int m_processIncrement;
    private boolean m_rampUpFinished = false;

    public RampUpTimerTask(ProcessStarter processStarter,
                           int processIncrement) {
      m_processStarter = processStarter;
      m_processIncrement = processIncrement;
    }

    public void run () {
      final boolean moreProcessesToStart =
        m_processStarter.startSomeProcesses(m_processIncrement);

      if (!moreProcessesToStart) {
        markFinished();
      }
    }

    private void markFinished() {
      super.cancel();

      synchronized (this) {
        m_rampUpFinished = true;
        notifyAll();
      }
    }

    public void waitUntilFinished() throws InterruptedException {
      synchronized (this) {
        while (!m_rampUpFinished) {
          wait();
        }
      }
    }
  }
}
