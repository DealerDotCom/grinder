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

import com.sun.net.ssl.KeyManager;
import com.sun.net.ssl.KeyManagerFactory;
import com.sun.net.ssl.SSLContext;
import com.sun.net.ssl.TrustManager;
import com.sun.net.ssl.X509TrustManager;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import javax.net.ServerSocketFactory;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;


/**
 *
 * @author Phil Dawes
 * @author Philip Aston
 * @version $Revision$
 */
public class SSLSnifferEngineImpl extends SnifferEngineImpl
{
    final SSLSocketFactory m_sslSocketFactory;

    // Should probably parameterise this.
    private static final String KEYSTORE_TYPE = "PKCS12";

    public SSLSnifferEngineImpl(SnifferFilter requestFilter,
				SnifferFilter responseFilter,
				String localHost,
				int localPort,
				String remoteHost, int remotePort,
				boolean useColour,
				String keystoreName, String keystorePassword)
	throws Exception
    {
	super(requestFilter, responseFilter, localHost, remoteHost, remotePort,
	      useColour);

	System.setProperty("java.protocol.handler.pkgs",
			   "com.sun.net.ssl.internal.www.protocol");

	java.security.Security.addProvider(
	    new com.sun.net.ssl.internal.ssl.Provider());

	final KeyManagerFactory keyManagerFactory = 
	    KeyManagerFactory.getInstance(
		KeyManagerFactory.getDefaultAlgorithm());


	if (keystorePassword != null) {
	    final KeyStore keystore = KeyStore.getInstance(KEYSTORE_TYPE);
	    final char password[] = keystorePassword.toCharArray();

	    keystore.load(new FileInputStream(keystoreName), password);

	    keyManagerFactory.init(keystore, password);
	}
	
	final SSLContext sslContext = SSLContext.getInstance("SSL");
	final TrustManager[] trustManagerArray = { new TrustEveryone() };

	sslContext.init(keyManagerFactory.getKeyManagers(),
			trustManagerArray,
			new java.security.SecureRandom());

	m_sslSocketFactory = sslContext.getSocketFactory();

	final ServerSocketFactory serverFactory =
	    sslContext.getServerSocketFactory(); 
	
	setServerSocket(serverFactory.createServerSocket(localPort));
    }

    protected Socket createRemoteSocket() throws IOException
    {
	final SSLSocket remoteSocket =
	    (SSLSocket)m_sslSocketFactory.createSocket(getRemoteHost(),
						       getRemotePort());

	remoteSocket.startHandshake();
	
	return remoteSocket;
    }

    protected boolean getIsSecure() 
    {
	return true;
    }

    /**
     * For the purposes of sniffing, we don't care whether the cert chain
     * is trusted or not, so here's an implementation which accepts everything -PD
     */
    private static class TrustEveryone implements X509TrustManager
    {
	public boolean isClientTrusted (X509Certificate[] chain)
	{
	    return true;
	}
	
	public boolean isServerTrusted (X509Certificate[] chain)
	{
	    /*
	      System.out.println("--- Certificate Chain:");

	      for (int i=0;i<chain.length;i++) {
	      System.out.println("--- Certificate " + i);
	      System.out.println(chain[i]);
	      }
	    */

	    return true;
	}

	public java.security.cert.X509Certificate[] getAcceptedIssuers()
	{
	    return null;
	}
    }
}
    
