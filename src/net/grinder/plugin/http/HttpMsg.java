// The Grinder
// Copyright (C) 2000  Paco Gomez
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
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import net.grinder.plugininterface.Logger;
import net.grinder.plugininterface.PluginException;
import net.grinder.plugininterface.PluginThreadContext;


/**
 * Util class for sending HTTP requests.
 *
 * Wrap up HTTP requests, cache a cookie across a number of calls,
 * simulate a browser cache.
 * 
 * BUGS:
 *   One shouldn't expect much from a class with such a the duffly
 *   capitalised name, but HttpURLConnection sucks. It refuses to accept
 *   the reality of multiple header fields. This means that only one
 *   Set-Cookie header can be honoured per message.
 *
 * @author Paco Gomez
 * @author Philip Aston
 * @version $Revision$
 */
class HttpMsg
{
    private final PluginThreadContext m_pluginThreadContext;
    private boolean m_useCookies;
    private boolean m_followRedirects;
    private CookieHandler m_cookieHandler;
    private boolean m_dontReadBody;

    public HttpMsg(PluginThreadContext pluginThreadContext, boolean useCookies,
		   boolean followRedirects)
    {
	m_pluginThreadContext = pluginThreadContext;
	m_useCookies = useCookies;
	m_followRedirects = followRedirects;
	reset();

	// Hack to work around buffering problem when used in
	// conjunction with the TCPSniffer.
	m_dontReadBody = 
	    pluginThreadContext.getPluginParameters().
	    getBoolean("dontReadBoolean", false);
    }

    public String sendRequest(HttpRequestData requestData)
	throws java.io.IOException, PluginException
    {
	final String urlString = requestData.getURLString();
	URL url = null;

	try {
	    url = new URL(urlString);
	}
	catch (MalformedURLException e) {
	    // Maybe it was a relative URL.
	    final URL contextURL = new URL(requestData.getContextURLString());

	    url = new URL(contextURL, urlString); // Let this one fail.
	}

	final String postString = requestData.getPostString();

	m_pluginThreadContext.startTimer();

	HttpURLConnection connection;

	try {
	    connection = (HttpURLConnection) url.openConnection();
	}
	finally {
	    m_pluginThreadContext.stopTimer();
	}

	final long ifModifiedSince = requestData.getIfModifiedSince();

	if (ifModifiedSince >= 0) {
	    connection.setIfModifiedSince(ifModifiedSince);
	}

	// Optionally stop URLConnection from handling http 302
	// forwards, because these contain the cookie when handling
	// web app form based authentication.
	connection.setInstanceFollowRedirects(m_followRedirects);
            
	// Think "=;" will match nothing but empty cookies. If your
	// bother by this, please read RFC 2109 and fix.
	if (m_useCookies) {
	    final String cookieString = m_cookieHandler.getCookieString(url);

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
	    // Sadly HttpURLConnection gives us no way to get at more
	    // than one Set-Cookie header.
	    final String setCookieString =
		connection.getHeaderField("Set-Cookie");

	    if (setCookieString != null) {
		m_cookieHandler.setCookies(setCookieString, url);
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
		while ((charsRead = in.read(buffer, 0, buffer.length)) > 0) {
		    stringWriter.write(buffer, 0, charsRead);
		}
	    }

	    in.close();
	    stringWriter.close();
	    
	    m_pluginThreadContext.logMessage(urlString + " OK");

	    return stringWriter.toString();
	}
	else if (responseCode == HttpURLConnection.HTTP_NOT_MODIFIED) {
	    m_pluginThreadContext.logMessage(urlString + " was not modified");
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
				       connection.getHeaderField("Location"));

	    // I've seen the code that slurps the body block for non
	    // 200 responses. Can't think off the top of my head how
	    // to code to poll for a body, so for now just ignore the
	    // problem.
	    return null;
	}
	else {
	    m_pluginThreadContext.logError("Unknown response code: " +
					   responseCode + " for " + urlString);
	}

	return null;
    }

    public void reset(){
        m_cookieHandler = new CookieHandler(m_pluginThreadContext);
    }
}
