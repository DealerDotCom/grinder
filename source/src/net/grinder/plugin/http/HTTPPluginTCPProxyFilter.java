// Copyright (C) 2000, 2001, 2002, 2003, 2004, 2005 Philip Aston
// Copyright (C) 2000 Phil Dawes
// Copyright (C) 2001 Kalle Burbeck
// Copyright (C) 2003 Bill Schnellinger
// Copyright (C) 2003 Bertrand Ave
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
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import HTTPClient.Codecs;
import HTTPClient.NVPair;
import HTTPClient.ParseException;

import net.grinder.common.GrinderBuild;
import net.grinder.common.Logger;
import net.grinder.common.UncheckedInterruptedException;
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
 * <li>Doesn't parse correctly if lines are broken across message
 * fragments.
 * </ul>
 *
 * @author Philip Aston
 * @author Bertrand Ave
 * @version $Revision$
 */
public class HTTPPluginTCPProxyFilter implements TCPProxyFilter {

  private static final String INITIAL_TEST_PROPERTY = "HTTPPlugin.initialTest";

  private static final String s_newLine =
    System.getProperty("line.separator");
  private static final String s_indent = "    ";

  private static long s_lastResponseTime = 0;

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
    "User-Agent",
  };

  private final PrintWriter m_out;
  private final String m_scriptFileName;
  private final PrintWriter m_scriptFileWriter;
  private final String m_testFileName;
  private final PrintWriter m_testFileWriter;

  private final Pattern m_contentTypePattern;
  private final Pattern m_contentLengthPattern;
  private final Pattern m_messageBodyPattern;
  private final Pattern[] m_mirroredHeaderPatterns =
    new Pattern[s_mirroredHeaders.length];
  private final Pattern m_lastURLPathElementPattern;
  private final Pattern m_requestLinePattern;

  /**
   * Map of {@link ConnectionDetails} to handlers.
   */
  private final Map m_handlers = new HashMap();

  private int m_currentRequestNumber;

  private final Map m_previousHeaders =
    Collections.synchronizedMap(new HashMap());

  /**
   * Constructor.
   *
   * @param logger A logger.
   * @exception IOException If an I/O error occurs.
   * @exception PatternSyntaxException If a regular expression
   * could not be compiled.
   */
  public HTTPPluginTCPProxyFilter(Logger logger)
    throws IOException, PatternSyntaxException {

    m_out = logger.getOutputLogWriter();

    m_currentRequestNumber =
      Integer.getInteger(INITIAL_TEST_PROPERTY, 0).intValue() - 1;

    m_messageBodyPattern = Pattern.compile("\\r\\n\\r\\n(.*)",
                                           Pattern.DOTALL);

    // From RFC 2616:
    //
    // Request-Line = Method SP Request-URI SP HTTP-Version CRLF
    // HTTP-Version = "HTTP" "/" 1*DIGIT "." 1*DIGIT
    // http_URL = "http:" "//" host [ ":" port ] [ abs_path [ "?" query ]]
    //
    // We're flexible about SP and CRLF, see RFC 2616, 19.3.

    m_requestLinePattern =
      Pattern.compile(
        "^([A-Z]+)[ \\t]+([^\\?]+)(\\?.*)?[ \\t]+HTTP/\\d.\\d[ \\t]*\\r?\\n",
        Pattern.MULTILINE | Pattern.UNIX_LINES);

    m_contentLengthPattern =
      Pattern.compile(
        getHeaderExpression("Content-Length"),
        Pattern.MULTILINE |
        Pattern.UNIX_LINES |
        Pattern.CASE_INSENSITIVE // Sigh...
        );

    m_contentTypePattern =
      Pattern.compile(
        getHeaderExpression("Content-Type"),
        Pattern.MULTILINE |
        Pattern.UNIX_LINES |
        Pattern.CASE_INSENSITIVE // and sigh again.
        );

    for (int i = 0; i < s_mirroredHeaders.length; i++) {
      m_mirroredHeaderPatterns[i] =
        Pattern.compile(
          getHeaderExpression(s_mirroredHeaders[i]),
          Pattern.MULTILINE | Pattern.UNIX_LINES);
    }

    // Ignore maximum amount of stuff thats not a '?' followed by
    // a '/', then grab the next until the first '?'.
    m_lastURLPathElementPattern =
      Pattern.compile("^[^\\?]*/([^\\?]*)");

    // Should generate unique file names?
    m_scriptFileName = "httpscript.py";

    m_scriptFileWriter =
      new PrintWriter(
        new BufferedWriter(new FileWriter(m_scriptFileName)), false);

    final String testFileNamePrefix = "httpscript_tests";
    m_testFileName = testFileNamePrefix + ".py";

    m_testFileWriter =
      new PrintWriter(
        new BufferedWriter(new FileWriter(m_testFileName)), false);

    final String version = GrinderBuild.getVersionString();

    m_scriptFileWriter.println("#");
    m_scriptFileWriter.println("# The Grinder version " + version);
    m_scriptFileWriter.println("#");
    m_scriptFileWriter.println("# Script recorded by the TCPProxy at " +
                               DateFormat.getDateTimeInstance().format(
                                 Calendar.getInstance().getTime()));
    m_scriptFileWriter.println("#");
    m_scriptFileWriter.println();
    m_scriptFileWriter.println("from " + testFileNamePrefix + " import *");
    m_scriptFileWriter.println(
      "from net.grinder.script.Grinder import grinder");
    m_scriptFileWriter.println();
    m_scriptFileWriter.println("class TestRunner:");
    m_scriptFileWriter.println(s_indent + "def __call__(self):");

    m_testFileWriter.println("#");
    m_testFileWriter.println("# The Grinder version " + version);
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

    m_out.println("Script will be generated to the files '" +
                  m_scriptFileName + "' and '" + m_testFileName + "'");
    m_out.flush();
  }

  /**
   * Called when any response activity is detected. Because the test
   * script is a single thread of control we need to calculate the
   * sleep deltas using the last time any activity occurred on any
   * connection.
   */
  public static final synchronized void markLastResponseTime() {
    s_lastResponseTime = System.currentTimeMillis();
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
   * @return Filters can optionally return a <code>byte[]</code> which
   * will be transmitted to the server instead of <code>buffer</code>.
   * @exception FilterException if an error occurs
   */
  public byte[] handle(ConnectionDetails connectionDetails, byte[] buffer,
                       int bytesRead) throws FilterException {
    try {
      getHandler(connectionDetails).handle(buffer, bytesRead);
    }
    catch (IOException e) {
      UncheckedInterruptedException.ioException(e);
      throw new FilterException("handle() failed", e);
    }

    return null;
  }

  /**
   * A connection has been opened.
   *
   * @param connectionDetails a <code>ConnectionDetails</code> value
   */
  public void connectionOpened(ConnectionDetails connectionDetails) {
    getHandler(connectionDetails);
  }

  /**
   * A connection has been closed.
   *
   * @param connectionDetails a <code>ConnectionDetails</code> value
   * @exception FilterException if an error occurs
   */
  public void connectionClosed(ConnectionDetails connectionDetails)
    throws FilterException {
    try {
      removeHandler(connectionDetails).endMessage();
    }
    catch (IOException e) {
      UncheckedInterruptedException.ioException(e);
      throw new FilterException("endMessage() failed", e);
    }
  }

  private Handler getHandler(ConnectionDetails connectionDetails) {
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

  private Handler removeHandler(ConnectionDetails connectionDetails) {
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

  private synchronized int incrementRequestNumber() {
    return ++m_currentRequestNumber;
  }

  /**
   * Factory for regular expression patterns that match HTTP headers.
   *
   * @param headerName a <code>String</code> value
   * @return The expression.
   */
  private String getHeaderExpression(String headerName) {
    return "^" + headerName + ":[ \\t]*(.*)\\r?\\n";
  }

  /**
   * Class that handles a particular connection.
   *
   * <p>Multi-threaded calls for a given connection are
   * serialised.</p>
   **/
  private final class Handler {
    private final ConnectionDetails m_connectionDetails;

    // Parse state.
    private boolean m_parsingHeaders = false;
    private boolean m_handlingBody = false;

    // Parse data.
    private long m_time;
    private String m_method = null;
    private String m_url = null;
    private String m_queryString = null;
    private final List m_headers = new ArrayList();
    private int m_contentLength = -1;
    private String m_contentType = null;
    private final ByteArrayOutputStream m_entityBodyByteStream =
      new ByteArrayOutputStream();

    public Handler(ConnectionDetails connectionDetails) {
      m_connectionDetails = connectionDetails;

      /*
        m_out.print(s_newLine + s_indent + s_indent +
        "# New connection: " +
        connectionDetails.getDescription());
      */
    }

    public synchronized void handle(byte[] buffer, int length)
      throws IOException {

      // String used to parse headers - header names are US-ASCII
      // encoded and anchored to start of line. The correct charset to
      // use for URL's is not well defined by RFC 2616, so we use
      // ISO8859_1. This way we are at least non-lossy (US-ASCII maps
      // characters above 0xFF to '?').
      final String asciiString = new String(buffer, 0, length, "ISO8859_1");

      final Matcher matcher = m_requestLinePattern.matcher(asciiString);

      if (matcher.find()) {
        // Packet is start of new request message.

        // Grab match results because endMessage plays with
        // out Matcher.
        final String newMethod = matcher.group(1);
        final String newURL = matcher.group(2);
        final String newQueryString = matcher.group(3);

        endMessage();

        m_method = newMethod;
        m_parsingHeaders = true;

        if (m_method.equals("GET") ||
            m_method.equals("HEAD")) {
          m_handlingBody = false;
          m_url = newURL;
          m_queryString = newQueryString;
        }
        else if (m_method.equals("DELETE") ||
                 m_method.equals("TRACE")) {
          m_handlingBody = false;

          if (newQueryString != null) {
            m_url = newURL + newQueryString;
            m_queryString = null;
          }
          else {
            m_url = newURL;
          }
        }
        else if (m_method.equals("OPTIONS") ||
                 m_method.equals("PUT") ||
                 m_method.equals("POST")) {
          m_handlingBody = true;
          m_entityBodyByteStream.reset();
          m_contentLength = -1;
          m_contentType = null;

          if (newQueryString != null) {
            m_url = newURL + newQueryString;
            m_queryString = null;
          }
          else {
            m_url = newURL;
          }
        }
        else {
          warn("Ignoring '" + m_method + "' from " + m_connectionDetails);
          return;
        }

        if (!m_url.startsWith("http")) {
          // Relative URL given, calculate absolute URL.
          m_url = ("http" + (m_connectionDetails.isSecure() ? "s://" : "://") +
                   m_connectionDetails.getRemoteEndPoint()) + m_url;
        }

        // Stuff we do at start of request only.
        m_time = s_lastResponseTime > 0 ?
          System.currentTimeMillis() - s_lastResponseTime : 0;

        m_headers.clear();
      }

      // Stuff we do whatever.

      if (m_parsingHeaders) {
        for (int i = 0; i < s_mirroredHeaders.length; i++) {
          final Matcher headerMatcher =
            m_mirroredHeaderPatterns[i].matcher(asciiString);

          if (headerMatcher.find()) {

            m_headers.add(
              new NVPair(s_mirroredHeaders[i],
                         headerMatcher.group(1).trim()));
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
          final Matcher contentLengthMatcher =
            m_contentLengthPattern.matcher(asciiString);

          // Look for the content length and type in the header.
          if (contentLengthMatcher.find()) {
            m_contentLength =
              Integer.parseInt(contentLengthMatcher.group(1).trim());
          }

          final Matcher contentTypeMatcher =
            m_contentTypePattern.matcher(asciiString);

          if (contentTypeMatcher.find()) {
            m_contentType = contentTypeMatcher.group(1).trim();
            m_headers.add(new NVPair("Content-Type", m_contentType));
          }

          final Matcher messageBodyMatcher =
            m_messageBodyPattern.matcher(asciiString);

          if (messageBodyMatcher.find()) {

            m_parsingHeaders = false;
            final int beginOffset = messageBodyMatcher.start(1);
            final int endOffset = messageBodyMatcher.end(1);
            addToEntityBody(buffer, beginOffset, endOffset - beginOffset);
          }
        }
      }
      else {
        if (m_handlingBody) {
          addToEntityBody(buffer, 0, length);
        }
        else {
          warn("UNEXPECTED - Not parsing headers or handling POST");
        }
      }
    }

    private void addToEntityBody(byte[] bytes, int start, int length)
      throws IOException {

      int l = length;

      if (m_contentLength != -1 &&
          l > m_contentLength - m_entityBodyByteStream.size()) {

        warn("Expected content length exceeded, truncating to content length");
        l = m_contentLength - m_entityBodyByteStream.size();
      }

      m_entityBodyByteStream.write(bytes, start, l);

      // We flush our entity data output now if we've reached the
      // specified Content-Length. If no contentLength was specified
      // we rely on next message or connection close event to flush
      // the data.
      if (m_contentLength != -1 &&
          m_entityBodyByteStream.size() >= m_contentLength) {

        endMessage();
      }
    }

    private void warn(String message) {
      m_out.println("# WARNING: " + message);
      m_out.flush();
    }

    /**
     * Called when end of message is reached.
     */
    public synchronized void endMessage() throws IOException {
      if (m_method == null) {
        return;
      }

      final StringBuffer scriptOutput = new StringBuffer();
      final StringBuffer testOutput = new StringBuffer();

      if (m_time > 10) {
        appendNewLineAndIndent(scriptOutput, 2);
        scriptOutput.append("grinder.sleep(");
        scriptOutput.append(Long.toString(m_time));
        scriptOutput.append(")");
      }

      testOutput.append(s_newLine);

      final int requestNumber = incrementRequestNumber();
      final String requestVariable = "request" + requestNumber;
      final String headerAssignment;

      // Assigned below to any new headers, if any.
      String newHeaderString = null;
      String newHeaderVariable = null;

      if (m_headers.size() > 0) {
        final StringBuffer headerStringBuffer = new StringBuffer();

        final Iterator iterator = m_headers.iterator();
        boolean first = true;

        while (iterator.hasNext()) {
          final NVPair entry = (NVPair)iterator.next();

          if (!first) {
            appendNewLineAndIndent(headerStringBuffer, 3);
          }
          else {
            first = false;
          }

          appendNVPair(headerStringBuffer, entry);
        }

        final String headerString = headerStringBuffer.toString();
        String headerVariable = (String)m_previousHeaders.get(headerString);

        if (headerVariable == null) {
          headerVariable = "headers" + requestNumber;

          testOutput.append(s_newLine);
          testOutput.append(headerVariable);
          testOutput.append(" = ( ");
          testOutput.append(headerString);
          testOutput.append(")");

          // We have new headers, but don't add them to
          // m_previousHeaders until we've flushed the header array to
          // the script. Otherwise there's a race condition where
          // another Handler can refer to the header array before its
          // declared.
          newHeaderString = headerString;
          newHeaderVariable = headerVariable;
        }

        headerAssignment = "headers = " + headerVariable;
      }
      else {
        headerAssignment = "";
      }

      testOutput.append(s_newLine);
      testOutput.append(requestVariable);
      testOutput.append(" = HTTPRequest(");
      testOutput.append(headerAssignment);
      testOutput.append(")");

      // Base default description on method and URL.
      final String description;

      final Matcher lastURLPathElementMatcher =
        m_lastURLPathElementPattern.matcher(m_url);

      if (lastURLPathElementMatcher.find()) {
        description = m_method + " " + lastURLPathElementMatcher.group(1);
      }
      else {
        description = m_method;
      }

      testOutput.append(s_newLine);
      testOutput.append("tests[");
      testOutput.append(requestNumber);
      testOutput.append("] = Test(");
      testOutput.append(requestNumber);
      testOutput.append(", '");
      testOutput.append(description);
      testOutput.append("').wrap(request");
      testOutput.append(requestNumber);
      testOutput.append(")");

      testOutput.append(s_newLine);

      appendNewLineAndIndent(scriptOutput, 2);
      scriptOutput.append("tests[");
      scriptOutput.append(Integer.toString(requestNumber));
      scriptOutput.append("].");
      scriptOutput.append(m_method);
      scriptOutput.append("('");
      scriptOutput.append(m_url);

      if (m_queryString != null && m_queryString.length() > 1) {

        try {
          final String queryStringAsNameValuePairs =
            parseNameValueString(m_queryString.substring(1), 3);

          scriptOutput.append("'");
          scriptOutput.append(",");
          appendNewLineAndIndent(scriptOutput, 2);
          scriptOutput.append("  ( ");
          scriptOutput.append(queryStringAsNameValuePairs);
          scriptOutput.append(")");
        }
        catch (ParseException e) {
          // Failed to split query string into name-value pairs. Oh
          // well, bolt it back onto the URL.
          scriptOutput.append(m_queryString);
          scriptOutput.append("'");
        }
      }
      else {
        scriptOutput.append("'");
      }

      if (!m_parsingHeaders && m_handlingBody) {
        m_handlingBody = false;

        boolean parsedFormData = false;

        if ("application/x-www-form-urlencoded".equals(m_contentType)) {
          try {
            final String nameValueString =
              parseNameValueString(m_entityBodyByteStream.toString("ISO8859_1"),
                                   3);

            parsedFormData = true;

            scriptOutput.append(",");
            appendNewLineAndIndent(scriptOutput, 2);
            scriptOutput.append("  ( ");
            scriptOutput.append(nameValueString);
            scriptOutput.append(")");
          }
          catch (ParseException e) {
            // Failed to parse form data as name-value pairs, we'll
            // treat it as raw data instead.
          }
        }

        if (!parsedFormData) {
          final String dataParameter = "data" + requestNumber;

          testOutput.append(s_newLine);

          final byte[] bytes = m_entityBodyByteStream.toByteArray();

          if (bytes.length > 0x400) {
            // Large amount of data, use a file.
            final String fileName = dataParameter + ".dat";

            final FileOutputStream dataStream = new FileOutputStream(fileName);
            dataStream.write(bytes, 0, bytes.length);
            dataStream.close();

            testOutput.append(requestVariable);
            testOutput.append(".setDataFromFile('");
            testOutput.append(fileName);
            testOutput.append("')");
            testOutput.append(s_newLine);
          }
          else {
            testOutput.append(dataParameter);
            testOutput.append(" = ( ");

            for (int i = 0; i < bytes.length; ++i) {
              final int x =
                bytes[i] < 0 ? bytes[i] + 0x100 : (int)bytes[i];

              testOutput.append(Integer.toString(x));
              testOutput.append(", ");
            }

            testOutput.append(")");

            scriptOutput.append(", ");
            scriptOutput.append(dataParameter);
          }
        }
      }

      scriptOutput.append(")");
      scriptOutput.append(s_newLine);

      m_scriptFileWriter.print(scriptOutput.toString());
      m_scriptFileWriter.flush();

      m_testFileWriter.print(testOutput.toString());
      m_testFileWriter.flush();

      if (newHeaderString != null) {
        // We created a new header array, stash for use by other
        // Handlers.
        m_previousHeaders.put(newHeaderString, newHeaderVariable);
      }

      m_method = null;
    }
  }

  private void appendNewLineAndIndent(StringBuffer resultBuffer,
                                      int indentLevel) {
    resultBuffer.append(s_newLine);

    for (int k = 0; k < indentLevel; ++k) {
      resultBuffer.append(s_indent);
    }
  }

  private String parseNameValueString(String input, int indentLevel)
    throws IOException, ParseException {

    final StringBuffer result = new StringBuffer();

    final NVPair[] pairs = Codecs.query2nv(input);

    for (int i = 0; i < pairs.length; ++i) {
      if (i != 0) {
        appendNewLineAndIndent(result, indentLevel);
      }

      appendNVPair(result, pairs[i]);
    }

    return result.toString();
  }

  private void appendNVPair(StringBuffer resultBuffer, NVPair pair) {
    resultBuffer.append("NVPair(");
    quoteForScript(resultBuffer, pair.getName());
    resultBuffer.append(", ");
    quoteForScript(resultBuffer, pair.getValue());
    resultBuffer.append("), ");
  }

  private void quoteForScript(StringBuffer resultBuffer, String value) {
    final String quotes = value.indexOf("\n") > -1 ? "'''" : "'";

    resultBuffer.append(quotes);

    final int length = value.length();

    for (int i = 0; i < length; ++i) {
      final char c = value.charAt(i);

      switch (c) {
      case '\'':
      case '\\':
        resultBuffer.append('\\');
        // fall through.
      default:
        resultBuffer.append(c);
      }
    }

    resultBuffer.append(quotes);
  }
}
