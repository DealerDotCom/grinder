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

