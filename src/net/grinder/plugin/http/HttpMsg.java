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
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URLConnection;
import java.net.URL;
import java.util.Iterator;
import java.util.Map;

import HTTPClient.Codecs;

import net.grinder.plugininterface.Logger;
import net.grinder.plugininterface.PluginException;
import net.grinder.plugininterface.PluginThreadContext;


/**
 * Util class for sending HTTP requests.
 *
 * Wrap up HTTP requests, cache cookies across a number of calls,
 * simulate a browser cache.
 * 
 * @author Paco Gomez
 * @author Philip Aston
 * @version $Revision$
 */
class HttpMsg implements HTTPHandler
{
    private final PluginThreadContext m_pluginThreadContext;
    private final boolean m_useCookies;
    private final boolean m_useCookiesVersionString;
    private final boolean m_followRedirects;
    private CookieHandler m_cookieHandler;
    private final boolean m_dontReadBody;
    private final boolean m_timeIncludesTransaction;

    public HttpMsg(PluginThreadContext pluginThreadContext, boolean useCookies,
		   boolean useCookiesVersionString, boolean followRedirects,
		   boolean timeIncludesTransaction)
    {
	m_pluginThreadContext = pluginThreadContext;
	m_useCookies = useCookies;
	m_useCookiesVersionString = useCookiesVersionString;
	m_followRedirects = followRedirects;
	m_timeIncludesTransaction = timeIncludesTransaction;

	// Hack to work around buffering problem when used in
	// conjunction with the TCPSniffer.
	m_dontReadBody = 
	    pluginThreadContext.getPluginParameters().
	    getBoolean("dontReadBody", false);
    }

    public String sendRequest(HTTPHandler.RequestData requestData)
	throws HTTPHandlerException
    {
	try {
	    final String urlString = requestData.getURLString();
	    final URL url = new URL(urlString);

	    final String postString = requestData.getPostString();

	    m_pluginThreadContext.startTimer();

	    HttpURLConnection connection;

	    try {
		connection = (HttpURLConnection)url.openConnection();
	    }
	    finally {
		if (!m_timeIncludesTransaction) {
		    m_pluginThreadContext.stopTimer();
		}
	    }

	    final Iterator headersIterator =
		requestData.getHeaders().entrySet().iterator();

	    while (headersIterator.hasNext()) {
		final Map.Entry entry = (Map.Entry)headersIterator.next();

		connection.setRequestProperty((String)entry.getKey(),
					      (String)entry.getValue());
	    }

	    final AuthorizationData authorizationData =
		requestData.getAuthorizationData();

	    if (authorizationData != null &&
		authorizationData instanceof
		HTTPHandler.BasicAuthorizationData) {

		final HTTPHandler.BasicAuthorizationData
		    basicAuthorizationData =
		    (HTTPHandler.BasicAuthorizationData)authorizationData;

		connection.setRequestProperty(
		    "Authorization",
		    "Basic " +
		    Codecs.base64Encode(
			basicAuthorizationData.getUser() + ":" +
			basicAuthorizationData.getPassword()));
	    }

	    // Optionally stop URLConnection from handling http 302
	    // forwards, because these contain the cookie when handling
	    // web app form based authentication.
	    connection.setInstanceFollowRedirects(m_followRedirects);
            
	    // Think "=;" will match nothing but empty cookies. If your
	    // bothered by this, please read RFC 2109 and fix.
	    if (m_useCookies) {
		final String cookieString =
		    m_cookieHandler.getCookieString(url,
						    m_useCookiesVersionString);

		if (cookieString != null) {
		    connection.setRequestProperty("Cookie", cookieString);
		}
	    }

	    connection.setUseCaches(false);
	
	    if (postString != null) {
		connection.setRequestMethod("POST");
		connection.setDoOutput(true);

		final BufferedOutputStream bos = 
		    new BufferedOutputStream(connection.getOutputStream());
		final PrintWriter out = new PrintWriter(bos);

		out.write(postString);
		out.close();
	    }
	
	    connection.connect();

	    // This is before the getHeaderField for a good reason.
	    // Otherwise the %^(*$ HttpURLConnection API silently catches
	    // the exception in getHeaderField and rethrows it in the
	    // getResponseCode (with the original stack trace). - PAGA
	    final int responseCode = connection.getResponseCode();

	    if (m_useCookies) {
		// set to 1 because we're skipping the HTTP status line
		int headerIndex = 1;
		String headerKey = null;
		String headerValue = connection.getHeaderField(headerIndex);
		// iterate through all available headers
		while(headerValue != null){
		    headerKey = connection.getHeaderFieldKey(headerIndex);
		    if(headerKey != null && "Set-Cookie".equals(headerKey)){
			m_cookieHandler.setCookies(headerValue, url);
		    }
		    headerValue = connection.getHeaderField(++headerIndex);
		}
	    }

	    if (responseCode == HttpURLConnection.HTTP_OK) {          
		// Slurp the response into a StringWriter.
		final InputStreamReader isr =
		    new InputStreamReader(connection.getInputStream());
		final BufferedReader in = new BufferedReader(isr);

		// Default StringWriter buffer size is usually 16 which is way small.
		final StringWriter stringWriter = new StringWriter(512);

		char[] buffer = new char[512];
		int charsRead = 0;

		if (!m_dontReadBody) {
		    while ((charsRead = 
			    in.read(buffer, 0, buffer.length)) > 0) {
			stringWriter.write(buffer, 0, charsRead);
		    }
		}

		in.close();
		stringWriter.close();
	    
		m_pluginThreadContext.logMessage(urlString + " OK");

		return stringWriter.toString();
	    }
	    else if (responseCode == HttpURLConnection.HTTP_NOT_MODIFIED) {
		m_pluginThreadContext.logMessage(urlString +
						 " was not modified");
	    }
	    else if (responseCode == HttpURLConnection.HTTP_MOVED_PERM ||
		     responseCode == HttpURLConnection.HTTP_MOVED_TEMP ||
		     responseCode == 307) {
		// It would be possible to perform the check
		// automatically, but for now just chuck out some
		// information.
		m_pluginThreadContext.logMessage(urlString +
						 " returned a redirect (" +
						 responseCode + "). " +
						 "Ensure the next URL is " +
						 connection.getHeaderField(
						     "Location"));

		// I've seen the code that slurps the body block for non
		// 200 responses. Can't think off the top of my head how
		// to code to poll for a body, so for now just ignore the
		// problem.
		return null;
	    }
	    else {
		m_pluginThreadContext.logError("Unknown response code: " +
					       responseCode + " for " +
					       urlString);
	    }

	    return null;
	}
	catch (Exception e) {
	    throw new HTTPHandlerException(e.getMessage(), e);
	}
	finally {
	    // Back stop.
	    m_pluginThreadContext.stopTimer();
	}
    }

    public void reset(){
        m_cookieHandler = new CookieHandler(m_pluginThreadContext);
    }
}
