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
import java.io.ObjectInputStream;
import java.io.StreamCorruptedException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Iterator;
import java.util.List;

import junit.framework.TestCase;


/**
 *  Unit tests for <code>ServerFanOutSender</code>.
 *
 * @author Philip Aston
 * @version $Revision$
 */
public class TestServerFanOutSender extends TestCase {

  public TestServerFanOutSender(String name) {
    super(name);
  }

  public void testBindTo() throws Exception {

    // Figure out a free local port.
    final ServerSocket serverSocket = new ServerSocket(0);
    final int port = serverSocket.getLocalPort();
    serverSocket.close();

    final ServerFanOutSender serverSender1 =
      ServerFanOutSender.bindTo("Test", "", port);

    final ServerFanOutSender serverSender2 =
      ServerFanOutSender.bindTo("Test", "", 0);

    serverSender1.shutdown();
    serverSender2.shutdown();
  }

  public void testSend() throws Exception {

    final ServerFanOutSender serverSender =
      ServerFanOutSender.bindTo("Test", "", 0);

    final Acceptor acceptor = serverSender.getAcceptor();

    final Socket[] socket = new Socket[5];

    for (int i=0; i<socket.length; ++i) {
      socket[i] = new Socket(InetAddress.getByName(null), acceptor.getPort());
    }

    // Sleep until we've accepted all connections. Give up after a few
    // seconds.
    for (int i=0;
         acceptor.getSocketSet().countActiveSockets() != 5 && i<10;
         ++i) {
      Thread.sleep(i * i * 10);
    }

    final SimpleMessage message1 = new SimpleMessage();
    final SimpleMessage message2 = new SimpleMessage();

    // Should be able to both route messages from other Senders and
    // originate their own.
    message1.setSenderInformation("Grinder ID", getClass().getName(), 1);

    serverSender.send(message1);
    serverSender.send(message2);

    assertEquals("Grinder ID", message1.getSenderGrinderID());
    assertEquals("Test", message2.getSenderGrinderID());

    for (int i=0; i<socket.length; ++i) {
      final InputStream socketInput = socket[i].getInputStream();

      final ObjectInputStream inputStream1 =
        new ObjectInputStream(socketInput);
      final Object o1 = inputStream1.readObject();

      final ObjectInputStream inputStream2 =
        new ObjectInputStream(socketInput);
      final Object o2 = inputStream2.readObject();

      assertEquals(message1, o1);
      assertTrue(message1.payloadEquals((Message) o1));

      assertEquals(message2, o2);
      assertTrue(message2.payloadEquals((Message) o2));

      assertEquals(0, socketInput.available());

      socket[i].close();
    }
    
    serverSender.shutdown();
  }

  public void testShutdown() throws Exception {

    final ServerFanOutSender serverSender =
      ServerFanOutSender.bindTo("Test", "", 0);

    final Acceptor acceptor = serverSender.getAcceptor();

    assertEquals(1, acceptor.getThreadGroup().activeCount());

    final Socket socket =
      new Socket(InetAddress.getByName(null), acceptor.getPort());

    // Sleep until we've accepted the connection. Give up after a few
    // seconds.
    for (int i=0;
         acceptor.getSocketSet().countActiveSockets() != 1 && i<10;
         ++i) {
      Thread.sleep(i * i * 10);
    }

    final Message message = new SimpleMessage();
    serverSender.send(message);

    final InputStream socketStream = socket.getInputStream();

    final ObjectInputStream inputStream1 =
      new ObjectInputStream(socketStream);
    final Object o1 = inputStream1.readObject();

    serverSender.shutdown();

    try {
      serverSender.send(message);
      fail("Expected CommunicationException");
    }
    catch (CommunicationException e) {
    }
    
    while (acceptor.getThreadGroup().activeCount() != 0) {
      Thread.sleep(10);
    }

    try {
      final ObjectInputStream inputStream2 =
        new ObjectInputStream(socketStream);
      final Object o2 = inputStream2.readObject();

      assertTrue(o2 instanceof CloseCommunicationMessage);
    }
    catch (StreamCorruptedException e) {
      // Occasionally this occurs because the connection is shutdown.
      // Whatever.
    }
  }
}
