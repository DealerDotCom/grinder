// Copyright (C) 2000 Paco Gomez
// Copyright (C) 2000, 2001, 2002 Philip Aston
// Copyright (C) 2000 Phil Dawes
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
