// Copyright (C) 2004 - 2008 Philip Aston
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
// COPYRIGHT HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
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

import net.grinder.common.GrinderProperties;
import net.grinder.common.UncheckedInterruptedException;
import net.grinder.communication.CommunicationException;
import net.grinder.communication.ConnectionType;
import net.grinder.communication.Message;
import net.grinder.communication.Sender;
import net.grinder.console.common.DisplayMessageConsoleException;
import net.grinder.console.common.ErrorHandler;
import net.grinder.console.common.Resources;
import net.grinder.console.common.ResourcesImplementation;
import net.grinder.console.messages.AgentProcessReportMessage;
import net.grinder.console.messages.WorkerProcessReportMessage;
import net.grinder.console.model.ConsoleProperties;
import net.grinder.engine.agent.StubAgentIdentity;
import net.grinder.engine.messages.ClearCacheMessage;
import net.grinder.engine.messages.DistributeFileMessage;
import net.grinder.engine.messages.ResetGrinderMessage;
import net.grinder.engine.messages.StartGrinderMessage;
import net.grinder.engine.messages.StopGrinderMessage;
import net.grinder.testutility.AbstractFileTestCase;
import net.grinder.testutility.RandomStubFactory;
import net.grinder.util.FileContents;


/**
 * Unit test case for {@link ConsoleCommunicationImplementation}. Also tests
 * {@link ProcessControlImplementation} and
 * {@link DistributionControlImplementation}.
 *
 * @author Philip Aston
 * @version $Revision$
 */
public class TestConsoleCommunicationImplementation
  extends AbstractFileTestCase {

  private static final Resources s_resources =
      new ResourcesImplementation("net.grinder.console.swingui.resources.Console");

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
                                             m_timer,
                                             10);
  }

  protected void tearDown() throws Exception {
    super.tearDown();

    // Force existing communications objects to shut down.
    if (!m_processMessagesThread.isAlive()) {
      m_processMessagesThread.start();
    }

    // We need another server socket to ensure setting the console port is not
    // a no-op.
    final ServerSocket anotherUsedSocket = new ServerSocket(0);
    m_usedServerSocket.close();

    m_properties.setConsolePort(anotherUsedSocket.getLocalPort());

    anotherUsedSocket.close();

    m_processMessagesThread.interrupt();
    m_processMessagesThread.join();

    m_timer.cancel();

    waitForNumberOfConnections(0);
  }

  public void testConstruction() throws Exception {
    final TimerTask timerTask = m_timer.getLastScheduledTimerTask();
    timerTask.run();

    // Need a thread to be attempting to process messages or
    // ConsoleCommunicationImplementation.reset() will not complete.
    m_processMessagesThread.start();

    // Cause the sender to be invalid.
    m_properties.setConsolePort(m_usedServerSocket.getLocalPort());
    timerTask.run();

    final ConsoleCommunicationImplementation consoleCommunication =
      new ConsoleCommunicationImplementation(s_resources,
                                             m_properties,
                                             m_timer,
                                             500);

    final TimerTask timerTask2 = m_timer.getLastScheduledTimerTask();
    assertNotSame(timerTask, timerTask2);
    timerTask2.run();

    assertEquals(0, consoleCommunication.getNumberOfConnections());

    final ConsoleCommunicationImplementation consoleCommunication2 =
      new ConsoleCommunicationImplementation(s_resources,
                                             m_properties,
                                             m_timer);

    final TimerTask timerTask3 = m_timer.getLastScheduledTimerTask();
    assertNotSame(timerTask, timerTask3);
    timerTask3.run();

    assertEquals(0, consoleCommunication2.getNumberOfConnections());
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

  public void testWithProcessControl() throws Exception {
    final Socket socket =
      new Socket(InetAddress.getByName(null), m_properties.getConsolePort());
    ConnectionType.AGENT.write(socket.getOutputStream());

    waitForNumberOfConnections(1);

    final ProcessControl processControl =
      new ProcessControlImplementation(m_timer, m_consoleCommunication);

    final RandomStubFactory listenerStubFactory =
      new RandomStubFactory(ProcessStatus.Listener.class);
    final ProcessStatus.Listener listener =
      (ProcessStatus.Listener)listenerStubFactory.getStub();

    processControl.addProcessStatusListener(listener);

    processControl.resetWorkerProcesses();
    processControl.stopAgentAndWorkerProcesses();

    assertTrue(readMessage(socket) instanceof ResetGrinderMessage);
    assertTrue(readMessage(socket) instanceof StopGrinderMessage);

    final GrinderProperties properties = new GrinderProperties();
    properties.setProperty("foo", "bah");

    processControl.startWorkerProcesses(properties);
    final StartGrinderMessage startGrinderMessage =
      (StartGrinderMessage)readMessage(socket);

    assertEquals(properties, startGrinderMessage.getProperties());

    processControl.startWorkerProcesses(null);
    final StartGrinderMessage startGrinderMessage2 =
      (StartGrinderMessage)readMessage(socket);
    assertEquals(0, startGrinderMessage2.getProperties().size());

    // This shouldn't call reset. If it does, we'll block because
    // nothing's processing the messages.
    m_properties.setIgnoreSampleCount(99);

    // Need a thread to be attempting to process messages or
    // ConsoleCommunicationImplementation.reset() will not complete.
    m_processMessagesThread.start();

    // Reset by changing properties and do another test.
    final ServerSocket freeServerSocket = new ServerSocket(0);
    freeServerSocket.close();
    m_properties.setConsolePort(freeServerSocket.getLocalPort());

    // Changing the port drops the existing connections.
    waitForNumberOfConnections(0);

    final Socket socket2 =
      new Socket(InetAddress.getByName(null), m_properties.getConsolePort());
    ConnectionType.AGENT.write(socket2.getOutputStream());

    // Make sure something is listening to our new connection.
    waitForNumberOfConnections(1);

    processControl.resetWorkerProcesses();

    assertTrue(readMessage(socket2) instanceof ResetGrinderMessage);
  }

  public void testDistributionControl() throws Exception {
    final Socket socket = new Socket(InetAddress.getByName(null), m_properties
        .getConsolePort());
    ConnectionType.AGENT.write(socket.getOutputStream());

    final DistributionControl distributionControl =
      new DistributionControlImplementation(m_consoleCommunication);

    final Socket socket2 = new Socket(InetAddress.getByName(null), m_properties
        .getConsolePort());
    ConnectionType.AGENT.write(socket2.getOutputStream());

    waitForNumberOfConnections(2);

    socket2.close();

    // Closing the socket isn't enough for the ConsoleCommunication's Sender to
    // know we've gone (and so close its end of the connection); we need to send
    // something too.
    // Sadly it appears we sometimes need to chuck more than one message the
    // socket before it figures out the other end is stuffed.
    int n = 0;

    while (m_consoleCommunication.getNumberOfConnections() != 1) {
      distributionControl.clearFileCaches();
      ++n;
      assertTrue(n < 10);
    }

    for (int i = 0; i < n; ++i) {
      assertTrue(readMessage(socket) instanceof ClearCacheMessage);
    }

    final File relativePath = new File("foo");
    final File fullPath = new File(getDirectory(), relativePath.getPath());
    createRandomFile(fullPath);

    final FileContents fileContents = new FileContents(getDirectory(),
      relativePath);

    distributionControl.sendFile(fileContents);

    assertTrue(readMessage(socket) instanceof DistributeFileMessage);
    socket.close();

    // Need a thread to be attempting to process messages or
    // ConsoleCommunicationImplementation.reset() will not complete.
    m_processMessagesThread.start();

    // Reset by changing properties and do another test.
    m_properties.setConsoleHost("localhost");

    // Reseting the properties should ditch the existing connections.
    waitForNumberOfConnections(0);

    final Socket socket3 = new Socket(InetAddress.getByName(null), m_properties
        .getConsolePort());
    ConnectionType.AGENT.write(socket3.getOutputStream());

    waitForNumberOfConnections(1);

    distributionControl.clearFileCaches();
    assertTrue(readMessage(socket3) instanceof ClearCacheMessage);
  }

  /**
   * Connections are accepted by separate threads so we need to spin a while.
   * @param n - Wait until there are this numnber of accepted connections.
   * @throws InterruptedException
   */
  private void waitForNumberOfConnections(int n) throws InterruptedException {
    for (int retry = 0;
         m_consoleCommunication.getNumberOfConnections() != n && retry < 200;
         ++retry) {
      Thread.sleep(10);
    }

    assertEquals(n, m_consoleCommunication.getNumberOfConnections());
  }

  public void testProcessOneMessage() throws Exception {
    final MessageHandlerStubFactory messageHandlerStubFactory =
      new MessageHandlerStubFactory();

    m_consoleCommunication.getMessageDispatchRegistry().addFallback(
      messageHandlerStubFactory.getMessageHandler());

    m_processMessagesThread.start();

    final ProcessControl processControl =
      new ProcessControlImplementation(m_timer, m_consoleCommunication);

    assertEquals(0, processControl.getNumberOfLiveAgents());

    final Socket socket =
      new Socket(InetAddress.getByName(null), m_properties.getConsolePort());
    ConnectionType.WORKER.write(socket.getOutputStream());

    final StubAgentIdentity agentIdentity =
      new StubAgentIdentity("agent");

    // We can currently send agent messages over a worker channel.
    sendMessage(socket, new AgentProcessReportMessage(agentIdentity, (short)0));

    sendMessage(
      socket,
      new WorkerProcessReportMessage(agentIdentity.createWorkerIdentity(),
                                     (short)0,
                                     (short)0,
                                     (short)0));

    sendMessage(socket, new MyMessage());

    messageHandlerStubFactory.waitUntilCalled(10000);

    messageHandlerStubFactory.assertSuccess("send", MyMessage.class);

    assertEquals(1, processControl.getNumberOfLiveAgents());

    // ConsoleCommunication should have handled the original
    // AgentProcessReportMessage and WorkerProcessReportMessage. We check here
    // so we're sure the've been processed.
    messageHandlerStubFactory.assertNoMoreCalls();

    sendMessage(socket, new StopGrinderMessage());

    messageHandlerStubFactory.waitUntilCalled(10000);

    messageHandlerStubFactory.assertSuccess("send",
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

    new DistributionControlImplementation(m_consoleCommunication)
    .clearFileCaches();

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
                                             m_timer,
                                             100);

    errorHandlerStubFactory2.assertNoMoreCalls();
    brokenConsoleCommunication.setErrorHandler(errorHandler2);

    errorHandlerStubFactory2.assertSuccess(
      "handleException", DisplayMessageConsoleException.class);

    new DistributionControlImplementation(brokenConsoleCommunication)
    .clearFileCaches();

    errorHandlerStubFactory2.assertSuccess("handleErrorMessage", String.class);
    errorHandlerStubFactory2.assertNoMoreCalls();
  }

  public void testErrorHandlingWithFurtherCommunicationProblems()
    throws Exception {

    final RandomStubFactory errorHandlerStubFactory =
      new RandomStubFactory(ErrorHandler.class);
    final ErrorHandler errorHandler =
      (ErrorHandler)errorHandlerStubFactory.getStub();

    m_consoleCommunication.setErrorHandler(errorHandler);

    final ServerSocket freeServerSocket = new ServerSocket(0);
    freeServerSocket.close();

    // Need a thread to be attempting to process messages or
    // ConsoleCommunicationImplementation.reset() will not complete.
    m_processMessagesThread.start();

    m_properties.setConsolePort(freeServerSocket.getLocalPort());

    final Socket socket = new Socket(freeServerSocket.getInetAddress(),
                                     freeServerSocket.getLocalPort());

    socket.getOutputStream().close();

    // Will be called via the Acceptor problem listener.
    errorHandlerStubFactory.waitUntilCalled(1000);

    errorHandlerStubFactory.assertSuccess("handleException",
                                          CommunicationException.class);
    errorHandlerStubFactory.assertNoMoreCalls();

    final Socket socket2 = new Socket(freeServerSocket.getInetAddress(),
                                      freeServerSocket.getLocalPort());
    ConnectionType.AGENT.write(socket2.getOutputStream());

    socket2.getOutputStream().write(new byte[100]);

    errorHandlerStubFactory.waitUntilCalled(1000);

    errorHandlerStubFactory.assertSuccess("handleException",
                                          CommunicationException.class);
    socket.close();
    socket2.close();

    errorHandlerStubFactory.assertNoMoreCalls();
  }

  private static final class MyMessage implements Message, Serializable {
    private static final long serialVersionUID = 1L;
  }

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
      super(Sender.class);
    }

    public boolean override_process(Object proxy, Message message) {
      return message instanceof MyMessage;
    }

    public Sender getMessageHandler() {
      return (Sender)getStub();
    }
  }

  private final class ProcessMessagesThread extends Thread {
    public ProcessMessagesThread() {
      super("Process messages");
    }

    public void run() {
      try {
        while (true) {
          m_consoleCommunication.processOneMessage();
        }
      }
      catch (UncheckedInterruptedException e) {
        // Time to go.
      }
    }
  }
}
