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
 *  Unit test case for <code>MulticastSender</code> and
 *  <code>MulticastReceiver</code>.
 *
 * @author Philip Aston
 * @version $Revision$
 **/
public class TestMulticastSenderAndReceiver
    extends AbstractSenderAndReceiverTests
{
    public static void main(String[] args)
    {
	TestRunner.run(TestMulticastSenderAndReceiver.class);
    }

    public TestMulticastSenderAndReceiver(String name)
    {
	super(name);
    }

    private final String MULTICAST_ADDRESS="237.0.0.1";
    private final int MULTICAST_PORT=1234;

    protected Receiver createReceiver() throws Exception
    {
	return new MulticastReceiver(MULTICAST_ADDRESS, MULTICAST_PORT);
    }

    protected Sender createSender() throws Exception
    {
	return new MulticastSender("Test Sender", MULTICAST_ADDRESS,
				   MULTICAST_PORT);
    }

    /**
     * Sigh, JUnit treats setUp and tearDown as non-virtual methods -
     * must define in concrete test case class.
     **/
    protected void setUp() throws Exception
    {
	m_receiver = createReceiver();
	m_sender = createSender();
    }

    protected void tearDown() throws Exception
    {
	m_receiver.shutdown();
	m_sender.shutdown();
    }
}
