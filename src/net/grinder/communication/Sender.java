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
import java.io.Serializable;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;


/**
 * <em>Not thread safe.</em>
 *
 * @author Philip Aston
 * @version $Revision$
 */
public class Sender
{
    private final MulticastSocket m_localSocket;
    private final InetAddress m_multicastAddress;
    private final int m_multicastPort;

    public Sender(String multicastAddressString, int multicastPort)
	throws CommunicationException
    {
	try {
	    // Our socket - bind to any port.
	    m_localSocket = new MulticastSocket();

	    // Remote address.
	    m_multicastAddress =
		InetAddress.getByName(multicastAddressString);
	}
	catch (IOException e) {
	    throw new CommunicationException(
		"Could not bind to multicast address " +
		multicastAddressString);
	}

	m_multicastPort = multicastPort;
    }

    public void send(Message message)
	throws CommunicationException
    {
	try {
	    final MyByteArrayOutputStream byteStream =
		new MyByteArrayOutputStream();

	    final ObjectOutputStream objectStream =
		new ObjectOutputStream(byteStream);
	
	    objectStream.writeObject(message);
	    objectStream.flush();
	
	    final byte[] bytes = byteStream.getBytes();

	    final DatagramPacket packet
		= new DatagramPacket(bytes, bytes.length, m_multicastAddress,
				     m_multicastPort);

	    m_localSocket.send(packet);
	}
	catch (IOException e) {
	    throw new CommunicationException(
		"Exception whilst sending message", e);
	}
    }
}


/**
 * Abuse Java API to avoid needless proliferation of temporary
 * objects.
 * @author Philip Aston
 * @version $Revision$
 */
class MyByteArrayOutputStream extends ByteArrayOutputStream
{
    public byte[] getBytes() 
    {
	return buf;
    }
}

