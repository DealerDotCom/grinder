// Copyright (C) 2002 Philip Aston
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

import java.util.Iterator;

import HTTPClient.CookieModule;
import HTTPClient.DefaultAuthHandler;
import HTTPClient.HTTPConnection;
import HTTPClient.NVPair;
import HTTPClient.ProtocolNotSuppException;
import HTTPClient.URI;

/**
 * @author Philip Aston
 * @version $Revision$
 **/
final class HTTPConnectionWrapper implements HTTPPluginConnection
{
    private final static Class s_redirectionModule;
    
    static
    {
	// Remove standard HTTPClient modules which we don't want.
	try {
	    // Don't want additional post-processing of response data.
	    HTTPConnection.removeDefaultModule(
		Class.forName("HTTPClient.ContentEncodingModule"));
	    HTTPConnection.removeDefaultModule(
		Class.forName("HTTPClient.TransferEncodingModule"));

	    // Don't want to retry requests.
	    HTTPConnection.removeDefaultModule(
		Class.forName("HTTPClient.RetryModule"));

	    s_redirectionModule =
		Class.forName("HTTPClient.RedirectionModule");
	}
	catch (ClassNotFoundException e) {
	    throw new RuntimeException("Failed to load HTTPClient classes");
	}

	// Turn off cookie permission checks.
	CookieModule.setCookiePolicyHandler(null);

	// Turn off authorisation UI.
	DefaultAuthHandler.setAuthorizationPrompter(null);
    }

    private final HTTPConnection m_httpConnection;

    public HTTPConnectionWrapper(HTTPConnection httpConnection,
				 HTTPPluginConnectionDefaults defaults)
    {
	m_httpConnection = httpConnection;
	m_httpConnection.setAllowUserInteraction(false);

	synchronized (defaults) {
	    setFollowRedirects(defaults.getFollowRedirects());
	    setUseCookies(defaults.getUseCookies());
	    setDefaultHeaders(defaults.getDefaultHeaders());
	    setTimeout(defaults.getTimeout());

	    final Iterator basicAuthenticationIterator = 
		defaults.getBasicAuthorizations().iterator();

	    while (basicAuthenticationIterator.hasNext()) {
		final HTTPPluginConnectionDefaults.AuthorizationDetails
		    authorizationDetails =
		    (HTTPPluginConnectionDefaults.AuthorizationDetails)
		    basicAuthenticationIterator.next();

		addBasicAuthorization(authorizationDetails.getRealm(),
				      authorizationDetails.getUser(),
				      authorizationDetails.getPassword());
	    }

	    final Iterator digestAuthenticationIterator = 
		defaults.getBasicAuthorizations().iterator();

	    while (digestAuthenticationIterator.hasNext()) {
		final HTTPPluginConnectionDefaults.AuthorizationDetails
		    authorizationDetails =
		    (HTTPPluginConnectionDefaults.AuthorizationDetails)
		    digestAuthenticationIterator.next();

		addDigestAuthorization(authorizationDetails.getRealm(),
				       authorizationDetails.getUser(),
				       authorizationDetails.getPassword());
	    }

	    setProxyServer(defaults.getProxyHost(), defaults.getProxyPort());
	}
    }

    final HTTPConnection getConnection() 
    {
	return m_httpConnection;
    }

    public final void setFollowRedirects(boolean followRedirects) 
    {
	if (followRedirects) {
	    m_httpConnection.addModule(s_redirectionModule, 0);
	}
	else {
	    m_httpConnection.removeModule(s_redirectionModule);
	}
    }

    public final void setUseCookies(boolean useCookies) 
    {
	if (useCookies) {
	    m_httpConnection.addModule(CookieModule.class, 0);
	}
	else {
	    m_httpConnection.removeModule(CookieModule.class);
	}
    }

    public final void setDefaultHeaders(NVPair[] defaultHeaders) 
    {
	m_httpConnection.setDefaultHeaders(defaultHeaders);
    }

    public void setTimeout(int timeout) 
    {
	m_httpConnection.setTimeout(timeout);
    }

    public final void addBasicAuthorization(String realm, String user,
					    String password)
    {
	m_httpConnection.addBasicAuthorization(realm, user, password);
    }

    public final void removeBasicAuthorization(String realm, String user,
					       String password)
    {
	// TODO
    }

    public final void clearAllBasicAuthorizations()
    {
	// TODO
    }

    public final void addDigestAuthorization(String realm, String user,
					     String password)
    {
	m_httpConnection.addDigestAuthorization(realm, user, password);
    }

    public final void removeDigestAuthorization(String realm, String user,
						String password)
    {
	// TODO
    }

    public final void clearAllDigestAuthorizations()
    {
	// TODO
    }

    public void setProxyServer(String host, int port) 
    {
	m_httpConnection.setCurrentProxy(host, port);
    }
}

