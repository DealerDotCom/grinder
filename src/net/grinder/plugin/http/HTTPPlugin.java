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
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.grinder.common.GrinderException;
import net.grinder.common.GrinderProperties;
import net.grinder.common.Test;
import net.grinder.plugininterface.GrinderPlugin;
import net.grinder.plugininterface.PluginProcessContext;
import net.grinder.plugininterface.PluginThreadContext;
import net.grinder.plugininterface.PluginException;
import net.grinder.plugininterface.ThreadCallbacks;
import net.grinder.statistics.ExpressionView;
import net.grinder.statistics.StatisticsIndexMap;
import net.grinder.statistics.StatisticsView;


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
    private List m_callData;
    private boolean m_followRedirects;
    private boolean m_logHTML;
    private boolean m_useCookies;
    private boolean m_useCookiesVersionString;
    private boolean m_useHTTPClient;
    private boolean m_disablePersistentConnections;
    private Class m_stringBeanClass;
    private Map m_beanMethodMap = null;
    private StatisticsIndexMap.LongIndex m_timeToFirstByteIndex;

    public void initialize(PluginProcessContext processContext,
			   Set testsFromPropertiesFile)
	throws PluginException
    {
	m_testsFromPropertiesFile = testsFromPropertiesFile;
	m_callData = new ArrayList(m_testsFromPropertiesFile.size());

	final Iterator testIterator = m_testsFromPropertiesFile.iterator();

	while (testIterator.hasNext()) {
	    m_callData.add(new CallData(processContext,
					(Test)testIterator.next()));
	}

	final GrinderProperties parameters =
	    processContext.getPluginParameters();

	m_followRedirects = parameters.getBoolean("followRedirects", false);
	m_logHTML = parameters.getBoolean("logHTML", false);
	m_useCookies = parameters.getBoolean("useCookies", true);
	// tily@sylo.org / 2000/02/16 mod to set version string on or off - breaks jrun otherwise
	m_useCookiesVersionString =
	    parameters.getBoolean("useCookiesVersionString", true);
	m_useHTTPClient = parameters.getBoolean("useHTTPClient", true);
	m_disablePersistentConnections =
	    parameters.getBoolean("disablePersistentConnections", false);

	if (!m_useHTTPClient && processContext.getRecordTime()) {
	    try {
		m_timeToFirstByteIndex =
		    StatisticsIndexMap.getInstance().getIndexForLong(
			"userLong0");

		final StatisticsView summaryView = new StatisticsView();

		summaryView.add(
		    new ExpressionView("Mean time to first byte",
				       "statistic.timeToFirstByte",
				       "(/ userLong0 timedTransactions)"));

		processContext.registerSummaryStatisticsView(summaryView);

		final StatisticsView detailView = new StatisticsView();

		detailView.add(new ExpressionView("Time to first byte", "",
						  "userLong0"));

		processContext.registerDetailStatisticsView(detailView);
	    }
	    catch (GrinderException e) {
		throw new PluginException(
		    "Failed to register 'time to first byte' statistic", e);
	    }
	}
	
	final String stringBeanClassName =
	    parameters.getProperty("stringBean", null);

	if (stringBeanClassName != null) {
	    try {
		m_stringBeanClass = Class.forName(stringBeanClassName);

		m_beanMethodMap = new HashMap();

		final Method[] methods = m_stringBeanClass.getMethods();
	    
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
		    stringBeanClassName + "' was not found.", e);
	    }
	}	
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

	public CallData(PluginProcessContext processContext, Test test)
	    throws PluginException
	{
	    m_test = test;
	    
	    final GrinderProperties testParameters = m_test.getParameters();

	    try {
		m_urlString = testParameters.getMandatoryProperty("url");
	    }
	    catch (GrinderException e) {
		throw new PluginException("URL for Test " +
					  m_test.getNumber() +
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
		    
		    final char[] buffer = new char[4096];
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

	public class ThreadData implements HTTPHandler.RequestData
	{
	    private final StringBuffer m_buffer = new StringBuffer();
	    private final Object m_bean;
	    private final Map m_headerMap;

	    public ThreadData(Object stringBean)
	    {
		m_bean = stringBean;
		m_headerMap = new HashMap(m_headers.size());
	    }

	    public Map getHeaders() throws HTTPHandlerException
	    {
		final Iterator iterator = m_headers.entrySet().iterator();
		
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
		if (m_authorizationData != null &&
		    m_authorizationData instanceof
		    HTTPHandler.BasicAuthorizationData) {

		    final HTTPHandler.BasicAuthorizationData basic =
			(HTTPHandler.BasicAuthorizationData)
			m_authorizationData;

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
		return replaceKeys(m_postString);
	    }

	    public String getURLString() throws HTTPHandlerException
	    {
		return replaceKeys(m_urlString);
	    }

	    public String getOKString() throws HTTPHandlerException
	    {
		return replaceKeys(m_okString);
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

		    // Look for <methodName> and replace with
		    // m_bean.methodName(). If we don't find an exact
		    // match, just chuck out the literal text -
		    // otherwise string bean tags are a pain in XML
		    // POST data.
		    OUTER:
		    while (true) {
			if ((p = original.indexOf('<', lastP)) == -1) {
			    // No more <'s, we're done.
			    m_buffer.append(original.substring(lastP));
			    break OUTER;
			}
			else {
			    // We've found a <, loop while there are
			    // more <'s before a <.
			    while (true) {
				m_buffer.append(original.substring(lastP, p));

				lastP = p;
		    
				p = original.indexOf('>', lastP + 1);
			    
				if (p == -1) {
				    // No more >'s, no point looking
				    // for any more matches.
				    m_buffer.append(
					original.substring(lastP));
				    break OUTER;
				}

				final int q = original.indexOf('<', lastP + 1);

				if (q > 0 && q < p) {
				    // Found an earlier <.
				    p = q;

				}
				else {
				    // Found <[^<>]*>.
				    break;
				}
			    }

			    final String methodName =
				original.substring(lastP + 1, p);

			    lastP = p + 1;

			    final Method method =
				(Method)m_beanMethodMap.get(methodName);

			    if (method == null) {
				m_buffer.append('<');
				m_buffer.append(methodName);
				m_buffer.append('>');
				continue OUTER;
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
			}
		    }

		    return m_buffer.toString();
		}
	    }
	}
    }

    protected class HTTPPluginThreadCallbacks implements ThreadCallbacks
    {
	private final Map m_threadData = new HashMap(m_callData.size());

	private PluginThreadContext m_pluginThreadContext = null;
	private HTTPHandler m_httpHandler = null;
	private int m_currentIteration = 0; // How many times we've done all the URL's
	private StringBean m_stringBean = null;
	private final DecimalFormat m_threeFiguresFormat =
	    new DecimalFormat("000");

	/**
	 * This method is executed when the thread starts. It is only
	 * executed once per thread.
	 */
	public void initialize(PluginThreadContext pluginThreadContext)
	    throws PluginException
	{
	    m_pluginThreadContext = pluginThreadContext;

	    final Object bean;
	    final HTTPClientResponseListener httpClientResponseListener;

	    if (m_stringBeanClass != null) {
		try {
		    m_pluginThreadContext.logMessage(
			"Instantiating instance of " +
			m_stringBeanClass.getName());

		    bean = m_stringBeanClass.newInstance();

		    if (StringBean.class.isAssignableFrom(m_stringBeanClass)) {
			m_stringBean = (StringBean)bean;
			m_stringBean.initialize(m_pluginThreadContext);

			m_pluginThreadContext.logMessage(
			    m_stringBeanClass.getName() +
			    " implements " +
			    StringBean.class.getName() +
			    ", will forward callbacks to bean");
		    }

		    if (HTTPClientResponseListener.class.isAssignableFrom(
			    m_stringBeanClass)) {
			httpClientResponseListener =
			    (HTTPClientResponseListener)bean;

			if (m_useHTTPClient) {
			    m_pluginThreadContext.logMessage(
				m_stringBeanClass.getName() +
				" implements " +
				HTTPClientResponseListener.class.getName() +
				", will forward responses to bean");
			}
			else {
			    m_pluginThreadContext.logMessage(
				"WARNING: " +
				m_stringBeanClass.getName() +
				" implements " +
				HTTPClientResponseListener.class.getName() +
				", but HTTPClient is not being used");
			}
		    }
		    else {
			httpClientResponseListener = null;
		    }
		}
		catch (Exception e){
		    throw new PluginException (
			"An instance of the string bean class '" +
			m_stringBeanClass.getName() +
			"' could not be created.", e);
		}
	    }
	    else {
		bean = null;
		httpClientResponseListener = null;
	    }
	    
	    if (m_useHTTPClient) {
		m_httpHandler =
		    new HTTPClientHandler(pluginThreadContext,
					  m_useCookies,
					  m_followRedirects,
					  m_disablePersistentConnections,
					  httpClientResponseListener);
	    }
	    else {
		m_httpHandler = new HttpMsg(pluginThreadContext, m_useCookies,
					    m_useCookiesVersionString,
					    m_followRedirects,
					    m_timeToFirstByteIndex);
	    }

	    final Iterator callDataIterator = m_callData.iterator();

	    while (callDataIterator.hasNext()) {
		final CallData callData = (CallData)callDataIterator.next();
		m_threadData.put(callData.getTest(),
				 callData.new ThreadData(bean));
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

	    final CallData.ThreadData threadData =
		(CallData.ThreadData)m_threadData.get(test);

	    // Do the call.
	    final String page = m_httpHandler.sendRequest(threadData);

	    final boolean error;
	    final String okString = threadData.getOKString();

	    if (page == null) {
		error = okString != null;
	    }
	    else {
		error = okString != null && page.indexOf(okString) == -1;
		
		if (m_logHTML || error) {
		    final String description = test.getDescription();

		    final String filename =
			m_pluginThreadContext.getFilenameFactory().
			createFilename(
			    "page",
			    "_" + m_currentIteration + "_" +
			    m_threeFiguresFormat.format(test.getNumber()) +
			    (description != null ? "_" + description : ""));

		    try {
			final BufferedWriter htmlFile =
			    new BufferedWriter(
				new FileWriter(filename, false));

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
    }
}
