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

package net.grinder.plugin.http;

import java.net.HttpURLConnection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.StringTokenizer;


/**
 * Util class for handling HTTP cookies.
 *
 * <p>This is a mickey mouse implementation of RFC 2109. If anyone has
 * time on their hands, please see http://www.ietf.org/rfc/rfc2109.txt
 * and fix.</p>
 *
 * <p>In particular, it does not support the behaviour defined by the
 * following Set-Cookie attributes: <ul><li>Domain</li><li>Max-age
 * (other than
 * "0")</li><li>Path</li><li>Secure</li><li>Version</li></ul>(That's
 * pretty much all of them!).</p>
 *
 * @author Philip Aston
 * @version $Revision$
 */
class CookieHandler
{
    private final Map m_cookies = new HashMap();

    public void captureCookies(String setCookieString)
    {
	final StringTokenizer tokenizer = new StringTokenizer(setCookieString,
							      ";");

	Cookie currentCookie = new Cookie(null, null);

	while (tokenizer.hasMoreTokens()) {
	    final String attributeValuePair = tokenizer.nextToken();
	    final int index = attributeValuePair.indexOf('=');
	    
	    final String name;
	    final String value;
	    
	    if (index >= 0) {
		name = attributeValuePair.substring(0, index).trim();
		value = attributeValuePair.substring(index+1).trim();
	    }
	    else {
		name = attributeValuePair;
		value = "";
	    }

	    currentCookie = currentCookie.addAttribute(name, value);
	}
    }

    String getCookieString()
    {
	if (m_cookies.size() == 0) {
	    return null;
	}
	else {
	    final StringBuffer result = new StringBuffer();

	    final Iterator cookieIterator = m_cookies.values().iterator();

	    boolean first = true;

	    while (cookieIterator.hasNext()) {
		final Cookie cookie = (Cookie)cookieIterator.next();

		if (first) {
		    first = false;
		}
		else {
		    result.append(';');
		}

		result.append(cookie);
	    }

	    return result.toString();
	}
    }
    
    class Cookie
    {
	private final String m_name;
	private final String m_value;
	private String m_comment = null;
	private String m_domain = null;
	private String m_path = null;
	private String m_maxAge = null;
	private boolean m_secure = false;
	private String m_version = null;

	public Cookie()
	{
	    m_name = null;
	    m_value = null;
	}

	private Cookie(String name, String value)
	{
	    m_name = name;
	    m_value = value;
	}

	public Cookie addAttribute(String name, String value)
	{
	    if (m_name != null) {
		if (name.equals("Comment")) {
		    m_comment = value;
		    return this;
		}
		else if (name.equals("Domain")) {
		    m_domain = value;
		    return this;
		}
		else if (name.equals("Max-Age")) {
		    m_maxAge = value;
		    return this;
		}
		else if (name.equals("Path")) {
		    m_path = value;
		    return this;
		}
		else if (name.equals("Secure")) {
		    m_secure = true;
		    return this;
		}
		else if (name.equals("Version")) {
		    m_version = value;
		    return this;
		}
	    }

	    updateStore();

	    return new Cookie(name, value);
	}

	private void updateStore()
	{
	    if (m_name != null) {
		if (m_maxAge != null && m_maxAge.equals("0") ||
		    m_value == "")	// IMHO this is bogus, but support E-Pizza for now.
		{
		    m_cookies.remove(m_name);
		}
		else {
		    m_cookies.put(m_name, this);
		}
	    }
	}

	public String toString()
	{
	    return m_name + "=" + m_value;
	}
    }    
}
