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


/**
 * Base class for messages.
 * 
 * @author Philip Aston
 * @version $Revision$
 */
public abstract class Message implements java.io.Serializable
{
    /**  The ID of the Grinder process which owns this {@link Sender}. **/
    private String m_senderGrinderID = null;

    /** Unique ID of {@link Sender}. **/
    private String m_senderUniqueID = null;

    /** Sequence ID of message. **/
    private long m_sequenceNumber = -1;

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
}
