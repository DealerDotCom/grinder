// Copyright (C) 2000 Paco Gomez
// Copyright (C) 2000, 2001, 2002 Philip Aston
// Copyright (C) 2000 Phil Dawes
// Copyright (C) 2001 Kalle Burbeck
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
import java.io.ByteArrayOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.text.DateFormat;

import org.apache.oro.text.regex.MalformedPatternException;
import org.apache.oro.text.regex.MatchResult;
import org.apache.oro.text.regex.Pattern;
import org.apache.oro.text.regex.PatternCompiler;
import org.apache.oro.text.regex.Perl5Compiler;
import org.apache.oro.text.regex.Perl5Matcher;

import HTTPClient.Codecs;
import HTTPClient.NVPair;

import net.grinder.TCPProxy;
import net.grinder.tools.tcpproxy.ConnectionDetails;
import net.grinder.tools.tcpproxy.TCPProxyFilter;


/**
 * {@link TCPProxyFilter} that outputs session in a form that can be
 * reused by the HTTP plugin.
 *
 * <p>Bugs:
 * <ul>
 * <li>Assumes Request-Line (GET ...) is first line of packet, and that
 * every packet that starts with such a line is the start of a request.
 * <li>Should filter chunked transfer coding from POST data.
 * <li>Doesn't handle line continuations.
 * <li>Doesn't parse correctly if lines are broken accross message
 * fragments.
 * </ul>
 *
 * @author Philip Aston
 * @version $Revision$
 */
public class HTTPPluginTCPProxyFilter implements TCPProxyFilter
{
    private static final String FILENAME_PREFIX = "http-plugin-tcpproxy-post-";
    private static final String s_newLine =
	System.getProperty("line.separator");
    private static final String s_indent = "    ";

    /**
     * A list of headers which we record and replay. HTTPConnection
     * sets up many things for us (Host, Connection, User-Agent,
     * Cookie, Content-Length and so on).
     */
    private static final String[] s_mirroredHeaders = {
	"Accept",
	"Accept-Charset",
	"Accept-Encoding",
	"Accept-Language",
	"Cache-Control",
	"If-Modified-Since",
	"Referer",
    };

    private PrintWriter m_out = new PrintWriter(System.out);
    private final PrintWriter m_testFileWriter;

    private final Pattern m_basicAuthorizationHeaderPattern;
    private final Pattern m_contentTypePattern;
    private final Pattern m_contentLengthPattern;
    private final Pattern m_messageBodyPattern;
    private final Pattern m_mirroredHeaderPatterns[] =
	new Pattern[s_mirroredHeaders.length];
    private final Pattern m_lastURLPathElementPattern;
    private final Pattern m_requestLinePattern;

    /**
     * Map of {@link ConnectionDetails} to handlers.
     */
    private final Map m_handlers = new HashMap();

    private int m_currentRequestNumber;
    private long m_lastTime = 0;

    /**
     * Constructor.
     *
     * @exception MalformedPatternException 
     */
    public HTTPPluginTCPProxyFilter()
	throws IOException, MalformedPatternException
    {
	m_currentRequestNumber =
	    Integer.getInteger(TCPProxy.INITIAL_TEST_PROPERTY, 0).intValue()
	    - 1;

	final PatternCompiler compiler = new Perl5Compiler();

	m_messageBodyPattern =
	    compiler.compile(
		"\\r\\n\\r\\n(.*)",
		Perl5Compiler.READ_ONLY_MASK | Perl5Compiler.SINGLELINE_MASK);

	// From RFC 2616:
	//
	// Request-Line = Method SP Request-URI SP HTTP-Version CRLF
	// HTTP-Version = "HTTP" "/" 1*DIGIT "." 1*DIGIT
	// http_URL = "http:" "//" host [ ":" port ] [ abs_path [ "?" query ]]
	//  
	// We're flexible about SP and CRLF, see RFC 2616, 19.3.

	m_requestLinePattern =
	    compiler.compile(
		"^([A-Z]+)[ \\t]+(.+)[ \\t]+HTTP/\\d.\\d[ \\t]*\\r?$", 
		Perl5Compiler.READ_ONLY_MASK | Perl5Compiler.MULTILINE_MASK);

	m_contentLengthPattern =
	    compiler.compile(
		getHeaderExpression("Content-Length"),
		Perl5Compiler.READ_ONLY_MASK |
		Perl5Compiler.MULTILINE_MASK |
		Perl5Compiler.CASE_INSENSITIVE_MASK // Sigh...
		);

	m_contentTypePattern =
	    compiler.compile(
		getHeaderExpression("Content-Type"),
		Perl5Compiler.READ_ONLY_MASK |
		Perl5Compiler.MULTILINE_MASK |
		Perl5Compiler.CASE_INSENSITIVE_MASK // and sigh again.
		);

	for (int i=0; i<s_mirroredHeaders.length; i++) {
	    m_mirroredHeaderPatterns[i] =
		compiler.compile(
		    getHeaderExpression(s_mirroredHeaders[i]),
		    Perl5Compiler.READ_ONLY_MASK |
		    Perl5Compiler.MULTILINE_MASK);
	}

	m_basicAuthorizationHeaderPattern =
	    compiler.compile(
		"^Authorization:[ \\t]*Basic[  \\t]*([a-zA-Z0-9+/]*=*).*\\r?$",
		Perl5Compiler.READ_ONLY_MASK | Perl5Compiler.MULTILINE_MASK);

	// Ignore maximum amount of stuff thats not a '?' followed by
	// a '/', then grab the next until the first '?'.
	m_lastURLPathElementPattern =
	    compiler.compile(
		"^[^\\?]*/([^\\?]*)",
		Perl5Compiler.READ_ONLY_MASK | Perl5Compiler.SINGLELINE_MASK);

	m_testFileWriter =
	    new PrintWriter(
		new BufferedWriter(
		    new FileWriter("httptests.py")), false);
    }

    /**
     * Set the {@link PrintWriter} that the filter should use for
     * output.
     *
     * @param out The PrintWriter.
     */
    public void setOutputPrintWriter(PrintWriter outputPrintWriter) 
    {
	m_out.flush();
	m_out = outputPrintWriter;

	m_out.println("#");
	m_out.println("# The Grinder version @version@");
	m_out.println("#");
	m_out.println("# Script recorded by the TCPProxy at " + 
		      DateFormat.getDateTimeInstance().format(
			  Calendar.getInstance().getTime()));
	m_out.println("#");
	m_out.println();
	m_out.println("from httptests import *");
	m_out.println();
	m_out.println("class TestRunner:");
	m_out.println(s_indent + "def __call__(self):");

	m_testFileWriter.println("#");
	m_testFileWriter.println("# The Grinder version @version@");
	m_testFileWriter.println("#");
	m_testFileWriter.println("# HTTP tests recorded by the TCPProxy at " + 
				 DateFormat.getDateTimeInstance().format(
				     Calendar.getInstance().getTime()));
	m_testFileWriter.println("#");
	m_testFileWriter.println();
	m_testFileWriter.println("from HTTPClient import NVPair");
	m_testFileWriter.println(
	    "from net.grinder.plugin.http import HTTPRequest");
	m_testFileWriter.println("from net.grinder.script import Test");
	m_testFileWriter.println();
	m_testFileWriter.println("tests = {}");
    }

    /**
     * The main handler method called by the sniffer engine.
     *
     * <p>NOTE, this is called for message fragments; don't assume
     * that its passed a complete HTTP message at a time.</p>
     *
     * @param connectionDetails The TCP connection.
     * @param buffer The message fragment buffer.
     * @param bytesRead The number of bytes of buffer to process.
     * @return Filters can optionally return a <code>byte[]</code>
     * which will be transmitted to the server instead of
     * <code>buffer</code.
     * @exception IOException if an error occurs
     */
    public byte[] handle(ConnectionDetails connectionDetails, byte[] buffer,
			 int bytesRead)
	throws IOException
    {
	getHandler(connectionDetails).handle(buffer, bytesRead);
	return null;
    }

    /**
     * A connection has been opened.
     *
     * @param connectionDetails a <code>ConnectionDetails</code> value
     */
    public void connectionOpened(ConnectionDetails connectionDetails)
    {
	getHandler(connectionDetails);
    }

    /**
     * A connection has been closed.
     *
     * @param connectionDetails a <code>ConnectionDetails</code> value
     * @exception IOException if an error occurs
     */
    public void connectionClosed(ConnectionDetails connectionDetails)
	throws IOException
    {
	removeHandler(connectionDetails).endMessage();
    }

    private Handler getHandler(ConnectionDetails connectionDetails)
    {
	synchronized (m_handlers) {
	    final Handler oldHandler =
		(Handler)m_handlers.get(connectionDetails);

	    if (oldHandler != null) {
		return oldHandler;
	    }
	    else {
		final Handler newHandler = new Handler(connectionDetails);
		m_handlers.put(connectionDetails, newHandler);
		return newHandler;
	    }
	}
    }

    private Handler removeHandler(ConnectionDetails connectionDetails)
    {
	final Handler handler;
	
	synchronized (m_handlers) {
	    handler = (Handler)m_handlers.remove(connectionDetails);
	}

	if (handler == null) {
	    throw new IllegalArgumentException(
		"Unknown connection " + connectionDetails);
	}

	return handler;
    }

    private synchronized int getRequestNumber()
    {
	return m_currentRequestNumber;
    }

    private synchronized int incrementRequestNumber() 
    {
	return ++m_currentRequestNumber;
    }

    private synchronized long markTime()
    {
	final long currentTime = System.currentTimeMillis();
	final long result = m_lastTime > 0 ? currentTime - m_lastTime : 0;
	m_lastTime = currentTime;
	return result;
    }

    /**
     * Factory for regular expression patterns that match HTTP headers.
     *
     * @param headerName a <code>String</code> value
     * @return The expression.
     */
    private final String getHeaderExpression(String headerName)
    {
	return "^" + headerName + ":[ \\t]*(.*)\\r?$";
    }

    /**
     * Class that handles a particular connection.
     *
     * <p>Multithreaded calls for a given connection are
     * serialised.</p>
     **/
    private final class Handler
    {
	private final ConnectionDetails m_connectionDetails;

	// Parse state.
	private int m_requestNumber;
	private boolean m_parsingHeaders = false;
	private boolean m_handlingBody = false;

	// Parse data.
	private long m_time;
	private String m_method = null;
	private String m_url = null;
	private final List m_headers = new ArrayList();
	private int m_contentLength = -1;
	private String m_contentType = null;
	private final ByteArrayOutputStream m_entityBodyByteStream =
	    new ByteArrayOutputStream();

	private final Perl5Matcher m_matcher = new Perl5Matcher();

	public Handler(ConnectionDetails connectionDetails)
	{
	    m_connectionDetails = connectionDetails;

	    /*
	    m_out.print(s_newLine + s_indent + s_indent +
			  "# New connection: " +
			connectionDetails.getDescription());
	    */
	}

	public synchronized void handle(byte[] buffer, int bufferBytes)
	    throws IOException
	{
	    // String used to parse headers - header names are
	    // US-ASCII encoded and anchored to start of line.
	    final String asciiString =
		new String(buffer, 0, bufferBytes, "US-ASCII");

	    if (m_matcher.contains(asciiString, m_requestLinePattern)) {
		// Packet is start of new request message.

		final MatchResult matchResult = m_matcher.getMatch();

		// Grab match results because endMessage plays with
		// out Matcher.
		final String newMethod = matchResult.group(1);
		final String newURL = matchResult.group(2);

		endMessage();

		m_method = newMethod;
		m_url = newURL;
		m_parsingHeaders = true;

		if (m_method.equals("DELETE") ||
		    m_method.equals("GET") ||
		    m_method.equals("HEAD") ||
		    m_method.equals("TRACE")) {
		    m_handlingBody = false;
		}
		else if (m_method.equals("OPTIONS") ||
			 m_method.equals("PUT") ||
			 m_method.equals("POST")) {
		    m_handlingBody = true;
		    m_entityBodyByteStream.reset();
		    m_contentLength = -1;
		    m_contentType = null;
		}
		else {
		    warn("Ignoring '" + m_method + "' from " +
			 m_connectionDetails.getDescription());
		    return;
		}

		if (!m_url.startsWith("http")) {
		    // Relative URL given, calculate absolute URL.
		    m_url = m_connectionDetails.getURLBase("http") + m_url;
		}

		// Stuff we do at start of request only.
		m_requestNumber = incrementRequestNumber();
		m_time = markTime();
		m_headers.clear();
	    }

	    // Stuff we do whatever.
	    if (m_parsingHeaders) {
		for (int i=0; i<s_mirroredHeaders.length; i++) {
		    if (m_matcher.contains(asciiString,
					   m_mirroredHeaderPatterns[i])) {

			m_headers.add(
			    new NVPair(s_mirroredHeaders[i],
				       m_matcher.getMatch().group(1).trim()));
		    }
		}
		/*
		if (m_matcher.contains(asciiString,
				       m_basicAuthorizationHeaderPattern)) {

		    final String decoded =
			Codecs.base64Decode(
			    m_matcher.getMatch().group(1).trim());
		    
		    final int colon = decoded.indexOf(":");
		    
		    if (colon < 0) {
			warn("Could not decode Authorization header");
		    }
		    else {
			outputProperty(
			    "parameter.basicAuthenticationUser",
			    decoded.substring(0, colon));
			
			outputProperty(
			    "parameter.basicAuthenticationPassword",
			    decoded.substring(colon+1));

			outputProperty(
			    "parameter.basicAuthenticationRealm",
			    HTTPPluginTCPProxyResponseFilter.
			    getLastAuthenticationRealm());
		    }
		}
		*/

		if (m_handlingBody) {
		    // Look for the content length and type in the header.
		    if (m_matcher.contains(asciiString,
					   m_contentLengthPattern)) {
			m_contentLength =
			    Integer.parseInt(
				m_matcher.getMatch().group(1).trim());
		    }

		    if (m_matcher.contains(asciiString,
					   m_contentTypePattern)) {
			m_contentType = m_matcher.getMatch().group(1).trim();
			m_headers.add(
			    new NVPair("Content-Type", m_contentType));
		    }

		    if (m_matcher.contains(asciiString,
					   m_messageBodyPattern)) {

			m_parsingHeaders = false;
			addToEntityBody(m_matcher.getMatch().group(1));
		    }
		}
	    }
	    else {
		if (m_handlingBody) {
		    addToEntityBody(asciiString);
		}
		else {
		    warn("UNEXPECTED - Not parsing headers or handling POST");
		}
	    }
	}

	private void addToEntityBody(String request)
	    throws IOException
	{
	    m_entityBodyByteStream.write(request.getBytes("US-ASCII"));
	
	    // We flush our entity data output now if we've reached or
	    // exceeded the specified Content-Length. If no
	    // contentLength was specified we rely on next message or
	    // connection close event to flush the data.
	    if (m_contentLength != -1 ) {
		final int bytesRead = m_entityBodyByteStream.size();

		if (bytesRead == m_contentLength) {
		    endMessage();
		}
		else if (bytesRead > m_contentLength) {
		    warn("Expected content length exceeded");
		    endMessage();
		}
	    }
	}

	private void warn(String message)
	{
	    m_out.print(
		"# WARNING request " + m_requestNumber + ": " + message +
		s_newLine);
	}

	public synchronized final void endMessage() throws IOException
	{
	    if (m_method == null) {
		return;
	    }

	    final StringBuffer scriptOutput = new StringBuffer();
	    final StringBuffer testOutput = new StringBuffer();

	    if (m_time > 0) {
		scriptOutput.append(s_newLine);
		scriptOutput.append(s_indent);
		scriptOutput.append(s_indent);
		scriptOutput.append("grinder.sleep(");
		scriptOutput.append(Long.toString(m_time));
		scriptOutput.append(")");
	    }

	    final String dataParameter;

	    if (!m_parsingHeaders && m_handlingBody) {
		m_handlingBody = false;

		if ("application/x-www-form-urlencoded".
		    equals(m_contentType)) {

		    scriptOutput.append(s_newLine);
		    scriptOutput.append(s_indent);
		    scriptOutput.append(s_indent);
		    scriptOutput.append("formData");
		    scriptOutput.append(m_requestNumber);
		    scriptOutput.append(" = [ ");

		    final String bodyAsString =
			m_entityBodyByteStream.toString("US-ASCII");

		    int i = 0;
		    int j = 0;
		    
		    while ((j = bodyAsString.indexOf("=", i)) >= 0) {
			scriptOutput.append(s_newLine);
			scriptOutput.append(s_indent);
			scriptOutput.append(s_indent);
			scriptOutput.append(s_indent);
			scriptOutput.append("NVPair('");
			scriptOutput.append(bodyAsString.substring(i, j));
			scriptOutput.append("', '");

			++j;
			i = bodyAsString.indexOf("&", j);

			if (i == -1) {
			    scriptOutput.append(bodyAsString.substring(j));
			    scriptOutput.append("')");
			    break;
			}

			scriptOutput.append(bodyAsString.substring(j, i));
			scriptOutput.append("'), ");

			++i;
		    }

		    dataParameter = "formData" + m_requestNumber;
		    scriptOutput.append("]");
		}
		else {
		    testOutput.append(s_newLine);
		    testOutput.append("data");
		    testOutput.append(m_requestNumber);
		    testOutput.append(" = [ ");

		    final byte[] bytes = m_entityBodyByteStream.toByteArray();

		    for (int i=0; i<bytes.length; ++i) {
			testOutput.append(Integer.toString(bytes[i]));
			testOutput.append(", ");
		    }

		    dataParameter = "data" + m_requestNumber;
		    testOutput.append("]");
		}
	    }
	    else {
		dataParameter = null;
	    }

	    testOutput.append(s_newLine);
	    testOutput.append("request");
	    testOutput.append(m_requestNumber);
	    testOutput.append(" = HTTPRequest(");

	    if (m_headers.size() > 0) {
		testOutput.append(s_newLine);
		testOutput.append(s_indent);
		testOutput.append("headers = ( ");

		final Iterator iterator = m_headers.iterator();
		boolean first = true;

		while (iterator.hasNext()) {
		    final NVPair entry = (NVPair)iterator.next();
		    
		    if (!first) {
			testOutput.append(s_newLine);
			testOutput.append(s_indent);
			testOutput.append(s_indent);
			testOutput.append(s_indent);
			testOutput.append(s_indent);
		    }
		    else {
			first = false;
		    }
		    
		    testOutput.append("NVPair('");
		    testOutput.append(entry.getName());
		    testOutput.append("', '");
		    testOutput.append(entry.getValue());
		    testOutput.append("'),");
		}

		testOutput.append(")");
	    }

	    testOutput.append(")");

	    // Base default description on test URL.
	    final String description;
	    
	    if (m_matcher.contains(m_url, m_lastURLPathElementPattern)) {
		description = m_matcher.getMatch().group(1);
	    }
	    else {
		description = "";
	    }

	    testOutput.append(s_newLine);
	    testOutput.append("tests[");
	    testOutput.append(m_requestNumber);
	    testOutput.append("] = Test(");
	    testOutput.append(m_requestNumber);
	    testOutput.append(", '");
	    testOutput.append(description);
	    testOutput.append("').wrap(request");
	    testOutput.append(m_requestNumber);
	    testOutput.append(")");

	    testOutput.append(s_newLine);

	    scriptOutput.append(s_newLine);
	    scriptOutput.append(s_indent);
	    scriptOutput.append(s_indent);
	    scriptOutput.append("tests[");
	    scriptOutput.append(Integer.toString(m_requestNumber));
	    scriptOutput.append("].");
	    scriptOutput.append(m_method);
	    scriptOutput.append("('");
	    scriptOutput.append(m_url);
	    scriptOutput.append("'");

	    if (dataParameter != null) {
		scriptOutput.append(", ");
		scriptOutput.append(dataParameter);
	    }

	    scriptOutput.append(")");

	    scriptOutput.append(s_newLine);

	    m_testFileWriter.print(testOutput.toString());
	    m_testFileWriter.flush();

	    m_out.print(scriptOutput.toString());
	    m_out.flush();

	    m_method = null;
	}
    }
}
