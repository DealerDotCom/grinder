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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;


/**
 * Base class for messages.
 * 
 * @author Philip Aston
 * @version $Revision$
 */
public abstract class Message implements Serializable
{
    private static final long serialVersionUID = 6389542594440493966L;

    /**  The ID of the Grinder process which owns this {@link Sender}. **/
    private transient String m_senderGrinderID = null;

    /** Unique ID of {@link Sender}. **/
    private transient String m_senderUniqueID = null;

    /** Sequence ID of message. **/
    private transient long m_sequenceNumber = -1;

    /**
     * Called by {@link Sender} before dispatching the Message.
     **/
    final void setSenderInformation(String grinderID, String senderUniqueID,
				    long sequenceNumber) 
    {
	m_senderGrinderID = grinderID;
	m_senderUniqueID = senderUniqueID;
	m_sequenceNumber = sequenceNumber;
    }

    /**
     * Returns a string describing the Grinder process associated of the {@link Sender}.
     *
     * @throws CommunicationException If {@link #setSenderInformation} has not been called.
     **/
    final String getSenderGrinderID() throws CommunicationException
    {
	assertInitialised();
	return m_senderGrinderID;
    }

    /**
     * Returns a unique ID for the {@link Sender}.
     *
     * @throws CommunicationException If {@link #setSenderInformation} has not been called.
     **/
    final String getSenderUniqueID() throws CommunicationException
    {
	assertInitialised();
	return m_senderUniqueID;
    }

    /**
     * Get the message sequence ID.
     *
     * @throws CommunicationException If {@link #setSenderInformation} has not been called.
     **/
    final long getSequenceNumber() throws CommunicationException
    {
	assertInitialised();
	return m_sequenceNumber;
    }

  /**
     * @throws CommunicationException If {@link #setSenderInformation} has not been called.
     **/
    private final void assertInitialised() throws CommunicationException
    {
	if (m_senderUniqueID == null) {
	    throw new CommunicationException("Message not initialised");
	}
    }

    /**
     * Customise serialisation for efficiency.
     *
     * @param out The stream to write our data to.
     **/
    private void writeObject(ObjectOutputStream out)
	throws IOException
    {
	out.defaultWriteObject();
	out.writeUTF(m_senderGrinderID);
	out.writeUTF(m_senderUniqueID);
	out.writeLong(m_sequenceNumber);
    }

    /**
     * Customise serialisation for efficiency.
     *
     * @param in The stream to read our data from.
     **/
    private void readObject(ObjectInputStream in)
	throws IOException, ClassNotFoundException
    {
	in.defaultReadObject();
	m_senderGrinderID = in.readUTF();
	m_senderUniqueID = in.readUTF();
	m_sequenceNumber = in.readLong();
    }

    /**
     * Compare two Messages. Sent messages have enhanced equality
     * semantics - they are equivalent if they have the same sender ID
     * and sequnce number.
     *
     * @param o The other object.
     **/
    public final boolean equals(Object o)
    {
	if (o == this) {
	    return true;
	}

	if (!(o instanceof Message)) {
	    return false;
	}
	
	final Message message = (Message)o;

	return
	    m_sequenceNumber != -1 && 
	    m_sequenceNumber == message.m_sequenceNumber &&
	    m_senderUniqueID.equals(message.m_senderUniqueID);
    }
}
