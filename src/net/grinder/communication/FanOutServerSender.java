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
import java.io.OutputStream;


/**
 * Manages the sending of messages to many TCP clients.
 *
 * @author Philip Aston
 * @version $Revision$
 */
public final class FanOutServerSender extends AbstractFanOutSender {

  /**
   * Factory method that creates a <code>FanOutServerSender</code>
   * that listens on the given address.
   *
   * @param addressString The TCP address to listen on. Zero-length
   * string => listen on all interfaces.
   * @param port The TCP port to listen to. 0 => any local port.
   * @return The FanOutServerSender.
   * @throws CommunicationException If server socket could not be
   * bound.
   */
  public static FanOutServerSender bindTo(String addressString, int port)
    throws CommunicationException {

    return new FanOutServerSender(new Acceptor(addressString, port, 1),
                                  new Kernel(3));
  }

  private final Acceptor m_acceptor;

  /**
   * Constructor.
   *
   * @param acceptor Acceptor that manages connections to our server socket.
   * @param kernel A kernel to use.
   * @throws CommunicationException If server socket could not be
   * bound.
   */
  private FanOutServerSender(Acceptor acceptor, Kernel kernel)
    throws CommunicationException {

    super(kernel, acceptor.getSocketSet());

    m_acceptor = acceptor;
  }

  /**
   * Return an output stream from a socket resource.
   *
   * @param resource The resource.
   * @return The output stream.
   * @throws IOException If the output stream could not be obtained
   * from the socket.
   */
  protected OutputStream resourceToOutputStream(
    ResourcePool.Resource resource) throws IOException {

    return ((Acceptor.SocketResource)resource).getOutputStream();
  }

  /**
   * Shut down this sender.
   *
   * @throws CommunicationException If an IO exception occurs.
   */
  public void shutdown() throws CommunicationException {
    super.shutdown();
    m_acceptor.shutdown();
  }

   /**
    * Return the Acceptor. Package scope, used by the unit tests.
    *
    * @return The acceptor.
    */
  Acceptor getAcceptor() {
    return m_acceptor;
  }
}
