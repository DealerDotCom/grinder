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

package net.grinder.plugin.http.example;

import HTTPClient.HTTPResponse;

import net.grinder.common.Test;
import net.grinder.plugininterface.PluginException;
import net.grinder.plugininterface.PluginThreadContext;
import net.grinder.plugin.http.HTTPClientResponseListener;
import net.grinder.plugin.http.StringBean;


/**
 * Example String Bean that implements StringBean and
 * HTTPClientResponseListener.
 * 
 * @author Philip Aston
 * @version $Revision$
 */
public class ExampleStringBean3
    implements StringBean, HTTPClientResponseListener
{
    private PluginThreadContext m_pluginThreadContext;
    private int m_count = 0;

    public void initialize(PluginThreadContext pluginThreadContext)
	throws PluginException
    {
	m_pluginThreadContext = pluginThreadContext;
	m_pluginThreadContext.logMessage("StringBean: initialize");
    }

    public void beginCycle()
    {
	m_pluginThreadContext.logMessage("StringBean: beginCycle");
	m_count = 0;
    }

    public boolean doTest(Test test)
    {
	m_pluginThreadContext.logMessage("StringBean: doTest");
	return false;
    }

    public void endCycle()
    {
	m_pluginThreadContext.logMessage("StringBean: endCycle");
    }
    
    public String getCount()
    {
	return Integer.toString(m_count++);
    }

    public String getTime()
    {
	return Long.toString(System.currentTimeMillis());
    }

    public String getCycle()
    {
	return Integer.toString(m_pluginThreadContext.getCurrentCycleID());
    }

    public void handleResponse(HTTPResponse httpResponse)
	throws PluginException
    {
	try {
	    m_pluginThreadContext.logMessage("Saw a " +
					     httpResponse.getStatusCode() +
					     " response from " +
					     httpResponse.getHeader("Server"));
	    m_pluginThreadContext.logMessage(httpResponse.toString());
	}
	catch (Exception e) {
	    throw new PluginException("HTTPClient threw " + e.getMessage(), e);
	}
    }
}
