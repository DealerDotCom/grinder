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

import java.io.InputStream;
import java.io.PrintWriter;
import java.io.Writer;

import HTTPClient.HTTPResponse;
import org.apache.xpath.XPathAPI;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.tidy.Tidy;

import net.grinder.plugininterface.PluginException;
import net.grinder.plugin.http.HTTPClientResponseListener;


/**
 * Example String Bean that uses JTidy and XPath to parse the response
 * and stores the last page title.
 * 
 * @author Philip Aston
 * @version $Revision$
 */
public class ExampleJTidyStringBean implements HTTPClientResponseListener
{
    private Tidy m_tidy = new Tidy();
    private String m_lastPageTitle;
    
    public ExampleJTidyStringBean()
    {
	// Shut up JTidy.
	m_tidy.setErrout(new PrintWriter(new Writer() {
		public void write(char[] buff, int offset, int length) {
		}
		public void close() {
		}
		public void flush() {
		}
	    }));
    }

    public String getLastPageTitle()
    {
	return m_lastPageTitle;
    }

    public void handleResponse(HTTPResponse httpResponse)
	throws PluginException
    {
	try {
	    final Document document =
		m_tidy.parseDOM(httpResponse.getInputStream(), null);

	    final Node titleText =
		XPathAPI.selectSingleNode(document, "//title[1]/text()");

	    m_lastPageTitle =
		titleText != null ? titleText.getNodeValue() : "";
	}
	catch (Exception e) {
	    throw new PluginException("Got a " + e.getMessage() +
				      " whilst parsing body", e);
	}   
    }
}
