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

package net.grinder.engine.process;

import junit.framework.TestCase;
import junit.swingui.TestRunner;
//import junit.textui.TestRunner;

import net.grinder.common.GrinderProperties;
import net.grinder.common.LogCounter;
import net.grinder.common.Logger;
import net.grinder.communication.ReportStatisticsMessage;
import net.grinder.communication.ResetGrinderMessage;
import net.grinder.communication.Sender;
import net.grinder.communication.SenderImplementation;
import net.grinder.communication.StartGrinderMessage;
import net.grinder.communication.StopGrinderMessage;


/**
 * Unit test case for <code>ConsoleListener</code>.
 *
 * @author Philip Aston
 * @version $Revision$
 **/
public class TestConsoleListener extends TestCase
{
    public static void main(String[] args)
    {
	TestRunner.run(TestConsoleListener.class);
    }

    public TestConsoleListener(String name)
    {
	super(name);
    }

    private GrinderProperties m_nullProperties = new GrinderProperties();
    private GrinderProperties m_properties = new GrinderProperties();
    private Sender m_sender;
    private Logger m_logger = new LogCounter();

    protected void setUp() throws Exception
    {
	m_nullProperties.setBoolean("grinder.receiveConsoleSignals", false);

	m_properties.setBoolean("grinder.receiveConsoleSignals", true);
	m_properties.put("grinder.multicastAddress", "233.1.1.1");
	m_properties.setInt("grinder.multicastPort", 9999);

	m_sender = new SenderImplementation("Test Sender", "233.1.1.1", 9999);
    }

    public void testConstruction() throws Exception
    {
	final MyMonitor myMonitor = new MyMonitor();

	final ConsoleListener listener0 =
	    new ConsoleListener(m_nullProperties, myMonitor, m_logger);

	final ConsoleListener listener1 =
	    new ConsoleListener(m_properties, myMonitor, m_logger);
    }

    public void testNullListener() throws Exception
    {
	final MyMonitor myMonitor = new MyMonitor();


	final ConsoleListener nullListener =
	    new ConsoleListener(m_nullProperties, myMonitor, m_logger);

	final MyMonitor.WaitForMessages t1 =
	    myMonitor.new WaitForMessages(500, nullListener,
					  ConsoleListener.ANY);

	t1.start();
	m_sender.send(new ResetGrinderMessage());
	m_sender.send(new StartGrinderMessage());
	m_sender.send(new StopGrinderMessage());
	m_sender.send(new ReportStatisticsMessage(null));

	t1.join();
	assert(t1.getTimerExpired());
    }

    public void testListener() throws Exception
    {
	final MyMonitor myMonitor = new MyMonitor();

	final ConsoleListener listener =
	    new ConsoleListener(m_properties, myMonitor, m_logger);

	assertEquals(0, listener.received(ConsoleListener.ANY));

	final MyMonitor.WaitForMessages t1 =
	    myMonitor.new WaitForMessages(1000, listener,
					  ConsoleListener.RESET |
					  ConsoleListener.START);
	t1.start();

	m_sender.send(new ResetGrinderMessage());
	m_sender.send(new StartGrinderMessage());
	t1.join();
	assert(!t1.getTimerExpired());
	assertEquals(0, listener.received(ConsoleListener.ANY));

	final MyMonitor.WaitForMessages t2 =
	    myMonitor.new WaitForMessages(1000, listener,
					  ConsoleListener.STOP);
	t2.start();

	m_sender.send(new StartGrinderMessage());
	m_sender.send(new StopGrinderMessage());
	t2.join();
	assert(!t2.getTimerExpired());

	assertEquals(0, listener.received(ConsoleListener.RESET));

	assertEquals(ConsoleListener.START,
		     listener.received(ConsoleListener.START |
				       ConsoleListener.STOP));
    }

    private final static class MyMonitor implements Monitor
    {
	private final class WaitForMessages extends Thread
	{
	    private final long m_time;
	    private final ConsoleListener m_listener;
	    private int m_expectedMessages;
	    private boolean m_timerExpired = false;

	    public WaitForMessages(long time, ConsoleListener listener,
				   int expectedMessages)
	    {
		m_time = time;
		m_listener = listener;
		m_expectedMessages = expectedMessages;
	    }

	    public final boolean getTimerExpired() 
	    {
		return m_timerExpired;
	    }

	    public final void run()
	    {
		synchronized(MyMonitor.this) {
		    long currentTime = System.currentTimeMillis();
		    final long wakeUpTime = currentTime + m_time;
		
		    while (currentTime < wakeUpTime) {
			final int receivedMessages =
			    m_listener.received(m_expectedMessages);

			m_expectedMessages ^= receivedMessages;

			if (m_expectedMessages == 0) {
			    return;
			}

			try {
			    MyMonitor.this.wait(wakeUpTime - currentTime);

			    currentTime = System.currentTimeMillis();

			    if (currentTime >= wakeUpTime) {
				m_timerExpired = true;
			    }
			}
			catch (InterruptedException e) {
			    currentTime = System.currentTimeMillis();
			}
		    }
		}
	    }
	}
    }
}
