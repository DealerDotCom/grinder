// The Grinder
// Copyright (C) 2000  Paco Gomez
// Copyright (C) 2000  Phil Dawes
// Copyright (C) 2001  Phil Aston

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

package net.grinder.tools.tcpsniffer;

import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
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

	m_filter.connectionOpened(m_connectionDetails);

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

		System.out.print(m_colour);
		m_filter.handle(m_connectionDetails, buffer, bytesRead);
		System.out.print(m_resetColour);

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

	System.out.print(m_colour);
	m_filter.connectionClosed(m_connectionDetails);
	System.out.print(m_resetColour);

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
