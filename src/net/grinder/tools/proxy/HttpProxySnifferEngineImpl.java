// The Grinder
// Copyright (C) 2000, 2001 Paco Gomez
// Copyright (C) 2000, 2001 Phil Dawes
// Copyright (C) 2001  Phil Aston
// Copyright (C) 2001  Paddy Spencer

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

package net.grinder.tools.proxy;

import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.InterruptedIOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

import org.apache.regexp.RE;
import org.apache.regexp.RESyntaxException;

import net.grinder.tools.tcpsniffer.ConnectionDetails;
import net.grinder.tools.tcpsniffer.SnifferFilter;
import net.grinder.tools.tcpsniffer.SnifferEngine;
import net.grinder.tools.tcpsniffer.StreamThread;

import net.grinder.util.TerminalColour;

/**
 *
 * @author Paddy Spencer
 * @version $Revision$
 */
public class HttpProxySnifferEngineImpl implements SnifferEngine
{
    // dummy values
    protected String m_remoteHost = "localhost";
    protected int m_remotePort = 7002; 
    
    protected final String m_localHost;

    protected final SnifferFilter m_requestFilter;
    protected final SnifferFilter m_responseFilter;
    protected ServerSocket m_serverSocket = null;
    protected int m_timeout;

    protected boolean m_useColour;

    public HttpProxySnifferEngineImpl(SnifferFilter requestFilter,
				      SnifferFilter responseFilter,
				      String localHost,
				      int localPort, int timeout,
				      boolean useColour)
        throws Exception
    {
        this(requestFilter, responseFilter, localHost, timeout);

        m_serverSocket = new ServerSocket(localPort);
    }

    protected HttpProxySnifferEngineImpl(SnifferFilter requestFilter,
					 SnifferFilter responseFilter,
					 String localHost,
					 int timeout)
    {
        m_requestFilter = requestFilter;
        m_responseFilter = responseFilter;
	m_localHost = localHost;
	m_timeout = timeout;
    }

    protected void setServerSocket(ServerSocket serverSocket)
    {
        m_serverSocket = serverSocket;
    }

    protected Socket createRemoteSocket() throws IOException
    {
	 return new Socket(m_remoteHost, m_remotePort);
    }

    public void run()
    {
        while (true) {
            try {
		m_serverSocket.setSoTimeout(m_timeout);
                final Socket localSocket = m_serverSocket.accept();

		// work out where we need to go, open a socket to it,
		// create two streamthreads to go back and forth,
		// passing the first tranche of data into the up
		// stream thread to make sure it gets set up right
		// away.

		final int BUFFER_SIZE = 65536;
		byte[] buffer = new byte[BUFFER_SIZE];
                int bytesRead = 
		    localSocket.getInputStream().read(buffer, 0, BUFFER_SIZE);

                if (bytesRead == -1) {
		    throw new IOException("Failed to read from socket.");
                }
		
		byte[] parsedBuffer = grokDestination(buffer, bytesRead);

		final Socket remoteSocket = createRemoteSocket();

		new StreamThread(new ConnectionDetails("localhost",
						       localSocket.getPort(),
						       m_remoteHost,
						       remoteSocket.getPort(),
						       false),
				 localSocket.getInputStream(),
				 remoteSocket.getOutputStream(),
				 m_requestFilter,
				 getColour(false),
				 parsedBuffer);

		new StreamThread(new ConnectionDetails(m_remoteHost,
						       remoteSocket.getPort(),
						       "localhost",
						       localSocket.getPort(),
						       false),
				 remoteSocket.getInputStream(),
				 localSocket.getOutputStream(),
				 m_responseFilter,
				 getColour(true));

            } catch (SocketException e) {
		// set so timeout failed
                e.printStackTrace();
	    } catch (InterruptedIOException e) {
		// socket timed out waiting for accept
		System.err.println("Proxy timed out waiting for request."
				   + " Exiting...");
		break;
            } catch(IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void stop() {
	try {
	    m_serverSocket.close();
	} catch (IOException e) {
	    e.printStackTrace();
	}
    }

    // reads the contents of the buffer, checks to see if it's a new
    // request and changes connection details and other stuff as
    // appropriate.
    protected byte[] grokDestination(byte[] buffer, int bytesRead) {

	byte[] returnBytes = null;

	try {

	    final String line = new String(buffer, 0, bytesRead, "US-ASCII");

	    RE re = new RE("^([:upper:]+) http://([^/:]+):?(\\d*)/");

	    if (re.match(line)) {
		m_remoteHost = re.getParen(2);

		m_remotePort = 80;
		try {
		    m_remotePort = Integer.parseInt(re.getParen(3));
		} catch (NumberFormatException e) {
		    // that's ok - no port was given, so getParen(3)
		    // was null. It doesn't matter as we're happy with
		    // the default of 80.
		}

		// lastly need to take the http://host:port bit out of the
		// method line, as the server's not expecting it to be
		// there.
		returnBytes = re.subst(line, re.getParen(1) + " /").getBytes();
	    }

	} catch (IOException e) {
	    e.printStackTrace();
	} catch (RESyntaxException e) {
	    e.printStackTrace();
	}

	return returnBytes;
    }

    protected String getColour(boolean response)
    {
	if (!m_useColour) {
	    return "";
	}
	else {
	    return response ? TerminalColour.BLUE : TerminalColour.RED;
	}
    }

}

