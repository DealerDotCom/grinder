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

package net.grinder.engine.agent;

import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import net.grinder.common.GrinderBuild;
import net.grinder.common.GrinderException;
import net.grinder.common.GrinderProperties;
import net.grinder.communication.ClientReceiver;
import net.grinder.communication.CommunicationDefaults;
import net.grinder.communication.CommunicationException;
import net.grinder.communication.ConnectionType;
import net.grinder.communication.Connector;
import net.grinder.communication.FanOutStreamSender;
import net.grinder.communication.MessagePump;
import net.grinder.communication.Receiver;
import net.grinder.engine.process.GrinderProcess;


/**
 * This is the entry point of The Grinder agent process.
 *
 * @author Paco Gomez
 * @author Philip Aston
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
    boolean ignoreInitialSignal = false;

    while (true) {
      final GrinderProperties properties =
        new GrinderProperties(m_alternateFile);

      // TODO.
      // - How to cope with no Receiver?
      Receiver receiver = null;

      if (properties.getBoolean("grinder.useConsole", true)) {
        final String consoleAddress =
          properties.getProperty("grinder.consoleAddress",
                                 CommunicationDefaults.CONSOLE_ADDRESS);

        final int consolePort =
          properties.getInt("grinder.consolePort",
                            CommunicationDefaults.CONSOLE_PORT);

        final Connector connector =
          new Connector(consoleAddress, consolePort, ConnectionType.CONTROL);

        try {
          receiver = ClientReceiver.connect(connector);
        }
        catch (CommunicationException e) {
          System.out.println(
            "Unable to connect to console (" + e.getMessage() +
            "); proceeding without the console. Set " +
            "grinder.useConsole=false to disable this warning.");
        }
      }

      final FanOutStreamSender fanOutStreamSender = new FanOutStreamSender(3);
      final MessagePump messagePump =
        new MessagePump(receiver, fanOutStreamSender, 1);

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

      if (ignoreInitialSignal) {
        command.add(
          "-D" + GrinderProcess.DONT_WAIT_FOR_SIGNAL_PROPERTY_NAME + "=true");
      }

      command.add(GrinderProcess.class.getName());

      final String hostIDString =
        properties.getProperty("grinder.hostID", getHostName());

      final int grinderIDIndex = command.size();
      command.add("");    // Place holder for grinder ID.

      if (m_alternateFile != null) {
        command.add(m_alternateFile.getPath());
      }

      final int numberOfProcesses = properties.getInt("grinder.processes", 1);

      final LauncherThread[] threads = new LauncherThread[numberOfProcesses];

      final String[] stringArray = new String[0];

      for (int i = 0; i < numberOfProcesses; i++) {
        final String grinderID = hostIDString + "-" + i;

        final String[] commandArray = (String[])command.toArray(stringArray);

        commandArray[grinderIDIndex] = grinderID;

        threads[i] = new LauncherThread(grinderID, commandArray);
        threads[i].start();

        fanOutStreamSender.add(threads[i].getOutputStream());
      }

      final String version = GrinderBuild.getVersionString();

      System.out.println("The Grinder version " + version + " started");

      int combinedExitStatus = 0;

      for (int i = 0; i < numberOfProcesses; ++i) {
        threads[i].join();

        final int exitStatus = threads[i].getExitStatus();

        if (exitStatus > 0) { // Not an error
          if (combinedExitStatus == 0) {
            combinedExitStatus = exitStatus;
          }
          else if (combinedExitStatus != exitStatus) {
            System.out.println("WARNING, threads disagree on exit status");
          }
        }
      }

      messagePump.shutdown();

      System.out.println("The Grinder version " + version + " finished");

      if (combinedExitStatus == GrinderProcess.EXIT_START_SIGNAL) {
        System.out.println("Start signal received");
        ignoreInitialSignal = true;
      }
      else if (combinedExitStatus == GrinderProcess.EXIT_RESET_SIGNAL) {
        System.out.println("Reset signal received");
        ignoreInitialSignal = false;
      }
      else {
        break;
      }

      System.out.println();
    }
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

