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

import java.net.URL;
import java.text.DecimalFormat;
import java.text.ParsePosition;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;

import net.grinder.plugininterface.Logger;


/**
 * Utility class for handling HTTP cookies.
 *
 * <p>This is an implementation of RFC 2109.</p>
 *
 * <p>It is deficient in at least the following ways:
 * <ul>
 * <li>Different version attributes for a single origin server are not handled - the first version is used</li>
 * <li>Semi-colons in quoted values are not handled correctly</li>
 * <li>Secure attribute is ignored</li>
 * <li>Support for the historical Expires attribute is missing.</li>
 * </ul>
 *
 * <p>This class is not <code>synchronized</code>.</p>
 *
 * @author Philip Aston
 * @version $Revision$
 */
class CookieHandler
{
    private Store m_store = new Store();
    private long m_currentTime = 0;
    private final Logger m_logger;

    public CookieHandler(Logger logger)
    {
	m_logger = logger;
    }

    public void setCookies(String setCookieString, URL requestURL)
    {
	updateCurrentTime();

	final String requestHost = requestURL.getHost();
	final String requestPath = canonicalisePath(requestURL.getPath());

	final StringTokenizer tokenizer = new StringTokenizer(setCookieString,
							      ";");

	Cookie currentCookie = null;

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

	    if (name.length() > 0) {
		if (currentCookie == null) {
		    currentCookie =
			new Cookie(requestHost, name, requestPath, value);
		}
		else {
		    final boolean recognisedAttribute =
			currentCookie.addAttribute(name, value, requestHost);

		    if (!recognisedAttribute) {
			addOrRemoveCookie(currentCookie);
			currentCookie =
			    new Cookie(requestHost, name, requestPath, value);
		    }
		}
	    }
	    else {
		m_logger.logError("Ignoring '" + attributeValuePair
				  +"' in Set-Cookie string");
	    }
	}

	if (currentCookie != null) {
	    addOrRemoveCookie(currentCookie);
	}
    }

    private void updateCurrentTime()
    {
	m_currentTime = System.currentTimeMillis();
    }

    private void addOrRemoveCookie(Cookie cookie)
    {
	if (cookie.isValid()) {
	    m_store.add(cookie);
	}
	else {
	    m_store.remove(cookie);
	}
    }

    String getCookieString(URL url)
    {
	final List cookies = m_store.get(url);

	if (cookies.size() == 0) {
	    return null;
	}
	else {
	    final Iterator cookieIterator = cookies.iterator();

	    final StringBuffer cookieString = new StringBuffer();
	    int version = -1;

	    while (cookieIterator.hasNext()) {
		final Cookie cookie = (Cookie)cookieIterator.next();

		final int cookieVersion = cookie.getVersion();

		if (cookieVersion != -1) {
		    if (version == -1) {
			version = cookieVersion;
		    }
		    else if (version != cookieVersion) {
			m_logger.logError("Ignoring multiple cookie versions");
		    }
		}

		cookieString.append("; ");
		cookieString.append(cookie.getCookieValue());
	    }

	    final StringBuffer result = new StringBuffer();

	    // RFC 2109 is inconsistent regarding the default value to
	    // use for "$Version". Section 4.3.1 indicates that it
	    // should default to the "old cookie" behavior as
	    // originally specified by Netscape (whatever than may
	    // be).This conflicts directly with section 4.3.4 which
	    // says it defaults to "0". I've chosen to implement the
	    // later.
	    result.append("$Version=\"");
	    result.append(version == -1 ? 0 : version);
	    result.append("\"");
	    result.append(cookieString);

	    return result.toString();
	}
    }

    /**
     * Expose for unit tests.
     */
    Store getStore() 
    {
	return m_store;
    }

    /**
     * Global store of current cookies. Implementation is fairly naïve
     * - there's plenty of scope for optimisation here, but please
     * measure first.
     */
    class Store 
    {
	// Use a TreeSet so that cookies are in appropriate order.
	// (We prefer gets to sets).
	private final Set m_cookies = new TreeSet();

	/** For unit test only. */
	private final Map m_cookiesByName = new HashMap();

	public void add(Cookie cookie)
	{
	    m_cookies.remove(cookie); // Remove any old value.
	    m_cookies.add(cookie);
	    m_cookiesByName.put(cookie.getName(), cookie);
	}
	
	public void remove(Cookie cookie)
	{
	    m_cookies.remove(cookie);
	    m_cookiesByName.remove(cookie.getName());
	}

	public int getSize()
	{
	    return m_cookies.size();
	}

	public List get(URL url)
	{
	    updateCurrentTime();

	    final String urlPath = canonicalisePath(url.getPath());

	    final List result = new LinkedList();

	    // For now, do a dumb linear search.
	    final Iterator cookieIterator = m_cookies.iterator();

	    while (cookieIterator.hasNext()) {
		final Cookie cookie = (Cookie)cookieIterator.next();

		// See RFC 2109 - 4.3.4
		if (cookie.isExpired()) {
		    cookieIterator.remove();
		}
		else if (urlPath.startsWith(cookie.getPath()) &&
		    domainMatch(url.getHost(), cookie.getDomain(), false))
		{
		    result.add(cookie);
		}
	    }

	    return result;
	}

	/**
	 * Access to last Cookie stored with given name for unit
	 * tests only.
	 */
	Cookie get(String name)
	{
	    return (Cookie)m_cookiesByName.get(name);
	}
    }
    
    class Cookie implements Comparable
    {
	private String m_domain;
	private final String m_name;
	private String m_path;
	private final String m_value;
	private String m_comment = null;
	private long m_expiryTime = -1;
	private boolean m_explicitPath = false;
	private boolean m_explicitDomain = false;
	private boolean m_secure = false;
	private int m_version = -1;

	private int m_hashCode;
	private boolean m_valid;


	private final DecimalFormat m_integerFormat = new DecimalFormat("0");
	
	{
	    m_integerFormat.setParseIntegerOnly(true);
	}
	
	/**
	 * From RFC 2109 - 4.3.1
	 *
	 *   Version - Defaults to "old cookie" behavior as originally
	 *   specified by Netscape. See the HISTORICAL section.
	 *
	 *   Domain - Defaults to the request-host. (Note that there
	 *   is no dot at the beginning of request-host.)
	 *
	 *   Max-Age - The default behavior is to discard the cookie
	 *   when the user agent exits.
	 * 
	 *   Path - Defaults to the path of the request URL that
	 *   generated the Set-Cookie response, up to, but not
	 *   including, the right-most /.
	 * 
	 *   Secure - If absent, the user agent may send the cookie
	 *   over an insecure channel.
	 *
	 */
	public Cookie(String domain, String name, String path, String value)
	{
	    m_domain = domain;
	    m_name = name;
	    m_path = path;
	    calculateHashCode();

	    m_value = value;

	    // AFAICT this is not specified by RFC 2109, but its what
	    // the browsers do.
	    m_valid = m_value.length() > 0;
	}

	public boolean equals(Object o)
	{
	    if (o == this) {
		return true;
	    }

	    if (!(o instanceof Cookie)) {
		return false;
	    }

	    final Cookie theOther = (Cookie)o;

	    return
		m_hashCode == theOther.m_hashCode &&
		m_domain.equals(theOther.m_domain) &&
		m_name.equals(theOther.m_name) &&
		// If neither path is explicit (i.e. both default),
		// they are equal...
		(!m_explicitPath && !theOther.m_explicitPath ||
		 // .. otherwise, both must be explicit.
		 m_explicitPath && theOther.m_explicitPath &&
		 m_path.equals(theOther.m_path));
	}

	/**
	 *  RFC 2109 - 4.3.4 says "If multiple cookies satisfy the
	 *  criteria above, they are ordered in the Cookie header such
	 *  that those with more specific Path attributes precede
	 *  those with less specific. Ordering with respect to other
	 *  attributes (e.g., Domain) is unspecified."
	 *
	 *  We order by domain, name, then path (as above). I interpet
	 *  default paths (which are set to the same as the request
	 *  URI) to be "less specific" than explicitly specified
	 *  paths.
	 */
	public int compareTo(Object o)
	{
	    if (equals(o)) {
		return 0;
	    }

	    final Cookie theOther = (Cookie)o;

	    int result = m_domain.compareTo(theOther.m_domain);

	    if (result == 0) {
		result = m_name.compareTo(theOther.m_name);

		if (result == 0) {
		    // Explicitly set paths come first.
		    if (m_explicitPath && !theOther.m_explicitPath) {
			result = -1;
		    }
		    else if (!m_explicitPath && theOther.m_explicitPath) {
			result = 1;
		    }
		    else if (m_explicitPath && theOther.m_explicitPath) {
			// If both are explicit, the more specific
			// path comes first. (N.B we know they're not
			// equal because of the equals() check above).
			if (m_path.startsWith(theOther.m_path))
			{
			    result = -1;
			}
			else if (theOther.m_path.startsWith(m_path))
			{
			    result = 1;
			}
			else
			{
			    // Nothing to chose between them, pick a
			    // deterministic ordering. This case won't
			    // happen when called by Store.get().
			    result = m_path.compareTo(theOther.m_path);
			}
		    }
		    else {
			// Both are implicit. Pick a deterministic
			// ordering. This case won't happen when
			// called by Store.get().
			result = m_path.compareTo(theOther.m_path);
		    }
		}
	    }

	    return result;
	}

	private void calculateHashCode()
	{
	    m_hashCode =
		m_domain.hashCode() ^
		m_name.hashCode() ^
		m_path.hashCode() ^
		(m_explicitPath ? 0 : -1);
	}

	public int hashCode()
	{
	    return m_hashCode;
	}

	/**
	 * Returns true if the name and value are recognised as part
	 * of this Cookie's specification.
	 */
	public boolean addAttribute(String name, String value,
				    String requestHost)
	{
	    if (name.equalsIgnoreCase("comment")) {
		m_comment = value;
		return true;
	    }
	    else if (name.equalsIgnoreCase("domain")) {

		// Strip the any quotes so comparing is easier.
		value = stripQuotes(value);

		final int secondDot = value.indexOf('.', 1);

		if (
		    // Reject if "The value for the Domain attribute
		    // contains no embedded dots or does not start
		    // with a dot."
		    !value.startsWith(".") ||
		    secondDot == -1 ||
		    secondDot == value.length()-1 
		    ||
		    // Reject if "The value for the request-host does
		    // not domain-match the Domain attribute." or "The
		    // request-host is a FQDN (not IP address) and has
		    // the form HD, where D is the value of the Domain
		    // attribute, and H is a string that contains one
		    // or more dots."
		    !domainMatch(requestHost, value, true)) {
		    m_logger.logError(
			"Set-Cookie has invalid Domain '" + value + 
			"' for request host '" + requestHost +
			"', discarding cookie as per RFC 2109, 4.3.2");

		    m_valid = false;
		}
		else {
		    m_domain = value;
		    m_explicitDomain = true;
		    calculateHashCode();
		}

		return true;
	    }
	    else if (name.equalsIgnoreCase("max-age")) {
		final ParsePosition position = new ParsePosition(0);

		value = stripQuotes(value);

		final Number number = m_integerFormat.parse(value, position);

		if (number == null ||
		    position.getIndex() != value.length() ||
		    number.intValue() < 0) {
		    m_logger.logError(
			"Set-Cookie has invalid Max-Age '" + value +
			"', ignoring");
		}
		else {
		    final long maxAge = number.longValue();
		    
		    if (maxAge == 0) {
			m_valid = false;
		    }
		    else {
			m_expiryTime = m_currentTime + maxAge * 1000;
		    }
		}

		return true;
	    }
	    else if (name.equalsIgnoreCase("path")) {
		//  Note, RFC 2109 - 4.3.2 says that cookies should be
		//  rejected if "The value for the Path attribute is
		//  not a prefix of the request-URI.". I haven't
		//  implemented this because it would it would make
		//  setting a path more restrictive than the request
		//  URI illegal. I suspect this is an error in the
		//  RFC. If anyone knows any better... - PA

		// Strip the any quotes so comparing is easier.
		value = stripQuotes(value);

		m_path = canonicalisePath(value);
		m_explicitPath = true;
		calculateHashCode();
		return true;
	    }
	    else if (name.equalsIgnoreCase("secure")) {
		m_secure = true;

		if (value.length() != 0) {
		    m_logger.logError(
			"Set-Cookie Secure attribute shouldn't have " +
			"value, found '" + value + "', ignoring");
		}

		return true;
	    }
	    else if (name.equalsIgnoreCase("version")) {
		final ParsePosition position = new ParsePosition(0);

		value = stripQuotes(value);

		final Number number = m_integerFormat.parse(value, position);

		if (number == null ||
		    position.getIndex() != value.length()) {
		    m_logger.logError(
			"Set-Cookie has invalid Version '" + value +
			"', ignoring");
		}
		else {
		    m_version = number.intValue();
		}

		return true;
	    }

	    return false;
	}

	/**
	 * Dumb handling of quoted strings.
	 */
	private String stripQuotes(String value)
	{
	    final int valueLength = value.length();

	    if (valueLength > 1 &&
		value.charAt(0) == '"') {

		return value.substring(1, valueLength-1);
	    }
	    else {
		return value;
	    }
	}

	public String getCookieValue()
	{
	    final StringBuffer result = new StringBuffer();

	    // We strip any quotes off Path and Domains on the way in
	    // so they are in a canonical form and always add quotes
	    // on the way out. We leave the value exactly as it was.
	    result.append(m_name);
	    result.append("=");
	    result.append(m_value);

	    if (m_explicitPath) {
		result.append("; $Path=\"");
		result.append(m_path);
		result.append("\"");
	    }

	    if (m_explicitDomain) {
		result.append("; $Domain=\"");
		result.append(m_domain);
		result.append("\"");
	    }

	    return result.toString();
	}

	public String toString()
	{
	    return "Cookie (" + m_domain + ", " + m_name + ", " + m_path + ")";
	}

	boolean isValid() 
	{
	    return m_valid;
	}

	String getName() 
	{
	    return m_name;
	}

	String getValue() 
	{
	    return m_value;
	}

	String getComment() 
	{
	    return m_comment;
	}

	String getDomain() 
	{
	    return m_domain;
	}

	boolean isExpired() 
	{
	    return (m_expiryTime > 0) && (m_expiryTime < m_currentTime);
	}

	String getPath() 
	{
	    return m_path;
	}

	boolean getSecure() 
	{
	    return m_secure;
	}

	int getVersion() 
	{
	    return m_version;
	}
    }

    /**
     * See RFC 2109, sections 2 and 4.3.2.
     */
    private static boolean domainMatch(String hostA, String hostB,
				       boolean atMostOnePlace)
    {
	if (hostA.equals(hostB)) {
	    return true;
	}
	
	final int index = hostA.lastIndexOf(hostB);

	if (hostB.startsWith(".") &&
	    index > 0 &&
	    (!atMostOnePlace ||
	     hostA.substring(0, index).indexOf(".") == -1)) {
	    return true;
	}

	return false;
    }

    private String canonicalisePath(String path) 
    {
	StringBuffer result = new StringBuffer();

	final int pathLength = path.length();
	
	// I can't find it explicitly stated in RFC 2109 that that
	// paths must start with '/'. This might be bogus...
	if (!path.startsWith("/")) {
	    result.append('/');
	}

	if (pathLength > 1 &&	// .. as might this.
	    path.endsWith("/")) {
	    result.append(path.substring(0, pathLength-1));
	}
	else {
	    result.append(path);
	}

	return result.toString();
    }
}
