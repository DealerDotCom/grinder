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
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;


/**
 * Class that manages the sending of multicast messages.
 *
 * @author Philip Aston
 * @version $Revision$
 **/
public class MulticastSender extends AbstractSender {

  private final MulticastSocket m_localSocket;
  private final DatagramPacket m_packet;

  /**
   * Constructor.
   *
   * @param grinderID A string describing our Grinder process.
   * @param multicastAddressString Multicast address to send to.
   * @param multicastPort Multicast port to send to.
   * @throws CommunicationException If failed to bind to socket or
   * failed to generate a unique process identifer.
   */
  public MulticastSender(String grinderID,
             String multicastAddressString,
             int multicastPort)
    throws CommunicationException {

    super(grinderID);

    final InetAddress multicastAddress;
    final String localHost;

    try {
      // Remote address.
      multicastAddress = InetAddress.getByName(multicastAddressString);

      // Our socket - bind to any port.
      m_localSocket = new MulticastSocket();

      localHost = InetAddress.getLocalHost().getHostName();
    }
    catch (IOException e) {
      throw new CommunicationException(
    "Could not bind to multicast address '" +
    multicastAddressString + "'",
    e);
    }

    m_packet = new DatagramPacket(getScratchByteStream().getBytes(), 0,
                  multicastAddress, multicastPort);

    // Calculate a globally unique string for this sender. We
    // avoid calling multicastAddress.toString() since this
    // involves a DNS lookup.
    setSenderID(multicastAddressString + ":" + multicastPort + ":" +
        localHost + ":" + m_localSocket.getLocalPort() + ":" +
        System.currentTimeMillis());
  }

  /**
   * Publish a message.
   *
   * @param message The message.
   * @exception IOException If an error occurs.
   */
  protected final void writeMessage(Message message) throws IOException {

    // Hang onto byte stream object and reset it
    // rather than creating garbage.
    final MyByteArrayOutputStream scratchByteStream =
      getScratchByteStream();

    scratchByteStream.reset();

    // Sadly reuse isn't possible with an ObjectOutputStream.
    final ObjectOutputStream objectStream =
      new ObjectOutputStream(scratchByteStream);

    objectStream.writeObject(message);
    objectStream.flush();

    m_packet.setData(scratchByteStream.getBytes(), 0,
             scratchByteStream.size());
    m_localSocket.send(m_packet);
  }
}
