// The Grinder
// Copyright (C) 2001  Paco Gomez
// Copyright (C) 2001  Philip Aston

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

// Author  Phil Dawes <pdawes@bea.com>

package net.grinder.plugin.http;

import com.sun.net.ssl.HostnameVerifier;
import com.sun.net.ssl.HttpsURLConnection;
import com.sun.net.ssl.KeyManager;
import com.sun.net.ssl.KeyManagerFactory;
import com.sun.net.ssl.SSLContext;
import com.sun.net.ssl.TrustManager;
import com.sun.net.ssl.X509TrustManager;
import java.io.FileInputStream;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.Set;
import javax.net.ssl.SSLSocketFactory;

import net.grinder.common.GrinderProperties;
import net.grinder.plugininterface.PluginException;
import net.grinder.plugininterface.PluginProcessContext;
import net.grinder.plugininterface.ThreadCallbacks;


/**
 * HTTPS support for HttpPlugin
 * 
 * @author Phil Dawes
 * @version $Revision$
 */
public class HttpsPlugin extends HttpPlugin
{
    private static final String KEYSTORE_TYPE = "PKCS12";

    static 
    {
	System.setProperty("java.protocol.handler.pkgs",
			   "com.sun.net.ssl.internal.www.protocol");
	java.security.Security.addProvider(
	    new com.sun.net.ssl.internal.ssl.Provider());
    }

    public void initialize(PluginProcessContext processContext)
	throws PluginException
    {
	super.initialize(processContext);

	final GrinderProperties parameters =
	    processContext.getPluginParameters();

	// optional parameters
	final String clientCertFilename = parameters.getProperty("clientCert");
	final String passwordString =
	    parameters.getProperty("clientCertPassword");
		
	try {
	    final KeyStore ks = KeyStore.getInstance(KEYSTORE_TYPE);
	    final X509TrustManager tm = new TrustEveryone();
	    final TrustManager[] tma = {tm};

	    final KeyManagerFactory kmf = 
		KeyManagerFactory.getInstance(
		    KeyManagerFactory.getDefaultAlgorithm());
			
	    if(clientCertFilename != null) {
		if(passwordString == null) {
		    throw new PluginException("You need to set property grinder.plugin.parameter.clientCertPassword");
		}

		final char password[] = new char[passwordString.length()];
		passwordString.getChars(0, password.length, password, 0);

		ks.load(new FileInputStream(clientCertFilename), password);
		kmf.init(ks, password);
	    }

	    final SSLContext sCtx = SSLContext.getInstance("SSL");

	    sCtx.init(kmf.getKeyManagers(), tma,
		      new java.security.SecureRandom());

	    final SSLSocketFactory sFactory = sCtx.getSocketFactory();
	    HttpsURLConnection.setDefaultSSLSocketFactory(sFactory);
	    HttpsURLConnection.setDefaultHostnameVerifier(new VerifyAll());
	}
	catch(Exception e) {
	    throw new PluginException(e.toString(), e);
	}
    }


    /**
     * For the purposes of grinding, we don't care whether the cert chain
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
	    return true;
	}

	public java.security.cert.X509Certificate[] getAcceptedIssuers()
	{
	    return null;
	}
    }


    /**
     * Ditto here - we don't care if the cert name doesn't match the hostname
     */
    private static class VerifyAll implements HostnameVerifier
    {
	public boolean verify(String urlHostname, String certHostName)
	{
	    return true;
	}
    }
}
