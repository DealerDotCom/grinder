// The Grinder
// Copyright (C) 2000  Paco Gomez
// Copyright (C) 2000  Philip Aston

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

package net.grinder.engine.communication;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;

/**
 * Abuse Java API to avoid a proliferation of temporary objects.
*
 * @author Philip Aston
 * @version $Revision$
 */
class Serialiser
{
    private class MyByteArrayOutputStream extends ByteArrayOutputStream
    {
	public byte[] getBytes() 
	{
	    return buf;
	}
    }

    private final MyByteArrayOutputStream m_byteStream =
	new MyByteArrayOutputStream();

    private final ObjectOutputStream m_out;

    public Serialiser() throws IOException
    {
	m_out = new ObjectOutputStream(m_byteStream);
    }
    

    public byte[] getBytes() 
    {
	return m_byteStream.getBytes();
    }

    public void writeObject(Serializable object) throws IOException
    {
	m_out.reset();
	m_out.writeObject(object);
	m_out.flush();
    }
}


/**
 * <em>Not thread safe.</em>
 *
 * @author Philip Aston
 * @version $Revision$
 */
public class Sender
{
    private MulticastSocket m_localSocket;
    private InetAddress m_multicastAddress;

    private final Serialiser m_serialiser;

    public Sender() throws IOException
    {
	m_serialiser = new Serialiser();

	// Our socket - bind to any port.
	m_localSocket = new MulticastSocket();

	// Remote address.
	m_multicastAddress =
	    InetAddress.getByName(
		CommunicationProperties.getMulticastAddressString());
    }

    public void sendToConsole(Message message) throws IOException
    {
	send(message, CommunicationProperties.getConsoleMulticastPort());
    }
    
    public void sendToGrinder(Message message) throws IOException
    {
	send(message, CommunicationProperties.getGrinderMulticastPort());
    }

    private void send(Message message, int port) throws IOException
    {
	m_serialiser.writeObject(message);

	final byte[] bytes = m_serialiser.getBytes();

	final DatagramPacket packet = new DatagramPacket(bytes, bytes.length,
							 m_multicastAddress,
							 port); 

	m_localSocket.send(packet);
    }
}
