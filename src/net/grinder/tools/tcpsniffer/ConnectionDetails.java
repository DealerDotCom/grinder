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
