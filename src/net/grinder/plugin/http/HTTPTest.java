// Copyright (C) 2001, 2002 Philip Aston
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

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;

import HTTPClient.NVPair;

import net.grinder.common.GrinderException;
import net.grinder.plugininterface.PluginException;
import net.grinder.plugininterface.PluginTest;


/**
 * Represents an individual HTTP test.
 *
 * @author Philip Aston
 * @version $Revision$
 */ 
public class HTTPTest extends PluginTest
{
    private String m_url;
    private NVPair[] m_headers;
    private byte[] m_postData;
    
    public HTTPTest(int number, String description)
	throws GrinderException
    {
	super(HTTPPlugin.class, number, description);
    }

    public final String getUrl() throws PluginException
    {
	if (m_url == null) {
	    throw new PluginException("URL has not been specified");
	}

	return m_url;
    }

    public final void setUrl(String url) 
    {
	m_url = url;
    }

    public final NVPair[] getHeaders()
    {
	return m_headers;
    }

    public final void setHeaders(NVPair[] headers) 
    {
	m_headers = headers;
    }

    public final byte[] getPostData() 
    {
	return m_postData;
    }

    public final void setPostData(byte[] postData) 
    {
	m_postData = postData;
    }

    public final void setPostFile(String filename) throws PluginException
    {
	try {
	    final FileInputStream in = new FileInputStream(filename);
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
	    throw new PluginException(
		"Could not read post data from " + filename);
	}
    }
}
