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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;


/**
 * Class that manages the receipt of multicast messages.
 *
 * @author Philip Aston
 * @version $Revision$
 */
public final class MulticastReceiver extends AbstractReceiver
{
    private final byte[] m_buffer = new byte[65536];
    private final MulticastSocket m_socket;
    private final DatagramPacket m_packet;


    /**
     * Constructor.
     *
     * @param multicastAddressString The multicast address to bind on.
     * @param multicastPort The port to bind to.
     *
     * @ throws CommunicationException If socket could not be bound to.
     **/
    public MulticastReceiver(String multicastAddressString, int multicastPort)
	throws CommunicationException
    {
	super(true);

	try {
	    m_socket = new MulticastSocket(multicastPort);
	    m_socket.joinGroup(InetAddress.getByName(multicastAddressString));
	}
	catch (IOException e) {
	    throw new CommunicationException(
		"Could not bind to multicast address " +
		multicastAddressString + ":" + multicastPort, e);
	}

	m_packet = new DatagramPacket(m_buffer, m_buffer.length);

	new ListenThread().start();
    }

    private final class ListenThread extends Thread
    {
	public ListenThread()
	{
	    super("Multicast listen thread");
	    setDaemon(true);
	}

	public void run()
	{
	    final MessageQueue messageQueue = getMessageQueue();

	    final ByteArrayInputStream byteStream =
		new ByteArrayInputStream(m_buffer, 0, m_buffer.length);

	    try {
		while (true) {
		    try {
			m_packet.setData(m_buffer, 0, m_buffer.length);
			m_socket.receive(m_packet);

			byteStream.reset();

			// ObjectInputStream does not support reset(),
			// we need a new one each time.
			final ObjectInputStream objectStream =
			    new ObjectInputStream(byteStream);

			messageQueue.queue((Message)objectStream.readObject());
		    }
		    catch (ClassNotFoundException e) {
			// Propagate exceptions to threads calling
			// waitForMessage.
			messageQueue.queue(e);
		    }
		    catch (IOException e) {
			// Propagate exceptions to threads calling
			// waitForMessage.
			messageQueue.queue(e);
		    }
		}
	    }
	    catch (MessageQueue.ShutdownException e) {
		// We've been shutdown, exit this thread.
	    }
	}
    }
}
