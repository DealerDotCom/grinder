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
import java.net.Socket;


/**
 * Class that manages the sending of messages to a server.
 *
 * @author Philip Aston
 * @version $Revision$
 **/
public final class ClientSender extends StreamSender {

  /**
   * Factory method that makes a TCP connection and returns a
   * corresponding <code>Sender</code>.
   *
   * @param connector Connector to use to make the connection to the
   * server.
   * @return The ClientSender.
   * @throws CommunicationException If failed to connect.
   */
  public static Sender connect(Connector connector)
    throws CommunicationException {

    final Socket socket = connector.connect();

    try {
      return new ClientSender(socket);
    }
    catch (IOException e) {
      throw new CommunicationException("Connection failed", e);
    }
  }

  private final Socket m_socket;

  private ClientSender(Socket socket)
    throws CommunicationException, IOException {

    super(socket.getOutputStream());
    m_socket = socket;
  }

  /**
   * Cleanly shutdown the <code>Sender</code>. Ignore most errors,
   * connection has probably been reset by peer.
   */
  public void shutdown() {

    super.shutdown();

    try {
      m_socket.close();
    }
    catch (IOException e) {
      // Ignore.
    }
  }
}

