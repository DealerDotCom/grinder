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

package net.grinder.tools.tcpsniffer;

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
    private final static int BUFFER_SIZE=65536;

    private final ConnectionDetails m_connectionDetails;
    private final InputStream m_in;
    private final OutputStream m_out;
    private final SnifferFilter m_filter;
    private final PrintWriter m_outputWriter;
    private final String m_colour;
    private final String m_resetColour;

    /**
     * this ctor exists as a kind of "init" method - we can't call
     * init() and have final members, but if we have a protected ctor,
     * we can call it as the first line of each public ctor, thereby
     * setting our members and keeping them final and not duplicating
     * code. We need an init-style method as we want to push some
     * bytes down the pipe before starting the Thread in one case.  Of
     * course, we need to make it have a different signature from
     * every other ctor otherwise it ain't gonna work...
     * 
     */
    protected StreamThread(Object dummy, ConnectionDetails connectionDetails,
			   InputStream in, OutputStream out,
			   SnifferFilter filter,
			   String colourString) 
    {
	m_connectionDetails = connectionDetails;
	m_in = in;
	m_out = out;
	m_filter = filter;
	m_colour = colourString;
	m_resetColour = m_colour.length() > 0 ? TerminalColour.NONE : "";

	m_outputWriter = new PrintWriter(System.out);
	m_filter.setOutputPrintWriter(m_outputWriter);
    }

    public StreamThread(ConnectionDetails connectionDetails,
			InputStream in, OutputStream out,
			SnifferFilter filter,
			String colourString)
    {
	// see above
	this(null, connectionDetails, in, out, filter, colourString);

	
	final Thread t = new Thread(this,
				    m_connectionDetails.getDescription());

	try {
	    m_filter.connectionOpened(m_connectionDetails);
	}
	catch (Exception e) {
	    e.printStackTrace(System.err);
	}

	t.start();
    }


    /**
     * this ctor is used by (at least) the proxies, which parse the
     * incoming request to determine remote host/port details. This
     * means that some of the data (potentially quite a lot) has been
     * sucked off the socket before the connection is made. This needs
     * to be squirted down the pipt before the thread is started off,
     * which is what this ctor does.
     */
    public StreamThread(ConnectionDetails connectionDetails,
			InputStream in, OutputStream out,
			SnifferFilter filter, String colourString,
			byte[] request)
    {
	this(null, connectionDetails, in, out, filter, colourString);

	try {
	    m_filter.handle(m_connectionDetails, request, request.length);
	    m_out.write(request);

	    final Thread t = new Thread(this,
					m_connectionDetails.getDescription());
	    t.start();
	} catch (IOException e) {
	    // no point continuing if we can't write to the
	    // remoteSocket
	    e.printStackTrace();
	} catch (Exception e) {
	    e.printStackTrace();
	}
    }


    public void run()
    {
	try {
	    byte[] buffer = new byte[BUFFER_SIZE];

	    while (true) {
		int bytesRead = m_in.read(buffer, 0, BUFFER_SIZE);

		if (bytesRead == -1) {
		    break;
		}

		m_outputWriter.print(m_colour);
		m_filter.handle(m_connectionDetails, buffer, bytesRead);
		m_outputWriter.print(m_resetColour);
		m_outputWriter.flush();

		// and write in out
		m_out.write(buffer, 0, bytesRead);
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
	// paired thread to exit to.
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
