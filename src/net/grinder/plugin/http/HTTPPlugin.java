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
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
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

import net.grinder.common.GrinderException;
import net.grinder.common.GrinderProperties;
import net.grinder.common.Test;
import net.grinder.plugininterface.GrinderPlugin;
import net.grinder.plugininterface.PluginException;
import net.grinder.plugininterface.PluginProcessContext;
import net.grinder.plugininterface.PluginThreadCallbacks;
import net.grinder.plugininterface.PluginThreadContext;
import net.grinder.script.TestResult;


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
    private final Map m_callData = new HashMap();

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

    // Will go once CallData stuff is moved to HTTPTest.
    final void registerTest(HTTPTest test)
	throws GrinderException
    {
	synchronized(m_callData) {
	    m_callData.put(test, new CallData(test));
	}
    }

    public PluginThreadCallbacks createThreadCallbackHandler()
	throws PluginException
    {
	return new HTTPPluginThreadCallbacks();
    }
    
    /**
     * Inner class that holds the configuration data for a call.
     */
    protected class CallData
    {
	private final HTTPTest m_test;
	private byte[] m_postData;
	private final Map m_headers;
	private final BasicAuthorizationData m_authorizationData;

	public CallData(HTTPTest test) throws PluginException
	{
	    m_test = test;
	    
	    final GrinderProperties testParameters = m_test.getParameters();

	    m_headers = testParameters.getPropertySubset("header.");

	    final String postFilename =
		testParameters.getProperty("post", null);

	    if (postFilename != null) {
		try {
		    final FileInputStream in =
			new FileInputStream(postFilename);
		    final ByteArrayOutputStream byteArrayStream =
			new ByteArrayOutputStream();
		    
		    final byte[] buffer = new byte[4096];
		    int bytesRead = 0;

		    while ((bytesRead = in.read(buffer)) > 0) {
			byteArrayStream.write(buffer, 0, bytesRead);
		    }

		    in.close();
		    byteArrayStream.close();
		    m_postData = byteArrayStream.toByteArray();
		}
		catch (IOException e) {
		     m_processContext.logError(
			"Could not read post data from " + postFilename);

		    e.printStackTrace(System.err);
		}
	    }

	    final String basicAuthenticationRealmString =
		testParameters.getProperty("basicAuthenticationRealm", null);

	    final String basicAuthenticationUserString =
		testParameters.getProperty("basicAuthenticationUser", null);

	    final String basicAuthenticationPasswordString =
		testParameters.getProperty("basicAuthenticationPassword",
					   null);

	    if (basicAuthenticationUserString != null &&
		basicAuthenticationPasswordString != null &&
		basicAuthenticationRealmString != null) {
		m_authorizationData =
		    new BasicAuthorizationData() {
			public String getRealm() {
			    return basicAuthenticationRealmString; }
			public String getUser() {
			    return basicAuthenticationUserString; }
			public String getPassword() {
			    return basicAuthenticationPasswordString; }
		    };
	    }
	    else if (basicAuthenticationUserString == null &&
		     basicAuthenticationPasswordString == null &&
		     basicAuthenticationRealmString == null) {
		m_authorizationData = null;
	    }
	    else {
		throw new PluginException("If you specify one of { basicAuthenticationUser, basicAuthenticationPassword, basicAuthenticationRealm } you must specify all three.");
	    }
	}

	public Test getTest()
	{
	    return m_test;
	}

	public class ThreadData
	{
	    private final StringBuffer m_buffer = new StringBuffer();
	    private final Map m_headerMap;

	    public ThreadData()
	    {
		m_headerMap = new HashMap(m_headers.size());
	    }

	    public Map getHeaders()
	    {
		final Iterator iterator = m_headers.entrySet().iterator();
		
		while (iterator.hasNext()) {
		    final Map.Entry entry = (Map.Entry)iterator.next();
		    final String key = (String)entry.getKey();
		    final String value = (String)entry.getValue();

		    m_headerMap.put(key, value);
		}

		return m_headerMap;
	    }

	    public BasicAuthorizationData getAuthorizationData()
	    {
		return m_authorizationData;
	    }
	    
	    public byte[] getPostData()
	    {
		return m_postData;
	    }

	    public String getURLString() throws PluginException
	    {
		return m_test.getUrl();
	    }
	}
    }

    protected class HTTPPluginThreadCallbacks implements PluginThreadCallbacks
    {
	private Map m_threadData;

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
	    
	    synchronized(m_callData) {
		m_threadData = new HashMap(m_callData.size());

		final Iterator callDataIterator =
		    m_callData.values().iterator();

		while (callDataIterator.hasNext()) {
		    initialiseThreadData((CallData)callDataIterator.next());
		}
	    }
	}

	private final CallData.ThreadData
	    initialiseThreadData(CallData callData)
	{
	    final CallData.ThreadData threadData = callData.new ThreadData();

	    m_threadData.put(callData.getTest(), threadData);

	    return threadData;
	}

	private final CallData.ThreadData getThreadData(Test test)
	{
	    // No need to synchronise, we're only invoked by one
	    // thread.
	    final CallData.ThreadData threadData =
		(CallData.ThreadData)m_threadData.get(test);

	    if (threadData != null) {
		return threadData;
	    }
	    else {
		synchronized(m_callData) {
		    final CallData callData = (CallData)m_callData.get(test);

		    // TODO: handle not registered case.
		    return initialiseThreadData(callData);
		}
	    }
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

	public TestResult invokeTest(Test test) throws PluginException
	{
	    final CallData.ThreadData threadData = getThreadData(test);

	    HTTPResponse httpResponse = null;

	    try {
		final URI uri = new URI(threadData.getURLString());
		final byte[] postData = threadData.getPostData();

		m_threadContext.startTimer();

		final HTTPConnection httpConnection = getConnection(uri);

		final BasicAuthorizationData basicAuthorizationData =
		    threadData.getAuthorizationData();

		if (basicAuthorizationData != null) {
		    httpConnection.addBasicAuthorization(
			basicAuthorizationData.getRealm(),
			basicAuthorizationData.getUser(),
			basicAuthorizationData.getPassword());
		}

		final Map headers = threadData.getHeaders();

		// HTTPClient ignores null header values.
		final NVPair[] additionalHeaders =
		    new NVPair[5 + headers.size()];
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
		    if (!seenContentType &&
			"Content-Type".equalsIgnoreCase(key)) {
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
		    m_threadContext.logMessage(
			message + " [Redirect, ensure the next URL is " + 
			httpResponse.getHeader("Location") + "]");
		    break;

		default:
		    m_threadContext.logMessage(message);
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
		    threadData.getURLString(), e);
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

    interface BasicAuthorizationData
    {
	public String getRealm();
	public String getUser();
	public String getPassword();
    }
}
