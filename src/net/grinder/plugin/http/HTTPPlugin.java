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

import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import net.grinder.plugininterface.GrinderPlugin;
import net.grinder.plugininterface.PluginProcessContext;
import net.grinder.plugininterface.PluginThreadContext;
import net.grinder.plugininterface.PluginException;
import net.grinder.plugininterface.Test;
import net.grinder.plugininterface.ThreadCallbacks;
import net.grinder.util.FilenameFactory;
import net.grinder.util.GrinderException;
import net.grinder.util.GrinderProperties;


/**
 * Simple HTTP client benchmark.
 * 
 * @author Paco Gomez
 * @author Philip Aston
 * @version $Revision$
 */
public class HttpPlugin implements GrinderPlugin
{
    private final static Object[] s_noArgs = new Object[0];

    private Set m_testsFromPropertiesFile;
    private CallData[] m_callData;
    private boolean m_followRedirects;
    private boolean m_logHTML;
    private boolean m_timeIncludesTransaction;
    private boolean m_useCookies;
    private boolean m_useCookiesVersionString;
    private boolean m_useHTTPClient;
    private String m_stringBeanClassName;

    public void initialize(PluginProcessContext processContext,
			   Set testsFromPropertiesFile)
	throws PluginException
    {
	m_testsFromPropertiesFile = testsFromPropertiesFile;
	m_callData = new CallData[m_testsFromPropertiesFile.size()];

	final Iterator i = m_testsFromPropertiesFile.iterator();

	while (i.hasNext()) {
	    final Test test = (Test)i.next();

	    m_callData[test.getIndex()] = new CallData(processContext, test);
	}

	final GrinderProperties parameters =
	    processContext.getPluginParameters();

	m_followRedirects = parameters.getBoolean("followRedirects", false);
	m_logHTML = parameters.getBoolean("logHTML", false);
	m_timeIncludesTransaction =
	    parameters.getBoolean("timeIncludesTransaction", true);
	m_useCookies = parameters.getBoolean("useCookies", true);
	// tily@sylo.org / 2000/02/16 mod to set version string on or off - breaks jrun otherwise
	m_useCookiesVersionString =
	    parameters.getBoolean("useCookiesVersionString", true);
	m_useHTTPClient = parameters.getBoolean("useHTTPClient", true);

	m_stringBeanClassName = parameters.getProperty("stringBean", null);
    }

    public Set getTests() throws PluginException
    {
	return m_testsFromPropertiesFile;
    }

    public ThreadCallbacks createThreadCallbackHandler()
	throws PluginException
    {
	return new HTTPPluginThreadCallbacks();
    }
    
    /**
     * Inner class that holds the configuration data for a call.
     */
    protected class CallData
    {
	private final Test m_test;
	private final String m_urlString;
	private final String m_okString;
	private String m_postString;
	private final Map m_headers;
	private final HTTPHandler.AuthorizationData m_authorizationData;

	public CallData(PluginProcessContext processContext,
			Test test) throws PluginException
	{
	    m_test = test;
	    
	    final GrinderProperties testParameters = m_test.getParameters();

	    try {
		m_urlString = testParameters.getMandatoryProperty("url");
	    }
	    catch (GrinderException e) {
		throw new PluginException("URL for Test " + m_test.getName() +
					  " not specified", e);
	    }

	    m_okString = testParameters.getProperty("ok", null);
	    m_headers = testParameters.getPropertySubset("header.");

	    final String postFilename =
		testParameters.getProperty("post", null);

	    if (postFilename != null) {
		try {
		    final FileReader in = new FileReader(postFilename);
		    final StringWriter writer = new StringWriter(512);
		    
		    char[] buffer = new char[4096];
		    int charsRead = 0;

		    while ((charsRead = in.read(buffer, 0, buffer.length)) > 0)
		    {
			writer.write(buffer, 0, charsRead);
		    }

		    in.close();
		    writer.close();
		    m_postString = writer.toString();
		}
		catch (IOException e) {
		    processContext.logError(
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
		    new HTTPHandler.BasicAuthorizationData() {
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

	public Map getHeaders() { return m_headers; }

	public HTTPHandler.AuthorizationData getAuthorizationData()
	    throws HTTPHandlerException
	{
	    return m_authorizationData;
	}

	public String getPostString() { return m_postString; }
	public String getURLString() { return m_urlString; }

	public String getOKString() { return m_okString; }
    }

    protected class HTTPPluginThreadCallbacks implements ThreadCallbacks
    {
	private final ThreadCallData[] m_threadCallData =
	    new ThreadCallData[m_callData.length];

	private PluginThreadContext m_pluginThreadContext = null;
	private FilenameFactory m_filenameFactory = null;
	private HTTPHandler m_httpHandler = null;
	private int m_currentIteration = 0; // How many times we've done all the URL's
	private Object m_bean = null;
	private StringBean m_stringBean = null;
	private Map m_beanMethodMap = null;

	/**
	 * This method is executed when the thread starts. It is only
	 * executed once per thread.
	 */
	public void initialize(PluginThreadContext pluginThreadContext)
	    throws PluginException
	{
	    m_pluginThreadContext = pluginThreadContext;

	    for (int i=0; i<m_callData.length; i++) {
		m_threadCallData[i] =
		    new ThreadCallData(m_callData[i]);
	    }

	    m_filenameFactory = pluginThreadContext.getFilenameFactory();

	    if (m_useHTTPClient) {
		m_httpHandler = new HTTPClientHandler(pluginThreadContext,
						      m_useCookies,
						      m_followRedirects);
	    }
	    else {
		m_httpHandler = new HttpMsg(pluginThreadContext, m_useCookies,
					    m_useCookiesVersionString,
					    m_followRedirects,
					    m_timeIncludesTransaction);
	    }

	    if (m_stringBeanClassName != null) {
		try {
		    m_pluginThreadContext.logMessage("Instantiating " +
						     m_stringBeanClassName);

		    final Class stringBeanClass =
			Class.forName(m_stringBeanClassName);

		    m_bean = stringBeanClass.newInstance();

		    if (StringBean.class.isAssignableFrom(stringBeanClass)) {
			m_stringBean = (StringBean)m_bean;
			m_stringBean.initialize(m_pluginThreadContext);
		    }
		    else {
			m_pluginThreadContext.logMessage(
			    m_stringBeanClassName + " does not implement " +
			    StringBean.class.getName() +
			    ", skipping initialisation");
		    }

		    m_beanMethodMap = new HashMap();

		    final Method[] methods = stringBeanClass.getMethods();

		    for (int i=0; i<methods.length; i++) {
			final String name = methods[i].getName();

			if (name.startsWith("get") &&
			    methods[i].getReturnType() == String.class &&
			    methods[i].getParameterTypes().length == 0) {
			    m_beanMethodMap.put(name, methods[i]);
			}
		    }
		}
		catch(ClassNotFoundException e) {
		    throw new PluginException(
			"The specified string bean class '" +
			m_stringBeanClassName + "' was not found.", e);
		}
		catch (Exception e){
		    throw new PluginException (
			"An instance of the string bean class '" +
			m_stringBeanClassName + "' could not be created.", e);
		}
	    }
	}

	public void beginCycle() throws PluginException
	{
	    if (m_stringBean != null) {
		m_stringBean.beginCycle();
	    }

	    // Reset cookie if necessary.
	    m_httpHandler.reset();      
	}

	/**
	 * This method processes the URLs.
	 */    
	public boolean doTest(Test test) throws PluginException
	{
	    if (m_stringBean != null) {
		m_stringBean.doTest(test);
	    }

	    final ThreadCallData threadCallData =
		m_threadCallData[test.getIndex()];

	    // Do the call.
	    final String page = m_httpHandler.sendRequest(threadCallData);

	    final boolean error;
	    final String okString = threadCallData.getOKString();

	    if (page == null) {
		error = okString != null;
	    }
	    else {
		error = okString != null && page.indexOf(okString) == -1;
		
		if (m_logHTML || error) {
		    final String filename =
			m_filenameFactory.createFilename("page",
							 "_" +
							 m_currentIteration +
							 "_" +
							 test.getName() +
							 ".html");
		    try {
			final BufferedWriter htmlFile =
			    new BufferedWriter(new FileWriter(filename,
							      false));

			htmlFile.write(page);
			htmlFile.close();
		    }
		    catch (IOException e) {
			throw new PluginException("Error writing to " +
						  filename +
						  ": " + e, e);
		    }

		    if (error) {
			m_pluginThreadContext.logError(
			    "The 'ok' string ('" + okString +
			    "') was not found in the page received. " +
			    "The output has been written to '" + filename +
			    "'");
		    }
		}
	    }

	    return !error;
	}

	public void endCycle() throws PluginException
	{
	    if (m_stringBean != null) {
		m_stringBean.endCycle();
	    }

	    m_currentIteration++;
	}

	public class ThreadCallData implements HTTPHandler.RequestData
	{
	    private final CallData m_callData;
	    private final StringBuffer m_buffer = new StringBuffer();
	    private final Map m_headerMap;

	    public ThreadCallData(CallData callData)
	    {
		m_callData = callData;
		m_headerMap = new HashMap(m_callData.getHeaders().size());
	    }

	    public Map getHeaders() throws HTTPHandlerException
	    {
		final Iterator iterator =
		    m_callData.getHeaders().entrySet().iterator();
		
		while (iterator.hasNext()) {
		    final Map.Entry entry = (Map.Entry)iterator.next();
		    final String key = (String)entry.getKey();
		    final String value = (String)entry.getValue();

		    m_headerMap.put(key, replaceKeys(value));
		}

		return m_headerMap;
	    }

	    public HTTPHandler.AuthorizationData getAuthorizationData()
		throws HTTPHandlerException
	    {
		final HTTPHandler.AuthorizationData original =
		    m_callData.getAuthorizationData();

		if (original != null &&
		    original instanceof HTTPHandler.BasicAuthorizationData) {

		    final HTTPHandler.BasicAuthorizationData basic =
			(HTTPHandler.BasicAuthorizationData)original;

		    final String realm = replaceKeys(basic.getRealm());
		    final String user = replaceKeys(basic.getUser());
		    final String password = replaceKeys(basic.getPassword());

		    return new HTTPHandler.BasicAuthorizationData() {
			    public String getRealm() { return realm; }
			    public String getUser() { return user; }
			    public String getPassword() { return password; }
			};
		}

		return null;
	    }
	    
	    public String getPostString() throws HTTPHandlerException
	    {
		return replaceKeys(m_callData.getPostString());
	    }

	    public String getURLString() throws HTTPHandlerException
	    {
		return replaceKeys(m_callData.getURLString());
	    }

	    public String getOKString() throws HTTPHandlerException
	    {
		return replaceKeys(m_callData.getOKString());
	    }

	    private String replaceKeys(String original) 
		throws HTTPHandlerException
	    {
		if (original == null ||
		    m_bean == null ||
		    original.length() == 0) {
		    return original;
		}
		else {
		    // We belong to a single thread so we can safely
		    // reuse our StringBuffer.
		    m_buffer.setLength(0);
		    
		    int p = 0;
		    int lastP = p;

		    while (true) {
			if ((p = original.indexOf('<', lastP)) == -1) {
			    m_buffer.append(original.substring(lastP));
			    break;
			}
			else {
			    m_buffer.append(original.substring(lastP, p));

			    lastP = p + 1;
		    
			    p = original.indexOf('>', lastP);
			    
			    if (p == -1) {
				throw new HTTPHandlerException(
				    "URL for Test " +
				    m_callData.getTest().getName() +
				    " malformed");    
			    }

			    final String methodName =
				original.substring(lastP, p);

			    final Method method =
				(Method)m_beanMethodMap.get(methodName);

			    if (method == null ) {
				throw new HTTPHandlerException(
				    "URL for Test " +
				    m_callData.getTest().getName() +
				    " refers to unknown string bean method '" +
				    methodName + "'");
			    }

			    try {
				m_buffer.append(
				    (String)method.invoke(m_bean, s_noArgs));
			    }
			    catch (Exception e) {
				throw new HTTPHandlerException(
				    "Failure invoking string bean method '" +
				    methodName + "'", e);
			    }

			    lastP = p + 1;
			}
		    }

		    return m_buffer.toString();
		}
	    }
	}
    }
}
