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

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.LinkedList;
import java.security.MessageDigest;


/**
 * Abstract class that manages the sending of messages.
 *
 * @author Philip Aston
 * @version $Revision$
 **/
abstract class AbstractSender implements Sender
{
    private final String m_grinderID;
    private String m_senderID;
    private long m_nextSequenceID = 0;
    private MessageQueue m_messageQueue = new MessageQueue(false);

    private final MyByteArrayOutputStream m_scratchByteStream =
	new MyByteArrayOutputStream();

    protected AbstractSender(String grinderID)
    {
	m_grinderID = grinderID;
    }

    protected final void setSenderID(String uniqueString)
	throws CommunicationException
    {
	try {
	    final BufferedWriter bufferedWriter = new BufferedWriter(
		new OutputStreamWriter(m_scratchByteStream));

	    bufferedWriter.write(uniqueString);
	    bufferedWriter.flush();

	    m_senderID =
		new String(MessageDigest.getInstance("MD5").digest(
			       m_scratchByteStream.getBytes()));
	}
	catch (Exception e) {
	    throw new CommunicationException("Could not calculate sender ID",
					     e);
	}
    }

    /**
     * First flush any pending messages queued with {@link #queue} and
     * then send the given message.
     *
     * @param message A {@link Message}.
     * @exception CommunicationException If an error occurs.
     **/
    public final void send(Message message) throws CommunicationException
    {
	synchronized(m_messageQueue.getMutex()) {
	    queue(message);
	    flush();
	}
    }

    /**
     * Queue the given message for later sending.
     *
     * @param message A {@link Message}.
     * @exception CommunicationException If an error occurs.
     * @see #flush
     * @see #send
     **/
    public final void queue(Message message) throws CommunicationException
    {
	synchronized (this) {
	    message.setSenderInformation(m_grinderID, m_senderID,
					 m_nextSequenceID++);
	}

	try {
	    m_messageQueue.queue(message);
	}
	catch (MessageQueue.ShutdownException e) {
	    // Assertion failure.
	    throw new RuntimeException(
		"MessageQueue unexpectedly shutdown");
	}
    }

    /**
     * Flush any pending messages queued with {@link #queue}.
     *
     * @exception CommunicationException if an error occurs
     **/
    public final void flush() throws CommunicationException
    {
	try {
	    synchronized (m_messageQueue.getMutex()) {
		Message message;

		while ((message = m_messageQueue.dequeue(false)) != null) {
		    writeMessage(message);
		}
	    }
	}
	catch (IOException e) {
	    throw new CommunicationException(
		"Exception whilst sending message", e);
	}
	catch (MessageQueue.ShutdownException e) {
	    // Assertion failure.
	    throw new RuntimeException(
		"MessageQueue unexpectedly shutdown");
	}
    }

    protected abstract void writeMessage(Message message) throws IOException;

    /**
     * Cleanly shutdown the <code>Sender</code>.
     *
     * <p>Any queued messages are discarded.</p>
     *
     * @exception CommunicationException If an error occurs.
     **/
    public void shutdown() throws CommunicationException
    {
	m_messageQueue.shutdown();
    }

    protected final MyByteArrayOutputStream getScratchByteStream()
    {
	return m_scratchByteStream;
    }

    /**
     * Abuse Java API to avoid needless proliferation of temporary
     * objects.
     * @author Philip Aston
     **/
    protected final static class MyByteArrayOutputStream
	extends ByteArrayOutputStream
    {
	public byte[] getBytes() 
	{
	    return buf;
	}
    }
}
