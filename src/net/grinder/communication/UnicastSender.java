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

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.LinkedList;


/**
 * Class that manages the sending of unicast messages.
 *
 * @author Philip Aston
 * @version $Revision$
 **/
public class UnicastSender extends AbstractSender
{
    private OutputStream m_outputStream;
    private final Socket m_socket;

    /**
     * Constructor.
     *
     * @param grinderID A string describing our Grinder process.
     * @param addressString TCP address to send to.
     * @param port TCP port to send to.
     * @throws CommunicationException If failed to bind to socket or
     * failed to generate a unique process identifer.
     **/    
    public UnicastSender(String grinderID, String addressString, int port)
	throws CommunicationException
    {
	super(grinderID);

	final String localHost;
	final int localPort;

	try {
	    // Our socket - bind to any local port.
	    m_socket = new Socket(addressString, port);

	    m_outputStream =
		new BufferedOutputStream(m_socket.getOutputStream());

	    localHost = InetAddress.getLocalHost().getHostName();
	    localPort = m_socket.getLocalPort();
	}
	catch (IOException e) {
	    throw new CommunicationException(
		"Could not bind to TCP address '" + addressString + ":" +
		port + "'",
		e);
	}

	// Calculate a globally unique string for this sender. We
	// avoid calling addressString.toString() since this involves
	// a DNS lookup.
	setSenderID(
	    addressString + ":" + port + ":" + localHost + ":" +
	    localPort + ":" + System.currentTimeMillis());
    }

    protected final void writeMessage(Message message) throws IOException
    {
	// I tried the model of using a single ObjectOutputStream for
	// the lifetime of the socket, but the corresponding
	// ObjectInputStream would get occasional EOF's during
	// readObject. Seems like voodoo to me, but creating a new
	// ObjectOutputStream for every message fixes this.

	final ObjectOutputStream objectStream =
	    new ObjectOutputStream(m_outputStream);
	
	objectStream.writeObject(message);
	objectStream.flush();
    }

    /**
     * Cleanly shutdown the <code>Sender</code>. Ignore most errors.
     * Connection has probably been reset by peer.
     *
     * @exception CommunicationException If an error occurs.
     **/
    public void shutdown() throws CommunicationException
    {
	try {
	    send(new CloseCommunicationMessage());
	    flush();
	}
	catch (CommunicationException e) {
	}

	super.shutdown();

	try {
	    m_socket.close();
	}
	catch (IOException e) {
	}

	try {
	    m_socket.close();
	}
	catch (IOException e) {
	}
    }
}
