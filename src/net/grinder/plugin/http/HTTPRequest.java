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

    private URI m_uri;
    private NVPair[] m_defaultHeaders = new NVPair[0];
    private byte[] m_defaultData;
    private NVPair[] m_defaultFormData;

    public HTTPRequest()
    {
    }

    /**
     * Gets the value of m_uri.
     *
     * @return the value of m_uri.
     */
    public final URI getUri() throws PluginException
    {
	if (m_uri == null) {
	    throw new PluginException(
		"Call setURI() before using an HTTPRequest");
	}
	
	return m_uri;
    }

    public final void setUrl(String url) throws PluginException
    {
	try {
	    m_uri = new URI(url);
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
	return DELETE(getUri().getPath(), getHeaders());
    }

    public final HTTPResponse DELETE(String path) throws Exception
    {
	return DELETE(path, getHeaders());
    }

    public final HTTPResponse DELETE(final String path,
				     final NVPair[] headers) throws Exception
    {
	final RequestState requestState = new RequestState(path);

	return requestState.processResponse(
	    requestState.getConnection().Delete(path, headers));
    }

    public final HTTPResponse GET() throws Exception
    {
	return GET(getUri().getPath(), getUri().getQueryString(),
		   getHeaders());
    }

    public final HTTPResponse GET(String path) throws Exception
    {
	// Path is specified, so don't use default query string.
	return GET(path, null, getHeaders());
    }

    public final HTTPResponse GET(String path, String queryString)
	throws Exception
    {
	return GET(path, queryString, getHeaders());
    }

    public final HTTPResponse GET(String path, NVPair[] headers)
	throws Exception
    {
	// Path is specified, so don't use default query string.
	return GET(path, null, headers);
    }

    public final HTTPResponse GET(final String path,
				  final String queryString,
				  final NVPair[] headers)
	throws Exception
    {
	final RequestState requestState = new RequestState(path);

	return requestState.processResponse(
	    requestState.getConnection().Get(path, queryString, headers));
    }

    public final HTTPResponse HEAD() throws Exception
    {
	return HEAD(getUri().getPath(), getUri().getQueryString(),
		    getHeaders());
    }

    public final HTTPResponse HEAD(String path) throws Exception
    {
	// Path is specified, so don't use default query string.
	return HEAD(path, null, getHeaders());
    }

    public final HTTPResponse HEAD(String path, String queryString)
	throws Exception
    {
	return HEAD(path, queryString, getHeaders());
    }

    public final HTTPResponse HEAD(String path, NVPair[] headers)
	throws Exception
    {
	// Path is specified, so don't use default query string.
	return HEAD(path, null, headers);
    }

    public final HTTPResponse HEAD(final String path,
				   final String queryString,
				   final NVPair[] headers) throws Exception
    {
	final RequestState requestState = new RequestState(path);

	return requestState.processResponse(
	    requestState.getConnection().Head(path, queryString, headers));
    }

    public final HTTPResponse OPTIONS() throws Exception
    {
	return OPTIONS(getUri().getPath(), getHeaders(), getData());
    }

    public final HTTPResponse OPTIONS(String path) throws Exception
    {
	return OPTIONS(path, getHeaders(), getData());
    }

    public final HTTPResponse OPTIONS(final String path,
				      final NVPair[] headers) throws Exception
    {
	return OPTIONS(path, headers, getData());
    }

    public final HTTPResponse OPTIONS(final String path,
				      final byte[] data) throws Exception
    {
	return OPTIONS(path, getHeaders(), data);
    }

    public final HTTPResponse OPTIONS(final String path,
				      final NVPair[] headers,
				      final byte[] data) throws Exception
    {
	final RequestState requestState = new RequestState(path);

	return requestState.processResponse(
	    requestState.getConnection().Options(path, headers, data));
    }

    public final HTTPResponse POST() throws Exception
    {
	return POST(getUri().getPath());
    }

    public final HTTPResponse POST(String path) throws Exception
    {
	final byte[] data = getData();

	if (data != null) {
	    return POST(path, data, getHeaders());
	}
	else {
	    return POST(path, getFormData(), getHeaders());
	}
    }

    public final HTTPResponse POST(String path, NVPair[] formData)
	throws Exception
    {
	return POST(path, formData, getHeaders());
    }

    public final HTTPResponse POST(final String path,
				   final NVPair[] formData,
				   final NVPair[] headers) throws Exception
    {
	final RequestState requestState = new RequestState(path);

	return requestState.processResponse(
	    requestState.getConnection().Post(path, formData, headers));
    }

    public final HTTPResponse POST(String path,
				   byte[] data) throws Exception
    {
	return POST(path, data, getHeaders());
    }

    public final HTTPResponse POST(final String path,
				   final byte[] data,
				   final NVPair[] headers) throws Exception
    {
	final RequestState requestState = new RequestState(path);

	return requestState.processResponse(
	    requestState.getConnection().Post(path, data, headers));
    }

    public final HTTPResponse PUT() throws Exception
    {
	return PUT(getUri().getPath(), getData(), getHeaders());
    }

    public final HTTPResponse PUT(String path) throws Exception
    {
	return PUT(path, getData(), getHeaders());
    }

    public final HTTPResponse PUT(String path, byte[] data) throws Exception
    {
	return PUT(path, data, getHeaders());
    }

    public final HTTPResponse PUT(String path, NVPair[] headers)
	throws Exception
    {
	return PUT(path, getData(), headers);
    }

    public final HTTPResponse PUT(final String path,
				  final byte[] data,
				  final NVPair[] headers) throws Exception
    {
	final RequestState requestState = new RequestState(path);

	return requestState.processResponse(
	    requestState.getConnection().Put(path, data, headers));
    }

    public final HTTPResponse TRACE() throws Exception
    {
	return TRACE(getUri().getPath(), getHeaders());
    }

    public final HTTPResponse TRACE(String path) throws Exception
    {
	return TRACE(path, getHeaders());
    }

    public final HTTPResponse TRACE(final String path, final NVPair[] headers)
	throws Exception
    {
	final RequestState requestState = new RequestState(path);

	return requestState.processResponse(
	    requestState.getConnection().Trace(path, headers));
    }

    private final class RequestState
    {
	private final HTTPPluginThreadState m_threadState;
	private final HTTPConnectionWrapper m_connectionWrapper;
	private final String m_path;

	public RequestState(String path)
	    throws GrinderException, ParseException, ProtocolNotSuppException
	{
	    m_threadState =
		(HTTPPluginThreadState)
		s_processContext.getPluginThreadListener();

	    if (isAbsolute(path)) {
		m_connectionWrapper =
		    m_threadState.getConnectionWrapper(new URI(path));

		m_path = m_uri.getPathAndQuery();
	    }
	    else {
		m_connectionWrapper =
		    m_threadState.getConnectionWrapper(m_uri);

		m_path = path;
	    }
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
