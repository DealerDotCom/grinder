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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;


/**
 * Class that manages the receipt of multicast messages.
 *
 * @author Philip Aston
 * @version $Revision$
 */
public final class MulticastReceiver extends AbstractReceiver
{
    private final byte[] m_buffer = new byte[65536];
    private final MulticastSocket m_socket;
    private final DatagramPacket m_packet;


    /**
     * Constructor.
     *
     * @param multicastAddressString The multicast address to bind on.
     * @param multicastPort The port to bind to.
     *
     * @ throws CommunicationException If socket could not be bound to.
     **/
    public MulticastReceiver(String multicastAddressString, int multicastPort)
	throws CommunicationException
    {
	try {
	    m_socket = new MulticastSocket(multicastPort);
	    m_socket.joinGroup(InetAddress.getByName(multicastAddressString));
	}
	catch (IOException e) {
	    throw new CommunicationException(
		"Could not bind to multicast address " +
		multicastAddressString + ":" + multicastPort, e);
	}

	m_packet = new DatagramPacket(m_buffer, m_buffer.length);

	new ListenThread().start();
    }

    private final class ListenThread extends Thread
    {
	public ListenThread()
	{
	    super("Multicast listen thread");
	    setDaemon(true);
	}

	public void run()
	{
	    final MessageQueue messageQueue = getMessageQueue();

	    final ByteArrayInputStream byteStream =
		new ByteArrayInputStream(m_buffer, 0, m_buffer.length);

	    try {
		while (true) {
		    try {
			m_packet.setData(m_buffer, 0, m_buffer.length);
			m_socket.receive(m_packet);

			byteStream.reset();

			// ObjectInputStream does not support reset(),
			// we need a new one each time.
			final ObjectInputStream objectStream =
			    new ObjectInputStream(byteStream);

			messageQueue.queue((Message)objectStream.readObject());
		    }
		    catch (ClassNotFoundException e) {
			// Propagate exceptions to threads calling
			// waitForMessage.
			messageQueue.queue(e);
		    }
		    catch (IOException e) {
			// Propagate exceptions to threads calling
			// waitForMessage.
			messageQueue.queue(e);
		    }
		}
	    }
	    catch (MessageQueue.ShutdownException e) {
		// We've been shutdown, exit this thread.
	    }
	}
    }
}
