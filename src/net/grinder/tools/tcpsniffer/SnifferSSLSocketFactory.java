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

import com.sun.net.ssl.SSLContext;
import com.sun.net.ssl.TrustManager;
import com.sun.net.ssl.X509TrustManager;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.GeneralSecurityException;
import java.security.cert.X509Certificate;
import javax.net.ServerSocketFactory;
import javax.net.SocketFactory;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;


/**
 * {@link SnifferSocketFactory} for SSL connections.
 *
 * <p>The JSSE docs rabbit on about being able to create factories
 * with the required parameters, this is a lie. Where is
 * "SSL[Server]SocketFactory.setEnabledCipherSuites()" and how would
 * you ensure SSLSocket.startHandshake() is called for new client
 * sockets? Hence the need for our own abstract factories.</p>
 *
 * @author Philip Aston
 * @author Phil Dawes
 * @version $Revision$
 */
public final class SnifferSSLSocketFactory implements SnifferSocketFactory
{
    final ServerSocketFactory m_serverSocketFactory;

    final SocketFactory m_clientSocketFactory;

    public SnifferSSLSocketFactory() throws GeneralSecurityException
    {
	final SSLContext sslContext = SSLContext.getInstance("SSL");

	sslContext.init(null,
			new TrustManager[] { new TrustEveryone() },
			null);

	m_clientSocketFactory = sslContext.getSocketFactory();

	m_serverSocketFactory = sslContext.getServerSocketFactory(); 
    }

    public final ServerSocket createServerSocket(String localHost,
						 int localPort,
						 int timeout)
	throws IOException
    {
	final SSLServerSocket socket =
	    (SSLServerSocket)m_serverSocketFactory.createServerSocket(
		localPort, 50, InetAddress.getByName(localHost));

	socket.setSoTimeout(timeout);

	socket.setEnabledCipherSuites(socket.getSupportedCipherSuites());

	return socket;
    }

    public final Socket createClientSocket(String remoteHost, int remotePort)
	throws IOException
    {
	final SSLSocket socket =
	    (SSLSocket)m_clientSocketFactory.createSocket(remoteHost,
							  remotePort);

	socket.setEnabledCipherSuites(socket.getSupportedCipherSuites());

	socket.startHandshake();

	return socket;
    }

    /**
     * For the purposes of sniffing, we don't care whether the cert
     * chain is trusted or not, so here's an implementation which
     * accepts everything -PD
     */
    private static class TrustEveryone implements X509TrustManager
    {
	public boolean isClientTrusted (X509Certificate[] chain)
	{
	    return true;
	}
	
	public boolean isServerTrusted (X509Certificate[] chain)
	{
	    return true;
	}

	public java.security.cert.X509Certificate[] getAcceptedIssuers()
	{
	    return null;
	}
    }
}
    
