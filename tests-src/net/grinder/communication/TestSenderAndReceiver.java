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

import junit.framework.TestCase;
import junit.swingui.TestRunner;
//import junit.textui.TestRunner;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;


/**
 *  Unit test case for <code>Sender</code> and <code>Receiver</code>.
 *
 * @author Philip Aston
 * @version $Revision$
 **/
public class TestSenderAndReceiver extends TestCase
{
    public static void main(String[] args)
    {
	TestRunner.run(TestSenderAndReceiver.class);
    }

    public TestSenderAndReceiver(String name)
    {
	super(name);
    }

    private final String MULTICAST_ADDRESS="237.0.0.1";
    private final int MULTICAST_PORT=1234;

    private Receiver m_receiver;
    private Sender m_sender;

    protected void setUp() throws Exception
    {
	m_receiver = new Receiver(MULTICAST_ADDRESS, MULTICAST_PORT);
	m_sender = new SenderImplementation("Test Sender", MULTICAST_ADDRESS,
					    MULTICAST_PORT);
    }

    public void testSendSimpleMessage() throws Exception
    {
	final ReceiverThread receiverThread = new ReceiverThread();

	receiverThread.start();

	// Hmm.. can't think of an easy way to ensure the
	// receiverThread is listening before we do this. Test seems
	// to work anyway. Hey ho.
	final SimpleMessage sentMessage = new SimpleMessage(0);
	m_sender.send(sentMessage);

	receiverThread.join();

	final Message receivedMessage = receiverThread.getMessage();
	assertEquals(sentMessage, receivedMessage);
	assert(sentMessage.payloadEquals(receivedMessage));
	assert(sentMessage != receivedMessage);
    }

    public void testSendManyMessages() throws Exception
    {
	long sequenceNumber = -1;

	for (int i=0; i<100; i++)
	{
	    final ReceiverThread receiverThread = new ReceiverThread();

	    receiverThread.start();

	    final SimpleMessage sentMessage = new SimpleMessage(i);
	    m_sender.send(sentMessage);

	    receiverThread.join();

	    final SimpleMessage receivedMessage =
		(SimpleMessage)receiverThread.getMessage();

	    if (sequenceNumber != -1) {
		assertEquals(sequenceNumber+1,
			     receivedMessage.getSequenceNumber());
	    }

	    sequenceNumber = receivedMessage.getSequenceNumber();

	    assertEquals(sentMessage, receivedMessage);
	    assert(sentMessage.payloadEquals(receivedMessage));
	    assert(sentMessage != receivedMessage);
	}
    }

    public void testSendLargeMessage() throws Exception
    {
	final ReceiverThread receiverThread = new ReceiverThread();

	receiverThread.start();

	// This causes a message size of about 38K. Should be limited
	// by the buffer size in Receiver.
	final SimpleMessage sentMessage = new SimpleMessage(8000);
	m_sender.send(sentMessage);

	receiverThread.join();

	final SimpleMessage receivedMessage =
	    (SimpleMessage)receiverThread.getMessage();

	assertEquals(sentMessage, receivedMessage);
	assert(sentMessage.payloadEquals(receivedMessage));
	assert(sentMessage != receivedMessage);
    }

    public void testShutdownReciever() throws Exception
    {
	final ReceiverThread receiverThread = new ReceiverThread();
	receiverThread.start();

	m_receiver.shutdown();

	receiverThread.join();

	assertNull(receiverThread.getMessage());
    }

    public void testTwoListenersException() throws Exception
    {
	final ReceiverThread r1 = new ReceiverThread();
	final ReceiverThread r2 = new ReceiverThread();
	r1.start();
	r2.start();

	Thread.yield();		// Give threads time to hit waitForMessage().
	m_receiver.shutdown();

	r1.join();
	r2.join();

	assert(r1.getException() == null ^ r2.getException() == null);
    }

    public void testQueueAndFlush() throws Exception
    {
	long sequenceNumber = -1;

	// This number is deliberately low. Tests show that the
	// multicast buffer on my NT machine only holds between about
	// 30 and 70 SimpleMessage(0)'s before dropping the least
	// recent message. Really need something more reliable than
	// this.
	SimpleMessage[] messages = new SimpleMessage[25];

	for (int i=0; i<messages.length; ++i)
	{
	    messages[i] = new SimpleMessage(0);
	    m_sender.queue(messages[i]);
	}

	final ReceiveNMessagesThread receiverThread =
	    new ReceiveNMessagesThread(messages.length);

	receiverThread.start();

	m_sender.flush();

	receiverThread.join();

	final SimpleMessage[] receivedMessages =
	    (SimpleMessage[])
	    receiverThread.getMessages().toArray(new SimpleMessage[0]);

	assertEquals(messages.length, receivedMessages.length);

	for (int i=0; i<messages.length; ++i) {
	    if (sequenceNumber != -1) {
		assertEquals(sequenceNumber+1,
			     receivedMessages[i].getSequenceNumber());
	    }

	    sequenceNumber = receivedMessages[i].getSequenceNumber();

	    assertEquals(messages[i], receivedMessages[i]);
	    assert(messages[i].payloadEquals(receivedMessages[i]));
	    assert(messages[i] != receivedMessages[i]);
	}
    }

    public void testQueueAndSend() throws Exception
    {
	long sequenceNumber = -1;

	// This number is deliberately low. Tests show that the
	// multicast buffer on my NT machine only holds between about
	// 30 and 70 SimpleMessage(0)'s before dropping the least
	// recent message. Really need something more reliable than
	// this.
	SimpleMessage[] messages = new SimpleMessage[25];

	for (int i=0; i<messages.length; ++i)
	{
	    messages[i] = new SimpleMessage(0);
	    m_sender.queue(messages[i]);
	}

	final ReceiveNMessagesThread receiverThread =
	    new ReceiveNMessagesThread(messages.length+1);

	receiverThread.start();

	final SimpleMessage finalMessage = new SimpleMessage(0);
	m_sender.send(finalMessage);

	receiverThread.join();

	final SimpleMessage[] receivedMessages =
	    (SimpleMessage[])
	    receiverThread.getMessages().toArray(new SimpleMessage[0]);

	assertEquals(messages.length + 1, receivedMessages.length);

	for (int i=0; i<messages.length; ++i) {
	    if (sequenceNumber != -1) {
		assertEquals(sequenceNumber+1,
			     receivedMessages[i].getSequenceNumber());
	    }

	    sequenceNumber = receivedMessages[i].getSequenceNumber();

	    assertEquals(messages[i], receivedMessages[i]);
	    assert(messages[i].payloadEquals(receivedMessages[i]));
	    assert(messages[i] != receivedMessages[i]);
	}

	final SimpleMessage receivedFinalMessage =
	    receivedMessages[messages.length];

	assertEquals(sequenceNumber+1,
		     receivedFinalMessage.getSequenceNumber());
	assertEquals(finalMessage, receivedFinalMessage);
	assert(finalMessage.payloadEquals(receivedFinalMessage));
	assert(finalMessage != receivedFinalMessage);
    }

    private class ReceiverThread extends Thread
    {
	private Message m_message;
	private Exception m_exception;

	public Message getMessage()
	{
	    return m_message;
	}

	public Exception getException()
	{
	    return m_exception;
	}

	public void run()
	{
	    m_message = null;
	    m_exception = null;

	    try {
		m_message = m_receiver.waitForMessage();
	    }
	    catch (Exception e) {
		m_exception = e;
	    }
	}
    }

    private class ReceiveNMessagesThread extends Thread
    {
	private List m_messages = new LinkedList();
	private Exception m_exception;
	private int m_howMany;

	public ReceiveNMessagesThread(int howMany) 
	{
	    m_howMany = howMany;
	}

	public List getMessages()
	{
	    return m_messages;
	}

	public Exception getException()
	{
	    return m_exception;
	}

	public void run()
	{
	    m_exception = null;

	    try {
		while (m_howMany-- > 0) {
		    m_messages.add(m_receiver.waitForMessage());
		}
	    }
	    catch (Exception e) {
		m_exception = e;
	    }
	}
    }

    private  static class SimpleMessage extends Message
    {
	private static Random s_random = new Random();

	private final String m_text = "Some message";
	private final int m_random = s_random.nextInt();
	private final int[] m_padding;

	public SimpleMessage(int paddingSize)
	{
	    m_padding = new int[paddingSize];

	    for (int i=0; i<paddingSize; i++) {
		m_padding[i] = i;
	    }
	}

	public String toString()
	{
	    return "(" + m_text + ", " + m_random + ")";
	}

	public boolean payloadEquals(Message o) 
	{
	    if (o == this) {
		return true;
	    }

	    if (!(o instanceof SimpleMessage)) {
		return false;
	    }

	    final SimpleMessage other = (SimpleMessage)o;

	    return
		m_text.equals(other.m_text) &&
		m_random == other.m_random;
	}
    }
}
