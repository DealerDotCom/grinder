// Copyright (C) 2000 Paco Gomez
// Copyright (C) 2000, 2001, 2002 Philip Aston
// All rights reserved.
//
// This file is part of The Grinder software distribution. Refer to
// the file LICENSE which is part of The Grinder distribution for
// licensing details. The Grinder distribution is available on the
// Internet at http://grinder.sourceforge.net/
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
// "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
// LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
// FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
// REGENTS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
// INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
// (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
// SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
// HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
// STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
// ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
// OF THE POSSIBILITY OF SUCH DAMAGE.

package net.grinder.communication;

import junit.framework.TestCase;
import junit.swingui.TestRunner;
//import junit.textui.TestRunner;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;


/**
 *  Abstract unit test cases for <code>Sender</code> and
 *  <code>Receiver</code> implementations..
 *
 * @author Philip Aston
 * @version $Revision$
 **/
public abstract class AbstractSenderAndReceiverTests extends TestCase
{
    public AbstractSenderAndReceiverTests(String name)
    {
	super(name);
    }

    private static Random s_random = new Random();
    protected Receiver m_receiver;
    protected Sender m_sender;

    protected abstract Receiver createReceiver() throws Exception;
    protected abstract Sender createSender() throws Exception;

    public void testSendSimpleMessage() throws Exception
    {
	final ReceiverThread receiverThread = new ReceiverThread();

	final SimpleMessage sentMessage = new SimpleMessage(0);
	m_sender.send(sentMessage);

	final Message receivedMessage = m_receiver.waitForMessage();
	assertEquals(sentMessage, receivedMessage);
	assert(sentMessage.payloadEquals(receivedMessage));
	assert(sentMessage != receivedMessage);
    }

    public void testSendManyMessages() throws Exception
    {
	long sequenceNumber = -1;

	for (int i=1; i<=10; ++i)
	{
	    final SimpleMessage[] sentMessages = new SimpleMessage[i];

	    for (int j=0; j<i; ++j) {
		sentMessages[j] = new SimpleMessage(i);
		m_sender.send(sentMessages[j]);
	    }

	    for (int j=0; j<i; ++j) {
		final SimpleMessage receivedMessage =
		    (SimpleMessage)m_receiver.waitForMessage();

		if (sequenceNumber != -1) {
		    assertEquals(sequenceNumber+1,
				 receivedMessage.getSequenceNumber());
		}

		sequenceNumber = receivedMessage.getSequenceNumber();

		assert(sentMessages[j].payloadEquals(receivedMessage));
		assert(sentMessages[j] != receivedMessage);
	    }
	}
    }

    static int s_numberOfMessages = 0;

    private class SenderThread extends Thread
    {
	public void run()
	{
	    try {
		final Sender m_sender = createSender();

		final int n = s_random.nextInt(10);

		for (int i=0; i<n; ++i) {
		    m_sender.send(new SimpleMessage(1));
		    sleep(s_random.nextInt(30));
		}

		synchronized(Sender.class) {
		    s_numberOfMessages += n;
		}

		m_sender.shutdown();
	    }
	    catch (Exception e) {
		e.printStackTrace();
	    }
	}
    }

    public void testManySenders() throws Exception
    {
	s_numberOfMessages = 0;

	final Thread[] senderThreads = new Thread[5];

	for (int i=0; i<senderThreads.length; ++i) {
	    senderThreads[i] = new SenderThread();
	    senderThreads[i].start();
	}

	for (int i=0; i<senderThreads.length; ++i) {
	    senderThreads[i].join();
	}

	for (int i=0; i<s_numberOfMessages; ++i) {
	    m_receiver.waitForMessage();
	}
    }

    public void testSendLargeMessage() throws Exception
    {
	// This causes a message size of about 38K. Should be limited
	// by the buffer size in Receiver.
	final SimpleMessage sentMessage = new SimpleMessage(8000);
	m_sender.send(sentMessage);

	final SimpleMessage receivedMessage =
	    (SimpleMessage)m_receiver.waitForMessage();

	assertEquals(sentMessage, receivedMessage);
	assert(sentMessage.payloadEquals(receivedMessage));
	assert(sentMessage != receivedMessage);
    }

    public void testShutdownReceiver() throws Exception
    {
	m_receiver.shutdown();
	assertNull(m_receiver.waitForMessage());
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

	m_sender.flush();

	for (int i=0; i<messages.length; ++i) {
	    final Message receivedMessage = m_receiver.waitForMessage();

	    if (sequenceNumber != -1) {
		assertEquals(sequenceNumber+1,
			     receivedMessage.getSequenceNumber());
	    }

	    sequenceNumber = receivedMessage.getSequenceNumber();

	    assertEquals(messages[i], receivedMessage);
	    assert(messages[i].payloadEquals(receivedMessage));
	    assert(messages[i] != receivedMessage);
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

	final SimpleMessage finalMessage = new SimpleMessage(0);
	m_sender.send(finalMessage);

	for (int i=0; i<messages.length; ++i) {
	    final Message receivedMessage = m_receiver.waitForMessage();

	    if (sequenceNumber != -1) {
		assertEquals(sequenceNumber+1,
			     receivedMessage.getSequenceNumber());
	    }

	    sequenceNumber = receivedMessage.getSequenceNumber();

	    assertEquals(messages[i], receivedMessage);
	    assert(messages[i].payloadEquals(receivedMessage));
	    assert(messages[i] != receivedMessage);
	}

	final Message receivedFinalMessage = m_receiver.waitForMessage();

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

    private static class SimpleMessage extends Message
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
