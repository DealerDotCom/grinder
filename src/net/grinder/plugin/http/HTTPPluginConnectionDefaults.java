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
import java.util.HashSet;
import java.util.Set;


/**
 * @author Philip Aston
 * @version $Revision$
 **/
final class HTTPPluginConnectionDefaults implements HTTPPluginConnection
{
    private boolean m_followRedirects = true;
    private boolean m_useCookies = true;
    private Set m_basicAuthorizations = new HashSet();
    private Set m_digestAuthorizations = new HashSet();

    public final boolean getFollowRedirects() 
    {
	return m_followRedirects;
    }

    public final void setFollowRedirects(boolean followRedirects) 
    {
	m_followRedirects = followRedirects;
    }

    public final boolean getUseCookies() 
    {
	return m_useCookies;
    }

    public final void setUseCookies(boolean useCookies) 
    {
	m_useCookies = useCookies;
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
    final Collection getBasicAuthorizations()
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
    final Collection getDigestAuthorizations()
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
}
