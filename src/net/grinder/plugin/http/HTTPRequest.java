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
import java.net.HttpURLConnection;

import HTTPClient.HTTPConnection;
import HTTPClient.HTTPResponse;
import HTTPClient.ModuleException;
import HTTPClient.NVPair;
import HTTPClient.ParseException;
import HTTPClient.ProtocolNotSuppException;
import HTTPClient.URI;

import net.grinder.common.GrinderException;
import net.grinder.common.Logger;
import net.grinder.plugininterface.PluginException;
import net.grinder.plugininterface.PluginProcessContext;
import net.grinder.engine.process.PluginRegistry;

/**
 * Represents an individual HTTP test.
 *
 * @author Philip Aston
 * @version $Revision$
 */ 
public class HTTPRequest
{
    private static final PluginProcessContext s_processContext;

    static
    {
	try {
	    s_processContext =
		PluginRegistry.getInstance().register(HTTPPlugin.class);
	}
	catch (GrinderException e) {
	    throw new RuntimeException("Failed to register HTTPPlugin: " +
				       e.getMessage());
	}
    }

    private URI m_defaultURL;
    private NVPair[] m_defaultHeaders = new NVPair[0];
    private byte[] m_defaultData;
    private NVPair[] m_defaultFormData;

    public HTTPRequest()
    {
    }

    /**
     * Gets the value of m_defaultURL.
     *
     * @return the value of m_defaultURL.
     */
    public final String getUrl() throws PluginException
    {
	return m_defaultURL.toString();
    }

    public final void setUrl(String url) throws PluginException
    {
	if (!isAbsolute(url)) {
	    throw new PluginException("URL must be absolute");
	}

	try {
	    m_defaultURL = new URI(url);
	}
	catch (ParseException e) {
	    throw new PluginException("Bad URI", e);
	}
    }

    /**
     * Gets the value of m_defaultHeaders
     *
     * @return the value of m_defaultHeaders
     */
    public final NVPair[] getHeaders() 
    {
	return m_defaultHeaders;
    }

    /**
     * Sets the value of m_defaultHeaders
     *
     * @param headers Value to assign to m_defaultHeaders
     */
    public final void setHeaders(NVPair[] headers)
    {
	m_defaultHeaders = headers;
    }

    /**
     * Gets the value of m_defaultData
     *
     * @return the value of m_defaultData
     */
    public final byte[] getData() 
    {
	return m_defaultData;
    }

    /**
     * Sets the value of m_defaultData
     *
     * @param data Value to assign to m_defaultData
     */
    public final void setData(byte[] data)
    {
	m_defaultData = data;
    }

    /**
     * Gets the value of m_defaultFormData
     *
     * @return the value of m_defaultFormData
     */
    public final NVPair[] getFormData() 
    {
	return m_defaultFormData;
    }

    /**
     * Sets the value of m_defaultFormData
     *
     * @param formData Value to assign to m_defaultFormData
     */
    public final void setFormData(NVPair[] formData)
    {
	m_defaultFormData = formData;
    }

    public final HTTPResponse DELETE() throws Exception
    {
	return DELETE(null, getHeaders());
    }

    public final HTTPResponse DELETE(String uri) throws Exception
    {
	return DELETE(uri, getHeaders());
    }

    public final HTTPResponse DELETE(final String uri, final NVPair[] headers)
	throws Exception
    {
	final RequestState requestState = new RequestState(uri);

	return requestState.processResponse(
	    requestState.getConnection().Delete(
		requestState.getPath(), headers));
    }

    public final HTTPResponse GET() throws Exception
    {
	return GET(null, getHeaders());
    }

    public final HTTPResponse GET(String uri) throws Exception
    {
	return GET(uri, getHeaders());
    }

    public final HTTPResponse GET(final String uri, final NVPair[] headers)
	throws Exception
    {
	final RequestState requestState = new RequestState(uri);

	return requestState.processResponse(
	    requestState.getConnection().Get(
		requestState.getPath(), (String)null, headers));
    }

    public final HTTPResponse HEAD() throws Exception
    {
	return HEAD(null, getHeaders());
    }

    public final HTTPResponse HEAD(String uri) throws Exception
    {
	return HEAD(uri, getHeaders());
    }

    public final HTTPResponse HEAD(final String uri, final NVPair[] headers)
	throws Exception
    {
	final RequestState requestState = new RequestState(uri);

	return requestState.processResponse(
	    requestState.getConnection().Head(
		requestState.getPath(), (String)null, headers));
    }

    public final HTTPResponse OPTIONS() throws Exception
    {
	return OPTIONS(null, getHeaders(), getData());
    }

    public final HTTPResponse OPTIONS(String uri) throws Exception
    {
	return OPTIONS(uri, getHeaders(), getData());
    }

    public final HTTPResponse OPTIONS(final String uri, final NVPair[] headers)
	throws Exception
    {
	return OPTIONS(uri, headers, getData());
    }

    public final HTTPResponse OPTIONS(final String uri, final byte[] data)
	throws Exception
    {
	return OPTIONS(uri, getHeaders(), data);
    }

    public final HTTPResponse OPTIONS(final String uri,
				      final NVPair[] headers,
				      final byte[] data) throws Exception
    {
	final RequestState requestState = new RequestState(uri);

	return requestState.processResponse(
	    requestState.getConnection().Options(
		requestState.getPath(), headers, data));
    }

    public final HTTPResponse POST() throws Exception
    {
	return POST((String)null);
    }

    public final HTTPResponse POST(String uri) throws Exception
    {
	final byte[] data = getData();

	if (data != null) {
	    return POST(uri, data, getHeaders());
	}
	else {
	    return POST(uri, getFormData(), getHeaders());
	}
    }

    public final HTTPResponse POST(NVPair[] formData)
	throws Exception
    {
	return POST(null, formData, getHeaders());
    }

    public final HTTPResponse POST(String uri, NVPair[] formData)
	throws Exception
    {
	return POST(uri, formData, getHeaders());
    }

    public final HTTPResponse POST(final String uri,
				   final NVPair[] formData,
				   final NVPair[] headers) throws Exception
    {
	final RequestState requestState = new RequestState(uri);

	return requestState.processResponse(
	    requestState.getConnection().Post(
		requestState.getPath(), formData, headers));
    }

    public final HTTPResponse POST(String uri, byte[] data) throws Exception
    {
	return POST(uri, data, getHeaders());
    }

    public final HTTPResponse POST(final String uri,
				   final byte[] data,
				   final NVPair[] headers) throws Exception
    {
	final RequestState requestState = new RequestState(uri);

	return requestState.processResponse(
	    requestState.getConnection().Post(uri, data, headers));
    }

    public final HTTPResponse PUT() throws Exception
    {
	return PUT(null, getData(), getHeaders());
    }

    public final HTTPResponse PUT(String uri) throws Exception
    {
	return PUT(uri, getData(), getHeaders());
    }

    public final HTTPResponse PUT(String uri, byte[] data) throws Exception
    {
	return PUT(uri, data, getHeaders());
    }

    public final HTTPResponse PUT(String uri, NVPair[] headers) 
	throws Exception
    {
	return PUT(uri, getData(), headers);
    }

    public final HTTPResponse PUT(final String uri,
				  final byte[] data,
				  final NVPair[] headers) throws Exception
    {
	final RequestState requestState = new RequestState(uri);

	return requestState.processResponse(
	    requestState.getConnection().Put(
		requestState.getPath(), data, headers));
    }

    public final HTTPResponse TRACE() throws Exception
    {
	return TRACE(null, getHeaders());
    }

    public final HTTPResponse TRACE(String uri) throws Exception
    {
	return TRACE(uri, getHeaders());
    }

    public final HTTPResponse TRACE(final String uri, final NVPair[] headers)
	throws Exception
    {
	final RequestState requestState = new RequestState(uri);

	return requestState.processResponse(
	    requestState.getConnection().Trace(
		requestState.getPath(), headers));
    }

    private final class RequestState
    {
	private final HTTPPluginThreadState m_threadState;
	private final HTTPConnectionWrapper m_connectionWrapper;
	private final String m_path;

	public RequestState(String uri)
	    throws GrinderException, ParseException, ProtocolNotSuppException
	{
	    m_threadState =
		(HTTPPluginThreadState)
		s_processContext.getPluginThreadListener();

	    final URI url;
	    
	    if (uri == null) {
		if (m_defaultURL == null) {
		    throw new PluginException("URL not specified");
		}

		url = m_defaultURL;
	    }
	    else if (isAbsolute(uri)) {
		url = new URI(uri);
	    }
	    else {
		if (m_defaultURL == null) {
		    throw new PluginException("URL must be absolute");
		}

		url = new URI(m_defaultURL, uri);
	    }

	    m_connectionWrapper = m_threadState.getConnectionWrapper(url);
	    m_path = url.getPathAndQuery(); // And for fragment, paramaters?
	}

	public final HTTPConnection getConnection()
	{
	    return m_connectionWrapper.getConnection();
	}

	public String getPath()
	{
	    return m_path;
	}

	public final HTTPResponse processResponse(HTTPResponse httpResponse)
	    throws IOException, ModuleException
	{
	    httpResponse.getData();
	    httpResponse.getInputStream().close();
	
	    final int statusCode = httpResponse.getStatusCode();

	    final String message =
		httpResponse.getOriginalURI() + " -> " + statusCode + " " +
		httpResponse.getReasonLine();

	    final Logger logger = m_threadState.getThreadContext();
	    
	    switch (statusCode) {
	    case HttpURLConnection.HTTP_MOVED_PERM:
	    case HttpURLConnection.HTTP_MOVED_TEMP:
	    case 307:
		// It would be possible to perform the check
		// automatically, but for now just chuck out some
		// information.
		logger.output(message +
			      " [Redirect, ensure the next URL is " + 
			      httpResponse.getHeader("Location") + "]");
		break;

	    default:
		logger.output(message);
		break;
	    }

	    return httpResponse;
	}
    }

    private static final boolean isAbsolute(String uri)
    {
	char ch = '\0';
	int  pos = 0;
	int len = uri.length();

	while (pos < len &&
	       (ch = uri.charAt(pos)) != ':' && 
	       ch != '/' &&
	       ch != '?' &&
	       ch != '#') {
	    pos++;
	}
	    
	return (ch == ':');
    }
}
