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

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.ServerSocket;


/**
 * Class that manages the receipt of unicast messages.
 *
 * @author Philip Aston
 * @version $Revision$
 **/
public final class UnicastReceiver extends AbstractReceiver {

  private int m_listenThreadIndex = 0;
  private final ServerSocket m_serverSocket;
  private final SocketSet m_connections = new SocketSet();
  private final ThreadGroup m_threadGroup =
    new ThreadGroup("UnicastReceiver");

  /**
   * Constructor.
   *
   * @param addressString The TCP address to listen on.
   * @param port The TCP port to listen to.
   *
   * @throws CommunicationException If socket could not be bound to.
   **/
  public UnicastReceiver(String addressString, int port)
    throws CommunicationException {

    super(false);        // TCP guarantees message sequence so
                // we don't have to.

    if (addressString.length() > 0) {
      try {
    m_serverSocket =
      new ServerSocket(port, 50,
               InetAddress.getByName(addressString));
      }
      catch (IOException e) {
    throw new CommunicationException(
      "Could not bind to TCP address '" + addressString + ":" +
      port + "'",
      e);
      }
    }
    else {
      try {
    m_serverSocket = new ServerSocket(port, 50);
      }
      catch (IOException e) {
    throw new CommunicationException(
      "Could not bind to port '" + port +
      "' on local interfaces",
      e);
      }
    }

    new AcceptorThread().start();

    for (int i = 0; i < 5; ++i) {
      new ListenThread().start();
    }
  }

  /**
   * Shut down this reciever.
   * @throws CommunicationException If an IO exception occurs.
   **/
  public final void shutdown() throws CommunicationException {

    super.shutdown();

    try {
      m_serverSocket.close();
    }
    catch (IOException e) {
      throw new CommunicationException("Error closing socket", e);
    }

    m_connections.close();

    m_threadGroup.interrupt();
  }

  private final class AcceptorThread extends Thread {

    public AcceptorThread() {
      super(m_threadGroup, "Acceptor thread");
    }

    public void run() {
      try {
    while (true) {
      final Socket localSocket = m_serverSocket.accept();

      try {
        m_connections.add(localSocket);
      }
      catch (Exception e) {
        // Propagate exceptions to threads calling
        // waitForMessage.
        getMessageQueue().queue(e);
      }
    }
      }
      catch (IOException e) {
    // Treat accept socket errors as fatal - we've
    // probably been shutdown.
      }
      catch (MessageQueue.ShutdownException e) {
    // We've been shutdown, exit this thread.
      }
      finally {
    // Best effort to ensure our server socket is closed.
    try {
      shutdown();
    }
    catch (CommunicationException ce) {
      // Ignore.
    }
      }
    }
  }

  private final class ListenThread extends Thread {

    public ListenThread() {
      super(m_threadGroup, "Unicast listen thread " + m_listenThreadIndex++);
      setDaemon(true);
    }

    public void run() {
      final MessageQueue messageQueue = getMessageQueue();

      try {
    // Did we do some work on the last pass?
    boolean idle = false;

    while (true) {
      final SocketSet.Handle socketHandle =
        m_connections.reserveNextHandle();

      try {
        if (socketHandle.isSentinel()) {
          if (idle) {
        Thread.sleep(500);
          }

          idle = true;
        }
        else {
          final Message m = socketHandle.pollForMessage();

          if (m instanceof CloseCommunicationMessage) {
        socketHandle.close();
        idle = false;
          }
          else if (m != null) {
        messageQueue.queue(m);
        idle = false;
          }
        }
      }
      catch (IOException e) {
        socketHandle.close();
        messageQueue.queue(e);
      }
      catch (ClassNotFoundException e) {
        socketHandle.close();
        messageQueue.queue(e);
      }
      finally {
        socketHandle.free();
      }
    }
      }
      catch (MessageQueue.ShutdownException e) {
    // We've been shutdown, exit this thread.
      }
      catch (InterruptedException e) {
    // Ignore.
      }
    }
  }
}
