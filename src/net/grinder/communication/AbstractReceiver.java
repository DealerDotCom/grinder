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

import java.util.HashMap;
import java.util.Map;


/**
 * Abstract class that manages the receipt of messages.
 *
 * @author Philip Aston
 * @version $Revision$
 **/
abstract class AbstractReceiver implements Receiver
{
    private final Map m_sequenceValues;
    private boolean m_listening = false;

    private final MessageQueue m_messageQueue = new MessageQueue(true);

    /**
     * Constructor.
     **/
    protected AbstractReceiver(boolean checkSequence)
    {
	m_sequenceValues = checkSequence ? new HashMap() : null;
    }

    protected final MessageQueue getMessageQueue()
    {
	return m_messageQueue;
    }

    /**
     * Block until a message is available, or another thread has
     * called {@link #shutdown}. Typically called from a message
     * dispatch loop.
     *
     * <p>Multiple threads can call this method, but only one thread
     * will receive a given message.</p>
     *
     * @return The message or <code>null</code> if shut down.
     * @throws CommunicationException If an error occured receiving a message.
     **/
    public final synchronized Message waitForMessage()
	throws CommunicationException
    {
	final Message message;
	
	try {
	    message = m_messageQueue.dequeue(true);
	}
	catch (MessageQueue.ShutdownException e) {
	    return null;
	}

	final String senderID = message.getSenderUniqueID();
	final long sequenceNumber = message.getSequenceNumber();

	if (m_sequenceValues != null) {
	    final SequenceValue sequenceValue =
		(SequenceValue)m_sequenceValues.get(senderID);

	    if (sequenceValue != null) {
		sequenceValue.nextValue(sequenceNumber, senderID);
	    }
	    else {
		m_sequenceValues.put(senderID,
				     new SequenceValue(sequenceNumber));
	    }
	}
	    
	return message;
    }

    /**
     * Shut down this reciever.
     * @throws CommunicationException If an IO exception occurs.
     **/
    public void shutdown() throws CommunicationException
    {
	m_messageQueue.shutdown();
    }

    /**
     * Numeric sequence checker. Relies on caller for synchronisation.
     **/
    private final class SequenceValue
    {
	private long m_value;

	/**
	 * Constructor.
	 * @param initialValue The initial sequence value.
	 **/
	public SequenceValue(long initialValue) 
	{
	    m_value = initialValue;
	}

	/**
	 * Check the next value in the sequence, and store it for next time.
	 *
	 * @param newValue The next value.
	 * @throws CommunicationException If the message is out of sequence.
	 **/
	public final void nextValue(long newValue, String senderID)
	    throws CommunicationException
	{
	    if (newValue != ++m_value) {
		final CommunicationException e = new CommunicationException(
			"Out of sequence message from Sender '" +
		    senderID + "' (received " + newValue + 
		    ", expected " + m_value + ")");

		m_value = newValue;

		throw e;
	    }
	}
    }
}
