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
import java.net.UnknownHostException;
import java.util.Iterator;


/**
 * Manages the sending of messages to many TCP clients.
 *
 * @author Philip Aston
 * @version $Revision$
 */
public final class FanOutServerSender extends AbstractSender {

  /**
   * Factory method that creates a <code>FanOutServerSender</code>
   * that listens on the given address.
   *
   * @param grinderID A string describing our Grinder process.
   * @param addressString The TCP address to listen on. Zero-length
   * string => listen on all interfaces.
   * @param port The TCP port to listen to. 0 => any local port.
   * @return The FanOutServerSender.
   * @throws CommunicationException If server socket could not be
   * bound.
   */
  public static FanOutServerSender bindTo(String grinderID,
                                          String addressString,
                                          int port)
    throws CommunicationException {

    final Acceptor acceptor = new Acceptor(addressString, port);

    try {
      final String senderID =
        addressString + ":" + acceptor.getPort() + ":" +
        InetAddress.getLocalHost().getHostName();

      return new FanOutServerSender(
        grinderID, senderID, acceptor, new Kernel(3));
    }
    catch (UnknownHostException e) {
      throw new CommunicationException("Can't get local host", e);
    }
  }

  private final Acceptor m_acceptor;
  private final Kernel m_kernel;

  /**
   * Constructor.
   *
   * @param grinderID A string describing our Grinder process.
   * @param senderID Unique string identifying sender.
   * @param acceptor Acceptor that manages connections to our server socket.
   * @param kernel A kernel to use.
   * @throws CommunicationException If server socket could not be
   * bound.
   */
  private FanOutServerSender(String grinderID, String senderID,
                             Acceptor acceptor, Kernel kernel)
    throws CommunicationException {

    super(grinderID, senderID);

    m_acceptor = acceptor;
    m_kernel = kernel;
  }

  /**
   * Send a message.
   *
   * @param message The message.
   * @exception IOException If an error occurs.
   */
  protected void writeMessage(Message message) throws IOException {

    try {
      final Iterator iterator =
        m_acceptor.getSocketSet().reserveAllHandles().iterator();

      while (iterator.hasNext()) {
        m_kernel.execute(
          new WriteMessageToStream(message,
                                   (SocketSet.Handle) iterator.next()));
      }
    }
    catch (Kernel.ShutdownException e) {
      // Assertion failure.
      throw new RuntimeException("Kernel unexpectedly shutdown");
    }
    catch (InterruptedException e) {
      // Assertion failure.
      throw new RuntimeException("Unexpectedly shutdown");
    }
  }

  /**
   * Shut down this sender.
   *
   * @throws CommunicationException If an IO exception occurs.
   */
  public void shutdown() throws CommunicationException {

    super.shutdown();

    m_kernel.forceShutdown();
    m_acceptor.shutdown();
  }

  /**
   * Return the Acceptor. Used by the unit tests.
   *
   * @return The number of connections.
   */
  public Acceptor getAcceptor() {
    return m_acceptor;
  }

  private static final class WriteMessageToStream implements Runnable {
    private final Message m_message;
    private final SocketSet.Handle m_handle;

    public WriteMessageToStream(Message message, SocketSet.Handle handle) {
      m_message = message;
      m_handle = handle;
    }

    public void run() {
      try {
        writeMessageToStream(m_message, m_handle.getOutputStream());
      }
      catch (IOException e) {
        m_handle.close();
        //            m_messageQueue.queue(e);
        e.printStackTrace();
      }
      finally {
        m_handle.free();
      }
    }
  }
}
