// The Grinder
// Copyright (C) 2000, 2001  Paco Gomez
// Copyright (C) 2000, 2001  Philip Aston

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

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.HttpURLConnection; // For the (incomplete!) status code definitions only.
import java.util.Iterator;
import java.util.HashMap;
import java.util.Map;

import HTTPClient.Cookie;
import HTTPClient.CookieModule;
import HTTPClient.CookiePolicyHandler;
import HTTPClient.DefaultAuthHandler;
import HTTPClient.HTTPConnection;
import HTTPClient.HttpOutputStream;
import HTTPClient.HTTPResponse;
import HTTPClient.ModuleException;
import HTTPClient.NVPair;
import HTTPClient.ParseException;
import HTTPClient.ProtocolNotSuppException;
import HTTPClient.RoRequest;
import HTTPClient.RoResponse;
import HTTPClient.URI;

import net.grinder.plugininterface.PluginThreadContext;


/**
 * @author Philip Aston
 * @version $Revision$
 */
class HTTPClientHandler implements HTTPHandler
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

    private final PluginThreadContext m_threadContext;
    private final boolean m_useCookies;
    private final boolean m_followRedirects;

    public HTTPClientHandler(PluginThreadContext threadContext,
			     boolean useCookies, boolean followRedirects)
    {
	m_threadContext = threadContext;
	m_useCookies = useCookies;
	m_followRedirects = followRedirects;
    }

    // For now, just handle a single server.
    private Map m_httpConnections = new HashMap();

    private HTTPConnection getConnection(URI uri)
	throws ParseException, ProtocolNotSuppException
    {
	final URI keyURI =
	    new URI(uri.getScheme(), uri.getHost(), uri.getPort(), "");

	HTTPConnection connection =
	    (HTTPConnection)m_httpConnections.get(keyURI);

	if (connection == null) {
	    connection = new HTTPConnection(uri);
	    connection.setContext(HTTPClientHandler.this);

	    if (m_useCookies) {
		connection.addModule(CookieModule.class, 0);
	    }
	    else {
		connection.removeModule(CookieModule.class);
	    }

	    if (m_followRedirects) {
		connection.addModule(s_redirectionModule, 0);
	    }
	    else {
		connection.removeModule(s_redirectionModule);
	    }

	    m_httpConnections.put(keyURI, connection);
	}

	return connection;
    }

    public String sendRequest(HTTPHandler.RequestData requestData)
	throws HTTPHandlerException
    {
	try {
	    final URI uri = new URI(requestData.getURLString());
	    final String postString = requestData.getPostString();

	    m_threadContext.startTimer();

	    final HTTPConnection httpConnection =
		getConnection(uri);

	    final AuthorizationData authorizationData =
		requestData.getAuthorizationData();

	    if (authorizationData != null &&
		authorizationData instanceof
		HTTPHandler.BasicAuthorizationData) {

		final HTTPHandler.BasicAuthorizationData
		    basicAuthorizationData =
		    (HTTPHandler.BasicAuthorizationData)authorizationData;

		httpConnection.addBasicAuthorization(
		    basicAuthorizationData.getRealm(),
		    basicAuthorizationData.getUser(),
		    basicAuthorizationData.getPassword());
	    }

	    final Map headers = requestData.getHeaders();

	    // HTTPClient ignores null header values.
	    final NVPair[] additionalHeaders = new NVPair[5 + headers.size()];
	    int nextHeader = 0;

	    final Iterator headersIterator = headers.entrySet().iterator();
	    boolean seenContentType = false;

	    while (headersIterator.hasNext()) {
		final Map.Entry entry = (Map.Entry)headersIterator.next();
		final String key = (String)entry.getKey();
		final String value = (String)entry.getValue();

		additionalHeaders[nextHeader++] = new NVPair(key, value);

		// Some browsers send "Content-type" instead of
		// "Content-Type."
		if (!seenContentType && "Content-Type".equalsIgnoreCase(key)) {
		    seenContentType = true;
		}
	    }

	    HTTPResponse response;

	    if (postString == null) {
		String queryString =  uri.getQueryString();

		if ((queryString != null) && (queryString.length() > 0)) {  
		    // Don't pass the query string to the second
		    // parameter, we don't want it to be URL encoded.
		    response = httpConnection.Get(uri.getPath() + '?' +
						  queryString, (String)null,
						  additionalHeaders);
		} else {
		    response = httpConnection.Get(uri.getPath(), (String)null,
						  additionalHeaders);
		}
	    }
	    else {
		// HTTPClient defaults to application/octet-stream,
		// but this is a better default.
		if (!seenContentType) {
		    additionalHeaders[nextHeader++] =
			new NVPair("Content-Type",
				   "application/x-www-form-urlencoded");
		}

		response = httpConnection.Post(uri.getPath(), postString,
					       additionalHeaders);
	    }

	    final int statusCode = response.getStatusCode();
	    final byte[] data = response.getData();
	    response.getInputStream().close();
	    m_threadContext.stopTimer();
	
	    if (statusCode == HttpURLConnection.HTTP_OK) {
		m_threadContext.logMessage(uri + " OK");
	    }
	    else if (statusCode == HttpURLConnection.HTTP_NOT_MODIFIED) {
		m_threadContext.logMessage(uri + " was not modified");
	    }
	    else if (statusCode == HttpURLConnection.HTTP_MOVED_PERM ||
		     statusCode == HttpURLConnection.HTTP_MOVED_TEMP ||
		     statusCode == 307) {
		// It would be possible to perform the check
		// automatically, but for now just chuck out some
		// information.
		m_threadContext.logMessage(
		    uri + " returned a redirect (" + statusCode + "). " +
		    "Ensure the next URL is " + 
		    response.getHeader("Location"));
	    }
	    else {
		m_threadContext.logError("Unknown response code: " +
					 statusCode + " (" + 
					 response.getReasonLine() +
					 ") for " + uri);
	    }

	    return data != null? new String(data) : null;
	}
	catch (Exception e) {
	    throw new HTTPHandlerException(e.getMessage(), e);
	}
	finally {
	    // Back stop.
	    m_threadContext.stopTimer();
	}
    }

    public void reset() {
	final Iterator i = m_httpConnections.values().iterator();
	
	while (i.hasNext()) {
	    ((HTTPConnection)i.next()).stop();
	}

	CookieModule.discardAllCookies(HTTPClientHandler.this);

        m_httpConnections.clear();
    }
}
