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

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;



public abstract class GrinderCommand
{
    final static String MULTICAST_ADDRESS_PROPERTY =
	"grinder.multicastAddress";
    final static String MULTICAST_PORT_PROPERTY = "grinder.multicastPort";

    private static MulticastSocket m_localSocket;

    // The address and port of the target socket.
    private static InetAddress m_address;
    private final static int m_port =
	Integer.getInteger(MULTICAST_PORT_PROPERTY).intValue();

    public void send()
    {
	try {
	    DatagramPacket packet =
		new DatagramPacket(getBuffer(), getLength(), getAddress(),
				   m_port); 

	    getLocalSocket().send(packet);
	}
	catch (IOException e) {
	    // FIXME
	    e.printStackTrace();
	}
    }

    public abstract byte[] getBuffer();

    public abstract int getLength();

    /** Return our local socket, creating it if necessary. Bind it to
     * any port.
     */
    private static DatagramSocket getLocalSocket() throws IOException
    {
	if (m_localSocket == null) {
	    synchronized (DatagramSocket.class) {
		if (m_localSocket == null) { // Double-checked locking.
		    // Our socket - bind to any port.
		    m_localSocket = new MulticastSocket();
		}
	    }
	}
	
	return m_localSocket;
    }

    private static InetAddress getAddress() throws IOException
    {
	if (m_address == null) {
	    synchronized (DatagramSocket.class) {
		if (m_address == null) { // Double-checked locking.
		    m_address =
			InetAddress.getByName(
			    System.getProperty(MULTICAST_PORT_PROPERTY));
		}
	    }
	}
	
	return m_address;
    }
}
