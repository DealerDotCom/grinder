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
import java.io.InputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.Socket;


/**
 * Manages reciept of messages from a server.
 *
 * @author Philip Aston
 * @version $Revision$
 */
public class ClientReceiver implements Receiver {

  private final InputStream m_inputStream;
  private boolean m_shutdown = false;

  /**
   * Factory method that makes a TCP connection and returns a
   * corresponding <code>ClientReceiver</code>.
   *
   * @param addressString TCP address to connect to.
   * @param port TCP port to connect to.
   * @return The ClientReceiver.
   * @throws CommunicationException If failed to connect to socket.
   */
  public static ClientReceiver connectTo(String addressString, int port)
    throws CommunicationException {

    try {
      // Our socket - bind to any local port.
      final Socket socket = new Socket(addressString, port);

      return new ClientReceiver(socket.getInputStream()) {
          public void shutdown() {
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
   * @param inputStream The input stream to read from.
   **/
  public ClientReceiver(InputStream inputStream) {
    m_inputStream = new BufferedInputStream(inputStream);
  }

  /**
   * Block until a message is available. Typically called from a
   * message dispatch loop.
   *
   * <p>Not thread safe.</p>
   *
   * @return The message or <code>null</code> if shut down.
   * @throws CommunicationException If an error occured receiving a message.
   */
  public final Message waitForMessage() throws CommunicationException {

    if (m_shutdown) {
      return null;
    }

    try {
      final ObjectInputStream objectStream =
        new ObjectInputStream(m_inputStream);

      final Message message = (Message)objectStream.readObject();

      if (message instanceof CloseCommunicationMessage) {
        shutdown();
        return null;
      }

      return message;
    }
    catch (IOException e) {
      throw new CommunicationException("Failed to read message", e);
    }
    catch (ClassNotFoundException e) {
      throw new CommunicationException("Failed to read message", e);
    }
  }

  /**
   * Cleanly shut down the <code>Receiver</code>. Ignore errors.
   * Connection has probably been reset by peer.
   */
  public void shutdown() {

    m_shutdown = true;

    try {
      m_inputStream.close();
    }
    catch (IOException e) {
      // Ignore.
    }
  }
}
