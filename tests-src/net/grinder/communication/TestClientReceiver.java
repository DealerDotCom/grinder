// Copyright (C) 2003 Philip Aston
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

import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

import junit.framework.TestCase;


/**
 *  Unit test case for <code>ClientReceiver</code>.
 *
 * @author Philip Aston
 * @version $Revision$
 */
public class TestClientReceiver extends TestCase {

  public TestClientReceiver(String name) {
    super(name);
  }

  private static final class SendAMessage implements Runnable {

    private final OutputStream m_outputStream;
    private final Message m_message;
    private Exception m_exception;

    public SendAMessage(OutputStream outputStream, Message message) {
      m_outputStream = outputStream;
      m_message = message;
    }

    public void run() {
      try {
        final ObjectOutputStream objectStream = 
          new ObjectOutputStream(m_outputStream);
        objectStream.writeObject(m_message);
        objectStream.flush();
      }
      catch (Exception e) {
        m_exception = e;
      }
    }

    public Exception getException() {
      return m_exception;
    }
  }

  public void testReceiveWithStreams() throws Exception {

    final PipedOutputStream outputStream = new PipedOutputStream();
    final InputStream inputStream = new PipedInputStream(outputStream);

    final ClientReceiver clientReceiver = new ClientReceiver(inputStream);

    final SimpleMessage message1 = new SimpleMessage();
    message1.setSenderInformation("Test", getClass().getName(), 1);

    final ObjectOutputStream objectStream1 =
      new ObjectOutputStream(outputStream);
    objectStream1.writeObject(message1);
    objectStream1.flush();

    final SimpleMessage message2 = new SimpleMessage();
    message2.setSenderInformation("Test", getClass().getName(), 2);

    final ObjectOutputStream objectStream2 =
      new ObjectOutputStream(outputStream);
    objectStream2.writeObject(message2);
    objectStream2.flush();

    final Message receivedMessage1 = clientReceiver.waitForMessage();
    final Message receivedMessage2 = clientReceiver.waitForMessage();

    assertEquals(message1, receivedMessage1);
    assertTrue(message1.payloadEquals(receivedMessage1));
    assertEquals(message2, receivedMessage2);
    assertTrue(message2.payloadEquals(receivedMessage2));

    assertTrue(new BlockingActionThread() {
        protected void blockingAction() throws CommunicationException {
          clientReceiver.waitForMessage();
        }
      }.getException() instanceof CommunicationException);

    outputStream.close();
    
    try {
      clientReceiver.waitForMessage();
      fail("Expected CommunicationException");
    }
    catch (CommunicationException e) {
    }
  }

  public void testReceiveWithSockets() throws Exception {

    final SocketAcceptorThread socketAcceptor = new SocketAcceptorThread();

    final ClientReceiver clientReceiver =
      ClientReceiver.connectTo(
        socketAcceptor.getHostName(), socketAcceptor.getPort());

    socketAcceptor.join();

    final OutputStream socketOutput =
      socketAcceptor.getAcceptedSocket().getOutputStream();

    final SimpleMessage message1 = new SimpleMessage();
    message1.setSenderInformation("Test", getClass().getName(), 1);
    
    final ObjectOutputStream objectStream1 =
      new ObjectOutputStream(socketOutput);
    objectStream1.writeObject(message1);
    objectStream1.flush();

    final SimpleMessage message2 = new SimpleMessage();
    message2.setSenderInformation("Test", getClass().getName(), 2);

    final ObjectOutputStream objectStream2 =
      new ObjectOutputStream(socketOutput);
    objectStream2.writeObject(message2);
    objectStream2.flush();

    final Message receivedMessage1 = clientReceiver.waitForMessage();
    final Message receivedMessage2 = clientReceiver.waitForMessage();

    assertEquals(message1, receivedMessage1);
    assertTrue(message1.payloadEquals(receivedMessage1));
    assertEquals(message2, receivedMessage2);
    assertTrue(message2.payloadEquals(receivedMessage2));

    socketAcceptor.close();

    try {
      ClientReceiver.connectTo(
        socketAcceptor.getHostName(), socketAcceptor.getPort());

      fail("Expected CommunicationException");
    }
    catch (CommunicationException e) {
    }
  }

  public void testShutdown() throws Exception {

    final PipedOutputStream outputStream = new PipedOutputStream();
    final InputStream inputStream = new PipedInputStream(outputStream);

    final ClientReceiver clientReceiver = new ClientReceiver(inputStream);

    final SimpleMessage message = new SimpleMessage();
    message.setSenderInformation("Test", getClass().getName(), 1);

    final ObjectOutputStream objectStream =
      new ObjectOutputStream(outputStream);
    objectStream.writeObject(message);
    objectStream.flush();

    final Message receivedMessage = clientReceiver.waitForMessage();

    clientReceiver.shutdown();

    try {
      clientReceiver.waitForMessage();
      fail("Expected CommunicationException");
    }
    catch (CommunicationException e) {
    }
  }

  public void testCloseCommunicationMessage() throws Exception {

    final PipedOutputStream outputStream = new PipedOutputStream();
    final InputStream inputStream = new PipedInputStream(outputStream);

    final ClientReceiver clientReceiver = new ClientReceiver(inputStream);

    final SimpleMessage message = new SimpleMessage();
    message.setSenderInformation("Test", getClass().getName(), 1);

    final ObjectOutputStream objectStream1 =
      new ObjectOutputStream(outputStream);
    objectStream1.writeObject(message);
    objectStream1.flush();

    final Message receivedMessage = clientReceiver.waitForMessage();

    final Message closeCommunicationMessage = new CloseCommunicationMessage();
    closeCommunicationMessage.setSenderInformation(
      "Test", getClass().getName(), 1);

    final ObjectOutputStream objectStream2 =
      new ObjectOutputStream(outputStream);
    objectStream2.writeObject(closeCommunicationMessage);
    objectStream2.flush();

    assertNull(clientReceiver.waitForMessage());
  }
}
  
