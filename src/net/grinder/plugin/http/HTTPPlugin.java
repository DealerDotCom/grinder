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

import java.net.HttpURLConnection; // For the (incomplete!) status code definitions only.
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import HTTPClient.CookieModule;
import HTTPClient.HTTPConnection;
import HTTPClient.HTTPResponse;
import HTTPClient.ParseException;
import HTTPClient.ProtocolNotSuppException;
import HTTPClient.URI;

import net.grinder.common.GrinderException;
import net.grinder.common.GrinderProperties;
import net.grinder.common.Test;
import net.grinder.script.ScriptPluginContext;
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
    private PluginProcessContext m_processContext;
    private final HTTPPluginScriptContext m_scriptPluginContext =
	new HTTPPluginScriptContext();

    public void initialize(PluginProcessContext processContext)
	throws PluginException
    {
	m_processContext = processContext;
    }

    public PluginThreadCallbacks createThreadCallbackHandler(
	PluginThreadContext threadContext)
	throws PluginException
    {
	return new HTTPPluginThreadCallbacks(threadContext);
    }

    class HTTPPluginThreadCallbacks implements PluginThreadCallbacks
    {
	private final PluginThreadContext m_threadContext;

	private final DecimalFormat m_threeFiguresFormat =
	    new DecimalFormat("000");

	private Map m_httpConnectionWrappers = new HashMap();

	private HTTPConnectionWrapper getConnectionWrapper(URI uri)
	    throws ParseException, ProtocolNotSuppException
	{
	    final URI keyURI =
		new URI(uri.getScheme(), uri.getHost(), uri.getPort(), "");

	    final HTTPConnectionWrapper existingConnectionWrapper =
		(HTTPConnectionWrapper)m_httpConnectionWrappers.get(keyURI);

	    if (existingConnectionWrapper != null) {
		return existingConnectionWrapper;
	    }

	    final HTTPPluginConnectionDefaults connectionDefaults =
		m_scriptPluginContext.getHTTPPluginConnectionDefaults(keyURI);

	    final HTTPConnection httpConnection = new HTTPConnection(uri);
	    httpConnection.setContext(HTTPPluginThreadCallbacks.this);

	    final HTTPConnectionWrapper newConnectionWrapper =
		new HTTPConnectionWrapper(httpConnection, connectionDefaults);

	    m_httpConnectionWrappers.put(keyURI, newConnectionWrapper);

	    return newConnectionWrapper;
	}

	public HTTPPluginThreadCallbacks(PluginThreadContext threadContext)
	    throws PluginException
	{
	    m_threadContext = threadContext;
	}

	public void beginRun() throws PluginException
	{
	    // Discard our cookies.
	    CookieModule.discardAllCookies(HTTPPluginThreadCallbacks.this);

	    // SHOULD ALSO REMOVE OLD AUTHORIZATIONS.

	    // Close connections from previous run.
	    final Iterator i = m_httpConnectionWrappers.values().iterator();
	
	    while (i.hasNext()) {
		((HTTPConnectionWrapper)i.next()).getConnection().stop();
	    }
	    
	    m_httpConnectionWrappers.clear();
	}

	public Object makeRequest(HTTPRequest.DelayedInvocation
				  delayedInvocation)
	    throws PluginException
	{
	    HTTPResponse httpResponse = null;

	    try {
		m_threadContext.startTimer();

		final HTTPConnection httpConnection =
		    getConnectionWrapper(delayedInvocation.getURI()).
		    getConnection();

		httpResponse = delayedInvocation.request(httpConnection);

		httpResponse.getData();
		httpResponse.getInputStream().close();
		m_threadContext.stopTimer();
	
		final int statusCode = httpResponse.getStatusCode();

		final String message =
		    httpResponse.getOriginalURI() + " -> " + statusCode + " " +
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
	    }
	    catch (Exception e) {
		throw new PluginException(
		    "Failed whilst making HTTP request" , e);
	    }
	    finally {
		// Back stop.
		m_threadContext.stopTimer();
	    }

	    return httpResponse;
	}

	public void endRun() throws PluginException
	{
	}
    }

    public final ScriptPluginContext getScriptPluginContext()
    {
	return m_scriptPluginContext;
    }

    public final class HTTPPluginScriptContext implements ScriptPluginContext
    {
	private final HTTPPluginConnectionDefaults
	    m_defaultConnectionDefaults = new HTTPPluginConnectionDefaults();

	private final Map m_connectionDefaults = new HashMap();

	final HTTPPluginConnectionDefaults
	    getHTTPPluginConnectionDefaults(URI keyURI)
	{
	    synchronized (m_connectionDefaults) {
		final HTTPPluginConnectionDefaults connectionSpecificDefaults =
		    (HTTPPluginConnectionDefaults)
		    m_connectionDefaults.get(keyURI);

		if (connectionSpecificDefaults != null) {
		    return connectionSpecificDefaults;
		}

		return m_defaultConnectionDefaults;
	    }
	}

	public final HTTPPluginConnection getDefaultConnectionDefaults() 
	{
	    return m_defaultConnectionDefaults;
	}

	public final HTTPPluginConnection getConnectionDefaults(
	    String uriString)
	    throws ParseException, ProtocolNotSuppException
	{
	    final URI uri = new URI(uriString);
	    
	    final URI keyURI =
		new URI(uri.getScheme(), uri.getHost(), uri.getPort(), "");

	    synchronized (m_connectionDefaults) {
		final HTTPPluginConnection existingConnectionDefaults =
		    (HTTPPluginConnection)m_connectionDefaults.get(keyURI);

		if (existingConnectionDefaults != null) {
		    return existingConnectionDefaults;
		}

		final HTTPPluginConnection newConnectionDefaults =
		    new HTTPPluginConnectionDefaults();

		m_connectionDefaults.put(keyURI, newConnectionDefaults);

		return newConnectionDefaults;
	    }
	}

	public final HTTPPluginConnection getConnection(String uriString)
	    throws GrinderException, ParseException, ProtocolNotSuppException
	{
	    final HTTPPluginThreadCallbacks threadCallbacks =
		(HTTPPluginThreadCallbacks)
		m_processContext.getPluginThreadCallbacks();
	    
	    return threadCallbacks.getConnectionWrapper(new URI(uriString));
	}
    }
}
