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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;


/**
 * Class that manages the a set of TCP sockets.
 *
 * <p>The sockets belong to connections accepted by an {@link
 * Acceptor}.</p>
 *
 * <p>Currently only alows for polling the sockets for received {@link
 * Message}s, but might be extended in the future to support broadcast
 * of a {@link Message} to all the sockets.
 *
 * @author Philip Aston
 * @version $Revision$
 **/
final class SocketSet {
  private static final int PURGE_FREQUENCY = 1000;

  private final Object m_handleListMutex = new Object();
  private final Object m_reservedHandleMutex = new Object();
  private List m_handles = new ArrayList();
  private int m_lastHandle = 0;
  private int m_nextPurge = 0;

  /**
   * Constructor.
   */
  public SocketSet() {
    m_handles.add(new SentinelHandle());
  }

  /**
   * Adds a <code>Socket</code> to this set.
   *
   * @param socket The socket to add.
   */
  public void add(Socket socket) {
    final Handle handle = new HandleImplementation(socket);

    synchronized (m_handleListMutex) {
      m_handles.add(handle);
      m_handleListMutex.notifyAll();
    }
  }

  /**
   * Returns a Handle. Free Handles are handed out to callers in
   * order. A SentinelHandle is returned once every cycle; if no
   * Handles are free the SentinelHandle is always returned.
   *
   * @returns The Handle. It is up to the caller to free the Handle.
   */
  public Handle reserveNextHandle() {
    synchronized (m_handleListMutex) {
      purgeZombieHandles();

      while (true) {
        if (++m_lastHandle >= m_handles.size()) {
          m_lastHandle = 0;
        }

        final Handle handle = (Handle)m_handles.get(m_lastHandle);

        if (handle.reserve()) {
          return handle;
        }
      }
    }
  }

  /**
   * Returns a list of all the current Handles. Blocks until Handles
   * can be reserved. The SentinelHandle is not included.
   *
   * @returns The Handles. It is up to the caller to free each Handle.
   * @throws InterruptedException If the caller's thread is
   * interrupted. This can occur if thread belongs to the Acceptor
   * thread group and the Acceptor is shut down.
   */
  public List reserveAllHandles() throws InterruptedException {

    final List result;
    final List reserveList;

    synchronized (m_handleListMutex) {
      purgeZombieHandles();

      result = new ArrayList(m_handles.size());
      reserveList = new ArrayList(m_handles);
    }

    while (reserveList.size() > 0) {
      // Iterate backwards so remove is cheap.
      final ListIterator iterator =
        reserveList.listIterator(reserveList.size());

      while (iterator.hasPrevious()) {
        final Handle handle = (Handle)iterator.previous();

        if (handle.isSentinel()) {
          iterator.remove();
        }
        else if (handle.reserve()) {
          result.add(handle);
          iterator.remove();
        }
        else if (handle.isClosed()) {
          iterator.remove();
        }
      }

      if (reserveList.size() > 0) {
        // Block until more handles are freed.
        synchronized (m_reservedHandleMutex) {
          m_reservedHandleMutex.wait();
        }
      }
    }

    return result;
  }

  public void close() {
    synchronized (m_handleListMutex) {
      final Iterator iterator = m_handles.iterator();

      while (iterator.hasNext()) {
        final Handle handle = (Handle)iterator.next();
        handle.close();
      }
    }
  }

  private void purgeZombieHandles() {
    synchronized (m_handleListMutex) {
      if (++m_nextPurge > PURGE_FREQUENCY) {
        m_nextPurge = 0;

        final List newHandles = new ArrayList(m_handles.size());

        final Iterator iterator = m_handles.iterator();

        while (iterator.hasNext()) {
          final Handle handle = (Handle)iterator.next();

          if (!handle.isClosed()) {
            newHandles.add(handle);
          }
        }

        m_handles = newHandles;
        m_lastHandle = 0;
      }
    }
  }

  /**
   * Interface to socket that can be reserved by a thread.
   */
  public interface Handle {

    boolean isSentinel();

    /**
     * Caller should {@link #reserve} the Handle before calling this
     * method.
     *
     * @returns The input stream.
     */
    InputStream getInputStream() throws IOException;

    /**
     * Caller should {@link #reserve} the Handle before calling this
     * method.
     *
     * @returns The output stream.
     */
    OutputStream getOutputStream() throws IOException;

    boolean reserve();
    void free();

    void close();
    boolean isClosed();
  }

  private static final class SentinelHandle implements Handle {
    public boolean isSentinel() {
      return true;
    }

    public InputStream getInputStream() {
      throw new RuntimeException("Assertion failure");
    }

    public OutputStream getOutputStream() {
      throw new RuntimeException("Assertion failure");
    }

    public boolean reserve() {
      return true;
    }

    public void free() {
    }

    public void close() {
    }

    public boolean isClosed() {
      return false;
    }
  }

  private final class HandleImplementation implements Handle {
    private final Socket m_socket;
    private final InputStream m_inputStream;
    private final OutputStream m_outputStream;
    private final IOException m_deferredIOException;

    private boolean m_busy = false;
    private boolean m_closed = false;

    HandleImplementation(Socket socket) {
      m_socket = socket;

      InputStream inputStream = null;
      OutputStream outputStream = null;
      IOException deferredIOException = null;

      try {
        inputStream = new BufferedInputStream(m_socket.getInputStream());
        outputStream = new BufferedOutputStream(m_socket.getOutputStream());
      }
      catch (IOException e) {
        close();
        deferredIOException = e;
      }

      m_inputStream = inputStream;
      m_outputStream = outputStream;
      m_deferredIOException = deferredIOException;
    }

    public boolean isSentinel() {
      return false;
    }

    public InputStream getInputStream() throws IOException {

      if (m_deferredIOException != null) {
        throw m_deferredIOException;
      }

      return m_inputStream;
    }

    public OutputStream getOutputStream() throws IOException {

      if (m_deferredIOException != null) {
        throw m_deferredIOException;
      }

      return m_outputStream;
    }

    public synchronized boolean reserve() {
      if (m_busy || m_closed) {
        return false;
      }

      m_busy = true;

      return true;
    }

    public void free() {

      final boolean stateChanged;

      synchronized (this) {
        stateChanged = m_busy;
        m_busy = false;
      }

      if (stateChanged) {
        synchronized (m_reservedHandleMutex) {
          m_reservedHandleMutex.notifyAll();
        }
      }
    }

    public void close() {

      final boolean stateChanged;

      synchronized (this) {
        stateChanged = !m_closed;

        if (stateChanged) {
          m_busy = false;
          m_closed = true;

          try {
            m_socket.close();
          }
          catch (IOException e) {
            // Ignore.
          }
        }
      }

      if (stateChanged) {
        synchronized (m_reservedHandleMutex) {
          m_reservedHandleMutex.notifyAll();
        }
      }
    }

    public synchronized boolean isClosed() {
      return m_closed;
    }
  }
}
