// The Grinder
// Copyright (C) 2001  Paddy Spencer

// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.

// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.	 See the
// GNU General Public License for more details.

// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.

package net.grinder.tools.snifferwebapp.util;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.io.IOException;

public class PortManager {
    private int m_lower;
    private int m_upper;
    private int[] m_activePorts;
    private static PortManager m_instance;

    private PortManager() {
	try {
	    InitialContext ctx = new InitialContext();

	    Integer i = (Integer)ctx.lookup("java:comp/env/sniffer.port.lower");
	    m_lower = i.intValue();

	    i = (Integer)ctx.lookup("java:comp/env/sniffer.port.upper");
	    m_upper = i.intValue();

	    int size = m_upper - m_lower;
	    m_activePorts = new int[size];

	} catch (NamingException e) {
	    e.printStackTrace();
	}
    }
    
    public synchronized int acquirePort() throws NoFreePortException {
	int size = m_upper - m_lower;
	
	// first we check our list to see if we have any ports marked
	// as free...
	for(int i = 0; i < size; ++i) {
	    if(m_activePorts[i] == 0) {
		m_activePorts[i] = i + m_lower;
		return m_activePorts[i];
	    }
	}

	// ...if that doesn't work, actually check that all the ports
	// are in use. We need this second check as it's possible that
	// one or more processes listening on a port have timed out
	// and we haven't been told.
	for (int i = 0; i < size; ++i) {
	    try {
		ServerSocket s = new ServerSocket(m_activePorts[i]);
		System.out.println("Port " + m_activePorts[i] + " not in use.");
		s.close();
		return m_activePorts[i];
	    } catch (IOException e) {
		// port is in use 
	    }
	}

	throw new NoFreePortException();
    }

    public void releasePort(int port) {
	int i = port - m_lower;
	m_activePorts[i] = 0;
    }

    public static synchronized PortManager getInstance() {
	if(m_instance == null) {
	    m_instance = new PortManager();
	}
	return m_instance;
    }
}
