// The Grinder
// Copyright (C) 2000  Paco Gomez

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

import java.io.*;
import java.net.*;
import java.util.Hashtable;

import net.grinder.plugininterface.PluginContext;


/**
 * Util class for sending HTTP requests.
 *
 * Wrap up HTTP requests, cache a cookie across a number of calls,
 * simulate a browser cache.
 * 
 * @author Paco Gomez
 * @author Philip Aston
 * @version $Revision$
 */
public class HttpMsg
{
    private final PluginContext m_pluginContext;
    private boolean m_useCookie;
    private boolean m_followRedirects;
    private String m_cookie;

    public HttpMsg(PluginContext pluginContext, boolean useCookie,
		   boolean followRedirects)
    {
	m_pluginContext = pluginContext;
	m_useCookie = useCookie;
	m_followRedirects = followRedirects;
	reset();
    }

    public String sendRequest(HttpRequestData requestData)
	throws java.io.IOException
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


	m_pluginContext.startTimer();

	HttpURLConnection connection;

	try {
	    connection = (HttpURLConnection) url.openConnection();
	}
	finally {
	    m_pluginContext.stopTimer();
	}

	final long ifModifiedSince = requestData.getIfModifiedSince();

	if (ifModifiedSince >= 0) {
	    connection.setIfModifiedSince(ifModifiedSince);
	}

	// Optionally stop URLConnection from handling http 302
	// forwards, because these contain the cookie when handling
	// web app form based authentication.
	connection.setInstanceFollowRedirects(m_followRedirects);
            
	if (m_useCookie && m_cookie != null) {
	    connection.setRequestProperty("Cookie", m_cookie);
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

	if (m_useCookie) {
	    final String s = connection.getHeaderField("Set-Cookie");
	    if (s != null) {
		m_cookie = s;
	    }
	}

	final int responseCode = connection.getResponseCode();

	if (responseCode == HttpURLConnection.HTTP_OK) {          
	    // Slurp the response into a StringWriter.
	    final InputStreamReader isr =
		new InputStreamReader(connection.getInputStream());
	    final BufferedReader in = new BufferedReader(isr);

	    // Default StringWriter buffer size is usually 16 which is way small.
	    final StringWriter stringWriter = new StringWriter(512);

	    char[] buffer = new char[512];
	    int charsRead = 0;

	    while ((charsRead = in.read(buffer, 0, buffer.length)) > 0) {
		stringWriter.write(buffer, 0, charsRead);
	    }    

	    in.close();
	    stringWriter.close();
	    
	    m_pluginContext.logMessage(urlString + " OK");

	    return stringWriter.toString();
	}
	else if (responseCode == HttpURLConnection.HTTP_NOT_MODIFIED) {
	    m_pluginContext.logMessage(urlString + " was not modified");
	}
	else if (responseCode == HttpURLConnection.HTTP_MOVED_PERM ||
		 responseCode == HttpURLConnection.HTTP_MOVED_TEMP ||
		 responseCode == 307) {
	    // It would be possible to perform the check
	    // automatically, but for now just chuck out some
	    // information.
	    m_pluginContext.logMessage(urlString +
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
	    m_pluginContext.logError("Unknown response code: " + responseCode +
			       " for " + urlString);
	}

	return null;
    }

    public void reset(){
        m_cookie = null;
    }
}
