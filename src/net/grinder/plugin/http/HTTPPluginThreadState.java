// Copyright (C) 2002 Philip Aston
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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import HTTPClient.CookieModule;
import HTTPClient.HTTPConnection;
import HTTPClient.ParseException;
import HTTPClient.ProtocolNotSuppException;
import HTTPClient.URI;

import net.grinder.plugininterface.PluginException;
import net.grinder.plugininterface.PluginThreadContext;
import net.grinder.plugininterface.PluginThreadListener;


/**
 * HTTP plugin thread state
 * 
 * @author Philip Aston
 * @version $Revision$
 **/
public class HTTPPluginThreadState implements PluginThreadListener
{
    private final PluginThreadContext m_threadContext;
    private Map m_httpConnectionWrappers = new HashMap();

    HTTPPluginThreadState(PluginThreadContext threadContext)
	throws PluginException
    {
	m_threadContext = threadContext;
    }

    public PluginThreadContext getThreadContext()
    {
	return m_threadContext;
    }

    public HTTPConnectionWrapper getConnectionWrapper(URI uri)
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
	    HTTPPluginConnectionDefaults.getDefaults(keyURI);

	final HTTPConnection httpConnection = new HTTPConnection(uri);
	httpConnection.setContext(this);

	final HTTPConnectionWrapper newConnectionWrapper =
	    new HTTPConnectionWrapper(httpConnection, connectionDefaults);

	m_httpConnectionWrappers.put(keyURI, newConnectionWrapper);

	return newConnectionWrapper;
    }

    public void beginRun() throws PluginException
    {
	// Discard our cookies.
	CookieModule.discardAllCookies(this);

	// SHOULD ALSO REMOVE OLD AUTHORIZATIONS.

	// Close connections from previous run.
	final Iterator i = m_httpConnectionWrappers.values().iterator();
	
	while (i.hasNext()) {
	    ((HTTPConnectionWrapper)i.next()).getConnection().stop();
	}
	    
	m_httpConnectionWrappers.clear();
    }

    public void endRun() throws PluginException
    {
    }
}

