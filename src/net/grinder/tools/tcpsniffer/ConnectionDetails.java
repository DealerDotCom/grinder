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


public class ConnectionDetails
{
    private String m_localHost;
    private int m_localPort;
    private String m_remoteHost;
    private int m_remotePort;
    private boolean m_isSecure;

    public ConnectionDetails(String localHost, int localPort,
			     String remoteHost, int remotePort,
			     boolean isSecure)
    {
	m_localHost = localHost;
	m_localPort = localPort;
	m_remoteHost = remoteHost;
	m_remotePort = remotePort;
	m_isSecure = isSecure;
    }

    public String getDescription()
    {
	return
	    m_localHost + ":" + m_localPort + "->" +
	    m_remoteHost + ":" + m_remotePort;
    }

    public String getURLBase(String protocol)
    {
	// Hackety do dah..

	return
	    protocol + (m_isSecure ? "s://" : "://" ) +
	    m_remoteHost + ":" + m_remotePort;
    }
	
	public boolean isSecure() {
		return m_isSecure;
	}
	
	public String getRemoteHost() {
		return m_remoteHost;
	}
	
	public String getLocalHost() {
		return m_localHost;
	}

	public int getRemotePort() {
		return m_remotePort;
	}
	
	public int getLocalPort() {
		return m_localPort;
	}

	public void setRemoteHost(String p) {
		m_remoteHost = p;
	}
	
	public void setLocalHost(String p) {
		m_localHost = p;
	}

	public void setRemotePort(int p) {
		m_remotePort = p;
	}
	
	public void setLocalPort(int p) {
		m_localPort = p;
	}
}
