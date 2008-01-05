// Copyright (C) 2005, 2006 Philip Aston
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

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;

import net.grinder.common.UncheckedInterruptedException;
import net.grinder.util.ListenerSupport;


/**
 * Wrapper for a {@link Socket} that is {ResourcePool.ResourcePool} and
 * understands our connection close protocol.
 *
 * <p>
 * Client classes that access the sockets streams through
 * {@link #getInputStream} or {@link #getOutputStream}, and that do not
 * otherwise know they have exclusive access, should synchronise on the
 * particular stream object while they use it.
 * </p>
 *
 * @author Philip Aston
 * @version $Revision$
 */
final class SocketWrapper
  implements CheckIfPeerShutdown, ResourcePool.Resource {

  /**
   * As large as the largest message we may receive when calling
   * {@link #isPeerShutdown}. Currently this means as large as a {@link
   * CloseCommunicationMessage}.
   */
  private static final int BUFFER_SIZE = 512;

  private final Socket m_socket;
  private final ConnectionIdentity m_connectionIdentity;
  private final BufferedInputStream m_inputStream;
  private final OutputStream m_outputStream;

  private final ListenerSupport m_closedListeners = new ListenerSupport();

  private final ListenerSupport.Informer m_closedInformer =
    new ListenerSupport.Informer() {
      public void inform(Object listener) {
        ((ClosedListener)listener).socketClosed();
      }
    };

  /**
   * Constructor.
   *
   * @param socket
   *          Socket to wrap. If the caller maintains any references to the
   *          socket, if should synchronise access to the socket streams as
   *          described in {@link SocketWrapper}.
   * @throws CommunicationException
   *           If an error occurred.
   */
  public SocketWrapper(Socket socket) throws CommunicationException {
    m_socket = socket;

    try {
      m_inputStream =
        new BufferedInputStream(m_socket.getInputStream(), BUFFER_SIZE);

      m_outputStream = m_socket.getOutputStream();

      m_connectionIdentity =
        new ConnectionIdentity(m_socket.getInetAddress(),
                               m_socket.getPort(),
                               System.currentTimeMillis());
    }
    catch (IOException e) {
      try {
        m_socket.close();
      }
      catch (IOException ignore) {
        // Ignore.
        UncheckedInterruptedException.ioException(e);
      }

      throw new CommunicationException("Could not establish communication", e);
    }
  }

  public boolean isPeerShutdown() {

    try {
      synchronized (m_inputStream) {
        if (m_inputStream.available() > 0) {
          m_inputStream.mark(BUFFER_SIZE);

          try {
            if (new StreamReceiver(m_inputStream).waitForMessage() == null) {
              close();
              return true;
            }
          }
          finally {
            m_inputStream.reset();
          }
        }
      }
    }
    catch (CommunicationException e) {
      close();
      return true;
    }
    catch (IOException e) {
      UncheckedInterruptedException.ioException(e);
      close();
      return true;
    }

    return false;
  }

  /**
   * Close the SocketWrapper and its underlying resources.
   *
   * <p>No need to synchronise access to the close, isClosed - they should be
   * thread safe. Also, we're careful not to hold locks around the listener
   * notification.</p>
   */
  public void close() {
    if (!m_socket.isClosed()) {
      // Java provides no way for socket code to enquire whether the
      // peer has closed the connection. We make an effort to tell the
      // peer.
      synchronized (m_outputStream) {
        new StreamSender(m_outputStream).shutdown();
      }

      try {
        m_socket.close();
      }
      catch (IOException e) {
        // Ignore errors, connection has probably been reset by peer.
        UncheckedInterruptedException.ioException(e);
      }

      // Close before informing listeners to prevent recursion.
      m_closedListeners.apply(m_closedInformer);
    }
  }

  public ConnectionIdentity getConnectionIdentity() {
    return m_connectionIdentity;
  }

  /**
   * See note in {@link SocketWrapper} class documentation about the need
   * to synchronise around any usage of the returned <code>InputStream</code>.
   *
   * @return The input stream.
   */
  public InputStream getInputStream() {
    return m_inputStream;
  }

  /**
   * See note in {@link SocketWrapper} class documentation about the need
   * to synchronise around any usage of the returned <code>OutputStream</code>.
   *
   * @return The output stream.
   */
  public OutputStream getOutputStream() {
    return m_outputStream;
  }

  /**
   * Socket event notification interface.
   */
  public interface ClosedListener {
    void socketClosed();
  }

  public void addClosedListener(ClosedListener listener) {
    m_closedListeners.add(listener);
  }
}
