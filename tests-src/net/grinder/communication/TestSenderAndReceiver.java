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

import java.util.Random;


/**
 * @author Philip Aston
 * @version $Revision$
 */
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
	m_sender = new Sender(MULTICAST_ADDRESS, MULTICAST_PORT);
    }

    public void testSendSimpleMessage() throws Exception
    {
	final ReceiverThread receiverThread = new ReceiverThread();

	receiverThread.start();

	// Hmm.. can't think of an easy way to ensure the
	// receiverThread is listening before we do this. Test seems
	// to work anyway. Hey ho.
	final Message sentMessage = new SimpleMessage(0);
	m_sender.send(sentMessage);

	receiverThread.join();

	final Message receivedMessage = receiverThread.getMessage();
	assertEquals(sentMessage, receivedMessage);
	assert(sentMessage != receivedMessage);
    }

    public void testSendManyMessages() throws Exception
    {
	for (int i=0; i<100; i++)
	{
	    final ReceiverThread receiverThread = new ReceiverThread();

	    receiverThread.start();

	    final SimpleMessage sentMessage = new SimpleMessage(0);
	    m_sender.send(sentMessage);

	    receiverThread.join();

	    final SimpleMessage receivedMessage =
		(SimpleMessage)receiverThread.getMessage();

	    assertEquals(sentMessage, receivedMessage);
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
    }

    private class ReceiverThread extends Thread
    {
	private Message m_message;
	private Exception m_exception;

	Message getMessage()
	{
	    return m_message;
	}

	Exception getException()
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

    private  static class SimpleMessage implements Message
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

	public boolean equals(Object o) 
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

	public String toString()
	{
	    return "(" + m_text + ", " + m_random + ")";
	}
    }
}
