// Copyright (C) 2000 Paco Gomez
// Copyright (C) 2000, 2001, 2002 Philip Aston
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
 **/
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

	    final String queryString =  uri.getQueryString();
	    final String pathString;

	    if ((queryString != null) && (queryString.length() > 0)) {  
		pathString = uri.getPath() + '?' + queryString;
	    }
	    else {
		pathString = uri.getPath();
	    }

	    HTTPResponse response;

	    if (postString == null) {
		// We don't pass the query string to the second
		// parameter of Get() as we don't want it to be URL
		// encoded.
		response = httpConnection.Get(pathString, (String)null,
					      additionalHeaders);
	    }
	    else {
		// HTTPClient defaults to application/octet-stream,
		// but this is a better default.
		if (!seenContentType) {
		    additionalHeaders[nextHeader++] =
			new NVPair("Content-Type",
				   "application/x-www-form-urlencoded");
		}

		response = httpConnection.Post(pathString, postString,
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
