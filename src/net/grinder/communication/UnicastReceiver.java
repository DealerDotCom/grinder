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

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


/**
 * Class that manages the receipt of unicast messages.
 *
 * @author Philip Aston
 * @version $Revision$
 */
public final class UnicastReceiver extends AbstractReceiver
{
    private int m_listenThreadIndex = 0;
    private final ServerSocket m_serverSocket;
    private final SocketSet m_connections = new SocketSet();
    private final ThreadGroup m_threadGroup =
	new ThreadGroup("UnicastReceiver");

    /**
     * Constructor.
     *
     * @param addressString The TCP address to listen on.
     * @param port The TCP port to listen to.
     *
     * @ throws CommunicationException If socket could not be bound to.
     **/
    public UnicastReceiver(String addressString, int port)
	throws CommunicationException
    {
	try {
	    m_serverSocket =
		new ServerSocket(port, 50,
				 InetAddress.getByName(addressString));
	}
	catch (IOException e) {
	    throw new CommunicationException(
		"Could not bind to TCP address " + addressString + ":" + port,
		e);
	}

	new AcceptorThread().start();

	for (int i=0; i<5; ++i) {
	    new ListenThread().start();
	}
    }

    /**
     * Shut down this reciever.
     * @throws CommunicationException If an IO exception occurs.
     **/
    public final void shutdown() throws CommunicationException
    {
	super.shutdown();

	try {
	    m_serverSocket.close();
	}
	catch (IOException e) {
	    throw new CommunicationException("Error closing socket", e);
	}

	m_connections.close();

	m_threadGroup.interrupt();
    }

    private final class AcceptorThread extends Thread
    {
	public AcceptorThread()
	{
	    super(m_threadGroup, "Acceptor thread");
	}

	public void run()
	{
	    try {
		while (true) {
		    final Socket localSocket = m_serverSocket.accept();
		    
		    try {
			m_connections.add(
			    new SocketSet.Handle(localSocket));
		    }
		    catch (Exception e) {
			// Propagate exceptions to threads calling
			// waitForMessage.
			getMessageQueue().queue(e);
		    }
		}
	    }
	    catch (IOException e) {
		// Treat accept socket errors as fatal - we've
		// probably been shutdown.
	    }
	    catch (MessageQueue.ShutdownException e) {
		// We've been shutdown, exit this thread.
	    }
	    finally {
		// Best effort to ensure our server socket is closed.
		try {
		    shutdown();
		}
		catch (CommunicationException ce) {
		}
	    }
	}
    }

    private final class ListenThread extends Thread
    {
	public ListenThread()
	{
	    super(m_threadGroup,
		  "Unicast listen thread " + m_listenThreadIndex++);
	    setDaemon(true);
	}

	public void run()
	{
	    final MessageQueue messageQueue = getMessageQueue();

	    try {
		while (true) {
		    final SocketSet.Handle socketHandle =
			m_connections.reserveNextHandle();

		    try {
			final Message m = socketHandle.pollForMessage();

			if (m != null) {
			    messageQueue.queue(m);
			}
			else {
			    // Can do better than this, but my head hurts.
			    Thread.sleep(100);
			}
		    }
		    catch (IOException e) {
			socketHandle.close();
			messageQueue.queue(e);
		    }
		    catch (ClassNotFoundException e) {
			socketHandle.close();
			messageQueue.queue(e);
		    }
		    finally {
			socketHandle.free();
		    }
		}
	    }
	    catch (MessageQueue.ShutdownException e) {
		// We've been shutdown, exit this thread.
	    }
	    catch (InterruptedException e) {
	    }
	}
    }
}
