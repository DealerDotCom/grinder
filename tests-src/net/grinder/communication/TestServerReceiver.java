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
import java.io.StreamCorruptedException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import junit.framework.TestCase;


/**
 *  Unit tests for <code>ServerReceiver</code>.
 *
 * @author Philip Aston
 * @version $Revision$
 */
public class TestServerReceiver extends TestCase {

  public TestServerReceiver(String name) {
    super(name);
  }

  public void testBindTo() throws Exception {

    // Figure out a free local port.
    final ServerSocket serverSocket = new ServerSocket(0);
    final int port = serverSocket.getLocalPort();
    serverSocket.close();

    final ServerReceiver serverReceiver1 = ServerReceiver.bindTo("", port);
    final ServerReceiver serverReceiver2 = ServerReceiver.bindTo("", 0);

    serverReceiver1.shutdown();
    serverReceiver2.shutdown();
  }

  public void testWaitForMessage() throws Exception {

    final ServerReceiver serverReceiver = ServerReceiver.bindTo("", 0);
    final Acceptor acceptor = serverReceiver.getAcceptor();

    final Socket[] socket = new Socket[5];

    for (int i=0; i<socket.length; ++i) {
      socket[i] = new Socket(InetAddress.getByName(null), acceptor.getPort());
    }

    // Sleep until we've accepted all connections. Give up after a few
    // seconds.
    for (int i=0; acceptor.getSocketSet().countActive() != 5 && i<10; ++i) {
      Thread.sleep(i * i * 10);
    }

    final SimpleMessage message1 = new SimpleMessage();
    final SimpleMessage message2 = new SimpleMessage();
    final SimpleMessage message3 = new SimpleMessage();

    final ObjectOutputStream objectStream1 =
      new ObjectOutputStream(socket[0].getOutputStream());
    objectStream1.writeObject(message1);
    objectStream1.flush();
        
    final ObjectOutputStream objectStream2 =
      new ObjectOutputStream(socket[1].getOutputStream());
    objectStream2.writeObject(message2);
    objectStream2.flush();

    final ObjectOutputStream objectStream3 =
      new ObjectOutputStream(socket[0].getOutputStream());
    objectStream3.writeObject(message3);
    objectStream3.flush();

    Message receivedMessage1 = serverReceiver.waitForMessage();
    Message receivedMessage2 = serverReceiver.waitForMessage();
    Message receivedMessage3 = serverReceiver.waitForMessage();

    assertNull(
      new BlockingActionThread() {
        protected void blockingAction() throws CommunicationException {
          serverReceiver.waitForMessage();
        }
      }.getException());

    if (receivedMessage1.equals(message2)) {
      final Message temp = receivedMessage2;
      receivedMessage2 = receivedMessage1;
      receivedMessage1 = temp;
    }
    else if (receivedMessage3.equals(message2)) {
      final Message temp = receivedMessage3;
      receivedMessage3 = receivedMessage2;
      receivedMessage2 = temp;
    }
    else {
      assertEquals(message2, receivedMessage2);
    }

    assertEquals(message1, receivedMessage1);
    assertEquals(message2, receivedMessage2);
    assertEquals(message3, receivedMessage3);

    serverReceiver.shutdown();
  }

  public void testShutdown() throws Exception {

    final ServerReceiver serverReceiver = ServerReceiver.bindTo("", 0);
    final Acceptor acceptor = serverReceiver.getAcceptor();

    assertEquals(1, acceptor.getThreadGroup().activeCount());
    assertEquals(5, serverReceiver.getThreadGroup().activeCount());

    final Socket socket =
      new Socket(InetAddress.getByName(null), acceptor.getPort());

    // Sleep until we've accepted the connection. Give up after a few
    // seconds.
    for (int i=0; acceptor.getSocketSet().countActive() != 1 && i<10; ++i) {
      Thread.sleep(i * i * 10);
    }

    final SimpleMessage message = new SimpleMessage();

    final ObjectOutputStream objectStream =
      new ObjectOutputStream(socket.getOutputStream());
    objectStream.writeObject(message);
    objectStream.flush();

    final Message receivedMessage = serverReceiver.waitForMessage();

    serverReceiver.shutdown();

    assertNull(serverReceiver.waitForMessage());
  }

  public void testCloseCommunicationMessage() throws Exception {

    final ServerReceiver serverReceiver = ServerReceiver.bindTo("", 0);
    final Acceptor acceptor = serverReceiver.getAcceptor();

    assertEquals(1, acceptor.getThreadGroup().activeCount());
    assertEquals(5, serverReceiver.getThreadGroup().activeCount());

    final Socket socket =
      new Socket(InetAddress.getByName(null), acceptor.getPort());

    // Sleep until we've accepted the connection. Give up after a few
    // seconds.
    for (int i=0; acceptor.getSocketSet().countActive() != 1 && i<10; ++i) {
      Thread.sleep(i * i * 10);
    }

    final SimpleMessage message = new SimpleMessage();

    final ObjectOutputStream objectStream1 =
      new ObjectOutputStream(socket.getOutputStream());
    objectStream1.writeObject(message);
    objectStream1.flush();

    final Message receivedMessage = serverReceiver.waitForMessage();

    final Message closeCommunicationMessage = new CloseCommunicationMessage();

    final ObjectOutputStream objectStream2 =
      new ObjectOutputStream(socket.getOutputStream());
    objectStream2.writeObject(closeCommunicationMessage);
    objectStream2.flush();

    // For a ServerReceiver, a CloseCommunicationMessage only closes
    // the individual connection.
    assertNull(
      new BlockingActionThread() {
        protected void blockingAction() throws CommunicationException {
          serverReceiver.waitForMessage();
        }
      }.getException());

    serverReceiver.shutdown();
  }
}
