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

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.net.HttpURLConnection; // For the (incomplete!) status code definitions only.
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import HTTPClient.CookieModule;
import HTTPClient.DefaultAuthHandler;
import HTTPClient.HTTPConnection;
import HTTPClient.HTTPResponse;
import HTTPClient.NVPair;
import HTTPClient.ParseException;
import HTTPClient.ProtocolNotSuppException;
import HTTPClient.URI;

import net.grinder.common.GrinderProperties;
import net.grinder.common.Test;
import net.grinder.plugininterface.GrinderPlugin;
import net.grinder.plugininterface.PluginException;
import net.grinder.plugininterface.PluginProcessContext;
import net.grinder.plugininterface.PluginThreadCallbacks;
import net.grinder.plugininterface.PluginThreadContext;


/**
 * HTTP plugin.
 * 
 * @author Paco Gomez
 * @author Philip Aston
 * @version $Revision$
 **/
public class HTTPPlugin implements GrinderPlugin
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

    private PluginProcessContext m_processContext;

    private boolean m_disablePersistentConnections;
    private boolean m_followRedirects;
    private boolean m_logHTML;
    private boolean m_useCookies;

    public void initialize(PluginProcessContext processContext)
	throws PluginException
    {
	m_processContext = processContext;

	final GrinderProperties parameters =
	    m_processContext.getPluginParameters();

	m_disablePersistentConnections =
	    parameters.getBoolean("disablePersistentConnections", false);
	m_followRedirects = parameters.getBoolean("followRedirects", false);
	m_logHTML = parameters.getBoolean("logHTML", false);
	m_useCookies = parameters.getBoolean("useCookies", true);
    }

    public PluginThreadCallbacks createThreadCallbackHandler()
	throws PluginException
    {
	return new HTTPPluginThreadCallbacks();
    }

    protected class HTTPPluginThreadCallbacks implements PluginThreadCallbacks
    {
	private PluginThreadContext m_threadContext = null;
	private int m_currentIteration = 0; // How many times we've done all the URL's
	private final DecimalFormat m_threeFiguresFormat =
	    new DecimalFormat("000");

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
		connection.setContext(HTTPPlugin.this);
		connection.setAllowUserInteraction(false);

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

		if (m_disablePersistentConnections) {
		    NVPair[] def_hdrs = { new NVPair("Connection", "close") };
		    connection.setDefaultHeaders(def_hdrs);
		}
	    
		m_httpConnections.put(keyURI, connection);
	    }

	    return connection;
	}


	/**
	 * This method is executed when the thread starts. It is only
	 * executed once per thread.
	 */
	public void initialize(PluginThreadContext threadContext)
	    throws PluginException
	{
	    m_threadContext = threadContext;
	}

	public void beginRun() throws PluginException
	{
	    // Discard our cookies.
	    CookieModule.discardAllCookies(HTTPPlugin.this);

	    // Close connections from previous run.
	    final Iterator i = m_httpConnections.values().iterator();
	
	    while (i.hasNext()) {
		((HTTPConnection)i.next()).stop();
	    }
	    
	    m_httpConnections.clear();
	}

	public Object invokeTest(Test test, Object parameters)
	    throws PluginException
	{
	    final HTTPTest httpTest = (HTTPTest)test;

	    //	    final CallData callData = (CallData)m_callData.get(test);

	    HTTPResponse httpResponse = null;

	    try {
		final URI uri = new URI(httpTest.getUrl());

		final NVPair[] headers = httpTest.getHeaders();
		final int numberOfTestHeaders =
		    headers != null ? headers.length : 0;

		// HTTPClient ignores null header values.
		final NVPair[] additionalHeaders =
		    new NVPair[5 + numberOfTestHeaders];

		int nextHeader = 0;

		boolean seenContentType = false;

		for (int i=0; i<numberOfTestHeaders; ++i) {
		    final NVPair header = headers[i];
		    
		    additionalHeaders[nextHeader++] = header;

		    // Some browsers send "Content-type" instead of
		    // "Content-Type."
		    if (!seenContentType &&
			"Content-Type".equalsIgnoreCase(header.getName())) {
			seenContentType = true;
		    }
		}

		final String queryString = uri.getQueryString();
		final String pathString;

		if ((queryString != null) && (queryString.length() > 0)) {  
		    pathString = uri.getPath() + '?' + queryString;
		}
		else {
		    pathString = uri.getPath();
		}

		final byte[] postData = httpTest.getPostData();

		//		final BasicAuthorizationData basicAuthorizationData =
		//		    callData.getAuthorizationData();

		m_threadContext.startTimer();

		final HTTPConnection httpConnection = getConnection(uri);

		/*
		if (basicAuthorizationData != null) {
		    httpConnection.addBasicAuthorization(
			basicAuthorizationData.getRealm(),
			basicAuthorizationData.getUser(),
			basicAuthorizationData.getPassword());
		}
		*/

		if (postData == null) {
		    // We don't pass the query string to the second
		    // parameter of Get() as we don't want it to be URL
		    // encoded.
		    httpResponse = httpConnection.Get(pathString, (String)null,
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

		    httpResponse = httpConnection.Post(pathString, postData,
						       additionalHeaders);
		}

		httpResponse.getData();
		httpResponse.getInputStream().close();
		m_threadContext.stopTimer();
	
		final int statusCode = httpResponse.getStatusCode();

		final String message =
		    uri + "->" + statusCode + " " +
		    httpResponse.getReasonLine();

		switch (statusCode) {
		case HttpURLConnection.HTTP_MOVED_PERM:
		case HttpURLConnection.HTTP_MOVED_TEMP:
		case 307:
		    // It would be possible to perform the check
		    // automatically, but for now just chuck out some
		    // information.
		    m_threadContext.output(
			message + " [Redirect, ensure the next URL is " + 
			httpResponse.getHeader("Location") + "]");
		    break;

		default:
		    m_threadContext.output(message);
		    break;
		}

		// ONLY WORKS FOR TEXT TYPES!
		// SHOULD LEAVE UP TO USER AND LOG BINARY?
		final String text = httpResponse.getText();

		if (m_logHTML && httpResponse.getData() != null) {
		    final String description = test.getDescription();

		    final String filename =
			m_threadContext.createFilename(
			    "page", "_" + m_currentIteration + "_" +
			    m_threeFiguresFormat.format(test.getNumber()) +
			    (description != null ? "_" + description : ""));

		    try {
			final BufferedWriter htmlFile =
			    new BufferedWriter(
				new FileWriter(filename, false));

			htmlFile.write(text);
			htmlFile.close();
		    }
		    catch (IOException e) {
			throw new PluginException("Error writing to " +
						  filename +
						  ": " + e, e);
		    }
		}
	    }
	    catch (Exception e) {
		throw new PluginException(
		    "Failed whilst making HTTP request to " +
		    httpTest.getUrl(), e);
	    }
	    finally {
		// Back stop.
		m_threadContext.stopTimer();
	    }

	    return new HTTPPluginTestResult(httpResponse);
	}

	public void endRun() throws PluginException
	{
	    m_currentIteration++;
	}
    }
    /*
    interface BasicAuthorizationData
    {
	public String getRealm();
	public String getUser();
	public String getPassword();
    }
    */
}
