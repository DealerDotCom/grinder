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


/**
 * Class that represents a TCP connection.
 *
 * @author <a href="mailto:paston@bea.com">Philip Aston</a>
 * @version 1.0
 */
public class ConnectionDetails
{
    private final int m_hashCode;

    private String m_localHost;
    private int m_localPort;
    private String m_remoteHost;
    private int m_remotePort;
    private boolean m_isSecure;

    /**
     * Creates a new <code>ConnectionDetails</code> instance.
     *
     * @param localHost a <code>String</code> value
     * @param localPort an <code>int</code> value
     * @param remoteHost a <code>String</code> value
     * @param remotePort an <code>int</code> value
     * @param isSecure a <code>boolean</code> value
     */
    public ConnectionDetails(String localHost, int localPort,
			     String remoteHost, int remotePort,
			     boolean isSecure)
    {
	m_localHost = localHost.toLowerCase();
	m_localPort = localPort;
	m_remoteHost = remoteHost.toLowerCase();
	m_remotePort = remotePort;
	m_isSecure = isSecure;

	m_hashCode =
	    m_localHost.hashCode() ^
	    m_remoteHost.hashCode() ^
	    m_localPort ^
	    m_remotePort ^
	    (m_isSecure ? 0x55555555 : 0);
    }

    /**
     * Return a description of the connection.
     *
     * @return a <code>String</code> value
     */
    public String getDescription()
    {
	return
	    m_localHost + ":" + m_localPort + "->" +
	    m_remoteHost + ":" + m_remotePort;
    }

    /**
     * Describe <code>getURLBase</code> method here.
     *
     * @param protocol a <code>String</code> value
     * @return a <code>String</code> value
     */
    public String getURLBase(String protocol)
    {
	// Hackety do dah..

	return
	    protocol + (m_isSecure ? "s://" : "://" ) +
	    m_remoteHost + ":" + m_remotePort;
    }

    /**
     * Accessor.
     *
     * @return a <code>boolean</code> value
     */
    public boolean isSecure()
    {
	return m_isSecure;
    }

    /**
     * Accessor.
     *
     * @return a <code>String</code> value
     */
    public String getRemoteHost() 
    {
	return m_remoteHost;
    }

    /**
     * Accessor.
     *
     * @return a <code>String</code> value
     */
    public String getLocalHost()
    {
	return m_localHost;
    }

    /**
     * Accessor.
     *
     * @return an <code>int</code> value
     */
    public int getRemotePort()
    {
	return m_remotePort;
    }

    /**
     * Accessor.
     *
     * @return an <code>int</code> value
     */
    public int getLocalPort()
    {
	return m_localPort;
    }

    /**
     * Mutator.
     *
     * @param p a <code>String</code> value
     */
    public void setRemoteHost(String p)
    {
	m_remoteHost = p;
    }

    /**
     * Mutator.
     *
     * @param p a <code>String</code> value
     */
    public void setLocalHost(String p)
    {
	m_localHost = p;
    }

    /**
     * Mutator.
     *
     * @param p an <code>int</code> value
     */
    public void setRemotePort(int p)
    {
	m_remotePort = p;
    }

    /**
     * Mutator.
     *
     * @param p an <code>int</code> value
     */
    public void setLocalPort(int p)
    {
	m_localPort = p;
    }

    /**
     * Value based equality.
     *
     * @param other an <code>Object</code> value
     * @return <code>true</code> => <code>other</code> is equal to this object.
     *
     */
    public boolean equals(Object other) 
    {
	if (other == this) {
	    return true;
	}
	
	if (!(other instanceof ConnectionDetails)) {
	    return false;
	}

	final ConnectionDetails otherConnectionDetails =
	    (ConnectionDetails)other;

	return
	    hashCode() == otherConnectionDetails.hashCode() &&
	    getLocalPort() == otherConnectionDetails.getLocalPort() &&
	    getRemotePort() == otherConnectionDetails.getRemotePort() &&
	    isSecure() == otherConnectionDetails.isSecure() &&
	    getLocalHost().equals(otherConnectionDetails.getLocalHost()) &&
	    getRemoteHost().equals(otherConnectionDetails.getRemoteHost());
    }

    /**
     * Implement {@link Object#hashCode}.
     *
     * @return an <code>int</code> value
     */
    public final int hashCode()
    {
	return m_hashCode;
    }

}
