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
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.text.DateFormat;

import org.apache.oro.text.regex.MalformedPatternException;
import org.apache.oro.text.regex.MatchResult;
import org.apache.oro.text.regex.Pattern;
import org.apache.oro.text.regex.PatternCompiler;
import org.apache.oro.text.regex.PatternMatcher;
import org.apache.oro.text.regex.Perl5Compiler;
import org.apache.oro.text.regex.Perl5Matcher;

import HTTPClient.Codecs;

import net.grinder.TCPSniffer;
import net.grinder.tools.tcpsniffer.ConnectionDetails;
import net.grinder.tools.tcpsniffer.EchoFilter;
import net.grinder.tools.tcpsniffer.SnifferFilter;


/**
 * {@link SnifferFilter} that outputs session in a form that can be
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
public class HttpPluginSnifferFilter implements SnifferFilter
{
    private final static String FILENAME_PREFIX = "http-plugin-sniffer-post-";
    private final static String s_newLine =
	System.getProperty("line.separator");

    private static final String[] s_mirroredHeaders = {
	"Content-Type",
	"Content-type",		// For the broken browsers of this world.
	"If-Modified-Since",
    };

    private PrintWriter m_out = new PrintWriter(System.out);

    private final Pattern m_basicAuthorizationHeaderPattern;
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
    private long m_lastTime;

    /**
     * Constructor.
     *
     * @exception MalformedPatternException 
     */
    public HttpPluginSnifferFilter() throws MalformedPatternException
    {
	m_currentRequestNumber =
	    Integer.getInteger(TCPSniffer.INITIAL_TEST_PROPERTY, 0).intValue()
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
		Perl5Compiler.CASE_INSENSITIVE_MASK // Sigh.
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

	markTime();
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

	m_out.println("");
	m_out.println("#");
	m_out.println("# The Grinder version @version@");
	m_out.println("#");
	m_out.println("# Script generated by the TCPSniffer at " + 
		      DateFormat.getDateTimeInstance().format(
			  Calendar.getInstance().getTime()));
	m_out.println("#");
	m_out.println("");
	m_out.println("grinder.processes=1");
	m_out.println("grinder.threads=1");
	m_out.println("# Change to grinder.cycles=0 to continue until console sends stop.");
	m_out.println("grinder.cycles=1");
	m_out.println("");
	m_out.println("grinder.receiveConsoleSignals=true");
	m_out.println("grinder.reportToConsole=true");
	m_out.println("");
	m_out.println("grinder.plugin=net.grinder.plugin.http.HttpPlugin");
    }

    /**
     * The main handler method called by the sniffer engine.
     *
     * <p>NOTE, this is called for message fragments, don't assume
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
	final long result = currentTime - m_lastTime;
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

	private final StringBuffer m_outputBuffer = new StringBuffer();

	// Parse state.
	private int m_requestNumber;
	private boolean m_parsingHeaders = false;
	private boolean m_handlingPost = false;
	private final StringBuffer m_entityBodyBuffer = new StringBuffer();
	private int m_contentLength = -1;

	private final Perl5Matcher m_matcher = new Perl5Matcher();

	public Handler(ConnectionDetails connectionDetails)
	{
	    m_connectionDetails = connectionDetails;


	    m_out.println(s_newLine +
			  "# New connection: " +
			  connectionDetails.getDescription());
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

		endMessage();
		m_parsingHeaders = true;

		final MatchResult matchResult = m_matcher.getMatch();
		final String method = matchResult.group(1);

		if (method.equals("GET")) {
		    m_handlingPost = false;
		}
		else if (method.equals("POST")) {
		    m_handlingPost = true;
		    m_entityBodyBuffer.setLength(0);
		    m_contentLength = -1;
		}
		else {
		    warn("Ignoring '" + method + "' from " +
			 m_connectionDetails.getDescription());
		    return;
		}

		String url = matchResult.group(2);

		if (!url.startsWith("http")) {
		    // Relative URL given, calculate absolute URL.
		    url = m_connectionDetails.getURLBase("http") + url;
		}

		// Stuff we do at start of request only.
		m_outputBuffer.append(s_newLine);
		m_requestNumber = incrementRequestNumber();
		outputProperty("parameter.url", url);
		outputProperty("sleepTime", Long.toString(markTime()));

		// Base default description on test URL.
		final String description;

		if (m_matcher.contains(url, m_lastURLPathElementPattern)) {
		    description = m_matcher.getMatch().group(1);
		}
		else {
		    description = "";
		}

		outputProperty("description", description);
	    }

	    // Stuff we do whatever.
	    if (m_parsingHeaders) {
		for (int i=0; i<s_mirroredHeaders.length; i++) {
		    if (m_matcher.contains(asciiString,
					   m_mirroredHeaderPatterns[i])) {
			outputProperty(
			    "parameter.header." + s_mirroredHeaders[i],
			    m_matcher.getMatch().group(1).trim());
		    }
		}

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
			    HttpPluginSnifferResponseFilter.
			    getLastAuthenticationRealm());
		    }
		}

		if (m_handlingPost) {
		    // Look for the content length in the header.
		    if (m_matcher.contains(asciiString,
					   m_contentLengthPattern)) {
			m_contentLength =
			    Integer.parseInt(
				m_matcher.getMatch().group(1).trim());
		    }

		    if (m_matcher.contains(asciiString,
					   m_messageBodyPattern)) {

			m_parsingHeaders = false;
			addToEntityBody(m_matcher.getMatch().group(1));
		    }
		}
	    }
	    else {
		if (m_handlingPost) {
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
	    m_entityBodyBuffer.append(request);
	
	    // We flush our entity data output now if we've reached or
	    // exceeded the specified Content-Length. If no
	    // contentLength was specified we rely on next message or
	    // connection close event to flush the data.
	    if (m_contentLength != -1 ) {
		final int bytesRead = m_entityBodyBuffer.length();

		if (bytesRead == m_contentLength) {
		    endMessage();
		}
		else if (bytesRead > m_contentLength) {
		    warn("Expected content length exceeded");
		    endMessage();
		}
	    }
	}

	private void outputProperty(String name, String value)
	{
	    m_outputBuffer.append(
		"grinder.test" + m_requestNumber + "." + name + "=" + value +
		s_newLine);
	}

	private void warn(String message)
	{
	    m_outputBuffer.append(
		"# WARNING request " + m_requestNumber + ": " + message +
		s_newLine);
	}

	public synchronized final void endMessage() throws IOException
	{
	    if (!m_parsingHeaders && m_handlingPost) {
		final String filename = FILENAME_PREFIX + m_requestNumber;
		final Writer writer =
		    new BufferedWriter(new FileWriter(filename));

		writer.write(m_entityBodyBuffer.toString());
		writer.close();

		outputProperty("parameter.post", filename);
		m_handlingPost = false;
	    }

	    m_out.print(m_outputBuffer.toString());
	    m_outputBuffer.setLength(0);
	}
    }
}
