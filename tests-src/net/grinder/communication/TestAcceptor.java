// Copyright (C) 2003, 2004 Philip Aston
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
import java.net.Socket;
import java.util.Iterator;
import java.util.List;

import junit.framework.TestCase;


/**
 *  Unit tests for <code>Acceptor</code>.
 *
 * @author Philip Aston
 * @version $Revision$
 */
public class TestAcceptor extends TestCase {

  public TestAcceptor(String name) {
    super(name);
  }

  public void testConstructor() throws Exception {

    final InetAddress[] localAddresses =
      InetAddress.getAllByName(InetAddress.getLocalHost().getHostName());

    final String[] testAddresses = new String[localAddresses.length + 2];

    // Loop back.
    testAddresses[0] = InetAddress.getByName(null).getHostName();

    // All addresses.
    testAddresses[1] = "";

    for (int i=0; i<localAddresses.length; ++i) {
      testAddresses[i + 2] = localAddresses[i].getHostName();
    }

    // Figure out a free local port.
    final ServerSocket serverSocket = new ServerSocket(0);
    final int port = serverSocket.getLocalPort();
    serverSocket.close();

    for (int i=0; i<testAddresses.length; ++i) {
      final Acceptor acceptor = new Acceptor(testAddresses[i], port, 2);
      assertEquals(port, acceptor.getPort());
      assertNull(acceptor.getPendingException(false));
      acceptor.shutdown();

      // Should also be able to use a Random port.
      final Acceptor acceptor2 = new Acceptor(testAddresses[i], 0, 2);
      assertEquals(port, acceptor.getPort());
      assertNull(acceptor2.getPendingException(false));
      acceptor2.shutdown();
    }
  }

  public void testGetSocketSet() throws Exception {

    final Acceptor acceptor = createAcceptor(2);

    final ResourcePool controlSocketSet =
      acceptor.getSocketSet(ConnectionType.CONTROL);

    assertNotNull(controlSocketSet);
    assertTrue(controlSocketSet.reserveNext().isSentinel());

    final Connector controlConnector =
      new Connector("localhost", acceptor.getPort(), ConnectionType.CONTROL);

    final Connector reportConnector =
      new Connector("localhost", acceptor.getPort(), ConnectionType.REPORT);

    controlConnector.connect();
    controlConnector.connect();
    reportConnector.connect();

    // Sleep until we've accepted both control connections. Give up
    // after a few seconds.
    for (int i=0; controlSocketSet.countActive() != 2 && i<10; ++i) {
      Thread.sleep(i * i * 10);
    }
    
    assertSame(controlSocketSet,
               acceptor.getSocketSet(ConnectionType.CONTROL));

    final List controlSocketResources = controlSocketSet.reserveAll();
    assertEquals(2, controlSocketResources.size());

    // Now do a similar checks with report socket set.
    final ResourcePool reportSocketSet =
      acceptor.getSocketSet(ConnectionType.REPORT);

    for (int i=0; reportSocketSet.countActive() != 1 && i<10; ++i) {
      Thread.sleep(i * i * 10);
    }

    assertSame(reportSocketSet, acceptor.getSocketSet(ConnectionType.REPORT));

    final List reportSocketResources = reportSocketSet.reserveAll();
    assertEquals(1, reportSocketResources.size());

    acceptor.shutdown();
  }

  private Acceptor createAcceptor(int numberOfThreads) throws Exception {
    // Figure out a free local port.
    final ServerSocket serverSocket = new ServerSocket(0);
    final int port = serverSocket.getLocalPort();
    serverSocket.close();

    return new Acceptor("", port, numberOfThreads);
  }

  public void testGetThreadGroup() throws Exception {

    final Acceptor acceptor1 = createAcceptor(2);
    final Acceptor acceptor2 = createAcceptor(1);

    final ThreadGroup threadGroup = acceptor1.getThreadGroup();

    assertTrue(!threadGroup.equals(acceptor2.getThreadGroup()));

    assertEquals(2, acceptor1.getThreadGroup().activeCount());

    acceptor1.shutdown();
    acceptor2.shutdown();

    assertEquals(threadGroup, acceptor1.getThreadGroup());

    while (!threadGroup.isDestroyed()) {
      Thread.sleep(10);
    }
  } 

  public void testShutdown() throws Exception {

    final Acceptor acceptor = createAcceptor(3);

    final ResourcePool socketSet =
      acceptor.getSocketSet(ConnectionType.CONTROL);

    final Connector connector =
      new Connector("localhost", acceptor.getPort(), ConnectionType.CONTROL);

    connector.connect();

    // Sleep until we've accepted the connection. Give up after a few
    // seconds.
    for (int i=0; socketSet.countActive() != 1 && i<10; ++i) {
      Thread.sleep(i * i * 10);
    }

    assertNull(acceptor.getPendingException(false));

    acceptor.shutdown();

    final ThreadGroup threadGroup = acceptor.getThreadGroup();

    while (!threadGroup.isDestroyed()) {
      Thread.sleep(10);
    }

    assertTrue(socketSet.reserveNext().isSentinel());
  } 

  public void testGetPendingException() throws Exception {

    final Acceptor acceptor = createAcceptor(3);

    // Non blocking.
    assertNull(acceptor.getPendingException(false));

    // Create a couple of problems.
    final Socket socket = new Socket("localhost", acceptor.getPort());
    socket.getOutputStream().write(123);
    socket.getOutputStream().flush();

    final Socket socket2 = new Socket("localhost", acceptor.getPort());
    socket2.getOutputStream().write(99);
    socket2.getOutputStream().flush();

    // Blocking, so we don't need to do fancy synchronisation to
    // ensure acceptor has encountered the problems.
    assertTrue(acceptor.getPendingException(true)
               instanceof CommunicationException);

    assertTrue(acceptor.getPendingException(true)
               instanceof CommunicationException);

    assertNull(acceptor.getPendingException(false));

    acceptor.shutdown();

    assertNull(acceptor.getPendingException(true));
  }
}
