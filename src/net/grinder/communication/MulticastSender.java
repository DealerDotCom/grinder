// The Grinder
// Copyright (C) 2000, 2001  Paco Gomez
// Copyright (C) 2000, 2001  Philip Aston

// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.

// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.

// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.

package net.grinder.communication;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.LinkedList;


/**
 * Class that manages the sending of multicast messages.
 *
 * @author Philip Aston
 * @version $Revision$
 **/
public class MulticastSender extends AbstractSender
{
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
     **/    
    public MulticastSender(String grinderID,
			   String multicastAddressString,
			   int multicastPort)
	throws CommunicationException
    {
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
		"Could not bind to multicast address " +
		multicastAddressString);
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

    protected final void writeMessage(Message message) throws IOException
    {
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
