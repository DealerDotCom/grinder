// Copyright (C) 2000 Phil Dawes
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

package net.grinder.tools.tcpproxy;

import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

import net.grinder.util.TerminalColour;

/**
 *
 * @author Phil Dawes
 * @author Philip Aston
 * @version $Revision$
 */
public class StreamThread implements Runnable
{
    // For simplicity, the filters take a buffer oriented approach.
    // This means that they all break at buffer boundaries. Our buffer
    // is huge, so we shouldn't practically cause a problem, but the
    // network clearly can by giving us message fragments. I consider
    // this a bug, we really ought to take a stream oriented approach.
    private final static int BUFFER_SIZE = 65536;

    private final ConnectionDetails m_connectionDetails;
    private final InputStream m_in;
    private final OutputStream m_out;
    private final TCPProxyFilter m_filter;
    private final PrintWriter m_outputWriter;
    private final String m_colour;
    private final String m_resetColour;

    public StreamThread(ConnectionDetails connectionDetails,
			InputStream in, OutputStream out,
			TCPProxyFilter filter,
			PrintWriter outputWriter, String colourString)
    {
	m_connectionDetails = connectionDetails;
	m_in = in;
	m_out = out;
	m_filter = filter;
	m_colour = colourString;
	m_resetColour = m_colour.length() > 0 ? TerminalColour.NONE : "";
	m_outputWriter = outputWriter;
	
	final Thread t =
	    new Thread(this,
		       "Filter thread for " +
		       m_connectionDetails.getDescription());

	try {
	    m_filter.connectionOpened(m_connectionDetails);
	}
	catch (Exception e) {
	    e.printStackTrace(System.err);
	}

	t.start();
    }

    public void run()
    {
	try {
	    byte[] buffer = new byte[BUFFER_SIZE];

	    while (true) {
		final int bytesRead = m_in.read(buffer, 0, BUFFER_SIZE);

		if (bytesRead == -1) {
		    break;
		}

		m_outputWriter.print(m_colour);

		final byte[] newBytes =
		    m_filter.handle(m_connectionDetails, buffer, bytesRead);

		m_outputWriter.print(m_resetColour);
		m_outputWriter.flush();

		if (newBytes != null) {
		    m_out.write(newBytes);
		}
		else {
		    m_out.write(buffer, 0, bytesRead);
		}
	    }
	}
	catch (SocketException e) {
	    // Be silent about SocketExceptions.
	}
	catch (Exception e) {
	    e.printStackTrace(System.err);
	}

	m_outputWriter.print(m_colour);

	try {
	    m_filter.connectionClosed(m_connectionDetails);
	}
	catch (Exception e) {
	    e.printStackTrace(System.err);
	}

	m_outputWriter.print(m_resetColour);
	m_outputWriter.flush();

	// We're exiting, usually because the in stream has been
	// closed. Whatever, close our streams. This will cause the
	// paired thread to exit too.
	try {
	    m_out.close();
	}
	catch (Exception e) {
	}
	
	try {
	    m_in.close();
	}
	catch (Exception e) {
	}
    }
}
