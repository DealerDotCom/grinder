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
import java.util.HashMap;
import java.util.Map;


/**
 * <em>Not thread safe.</em>
 *
 * @author Philip Aston
 * @version $Revision$
 */
public class Receiver
{
    private final byte[] m_buffer = new byte[65536];
    private final String m_multicastAddressString;
    private final int m_multicastPort;
    private final MulticastSocket m_socket;
    private final DatagramPacket m_packet;
    private final Map m_sequenceValues = new HashMap();
    private boolean m_shuttingDown = false;
    private boolean m_shutDown = false;
    private boolean m_listening = false;

    public Receiver(String multicastAddressString, int multicastPort)
	throws CommunicationException
    {
	m_multicastAddressString = multicastAddressString;
	m_multicastPort = multicastPort;

	try {
	    m_socket = new MulticastSocket(m_multicastPort);
	    m_socket.joinGroup(
		InetAddress.getByName(m_multicastAddressString));
	}
	catch (IOException e) {
	    throw new CommunicationException(
		"Could not bind to multicast address " +
		multicastAddressString + ":" + multicastPort, e);
	}

	m_packet = new DatagramPacket(m_buffer, m_buffer.length);
    }

    /**
     * Only one thread should call this method at any one time.
     * Typically called from a message dispatch loop.
     *
     * @return The message or null if shutting down.
     **/
    public Message waitForMessage() throws CommunicationException
    {
	synchronized (this) {
	    if (m_listening) {
		throw new CommunicationException(
		    "More than one thread called waitForMessage()");
	    }

	    m_listening = true;
	}

	try {
	    final Message message;

	    if (m_shuttingDown) {
		shutdownComplete();
		return null;
	    }
	
	    try {
		m_packet.setData(m_buffer, 0, m_buffer.length);
		m_socket.receive(m_packet);

		final ByteArrayInputStream byteStream =
		    new ByteArrayInputStream(m_buffer, 0, m_buffer.length);

		final ObjectInputStream objectStream =
		    new ObjectInputStream(byteStream);

		message = (Message)objectStream.readObject();
	    }
	    catch (Exception e) {
		throw new CommunicationException(
		    "Error receving multicast packet", e);
	    }

	    if (message instanceof ShutdownMessage) {
		shutdownComplete();
		return null;
	    }

	    final String senderID = message.getSenderUniqueID();
	    final long sequenceNumber = message.getSequenceNumber();

	    final SequenceValue sequenceValue =
		(SequenceValue)m_sequenceValues.get(senderID);

	    if (sequenceValue != null) {
		sequenceValue.nextValue(sequenceNumber);
	    }
	    else {
		m_sequenceValues.put(senderID,
				     new SequenceValue(sequenceNumber));
	    }
	    
	    return message;
	}
	finally {
	    synchronized (this) {
		m_listening = false;
	    }
	}
    }

    /**
     * Shut down this reciever. Assumes some other thread is blocked
     * in, or will call, waitForMessage.
     **/
    public void shutdown() throws CommunicationException
    {
	if (!m_shutDown) {
	    m_shuttingDown = true;

	    final boolean needSuicideMessage;

	    synchronized(this) {
		needSuicideMessage = m_listening;
	    }

	    if (needSuicideMessage) {
		// Pretty hacky way of shutting down the receiver. The
		// packet goes out on the wire. Can't do much else
		// with the DatagramSocket API though.
		new Sender("suicide is painless", m_multicastAddressString,
			   m_multicastPort).send(new ShutdownMessage());
	    }
	    else {
		shutdownComplete();
	    }

	    while (!m_shutDown) {
		try {
		    synchronized (this) {
			wait();
		    }
		}
		catch (InterruptedException e) {
		}
	    }
	}
    }

    private synchronized void shutdownComplete()
    {
	m_shutDown = true;
	notifyAll();
    }

    private class SequenceValue
    {
	private long m_value;

	public SequenceValue(long initialValue) 
	{
	    m_value = initialValue;
	}

	public void nextValue(long newValue)
	    throws CommunicationException
	{
	    if (newValue != ++m_value) {
		final CommunicationException e =
		    new CommunicationException(
			"Out of sequence message (received " + newValue + 
			", expected " + m_value + ")");

		m_value = newValue;

		throw e;
	    }
	}
    }

    private static final class ShutdownMessage extends Message
    {
    }
}
