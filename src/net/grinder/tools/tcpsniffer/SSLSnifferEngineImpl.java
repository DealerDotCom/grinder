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
				int localPort,
				String remoteHost, int remotePort,
				String keystoreName, String keystorePassword)
	throws Exception
    {
	super(requestFilter, responseFilter, remoteHost, remotePort);
		
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
    
