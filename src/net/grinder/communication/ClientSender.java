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

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.Socket;


/**
 * Class that manages the sending of messages to a server.
 *
 * @author Philip Aston
 * @version $Revision$
 **/
public class ClientSender extends AbstractSender {

  private final OutputStream m_outputStream;

  /**
   * Factory method that makes a TCP connection and returns a
   * corresponding <code>ClientSender</code>.
   *
   * @param grinderID A string describing our Grinder process.
   * @param addressString TCP address to connect to.
   * @param port TCP port to connect to.
   * @return The ClientSender.
   * @throws CommunicationException If failed to connect to socket.
   **/
  public static ClientSender connectTo(String grinderID, String addressString,
                                       int port)
    throws CommunicationException {

    try {
      // Our socket - bind to any local port.
      final Socket socket = new Socket(addressString, port);

      final String localHost = socket.getLocalAddress().getHostName();
      final int localPort = socket.getLocalPort();

      // Calculate a globally unique string for this sender.
      final String senderID =
        addressString + ":" + port + ":" + localHost + ":" + localPort;

      return new ClientSender(grinderID, senderID, socket.getOutputStream()) {

          public void shutdown() throws CommunicationException {
            super.shutdown();

            try {
              socket.close();
            }
            catch (IOException e) {
              // Ignore.
            }
          }
        };
    }
    catch (IOException e) {
      throw new CommunicationException(
        "Could not connect to '" + addressString + ":" + port + "'", e);
    }
  }

  /**
   * Constructor.
   *
   * @param grinderID A string describing our Grinder process.
   * @param senderID Unique string identifying sender.
   * @param outputStream The output stream to write to.
   */
  private ClientSender(String grinderID, String senderID,
                       OutputStream outputStream)
    throws CommunicationException {

    super(grinderID, senderID);
    m_outputStream = new BufferedOutputStream(outputStream);
  }

  /**
   * Constructor.
   *
   * @param outputStream The output stream to write to.
   */
  public ClientSender(OutputStream outputStream) {
    m_outputStream = new BufferedOutputStream(outputStream);
  }

  /**
   * Send a message.
   *
   * @param message The message.
   * @exception IOException If an error occurs.
   */
  protected final void writeMessage(Message message) throws IOException {

    // I tried the model of using a single ObjectOutputStream for the
    // lifetime of the socket and a single ObjectInputStream. However,
    // the corresponding ObjectInputStream would get occasional EOF's
    // during readObject. Seems like voodoo to me, but creating a new
    // ObjectOutputStream for every message fixes this.

    final ObjectOutputStream objectStream =
      new ObjectOutputStream(m_outputStream);

    objectStream.writeObject(message);
    objectStream.flush();
  }

  /**
   * Cleanly shutdown the <code>Sender</code>. Ignore most errors.
   * Connection has probably been reset by peer.
   *
   * @exception CommunicationException If an error occurs.
   */
  public void shutdown() throws CommunicationException {

    try {
      send(new CloseCommunicationMessage());
    }
    catch (CommunicationException e) {
      // Ignore.
    }

    super.shutdown();

    try {
      m_outputStream.close();
    }
    catch (IOException e) {
      // Ignore.
    }
  }
}

