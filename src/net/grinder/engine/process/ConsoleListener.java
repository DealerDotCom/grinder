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

import net.grinder.common.GrinderProperties;
import net.grinder.common.Logger;
import net.grinder.communication.CommunicationDefaults;
import net.grinder.communication.CommunicationException;
import net.grinder.communication.Message;
import net.grinder.communication.Receiver;
import net.grinder.communication.ResetGrinderMessage;
import net.grinder.communication.StartGrinderMessage;
import net.grinder.communication.StopGrinderMessage;
import net.grinder.engine.EngineException;


/**
 * Active object which listens for console messages.
 *
 * @author Philip Aston
 * @version $Revision$
 * @see net.grinder.engine.process.GrinderProcess
 **/
final class ConsoleListener
{
    public final static int START = 1 << 0;
    public final static int RESET = 1 << 1;
    public final static int STOP =  1 << 2;
    public final static int ANY = START | RESET | STOP;

    private final Monitor m_notifyOnMessage;
    private final Logger m_logger;
    private int m_messagesReceived = 0;

    /**
     * Constructor that creates an appropriate {@link ConsoleListener}
     * based on the passed {@link GrinderProperties}
     *
     * <p>If <code>properties</code> specifies that this process
     * should receive console signals, a thread is created to listen
     * for messages. Otherwise we simply do nothing and {@link
     * #received} will always return 0. </p>
     *
     * @param properties The {@link GrinderProperties}
     * @param notifyOnMessage A {@link Monitor} to notify when a message arrives.
     * @param logger A {@link Logger} to log receive event messages to.
     * @exception CommunicationException If a multicast error occurs.
     **/
    public ConsoleListener(GrinderProperties properties,
			   Monitor notifyOnMessage, Logger logger)
	throws CommunicationException
    {
	m_notifyOnMessage = notifyOnMessage;
	m_logger = logger;

	if (properties.getBoolean("grinder.receiveConsoleSignals", true)) {

	    // Parse console configuration.
	    final String multicastAddress =
		properties.getProperty(
		    "grinder.multicastAddress",
		    CommunicationDefaults.MULTICAST_ADDRESS);

	    final int grinderPort =
		properties.getInt("grinder.multicastPort",
				  CommunicationDefaults.GRINDER_PORT);

	    final ReceiverThread receiverThread =
		new ReceiverThread(multicastAddress, grinderPort);

	    receiverThread.setDaemon(true);
	    receiverThread.start();
	}
    }

    /**
     * The <code>ConsoleListener</code> has a bit mask representing
     * messages received but not acknowledged. This method returns a
     * bit mask representing the messages received that match the
     * <code>mask</code> parameter and acknowledges the messages
     * representingt by <code>mask</code>.
     *
     * @param mask The messages to check for.
     * @return The subset of <code>mask</code> received.
     **/
    public final synchronized int received(int mask)
    {
	final int intersection = m_messagesReceived & mask;

	try {
	    return intersection;
	}
	finally {
	    m_messagesReceived ^= intersection;
	}
    }

    /**
     * Thread that uses a {@link net.grinder.communication.Receiver}
     * to receive console messages.
     **/
    private final class ReceiverThread extends Thread
    {
	private final Receiver m_receiver;

	/**
	 * Creates a new <code>ReceiverThread</code> instance.
	 *
	 * @param address Console multicast address.
	 * @param port Console multicast port.
	 * @exception CommunicationException If an error occurs binding to the multicast port.
	 **/
	private ReceiverThread(String address, int port)
	    throws CommunicationException
	{
	    super("Console Listener");

	    m_receiver = new Receiver(address, port);
	}

	/**
	 * Event loop that receives messages from the console.
	 **/
	public final void run()
	{
	    while (true) {
		final Message message;
		
		try {
		    message = m_receiver.waitForMessage();
		}
		catch (CommunicationException e) {
		    m_logger.logError("error receiving console signal: " + e,
				      Logger.LOG | Logger.TERMINAL);
		    continue;
		}

		if (message instanceof StartGrinderMessage) {
		    m_logger.logMessage("got a start message from console");
		    setReceived(START);
		}
		else if (message instanceof StopGrinderMessage) {
		    m_logger.logMessage("got a stop message from console");
		    setReceived(STOP);
		}
		else if (message instanceof ResetGrinderMessage) {
		    m_logger.logMessage("got a reset message from console");
		    setReceived(RESET);
		}
		else {
		    m_logger.logMessage("got an unknown message from console");
		}
	    }
	}

	private final void setReceived(int message)
	{
	    synchronized (ConsoleListener.this) {
		m_messagesReceived |= message;
	    }

	    synchronized (m_notifyOnMessage) {
		m_notifyOnMessage.notifyAll();
	    }
	}
    }
}

