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

import HTTPClient.HTTPConnection;

/**
 * @author Philip Aston
 * @version $Revision$
 **/
public final class HTTPConnectionWrapper implements HTTPPluginConnection
{
    private final HTTPConnection m_httpConnection;
    private boolean m_followRedirects;

    public HTTPConnectionWrapper(HTTPConnection httpConnection) 
    {
	m_httpConnection = httpConnection;
    }

    public final boolean getFollowRedirects() 
    {
	return m_followRedirects;

    }

    public final void setFollowRedirects(boolean followRedirects) 
    {
	m_followRedirects = followRedirects;

	final Class redirectionModule = HTTPPlugin.getRedirectionModule();

	if (m_followRedirects) {
	    m_httpConnection.addModule(redirectionModule, 0);
	}
	else {
	    m_httpConnection.removeModule(redirectionModule);
	}
    }

    public final void addBasicAuthorization(String realm, String user,
					    String password)
    {
	m_httpConnection.addBasicAuthorization(realm, user, password);
    }

    public final void addDigestAuthorization(String realm, String user,
					     String password)
    {
	m_httpConnection.addDigestAuthorization(realm, user, password);
    }
}

