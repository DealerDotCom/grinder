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

import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import net.grinder.util.thread.ThreadPool;
import net.grinder.util.thread.ThreadSafeQueue;


/**
 * Active object that accepts connections on a ServerSocket.
 *
 * @author Philip Aston
 * @version $Revision$
 */
public final class Acceptor {

  private final ServerSocket m_serverSocket;
  private final ThreadPool m_threadPool;
  private final ThreadSafeQueue m_exceptionQueue = new ThreadSafeQueue();

  /**
   * {@link ResourcePool}s indexed by {@link ConnectionType}.
   */
  private final Map m_socketSets = new HashMap();

  /**
   * Linked lists of {@link Listener}'s indexed by {@link
   * ConnectionType}. Synchronise on a list before accessing it.
   */
  private final Map m_listenerMap = new HashMap();

  private final ListenerNotification m_notifyAccept =
    new ListenerNotification() {
      protected void doNotification(Listener listener,
                                    ConnectionType connectionType,
                                    ConnectionIdentity connection) {
        listener.connectionAccepted(connectionType, connection);
      }
    };

  private final ListenerNotification m_notifyClose =
    new ListenerNotification() {
      protected void doNotification(Listener listener,
                                    ConnectionType connectionType,
                                    ConnectionIdentity connection) {
        listener.connectionClosed(connectionType, connection);
      }
    };

  /**
   * Constructor.
   *
   * @param addressString The TCP address to listen on. Zero-length
   * string => listen on all interfaces.
   * @param port The TCP port to listen to. 0 => use any free port.
   * @param numberOfThreads Number of acceptor threads.
   * @throws CommunicationException If server socket could not be
   * bound.
   */
  public Acceptor(String addressString, int port, int numberOfThreads)
    throws CommunicationException {

    if (addressString.length() > 0) {
      try {
        m_serverSocket =
          new ServerSocket(port, 50, InetAddress.getByName(addressString));
      }
      catch (IOException e) {
        throw new CommunicationException(
          "Could not bind to address '" + addressString + ':' + port + '\'', e);
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

    final ThreadPool.RunnableFactory runnableFactory =
      new ThreadPool.RunnableFactory() {
        public Runnable create() {
          return new Runnable() {
              public void run() { process(); }
            };
        }
      };

    m_threadPool =
      new ThreadPool("Acceptor", numberOfThreads, runnableFactory);

    m_threadPool.start();
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
      synchronized (m_socketSets) {
        final Iterator iterator = m_socketSets.values().iterator();

        while (iterator.hasNext()) {
          final ResourcePool resourcePool = (ResourcePool)iterator.next();
          resourcePool.close();
        }
      }

      m_threadPool.stop();
    }

    m_exceptionQueue.shutdown();
  }

  /**
   * Get the port this Acceptor is listening on.
   *
   * @return The port.
   */
  public int getPort() {
    return m_serverSocket.getLocalPort();
  }

  /**
   * Asynchronous exception handling.
   * @param block <code>true</code> => block until an exception is
   * available, <code>false</code => return <code>null</code> if no
   * exception is available.
   * @return The exception, or <code>null</code> if no exception is
   * available or this Acceptor has been shut down.
   */
  public Exception getPendingException(boolean block) {
    try {
      return (Exception) m_exceptionQueue.dequeue(block);
    }
    catch (ThreadSafeQueue.ShutdownException e) {
      return null;
    }
  }

  /**
   * Listener interface.
   */
  public interface Listener {
    /**
     * A connection has been accepted.
     *
     * @param connectionType The type of the connection.
     * @param connection The connection identity.
     */
    void connectionAccepted(ConnectionType connectionType,
                            ConnectionIdentity connection);

    /**
     * A connection has been closed.
     *
     * @param connectionType The type of the connection.
     * @param connection The connection identity.
     */
    void connectionClosed(ConnectionType connectionType,
                          ConnectionIdentity connection);
  }

  /**
   * Add a new listener.
   *
   * @param connectionType The connection type.
   * @param listener The listener.
   */
  public void addListener(ConnectionType connectionType, Listener listener) {
    final List listenerList = getListenerList(connectionType);
    synchronized (listenerList) {
      listenerList.add(listener);
    }
  }

  private abstract class ListenerNotification {
    public final void notify(ConnectionType connectionType,
                             ConnectionIdentity connection) {

      final List listenerList = getListenerList(connectionType);

      synchronized (listenerList) {
        final Iterator iterator = listenerList.iterator();

        while (iterator.hasNext()) {
          doNotification((Listener)iterator.next(),
                         connectionType, connection);
        }
      }
    }

    protected abstract void doNotification(Listener listener,
                                           ConnectionType connectionType,
                                           ConnectionIdentity connection);
  }

  /**
   * Get a set of accepted connections.
   *
   * @param connectionType Identifies the set of connections to
   * return.
   * @return A set of sockets, each wrapped in a {@link
   * SocketResource}.
   */
  ResourcePool getSocketSet(final ConnectionType connectionType) {

    synchronized (m_socketSets) {
      final ResourcePool original =
        (ResourcePool)m_socketSets.get(connectionType);

      if (original != null) {
        return original;
      }
      else {
        final ResourcePool newSocketSet = new ResourcePool();

        newSocketSet.addListener(
          new ResourcePool.Listener() {
            public void resourceAdded(ResourcePool.Resource resource) {
              final ConnectionIdentity connectionIdentity =
                ((SocketResource)resource).getConnectionIdentity();
              m_notifyAccept.notify(connectionType, connectionIdentity);
            }

            public void resourceClosed(ResourcePool.Resource resource) {
              final ConnectionIdentity connectionIdentity =
                ((SocketResource)resource).getConnectionIdentity();

              m_notifyClose.notify(connectionType, connectionIdentity);
            }
          });

        m_socketSets.put(connectionType, newSocketSet);
        return newSocketSet;
      }
    }
  }

  /**
   * Get the listener list for a particular connection type.
   */
  List getListenerList(ConnectionType connectionType) {
    synchronized (m_listenerMap) {
      final List original = (List)m_listenerMap.get(connectionType);

      if (original != null) {
        return original;
      }
      else {
        final List newList = new LinkedList();
        m_listenerMap.put(connectionType, newList);
        return newList;
      }
    }
  }

  /**
   * Return the thread group used for our threads. Package scope; used
   * by the unit tests.
   *
   * @return The thread group.
   */
  ThreadGroup getThreadGroup() {
    return m_threadPool.getThreadGroup();
  }

  private void discriminateConnection(Socket localSocket) throws IOException {
    try {
      final ConnectionType connectionType =
        ConnectionType.read(localSocket.getInputStream());

      synchronized (m_socketSets) {
        getSocketSet(connectionType).add(new SocketResource(localSocket));
      }
    }
    catch (CommunicationException e) {
      try {
        m_exceptionQueue.queue(e);
      }
      catch (ThreadSafeQueue.ShutdownException shutdownException) {
        // Should never happen.
        shutdownException.printStackTrace();
      }

      try {
        localSocket.close();
      }
      catch (IOException ioException) {
        // Ignore.
      }
    }
  }

  private void process() {
    try {
      while (true) {
        final Socket localSocket = m_serverSocket.accept();
        discriminateConnection(localSocket);
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

  /**
   * Wrapper for sockets that are managed by our {@link
   * ResourcePool}s.
   */
  static final class SocketResource implements ResourcePool.Resource {
    private final Socket m_socket;
    private final ConnectionIdentity m_connectionIdentity;

    private SocketResource(Socket socket) {
      m_socket = socket;
      m_connectionIdentity =
        new ConnectionIdentity(m_socket.getInetAddress(),
                               m_socket.getPort(),
                               System.currentTimeMillis());
    }

    public InputStream getInputStream() throws IOException {
      return m_socket.getInputStream();
    }

    public OutputStream getOutputStream() throws IOException {
      return m_socket.getOutputStream();
    }

    public void close() {
      try {
        m_socket.close();
      }
      catch (IOException e) {
        // Ignore.
      }
    }

    public ConnectionIdentity getConnectionIdentity() {
      return m_connectionIdentity;
    }
  }
}
