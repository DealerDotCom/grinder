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
import HTTPClient.HTTPConnection;

/**
 * @author Philip Aston
 * @version $Revision$
 **/
public final class HTTPConnectionWrapper implements HTTPPluginConnection
{
    private final HTTPConnection m_httpConnection;
    private boolean m_followRedirects;
    private boolean m_useCookies;

    public HTTPConnectionWrapper(HTTPConnection httpConnection,
				 HTTPPluginConnectionDefaults defaults) 
    {
	m_httpConnection = httpConnection;


	synchronized (defaults) {
	    setFollowRedirects(defaults.getFollowRedirects());
	    setUseCookies(defaults.getUseCookies());

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

	}
    }

    final HTTPConnection getConnection() 
    {
	return m_httpConnection;
    }

    public final boolean getFollowRedirects() 
    {
	return m_followRedirects;
    }

    public final void setFollowRedirects(boolean followRedirects) 
    {
	m_followRedirects = followRedirects;

	final Class redirectionModule = HTTPPlugin.getRedirectionModule();

	if (m_followRedirects) {
	    m_httpConnection.addModule(redirectionModule, 0);
	}
	else {
	    m_httpConnection.removeModule(redirectionModule);
	}
    }

    public final boolean getUseCookies() 
    {
	return m_useCookies;
    }

    public final void setUseCookies(boolean useCookies) 
    {
	m_useCookies = useCookies;

	if (m_useCookies) {
	    m_httpConnection.addModule(CookieModule.class, 0);
	}
	else {
	    m_httpConnection.removeModule(CookieModule.class);
	}
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
}

