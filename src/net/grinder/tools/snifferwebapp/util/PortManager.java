// Copyright (C) 2001 Paddy Spencer
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
