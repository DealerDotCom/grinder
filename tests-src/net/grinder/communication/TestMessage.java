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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;


/**
 *  Unit test case for <code>Message</code>.
 *
 * @author Philip Aston
 * @version $Revision$
 **/
public class TestMessage extends TestCase
{
    public static void main(String[] args)
    {
	TestRunner.run(TestMessage.class);
    }

    public TestMessage(String name)
    {
	super(name);
    }

    public void testSenderInformation() throws Exception
    {
	final Message message = new MyMessage();

	try {
	    message.getSenderGrinderID();
	    fail("Expected RuntimeException");
	}
	catch (RuntimeException e) {
	}

	try {
	    message.getSenderUniqueID();
	    fail("Expected RuntimeException");
	}
	catch (RuntimeException e) {
	}

	try {
	    message.getSequenceNumber();
	    fail("Expected RuntimeException");
	}
	catch (RuntimeException e) {
	}

	message.setSenderInformation("grinderID", "uniqueID", 12345l);

	assertEquals("grinderID", message.getSenderGrinderID());
	assertEquals("uniqueID", message.getSenderUniqueID());
	assertEquals(12345l, message.getSequenceNumber());
    }

    public void testSerialisation() throws Exception
    {
	long sequenceNumber = Integer.MAX_VALUE;

	final Message original0 = new MyMessage();
	original0.setSenderInformation("grinderID", "uniqueID",
				       sequenceNumber++);

	final Message original1 = new MyMessage();
	original1.setSenderInformation("grinderID", "uniqueID",
				       sequenceNumber++);

	final ByteArrayOutputStream byteOutputStream =
	    new ByteArrayOutputStream();

	final ObjectOutputStream objectOutputStream =
	    new ObjectOutputStream(byteOutputStream);

	objectOutputStream.writeObject(original0);
	objectOutputStream.writeObject(original1);

	objectOutputStream.close();

	final ObjectInputStream objectInputStream =
	    new ObjectInputStream(
		new ByteArrayInputStream(byteOutputStream.toByteArray()));

	final Message received0 = (Message)objectInputStream.readObject();
	final Message received1 = (Message)objectInputStream.readObject();

	assertEquals(original0, received0);
	assertEquals(original1, received1);
    }

    public void testEquals() throws Exception
    {
	final Message m1 = new MyMessage();
	final Message m2 = new MyMessage();

	assertTrue("No uninitialised message is equal to another Message",
		   !m1.equals(m2));

	m1.setSenderInformation("grinderID", "uniqueID", 12345l);

	assertTrue("No uninitialised message is equal to another Message",
		   !m1.equals(m2));

	m2.setSenderInformation("grinderID2", "uniqueID", 12345l);

	assertEquals(
	    "Initialised messages equal iff uniqueID and sequenceID equal",
	    m1, m2);

	assertEquals("Reflexive", m2, m1);

	final Message m3 = new MyMessage();
	m3.setSenderInformation("grinderID3", "uniqueID", 12345l);

	assertEquals("Transitive", m2, m3);
	assertEquals("Transitive", m3, m1);

	m2.setSenderInformation("grinderID2", "uniqueID2", 12345l);

	assertTrue(
	    "Initialised messages equal iff uniqueID and sequenceID equal",
	    !m1.equals(m2));

	m1.setSenderInformation("grinderID2", "uniqueID2", 12445l);

	assertTrue(
	    "Initialised messages equal iff uniqueID and sequenceID equal",
	    !m1.equals(m2));
    }

    private static class MyMessage extends Message
    {
    }
}
