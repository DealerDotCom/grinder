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
	    fail("Expected CommunicationException");
	}
	catch (CommunicationException e) {
	}

	try {
	    message.getSenderUniqueID();
	    fail("Expected CommunicationException");
	}
	catch (CommunicationException e) {
	}

	try {
	    message.getSequenceNumber();
	    fail("Expected CommunicationException");
	}
	catch (CommunicationException e) {
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

	assert("No uninitialised message is equal to another Message",
	       !m1.equals(m2));

	m1.setSenderInformation("grinderID", "uniqueID", 12345l);

	assert("No uninitialised message is equal to another Message",
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

	assert("Initialised messages equal iff uniqueID and sequenceID equal",
	       !m1.equals(m2));

	m1.setSenderInformation("grinderID2", "uniqueID2", 12445l);

	assert("Initialised messages equal iff uniqueID and sequenceID equal",
	       !m1.equals(m2));
    }

    private static class MyMessage extends Message
    {
    }
}
