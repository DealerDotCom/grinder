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

package net.grinder.communication;

import java.net.InetAddress;
import java.net.ServerSocket;

import junit.framework.TestCase;


/**
 *  Abstract unit test cases for <code>Sender</code> and
 *  <code>Receiver</code> implementations..
 *
 * @author Philip Aston
 * @version $Revision$
 */
public abstract class AbstractSenderAndReceiverTests extends TestCase {

  private final boolean m_messagesNeedInitialising;
  private final String m_hostName;
  private final int m_port;

  protected Receiver m_receiver;
  protected Sender m_sender;

  private ExecuteThread m_executeThread;

  public AbstractSenderAndReceiverTests(String name) throws Exception {
    this(name, false);
  }

  public AbstractSenderAndReceiverTests(String name,
                                        boolean messagesNeedInitialising) 
    throws Exception {
    
    super(name);

    m_messagesNeedInitialising = messagesNeedInitialising;

    m_hostName = InetAddress.getByName(null).getHostName();

    // Find a free port.
    final ServerSocket socket = new ServerSocket(0);
    m_port = socket.getLocalPort();
    socket.close();
  }

  protected final String getHostName() {
    return m_hostName;
  }

  protected final int getPort() {
    return m_port;
  }

  protected void setUp() throws Exception {
    m_executeThread = new ExecuteThread();
  }

  protected void tearDown() throws Exception {
    m_executeThread.shutdown();
  }
  

  public void testSendSimpleMessage() throws Exception {

    final SimpleMessage sentMessage = new SimpleMessage(0);
    maybeInitialiseMessage(sentMessage);
    m_sender.send(sentMessage);

    final Message receivedMessage = m_executeThread.waitForMessage();
    assertEquals(sentMessage, receivedMessage);
    assertTrue(sentMessage.payloadEquals(receivedMessage));
    assertTrue(sentMessage != receivedMessage);
  }

  public void testSendManyMessages() throws Exception {
    long sequenceNumber = -1;

    for (int i=1; i<=10; ++i) {
      final SimpleMessage[] sentMessages = new SimpleMessage[i];

      for (int j=0; j<i; ++j) {
        sentMessages[j] = new SimpleMessage(i);
        maybeInitialiseMessage(sentMessages[j]);
        m_sender.send(sentMessages[j]);
      }

      for (int j=0; j<i; ++j) {
        final SimpleMessage receivedMessage =
          (SimpleMessage) m_executeThread.waitForMessage();

        if (sequenceNumber != -1) {
          assertEquals(sequenceNumber+1, receivedMessage.getSequenceNumber());
        }

        sequenceNumber = receivedMessage.getSequenceNumber();

        assertTrue(sentMessages[j].payloadEquals(receivedMessage));
        assertTrue(sentMessages[j] != receivedMessage);
      }
    }
  }

  public void testSendLargeMessage() throws Exception {
    // This causes a message size of about 38K. Should be limited by
    // the buffer size in Receiver.
    final SimpleMessage sentMessage = new SimpleMessage(8000);
    maybeInitialiseMessage(sentMessage);
    m_sender.send(sentMessage);

    final SimpleMessage receivedMessage =
      (SimpleMessage) m_executeThread.waitForMessage();

    assertEquals(sentMessage, receivedMessage);
    assertTrue(sentMessage.payloadEquals(receivedMessage));
    assertTrue(sentMessage != receivedMessage);
  }

  public void testShutdownReceiver() throws Exception {
    m_receiver.shutdown();
    assertNull(m_executeThread.waitForMessage());
  }

  public void testQueueAndFlush() throws Exception {

    final QueuedSender sender = new QueuedSenderDecorator(m_sender);

    long sequenceNumber = -1;

    final SimpleMessage[] messages = new SimpleMessage[25];

    for (int i=0; i<messages.length; ++i) {
      messages[i] = new SimpleMessage(0);
      maybeInitialiseMessage(messages[i]);
      sender.queue(messages[i]);
    }

    sender.flush();

    for (int i=0; i<messages.length; ++i) {
      final Message receivedMessage = m_executeThread.waitForMessage();

      if (sequenceNumber != -1) {
        assertEquals(sequenceNumber+1, receivedMessage.getSequenceNumber());
      }

      sequenceNumber = receivedMessage.getSequenceNumber();

      assertEquals(messages[i], receivedMessage);
      assertTrue(messages[i].payloadEquals(receivedMessage));
      assertTrue(messages[i] != receivedMessage);
    }
  }

  public void testQueueAndSend() throws Exception {

    final QueuedSender sender = new QueuedSenderDecorator(m_sender);

    long sequenceNumber = -1;

    final SimpleMessage[] messages = new SimpleMessage[25];

    for (int i=0; i<messages.length; ++i) {
      messages[i] = new SimpleMessage(0);
      maybeInitialiseMessage(messages[i]);
      sender.queue(messages[i]);
    }

    final SimpleMessage finalMessage = new SimpleMessage(0);
    maybeInitialiseMessage(finalMessage);
    sender.send(finalMessage);

    for (int i=0; i<messages.length; ++i) {
      final Message receivedMessage = m_executeThread.waitForMessage();

      if (sequenceNumber != -1) {
        assertEquals(sequenceNumber+1, receivedMessage.getSequenceNumber());
      }

      sequenceNumber = receivedMessage.getSequenceNumber();

      assertEquals(messages[i], receivedMessage);
      assertTrue(messages[i].payloadEquals(receivedMessage));
      assertTrue(messages[i] != receivedMessage);
    }

    final Message receivedFinalMessage = m_executeThread.waitForMessage();

    assertEquals(sequenceNumber+1, receivedFinalMessage.getSequenceNumber());
    assertEquals(finalMessage, receivedFinalMessage);
    assertTrue(finalMessage.payloadEquals(receivedFinalMessage));
    assertTrue(finalMessage != receivedFinalMessage);
  }

  /**
   * Pico-kernel! Need a long running thread because of the half-baked
   * PipedInputStream/PipedOutputStream thread checking.
   */
  private final class ExecuteThread extends Thread {

    private Action m_action;

    public ExecuteThread() {
      super("ExecuteThread");
      start();
    }

    public synchronized void run() {

      try {
        while (true) {
          while (m_action == null) {
            wait();
          }

          m_action.run();
          m_action = null;

          notifyAll();
        }
      }
      catch (InterruptedException e) {
      }
    }

    private synchronized Object execute(Action action) throws Exception {

      m_action = action;
      notifyAll();

      while (!action.getHasRun()) {
        wait();
      }

      return action.getResult();
    }

    public Message waitForMessage() throws Exception {
      return (Message) execute(
        new Action() {
          public Object doAction() throws Exception {
            return m_receiver.waitForMessage();
          }
        }
        );
    }

    public void shutdown() throws Exception {
      execute(
        new Action() {
          public Object doAction() throws Exception {
            throw new InterruptedException();
          }
        }
        );
    }    

    private abstract class Action {

      private Object m_result;
      private Exception m_exception;
      private boolean m_hasRun = false;

      public void run() throws InterruptedException {
        try {
          m_result = doAction();
        }
        catch (InterruptedException e) {
          throw e;
        }
        catch (Exception e) {
          m_exception = e;
        }
        finally {
          m_hasRun = true;
        }
      }

      public Object getResult() throws Exception {
        if (m_exception != null) {
          throw m_exception;
        }

        return m_result;
      }

      public boolean getHasRun() {
        return m_hasRun;
      }

      protected abstract Object doAction() throws Exception;
    }
  }
  
  private int m_sequenceNumber = 0;

  private void maybeInitialiseMessage(Message message) {
    if (m_messagesNeedInitialising) {
      message.setSenderInformation("Test", getClass().getName(),
                                   m_sequenceNumber++);
    }
  }
}
