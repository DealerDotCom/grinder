// Copyright (C) 2005 Philip Aston
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

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;

import net.grinder.util.ListenerSupport;


/**
 * Wrapper for a {@link Socket} that is {ResourcePool.ResourcePool}
 * and understands our connection close protocol.
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

  private final ListenerSupport m_closedListeners = new ListenerSupport();

  private final ListenerSupport.Informer m_closedInformer =
    new ListenerSupport.Informer() {
      public void inform(Object listener) {
        ((ClosedListener)listener).socketClosed();
      }
    };

  /**
   * Needed because J2SE 1.3 does not have <code>Socket.isClosed()</code>.
   */
  private boolean m_closed = false;

  public SocketWrapper(Socket socket) throws CommunicationException {
    m_socket = socket;

    try {
      m_inputStream =
        new BufferedInputStream(m_socket.getInputStream(), BUFFER_SIZE);

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
      }

      throw new CommunicationException("Could not establish communication", e);
    }
  }

  public boolean isPeerShutdown() {

    try {
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
    catch (CommunicationException e) {
      close();
      return true;
    }
    catch (IOException e) {
      close();
      return true;
    }

    return false;
  }

  public void close() {
    if (!m_closed) {
      // Java provides no way for socket code to enquire whether the
      // peer has closed the connection. We make an effort to tell the
      // peer.
      try {
        new StreamSender(getOutputStream()).shutdown();
      }
      catch (CommunicationException e) {
        // Ignore.
      }

      try {
        m_socket.close();
      }
      catch (IOException e) {
        // Ignore errors, connection has probably been reset by peer.
      }

      // Mark closed before informing listeners to prevent recursion.
      m_closed = true;
      m_closedListeners.apply(m_closedInformer);
    }
  }

  public ConnectionIdentity getConnectionIdentity() {
    return m_connectionIdentity;
  }

  public InputStream getInputStream() {
    return m_inputStream;
  }

  public OutputStream getOutputStream() throws CommunicationException {
    try {
      return m_socket.getOutputStream();
    }
    catch (IOException e) {
      throw new CommunicationException("Communication failed", e);
    }
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

