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

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import HTTPClient.NVPair;
import HTTPClient.ParseException;
import HTTPClient.ProtocolNotSuppException;
import HTTPClient.URI;


/**
 * @author Philip Aston
 * @version $Revision$
 **/
final class HTTPPluginConnectionDefaults implements HTTPPluginConnection
{
    private boolean m_followRedirects = true;
    private boolean m_useCookies = true;
    private NVPair[] m_defaultHeaders = new NVPair[0];
    private int m_timeout = 0;
    private Set m_basicAuthorizations = new HashSet();
    private Set m_digestAuthorizations = new HashSet();
    private String m_proxyHost;
    private int m_proxyPort;

    public final boolean getFollowRedirects() 
    {
	return m_followRedirects;
    }

    public final void setFollowRedirects(boolean followRedirects) 
    {
	m_followRedirects = followRedirects;
    }

    final boolean getUseCookies() 
    {
	return m_useCookies;
    }

    public final void setUseCookies(boolean useCookies) 
    {
	m_useCookies = useCookies;
    }

    public final void setDefaultHeaders(NVPair[] defaultHeaders) 
    {
	m_defaultHeaders = defaultHeaders;
    }

    final NVPair[] getDefaultHeaders()
    {
	return m_defaultHeaders;
    }

    public final void setTimeout(int timeout)
    {
	m_timeout = timeout;
    }

    final int getTimeout() 
    {
	return m_timeout;
    }

    public synchronized final void addBasicAuthorization(
	String realm, String user, String password)
    {
	m_basicAuthorizations.add(
	    new AuthorizationDetails(realm, user, password));
    }

    public synchronized final void removeBasicAuthorization(
	String realm, String user, String password)
    {
	m_basicAuthorizations.remove(
	    new AuthorizationDetails(realm, user, password));
    }

    public synchronized final void clearAllBasicAuthorizations()
    {
	m_basicAuthorizations.clear();
    }

    /**
     * Access to result should be synchronized on the
     * <code>HTTPPluginConnectionDefaults</code>.
     */
    Collection getBasicAuthorizations()
    {
	return m_basicAuthorizations;
    }

    public synchronized final void addDigestAuthorization(
	String realm, String user, String password)
    {
	m_digestAuthorizations.add(
	    new AuthorizationDetails(realm, user, password));
    }

    public synchronized final void removeDigestAuthorization(
	String realm, String user, String password)
    {
	m_digestAuthorizations.remove(
	    new AuthorizationDetails(realm, user, password));
    }

    public synchronized final void clearAllDigestAuthorizations()
    {
	m_digestAuthorizations.clear();
    }

    /**
     * Access to result should be synchronized on the
     * <code>HTTPPluginConnectionDefaults</code>.
     */
    Collection getDigestAuthorizations()
    {
	return m_digestAuthorizations;
    }

    final static class AuthorizationDetails
    {
	private final String m_realm;
	private final String m_user;
	private final String m_password;
	private final int m_hashCode;

	public AuthorizationDetails(String realm, String user, String password)
	{
	    m_realm = realm;
	    m_user = user;
	    m_password = password;
	    m_hashCode = 
		realm.hashCode() ^ m_user.hashCode() ^ m_password.hashCode();
	}

	public final String getRealm()
	{
	    return m_realm;
	}

	public final String getUser()
	{
	    return m_user;
	}

	public final String getPassword()
	{
	    return m_password;
	}

	public final int hashCode() 
	{
	    return m_hashCode;
	}

	public final boolean equals(Object o) 
	{
	    if (!(o instanceof AuthorizationDetails)) {
		return false;
	    }

	    final AuthorizationDetails other = (AuthorizationDetails)o;

	    return hashCode() == other.hashCode() &&
		getUser().equals(other.getUser()) &&
		getPassword().equals(other.getPassword()) &&
		getRealm().equals(other.getRealm());
	}
    }

    public final void setProxyServer(String host, int port) 
    {
	m_proxyHost = host;
	m_proxyPort = port;
    }

    final String getProxyHost() 
    {
	return m_proxyHost;
    }

    final int getProxyPort()
    {
	return m_proxyPort;
    }

    private static final HTTPPluginConnectionDefaults
	s_defaultConnectionDefaults = new HTTPPluginConnectionDefaults();

    private static final Map m_connectionDefaults = new HashMap();

    final static HTTPPluginConnectionDefaults getDefaults(URI keyURI)
    {
	synchronized (m_connectionDefaults) {
	    final HTTPPluginConnectionDefaults connectionSpecificDefaults =
		(HTTPPluginConnectionDefaults)
		m_connectionDefaults.get(keyURI);

	    if (connectionSpecificDefaults != null) {
		return connectionSpecificDefaults;
	    }

	    return s_defaultConnectionDefaults;
	}
    }

    public static final HTTPPluginConnection getDefaultConnectionDefaults() 
    {
	return s_defaultConnectionDefaults;
    }

    public static final HTTPPluginConnection getConnectionDefaults(
	String uriString)
	throws ParseException, ProtocolNotSuppException
    {
	final URI uri = new URI(uriString);
	    
	final URI keyURI =
	    new URI(uri.getScheme(), uri.getHost(), uri.getPort(), "");

	synchronized (m_connectionDefaults) {
	    final HTTPPluginConnection existingConnectionDefaults =
		(HTTPPluginConnection)m_connectionDefaults.get(keyURI);

	    if (existingConnectionDefaults != null) {
		return existingConnectionDefaults;
	    }

	    final HTTPPluginConnection newConnectionDefaults =
		new HTTPPluginConnectionDefaults();

	    m_connectionDefaults.put(keyURI, newConnectionDefaults);

	    return newConnectionDefaults;
	}
    }
}
