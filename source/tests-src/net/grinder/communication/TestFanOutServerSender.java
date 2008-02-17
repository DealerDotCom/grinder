// Copyright (C) 2003 - 2008 Philip Aston
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

package net.grinder.communication;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.StreamCorruptedException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Iterator;

import junit.framework.TestCase;
import net.grinder.communication.ResourcePool.Reservation;


/**
 *  Unit tests for <code>FanOutServerSender</code>.
 *
 * @author Philip Aston
 * @version $Revision$
 */
public class TestFanOutServerSender extends TestCase {

  public TestFanOutServerSender(String name) {
    super(name);
  }

  public void testConstructor() throws Exception {

    final Acceptor acceptor = new Acceptor("localhost", 0, 1);

    final FanOutServerSender serverSender =
      new FanOutServerSender(acceptor, ConnectionType.AGENT, 3);

    serverSender.shutdown();
    acceptor.shutdown();
  }

  public void testSend() throws Exception {

    final Acceptor acceptor = new Acceptor("localhost", 0, 1);

    final FanOutServerSender serverSender =
      new FanOutServerSender(acceptor, ConnectionType.AGENT, 3);

    final Socket[] socket = new Socket[5];

    for (int i=0; i<socket.length; ++i) {
      socket[i] = new Socket(InetAddress.getByName(null), acceptor.getPort());
      ConnectionType.AGENT.write(socket[i].getOutputStream());
    }

    // Sleep until we've accepted all connections. Give up after a few
    // seconds.
    final ResourcePool socketSet =
      acceptor.getSocketSet(ConnectionType.AGENT);

    for (int i=0; socketSet.countActive() != 5 && i<10; ++i) {
      Thread.sleep(i * i * 10);
    }

    final SimpleMessage message1 = new SimpleMessage();
    final SimpleMessage message2 = new SimpleMessage();

    serverSender.send(message1);
    serverSender.send(message2);

    for (int i=0; i<socket.length; ++i) {
      final InputStream socketInput = socket[i].getInputStream();

      final Message m1 = readMessage(socketInput);
      final Message m2 = readMessage(socketInput);

      assertEquals(message1, m1);
      assertEquals(message2, m2);

      assertEquals(0, socketInput.available());

      socket[i].close();
    }

    serverSender.shutdown();
    acceptor.shutdown();
  }

  private static Message readMessage(final InputStream socketInput)
    throws IOException, ClassNotFoundException {
    final ObjectInputStream inputStream =
      new ObjectInputStream(socketInput);
    return (Message)inputStream.readObject();
  }

  public void testSendAddressedMessage() throws Exception {

    final Acceptor acceptor = new Acceptor("localhost", 0, 1);

    final FanOutServerSender serverSender =
      new FanOutServerSender(acceptor, ConnectionType.AGENT, 3);

    final Socket[] socket = new Socket[5];

    for (int i = 0; i < socket.length; ++i) {
      socket[i] = new Socket(InetAddress.getByName(null), acceptor.getPort());
      ConnectionType.AGENT.write(socket[i].getOutputStream());
    }

    // Sleep until we've accepted all connections. Give up after a few
    // seconds.
    final ResourcePool socketSet =
      acceptor.getSocketSet(ConnectionType.AGENT);

    for (int i = 0; socketSet.countActive() != 5 && i < 10; ++i) {
      Thread.sleep(i * i * 10);
    }

    final Iterator iterator = socketSet.reserveAll().iterator();

    while (iterator.hasNext()) {
      final Reservation reservation = (Reservation)iterator.next();
      final SocketWrapper socketWrapper =
        (SocketWrapper)reservation.getResource();
      socketWrapper.setAddress(
        socketWrapper.getSocket().getRemoteSocketAddress());
      reservation.free();
    }

    final SimpleMessage message1 = new SimpleMessage();
    final SimpleMessage message2 = new SimpleMessage();

    serverSender.send(
      new SimpleAddressedMessage(socket[1].getLocalSocketAddress(), message1));
    serverSender.send(
      new SimpleAddressedMessage(socket[2].getLocalSocketAddress(), message2));

    for (int i = 0; i < socket.length; ++i) {
      final InputStream socketInput = socket[i].getInputStream();

      if (i == 1) {
        final Message m = readMessage(socketInput);
        assertEquals(message1, m);
      }
      else if (i == 2) {
        final Message m = readMessage(socketInput);
        assertEquals(message2, m);
      }

      assertEquals(0, socketInput.available());

      socket[i].close();
    }

    serverSender.shutdown();

    try {
      serverSender.send(new SimpleAddressedMessage(message1, message1));
      fail("Expected CommunicationException");
    }
    catch (CommunicationException e) {
    }

    acceptor.shutdown();
  }

  public void testShutdown() throws Exception {

    final Acceptor acceptor = new Acceptor("localhost", 0, 1);

    final FanOutServerSender serverSender =
      new FanOutServerSender(acceptor, ConnectionType.AGENT, 3);

    assertEquals(1, acceptor.getThreadGroup().activeCount());

    final Socket socket =
      new Socket(InetAddress.getByName(null), acceptor.getPort());

    ConnectionType.AGENT.write(socket.getOutputStream());

    // Sleep until we've accepted the connection. Give up after a few
    // seconds.
    final ResourcePool socketSet =
      acceptor.getSocketSet(ConnectionType.AGENT);

    for (int i=0; socketSet.countActive() != 1 && i<10; ++i) {
      Thread.sleep(i * i * 10);
    }

    final Message message = new SimpleMessage();
    serverSender.send(message);

    final InputStream socketStream = socket.getInputStream();

    final Object o1 = readMessage(socketStream);
    assertNotNull(o1);

    serverSender.shutdown();

    try {
      serverSender.send(message);
      fail("Expected CommunicationException");
    }
    catch (CommunicationException e) {
    }

    try {
      final Object o2 = readMessage(socketStream);

      assertTrue(o2 instanceof CloseCommunicationMessage);
    }
    catch (StreamCorruptedException e) {
      // Occasionally this occurs because the connection is shutdown.
      // Whatever.
    }

    acceptor.shutdown();
  }

  public void testIsPeerShutdown() throws Exception {

    final Acceptor acceptor = new Acceptor("localhost", 0, 1);

    final FanOutServerSender serverSender =
      new FanOutServerSender(acceptor, ConnectionType.AGENT, 3);

    final Socket socket =
      new Socket(InetAddress.getByName(null), acceptor.getPort());

    ConnectionType.AGENT.write(socket.getOutputStream());

    // Use a second socket to get cover freeing of other Reservations in
    // isPeerShutdown.
    final Socket socket2 =
      new Socket(InetAddress.getByName(null), acceptor.getPort());

    ConnectionType.AGENT.write(socket2.getOutputStream());

    // Sleep until we've accepted the connections. Give up after a few
    // seconds.
    final ResourcePool socketSet =
      acceptor.getSocketSet(ConnectionType.AGENT);

    for (int i=0; socketSet.countActive() != 2 && i<10; ++i) {
      Thread.sleep(i * i * 10);
    }

    assertTrue(!serverSender.isPeerShutdown());

    final Message message = new SimpleMessage();
    serverSender.send(message);

    assertTrue(!serverSender.isPeerShutdown());

    new SocketWrapper(socket2).close();

    assertTrue(serverSender.isPeerShutdown());

    serverSender.shutdown();
    acceptor.shutdown();
  }
}
