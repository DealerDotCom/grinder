// Copyright (C) 2000 Phil Dawes
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

package net.grinder.tools.tcpproxy;

import java.io.InterruptedIOException;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;


/**
 * Simple implementation of TCPProxyEngine that connects to a single
 * remote server.
 *
 * @author Phil Dawes
 * @author Philip Aston
 * @version $Revision$
 */
public final class PortForwarderTCPProxyEngine extends AbstractTCPProxyEngine {

  private final ConnectionDetails m_connectionDetails;

  /**
   * Constructor.
   *
   * @param socketFactory Factory for plain old sockets.
   * @param requestFilter Request filter.
   * @param responseFilter Response filter.
   * @param outputWriter Writer to terminal.
   * @param connectionDetails Connection details.
   * @param useColour Whether to use colour.
   * @param timeout Timeout in milliseconds.
   *
   * @exception IOException If an I/O error occurs.
   */
  public PortForwarderTCPProxyEngine(TCPProxySocketFactory socketFactory,
                                     TCPProxyFilter requestFilter,
                                     TCPProxyFilter responseFilter,
                                     PrintWriter outputWriter,
                                     ConnectionDetails connectionDetails,
                                     boolean useColour,
                                     int timeout)
    throws IOException {

    super(socketFactory, requestFilter, responseFilter, outputWriter,
          connectionDetails.getLocalEndPoint(), useColour, timeout);

    m_connectionDetails = connectionDetails;
  }

  /**
   * Main event loop.
   */
  public void run() {

    while (true) {
      final Socket localSocket;

      try {
        localSocket = getServerSocket().accept();
      }
      catch (InterruptedIOException e) {
        System.err.println("Listen time out");
        return;
      }
      catch (IOException e) {
        e.printStackTrace(System.err);
        return;
      }

      try {
        launchThreadPair(localSocket, m_connectionDetails.getRemoteEndPoint(),
                         EndPoint.clientEndPoint(localSocket), false);
      }
      catch (IOException e) {
        e.printStackTrace(System.err);
      }
    }
  }
}
