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
import java.net.InetAddress;

import net.grinder.util.TerminalColour;


/**
 *
 * @author Phil Dawes
 * @author Philip Aston
 * @version $Revision$
 */
public class SnifferEngineImpl implements SnifferEngine
{
    private final SnifferFilter m_requestFilter;
    private final SnifferFilter m_responseFilter;

    // these two need to be mutable
    private String m_remoteHost;
    private int m_remotePort;

    private final boolean m_useColour;
    private final String m_localHost;
    private ServerSocket m_serverSocket = null;
    

    public SnifferEngineImpl(SnifferFilter requestFilter,
			     SnifferFilter responseFilter,
			     String localHost,
			     int localPort, String remoteHost, 
			     int remotePort, boolean useColour)
	throws Exception
    {
	this(requestFilter, responseFilter, localHost, remoteHost, 
	     remotePort, useColour);
	
	m_serverSocket = new ServerSocket(localPort, 50, 
					  InetAddress.getByName(localHost));
    }

    protected SnifferEngineImpl(SnifferFilter requestFilter,
				SnifferFilter responseFilter,
				String localHost,
				String remoteHost, int remotePort,
				boolean useColour) 
    {
	m_requestFilter = requestFilter;
	m_responseFilter = responseFilter;
	m_localHost = localHost;
	m_remoteHost = remoteHost;
	m_remotePort = remotePort;
	m_useColour = useColour;
    }

    protected void setServerSocket(ServerSocket serverSocket) 
    {
	m_serverSocket = serverSocket;
    }

    protected String getRemoteHost()
    {
	return m_remoteHost;
    }

    protected int getRemotePort()
    {
	return m_remotePort;
    }

    public synchronized void setRemoteDestination(String host, int port) {
	m_remoteHost = host;
	m_remotePort = port;
    }

    protected Socket createRemoteSocket() throws IOException
    {
	 return new Socket(m_remoteHost, m_remotePort);
    }
    
    public void run()
    {
	while (true) {
	    try {
		final Socket localSocket = m_serverSocket.accept();
		final Socket remoteSocket = createRemoteSocket();

		new StreamThread(new ConnectionDetails(m_localHost,
						       localSocket.getPort(),
						       m_remoteHost,
						       remoteSocket.getPort(),
						       getIsSecure()),
				 localSocket.getInputStream(),
				 remoteSocket.getOutputStream(),
				 m_requestFilter,
				 getColour(false));

		new StreamThread(new ConnectionDetails(m_remoteHost,
						       remoteSocket.getPort(),
						       m_localHost,
						       localSocket.getPort(),
						       getIsSecure()),
				 remoteSocket.getInputStream(),
				 localSocket.getOutputStream(),
				 m_responseFilter,
				 getColour(true));
	    }
	    catch(IOException e) {
		e.printStackTrace(System.err);
	    }
	}
    }

    protected boolean getIsSecure() 
    {
	return false;
    }

    private String getColour(boolean response)
    {
	if (!m_useColour) {
	    return "";
	}
	else {
	    return response ? TerminalColour.BLUE : TerminalColour.RED;
	}
    }
}

