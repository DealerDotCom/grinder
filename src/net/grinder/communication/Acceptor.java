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

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.ServerSocket;


/**
 * Class that accepts connections on a ServerSocket.
 *
 * @author Philip Aston
 * @version $Revision$
 */
final class Acceptor {

  private final ServerSocket m_serverSocket;
  private final SocketSet m_connections = new SocketSet();
  private final ThreadGroup m_threadGroup = new ThreadGroup("Acceptor");

  /**
   * Constructor.
   *
   * @param addressString The TCP address to listen on. Zero-length
   * string => listen on all interfaces.
   * @param port The TCP port to listen to. 0 => use any free port.
   * @throws CommunicationException If server socket could not be
   * bound.
   */
  public Acceptor(String addressString, int port)
    throws CommunicationException {

    if (addressString.length() > 0) {
      try {
        m_serverSocket =
          new ServerSocket(port, 50, InetAddress.getByName(addressString));
      }
      catch (IOException e) {
        throw new CommunicationException(
          "Could not bind to address '" + addressString + ":" + port + "'", e);
      }
    }
    else {
      try {
        m_serverSocket = new ServerSocket(port, 50);
      }
      catch (IOException e) {
        throw new CommunicationException(
          "Could not bind to port '" + port + "' on local interfaces", e);
      }
    }

    new AcceptorThread().start();

    m_threadGroup.setDaemon(true);
  }

  /**
   * Shut down this acceptor.
   *
   * @throws CommunicationException If an IO exception occurs.
   */
  public void shutdown() throws CommunicationException {

    try {
      m_serverSocket.close();
    }
    catch (IOException e) {
      throw new CommunicationException("Error closing socket", e);
    }
    finally {
      m_connections.close();
      m_threadGroup.interrupt();
    }
  }

  /**
   * Get the set of accepted connections.
   *
   * @return The set of accepted connections.
   */
  public SocketSet getSocketSet() {
    return m_connections;
  }

  /**
   * Get a <code>ThreadGroup</code> which should be used for threads
   * to be shutdown with the Acceptor.
   *
   * @return The thread group.
   */
  public ThreadGroup getThreadGroup() {
    return m_threadGroup;
  }

  private final class AcceptorThread extends Thread {

    public AcceptorThread() {
      super(m_threadGroup, "Acceptor thread");
    }

    public void run() {
      try {
        while (true) {
          final Socket localSocket = m_serverSocket.accept();

          m_connections.add(localSocket);
        }
      }
      catch (IOException e) {
        // Treat accept socket errors as fatal - we've probably been
        // shutdown.
      }
      finally {
        // Best effort to ensure our server socket is closed.
        try {
          shutdown();
        }
        catch (CommunicationException e) {
          // Ignore.
        }
      }
    }
  }
}
