// Copyright (C) 2004, 2005 Philip Aston
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

package net.grinder.console.communication;

import java.io.File;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Timer;
import java.util.TimerTask;

import net.grinder.communication.ConnectionType;
import net.grinder.communication.HandlerChainSender.MessageHandler;
import net.grinder.communication.Message;
import net.grinder.console.common.ConsoleException;
import net.grinder.console.common.DisplayMessageConsoleException;
import net.grinder.console.common.ErrorHandler;
import net.grinder.console.common.Resources;
import net.grinder.console.messages.WorkerProcessReportMessage;
import net.grinder.console.model.ConsoleProperties;
import net.grinder.engine.agent.PublicAgentIdentityImplementation;
import net.grinder.engine.messages.ClearCacheMessage;
import net.grinder.engine.messages.DistributeFileMessage;
import net.grinder.engine.messages.ResetGrinderMessage;
import net.grinder.engine.messages.StartGrinderMessage;
import net.grinder.engine.messages.StopGrinderMessage;
import net.grinder.testutility.AbstractFileTestCase;
import net.grinder.testutility.RandomStubFactory;
import net.grinder.util.FileContents;


/**
 * Unit test case for {@link ConsoleControlImplementation}.
 *
 * @author Philip Aston
 * @version $Revision$
 */
public class TestConsoleCommunicationImplementation
  extends AbstractFileTestCase {

  private static final Resources s_resources =
      new Resources("net.grinder.console.swingui.resources.Console");

  private ConsoleCommunication m_consoleCommunication;
  private ConsoleProperties m_properties;
  private ServerSocket m_usedServerSocket;
  private final ProcessMessagesThread m_processMessagesThread =
    new ProcessMessagesThread();
  private final StubTimer m_timer = new StubTimer();

  protected void setUp() throws Exception {
    super.setUp();

    // Figure out a used and free local port.
    m_usedServerSocket = new ServerSocket(0);
    final ServerSocket freeServerSocket = new ServerSocket(0);
    freeServerSocket.close();

    final File file = new File(getDirectory(), "properties");
    m_properties = new ConsoleProperties(s_resources, file);

    m_properties.setConsolePort(freeServerSocket.getLocalPort());

    m_consoleCommunication =
      new ConsoleCommunicationImplementation(s_resources,
                                             m_properties,
                                             m_timer);
  }

  protected void tearDown() throws Exception {
    // Force existing communications objects to shut down.
    if (!m_processMessagesThread.isAlive()) {
      m_processMessagesThread.start();
    }

    // ProcessMessagesThread will process next message, then shutdown.
    m_processMessagesThread.shutdown();

    m_properties.setConsolePort(m_usedServerSocket.getLocalPort());

    m_usedServerSocket.close();

    m_timer.cancel();
  }

  public void testConstruction() throws Exception {
    assertNotNull(m_consoleCommunication.getProcessControl());
    assertNotNull(m_consoleCommunication.getDistributionControl());

    final TimerTask timerTask = m_timer.getLastScheduledTimerTask();
    timerTask.run();

    // Need a thread to be attempting to process messages or the
    // receiver will never be shutdown correctly.
    m_processMessagesThread.start();

    // Cause the sender to be invalid.
    m_properties.setConsolePort(m_usedServerSocket.getLocalPort());
    timerTask.run();

    final ConsoleCommunication consoleCommunicationWithNullSender =
      new ConsoleCommunicationImplementation(s_resources,
                                             m_properties,
                                             m_timer);

    final TimerTask timerTask2 = m_timer.getLastScheduledTimerTask();
    assertNotSame(timerTask, timerTask2);
    timerTask2.run();
  }

  private Message readMessage(Socket socket) throws Exception {
    final ObjectInputStream objectStream =
      new ObjectInputStream(socket.getInputStream());

    return (Message)objectStream.readObject();
  }

  private void sendMessage(Socket socket, Message message) throws Exception {
    final ObjectOutputStream objectStream =
      new ObjectOutputStream(socket.getOutputStream());

    objectStream.writeObject(message);
    objectStream.flush();
  }

  public void testProcessControl() throws Exception {
    final Socket socket =
      new Socket(InetAddress.getByName(null), m_properties.getConsolePort());
    ConnectionType.AGENT.write(socket.getOutputStream());

    final ProcessControl processControl =
      m_consoleCommunication.getProcessControl();

    final RandomStubFactory listenerStubFactory =
      new RandomStubFactory(ProcessStatus.Listener.class);
    final ProcessStatus.Listener listener =
      (ProcessStatus.Listener)listenerStubFactory.getStub();

    processControl.addProcessStatusListener(listener);

    processControl.resetWorkerProcesses();
    processControl.stopWorkerProcesses();

    assertTrue(readMessage(socket) instanceof ResetGrinderMessage);
    assertTrue(readMessage(socket) instanceof StopGrinderMessage);

    final File file = new File("foo");

    processControl.startWorkerProcesses(file);
    final StartGrinderMessage startGrinderMessage =
      (StartGrinderMessage)readMessage(socket);

    assertEquals(file, startGrinderMessage.getScriptFile());

    // This shouldn't call reset. If it does, we'll block because
    // nothing's processing the messages.
    m_properties.setIgnoreSampleCount(99);

    // Need a thread to be attempting to process messages or the
    // receiver will never be shutdown correctly.
    m_processMessagesThread.start();

    // Reset by changing properties and do another test.
    final ServerSocket freeServerSocket = new ServerSocket(0);
    freeServerSocket.close();
    m_properties.setConsolePort(freeServerSocket.getLocalPort());

    final Socket socket2 =
      new Socket(InetAddress.getByName(null), m_properties.getConsolePort());
    ConnectionType.AGENT.write(socket2.getOutputStream());

    processControl.resetWorkerProcesses();
    assertTrue(readMessage(socket2) instanceof ResetGrinderMessage);
  }

  public void testDistributionControl() throws Exception {
    final Socket socket =
      new Socket(InetAddress.getByName(null), m_properties.getConsolePort());
    ConnectionType.AGENT.write(socket.getOutputStream());

    final DistributionControl distributionControl =
      m_consoleCommunication.getDistributionControl();

    final Socket socket2 =
      new Socket(InetAddress.getByName(null), m_properties.getConsolePort());
    ConnectionType.AGENT.write(socket2.getOutputStream());

    // Closing the socket isn't enough for the ConsoleCommunication's
    // Sender to know we've gone, we need to send something too.
    socket2.close();

    distributionControl.clearFileCaches();

    for (int retry = 0;
         m_consoleCommunication.getProcessControl().getNumberOfConnectedAgents()
           != 1 && retry < 10;
         ++retry) {
      Thread.sleep(10);
    }

    assertTrue(readMessage(socket) instanceof ClearCacheMessage);

    final File relativePath = new File("foo");
    final File fullPath = new File(getDirectory(), relativePath.getPath());
    createRandomFile(fullPath);

    final FileContents fileContents =
      new FileContents(getDirectory(), relativePath);

    distributionControl.sendFile(fileContents);

    assertTrue(readMessage(socket) instanceof DistributeFileMessage);

    // Need a thread to be attempting to process messages or the
    // receiver will never be shutdown correctly.
    m_processMessagesThread.start();

    // Reset by changing properties and do another test.
    m_properties.setConsoleHost("localhost");

    final Socket socket3 =
      new Socket(InetAddress.getByName(null), m_properties.getConsolePort());
    ConnectionType.AGENT.write(socket3.getOutputStream());

    distributionControl.clearFileCaches();
    assertTrue(readMessage(socket3) instanceof ClearCacheMessage);
  }

  public void testProcessOneMessage() throws Exception {
    final MessageHandlerStubFactory messageHandlerStubFactory =
      new MessageHandlerStubFactory();

    m_consoleCommunication.addMessageHandler(
      messageHandlerStubFactory.getMessageHandler());

    m_processMessagesThread.start();

    final Socket socket =
      new Socket(InetAddress.getByName(null), m_properties.getConsolePort());
    ConnectionType.WORKER.write(socket.getOutputStream());

    sendMessage(
      socket,
      new WorkerProcessReportMessage(
        new PublicAgentIdentityImplementation("agent").createWorkerIdentity(),
        (short)0, (short)0, (short)0));

    sendMessage(socket, new MyMessage());

    for (int retry = 0;
         !messageHandlerStubFactory.hasBeenCalled() && retry < 100;
         ++retry) {
      Thread.sleep(10);
    }

    messageHandlerStubFactory.assertSuccess("process", MyMessage.class);

    // ConsoleCommunication should have handled the original
    // ReportStatusMessage. We check here so we're sure the
    // ReportStatusMessage has been processed.
    messageHandlerStubFactory.assertNoMoreCalls();

    sendMessage(socket, new StopGrinderMessage());

    for (int retry = 0;
         !messageHandlerStubFactory.hasBeenCalled() && retry < 100;
         ++retry) {
      Thread.sleep(10);
    }

    messageHandlerStubFactory.assertSuccess("process",
                                            StopGrinderMessage.class);
  }

  public void testErrorHandling() throws Exception {
    final RandomStubFactory errorHandlerStubFactory =
      new RandomStubFactory(ErrorHandler.class);
    final ErrorHandler errorHandler =
      (ErrorHandler)errorHandlerStubFactory.getStub();

    m_consoleCommunication.setErrorHandler(errorHandler);

    // Need a thread to be attempting to process messages or the
    // receiver will never be shutdown correctly.
    m_processMessagesThread.start();

    errorHandlerStubFactory.assertNoMoreCalls();

    m_properties.setConsolePort(m_usedServerSocket.getLocalPort());

    errorHandlerStubFactory.assertSuccess(
      "handleException", DisplayMessageConsoleException.class);
    errorHandlerStubFactory.assertNoMoreCalls();

    m_consoleCommunication.getDistributionControl().clearFileCaches();

    errorHandlerStubFactory.assertSuccess(
      "handleException", DisplayMessageConsoleException.class);
    errorHandlerStubFactory.assertNoMoreCalls();

    final RandomStubFactory errorHandlerStubFactory2 =
      new RandomStubFactory(ErrorHandler.class);
    final ErrorHandler errorHandler2 =
      (ErrorHandler)errorHandlerStubFactory2.getStub();

    // Test a ConsoleCommunication with an invalid Sender.
    m_properties.setConsolePort(m_usedServerSocket.getLocalPort());
    final ConsoleCommunication brokenConsoleCommunication =
      new ConsoleCommunicationImplementation(s_resources,
                                             m_properties,
                                             m_timer);

    errorHandlerStubFactory2.assertNoMoreCalls();
    brokenConsoleCommunication.setErrorHandler(errorHandler2);

    errorHandlerStubFactory2.assertSuccess(
      "handleException", DisplayMessageConsoleException.class);

    brokenConsoleCommunication.getDistributionControl().clearFileCaches();

    errorHandlerStubFactory2.assertSuccess(
      "handleResourceErrorMessage", String.class, String.class);
    errorHandlerStubFactory2.assertNoMoreCalls();
  }

  private static final class MyMessage implements Message, Serializable { }

  private static final class StubTimer extends Timer {
    private TimerTask m_lastScheduledTimerTask;

    public void schedule(TimerTask timerTask, long delay, long period) {
      m_lastScheduledTimerTask = timerTask;
    }

    public TimerTask getLastScheduledTimerTask() {
      return m_lastScheduledTimerTask;
    }
  }

  public static final class MessageHandlerStubFactory
    extends RandomStubFactory {

    public MessageHandlerStubFactory() {
      super(MessageHandler.class);
    }

    public boolean override_process(Object proxy, Message message) {
      return message instanceof MyMessage;
    }

    public MessageHandler getMessageHandler() {
      return (MessageHandler)getStub();
    }
  };

  private final class ProcessMessagesThread extends Thread {
    private boolean m_shutdown = false;

    public ProcessMessagesThread() {
      super("Process messages");
    }

    public void run() {
      try {
        while (true) {
          m_consoleCommunication.processOneMessage();

          synchronized (this) {
            if (m_shutdown) {
              break;
            }
          }
        }
      }
      catch (ConsoleException e) {
      }
    }

    public synchronized void shutdown() {
      m_shutdown = true;
    }
  }
}
