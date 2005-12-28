// Copyright (C) 2005 Philip Aston
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

package net.grinder.plugin.http.tcpproxyfilter;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.picocontainer.Disposable;

import HTTPClient.Codecs;
import HTTPClient.NVPair;
import HTTPClient.ParseException;

import net.grinder.common.GrinderBuild;
import net.grinder.common.Logger;
import net.grinder.plugin.http.xml.BodyType;
import net.grinder.plugin.http.xml.CommonHeadersType;
import net.grinder.plugin.http.xml.FormDataType;
import net.grinder.plugin.http.xml.FormFieldType;
import net.grinder.plugin.http.xml.HTTPRecordingType;
import net.grinder.plugin.http.xml.HeaderType;
import net.grinder.plugin.http.xml.HttpRecordingDocument;
import net.grinder.plugin.http.xml.ParameterType;
import net.grinder.plugin.http.xml.ParsedQueryStringType;
import net.grinder.plugin.http.xml.QueryStringType;
import net.grinder.plugin.http.xml.RequestHeadersType;
import net.grinder.plugin.http.xml.RequestType;
import net.grinder.plugin.http.xml.URLType;
import net.grinder.tools.tcpproxy.ConnectionDetails;
import net.grinder.tools.tcpproxy.TCPProxyFilter;


/**
 * {@link TCPProxyFilter} that transforms an HTTP request stream into
 * an XML document.
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
 * TODO losing requests - on shut down?
 * TODO make URLs a top level type?
 *
 * @author Philip Aston
 * @author Bertrand Ave
 * @version $Revision$
 */
public final class RequestStreamToXMLFilter
  implements TCPProxyFilter, ResponseEventListener, Disposable {

  /**
   * A list of headers which we record.
   */
  private static final String[] MIRRORED_HEADERS = {
    "Accept",
    "Accept-Charset",
    "Accept-Encoding",
    "Accept-Language",
    "Cache-Control",
    "If-Modified-Since",
    "Referer", // Deliberate misspelling to match specification.
    "User-Agent",
  };

  private static final Set HTTP_METHODS_WITH_BODY = new HashSet(Arrays.asList(
    new String[] { "OPTIONS", "POST", "POST" }
  ));

  private static final Set COMMON_HEADERS = new HashSet(Arrays.asList(
    new String[] {
      "Accept",
      "Accept-Charset",
      "Accept-Encoding",
      "Accept-Language",
      "Cache-Control",
      "User-Agent",
    }
  ));

  private final Logger m_logger;

  private final Pattern m_basicAuthorizationHeaderPattern;
  private final Pattern m_contentTypePattern;
  private final Pattern m_contentLengthPattern;
  private final Pattern m_lastURLPathElementPattern;
  private final Pattern m_messageBodyPattern;
  private final Pattern m_requestLinePattern;

  /** Entries correspond to MIRRORED_HEADERS. */
  private final Pattern[] m_mirroredHeaderPatterns;

  private final IntGenerator m_requestIDGenerator = new IntGenerator();

  private long m_lastResponseTime = 0;

  private final HandlerMap m_handlers = new HandlerMap();

  private final CommonHeadersMap m_commonHeadersMap = new CommonHeadersMap();

  private final HttpRecordingDocument m_recordingDocument;

  /**
   * Constructor.
   *
   * @param logger
   *          Logger to direct output to.
   */
  public RequestStreamToXMLFilter(Logger logger) {

    m_logger = logger;

    m_recordingDocument = HttpRecordingDocument.Factory.newInstance();
    final HTTPRecordingType.Metadata httpRecording =
      m_recordingDocument.addNewHttpRecording().addNewMetadata();

    httpRecording.setVersion("The Grinder " + GrinderBuild.getVersionString());
    httpRecording.setTime(Calendar.getInstance());

    m_messageBodyPattern = Pattern.compile("\\r\\n\\r\\n(.*)", Pattern.DOTALL);

    // From RFC 2616:
    //
    // Request-Line = Method SP Request-URI SP HTTP-Version CRLF
    // HTTP-Version = "HTTP" "/" 1*DIGIT "." 1*DIGIT
    // http_URL = "http:" "//" host [ ":" port ] [ abs_path [ "?" query ]]
    //
    // We're flexible about SP and CRLF, see RFC 2616, 19.3.

    m_requestLinePattern =
      Pattern.compile(
        "^([A-Z]+)[ \\t]+" +          // Method.
        "(?:(https?)://([^/]))?"  +   // Optional scheme, host, port.
        "([^\\?]+)" +                 // Path.
        "(?:\\?(.*))?" +              // Optional query string.
        "[ \\t]+HTTP/\\d.\\d[ \\t]*\\r?\\n",
        Pattern.MULTILINE | Pattern.UNIX_LINES);

    m_contentLengthPattern = getHeaderPattern("Content-Length", true);

    m_contentTypePattern = getHeaderPattern("Content-Type", true);

    m_mirroredHeaderPatterns = new Pattern[MIRRORED_HEADERS.length];

    for (int i = 0; i < MIRRORED_HEADERS.length; i++) {
      m_mirroredHeaderPatterns[i] =
        getHeaderPattern(MIRRORED_HEADERS[i], false);
    }

    m_basicAuthorizationHeaderPattern =
      Pattern.compile(
        "^Authorization:[ \\t]*Basic[  \\t]*([a-zA-Z0-9+/]*=*).*\\r?\\n",
        Pattern.MULTILINE | Pattern.UNIX_LINES);

    // Ignore maximum amount of stuff that's not a '?' followed by
    // a '/', then grab the next until the first '?'.
    m_lastURLPathElementPattern = Pattern.compile("^[^\\?]*/([^\\?]*)");
  }

  /**
   * Factory for regular expression patterns that match HTTP headers.
   *
   * @param headerName
   *          The header name.
   * @param caseInsensitive
   *          Some headers are commonly used in the wrong case.
   * @return The expression.
   */
  private static Pattern getHeaderPattern(String headerName,
                                          boolean caseInsensitive) {
    return Pattern.compile(
      "^" + headerName + ":[ \\t]*(.*)\\r?\\n",
      Pattern.MULTILINE |
      Pattern.UNIX_LINES |
      (caseInsensitive ? Pattern.CASE_INSENSITIVE : 0));
  }

  /**
   * Called when any response activity is detected. Because the test script
   * represents a single thread of control we need to calculate the sleep deltas
   * using the last time any activity occurred on any connection.
   */
  public void markLastResponseTime() {
    synchronized (this) {
      m_lastResponseTime = System.currentTimeMillis();
    }
  }

  /**
   * The main handler method called by the sniffer engine.
   *
   * <p>
   * This is called for message fragments; we don't assume that its passed a
   * complete HTTP message at a time.
   * </p>
   *
   * @param connectionDetails
   *          The TCP connection.
   * @param buffer
   *          The message fragment buffer.
   * @param bytesRead
   *          The number of bytes of buffer to process.
   * @return Filters can optionally return a <code>byte[]</code> which will be
   *         transmitted to the server instead of <code>buffer</code>.
   */
  public byte[] handle(ConnectionDetails connectionDetails, byte[] buffer,
                       int bytesRead) {

    m_handlers.getHandler(connectionDetails).handle(buffer, bytesRead);
    return null;
  }

  /**
   * A connection has been opened.
   *
   * @param connectionDetails a <code>ConnectionDetails</code> value
   */
  public void connectionOpened(ConnectionDetails connectionDetails) {
    m_handlers.getHandler(connectionDetails);
  }

  /**
   * A connection has been closed.
   *
   * @param connectionDetails a <code>ConnectionDetails</code> value
   */
  public void connectionClosed(ConnectionDetails connectionDetails) {
    m_handlers.closeHandler(connectionDetails);
  }

  /**
   * Called after the filter has been stopped.
   */
  public void dispose() {
    m_handlers.closeAllHandlers();

    final String result;

    synchronized (m_recordingDocument) {
      result = m_recordingDocument.toString();
    }

    m_logger.getOutputLogWriter().println(result);
    m_logger.getOutputLogWriter().flush();
  }

  /**
   * Class that handles a particular connection.
   *
   * <p>Multi-threaded calls for a given connection are
   * serialised.</p>
   **/
  private final class Handler {
    private final ConnectionDetails m_connectionDetails;

    // Parse data.
    private Request m_request;

    public Handler(ConnectionDetails connectionDetails) {
      m_connectionDetails = connectionDetails;
    }

    public synchronized void handle(byte[] buffer, int length) {

      // String used to parse headers - header names are US-ASCII encoded and
      // anchored to start of line. The correct character set to use for URL's
      // is not well defined by RFC 2616, so we use ISO8859_1. This way we are
      // at least non-lossy (US-ASCII maps characters above 0xFF to '?').
      final String asciiString;
      try {
        asciiString = new String(buffer, 0, length, "ISO8859_1");
      }
      catch (UnsupportedEncodingException e) {
        throw new AssertionError(e);
      }

      final Matcher matcher = m_requestLinePattern.matcher(asciiString);

      if (matcher.find()) {
        // Packet is start of new request message.

        endMessage();

        m_request = new Request(matcher.group(1),
                                matcher.group(2),
                                matcher.group(3),
                                matcher.group(4),
                                matcher.group(5));
      }

      // Stuff we do whatever.

      if (m_request == null) {
        m_logger.error("UNEXPECTED - No current request");
      }
      else if (m_request.getParsingBody()) {
        if (m_request.hasBody()) {
          m_request.addToEntityBody(buffer, 0, length);
        }
        else {
          m_logger.error("UNEXPECTED - Not parsing headers or handling POST");
        }
      }
      else {
        for (int i = 0; i < MIRRORED_HEADERS.length; i++) {
          final Matcher headerMatcher =
            m_mirroredHeaderPatterns[i].matcher(asciiString);

          if (headerMatcher.find()) {
            m_request.addHeader(
              MIRRORED_HEADERS[i], headerMatcher.group(1).trim());
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

        if (m_request.hasBody()) {
          final Matcher contentLengthMatcher =
            m_contentLengthPattern.matcher(asciiString);

          // Look for the content length and type in the header.
          if (contentLengthMatcher.find()) {
            m_request.setContentLength(
              Integer.parseInt(contentLengthMatcher.group(1).trim()));
          }

          final Matcher contentTypeMatcher =
            m_contentTypePattern.matcher(asciiString);

          if (contentTypeMatcher.find()) {
            m_request.setContentType(contentTypeMatcher.group(1).trim());
          }

          final Matcher messageBodyMatcher =
            m_messageBodyPattern.matcher(asciiString);

          if (messageBodyMatcher.find()) {
            final int beginOffset = messageBodyMatcher.start(1);
            final int endOffset = messageBodyMatcher.end(1);
            m_request.addToEntityBody(
              buffer, beginOffset, endOffset - beginOffset);
          }
        }
      }
    }

    /**
     * Called when end of message is reached.
     */
    public synchronized void endMessage() {
      if (m_request != null) {
        m_request.record();
      }

      m_request = null;
    }

    private final class Request {
      private final RequestType m_request = RequestType.Factory.newInstance();
      private final RequestHeadersType m_headers =
        RequestHeadersType.Factory.newInstance();

      private boolean m_parsingBody = false;
      private int m_contentLength = -1;
      private String m_contentType = null;
      private final ByteArrayOutputStream m_entityBodyByteStream =
        new ByteArrayOutputStream();

      public Request(String method, String scheme, String hostAndPort,
                     String path, String queryString) {

        final Matcher lastURLPathElementMatcher =
          m_lastURLPathElementPattern.matcher(path);

        m_request.setRequestID(m_requestIDGenerator.next());

        if (lastURLPathElementMatcher.find()) {
          m_request.setShortDescription(
            method + " " + lastURLPathElementMatcher.group(1));
        }
        else {
          m_request.setShortDescription(
            method + " " + m_request.getRequestID());
        }

        m_request.setMethod(RequestType.Method.Enum.forString(method));
        m_request.setTime(Calendar.getInstance());

        final URLType url = m_request.addNewUrl();
        if (scheme != null) {
          url.setScheme(URLType.Scheme.Enum.forString(scheme));
        }
        else {
          url.setScheme(m_connectionDetails.isSecure() ?
                        URLType.Scheme.HTTPS : URLType.Scheme.HTTP);
        }

        if (hostAndPort != null) {
          url.setHostAndPort(hostAndPort);
        }
        else {
          // Relative URL given, calculate absolute URL.
          url.setHostAndPort(
            m_connectionDetails.getRemoteEndPoint().toString());
        }

        url.setPath(path);

        if (queryString != null) {
          final QueryStringType urlQueryString = url.addNewQueryString();

          try {
            final NameValue[] queryStringAsNameValuePairs =
              parseNameValueString(queryString);

            final ParsedQueryStringType parsedQuery =
              ParsedQueryStringType.Factory.newInstance();

            for (int i = 0; i < queryStringAsNameValuePairs.length; ++i) {
              final ParameterType parameter = parsedQuery.addNewParameter();
              parameter.setName(queryStringAsNameValuePairs[i].getName());
              parameter.setValue(queryStringAsNameValuePairs[i].getValue());
            }

           urlQueryString.setParsed(parsedQuery);
          }
          catch (ParseException e) {
            urlQueryString.setUnparsed(queryString);
          }
        }

        final long lastResponseTime;

        synchronized (RequestStreamToXMLFilter.this) {
          lastResponseTime = m_lastResponseTime;
        }

        if (lastResponseTime > 0) {
          final long time = System.currentTimeMillis() - lastResponseTime;

          if (time > 10) {
            m_request.setSleeptime(time);
          }
        }
      }

      public boolean getParsingBody() {
        return m_parsingBody;
      }

      public boolean hasBody() {
        return HTTP_METHODS_WITH_BODY.contains(
          m_request.getMethod().toString());
      }

      public void setContentType(String contentType) {
        m_contentType = contentType;
        addHeader("Content-Type", m_contentType);
      }

      public void setContentLength(int contentLength) {
        m_contentLength = contentLength;
      }

      public void addHeader(String name, String value) {
        final HeaderType header = m_headers.addNewHeader();
        header.setName(name);
        header.setValue(value);
      }

      public void addToEntityBody(byte[] bytes, int start, int length) {
        m_parsingBody = true;

        final int lengthToWrite;

        if (m_contentLength != -1 &&
            length > m_contentLength - m_entityBodyByteStream.size()) {

          m_logger.error("Expected content length exceeded, truncating");
          lengthToWrite = m_contentLength - m_entityBodyByteStream.size();
        }
        else {
          lengthToWrite = length;
        }

        m_entityBodyByteStream.write(bytes, start, lengthToWrite);

        // We flush our entity data output now if we've reached the
        // specified Content-Length. If no contentLength was specified
        // we rely on next message or connection close event to flush
        // the data.
        if (m_contentLength != -1 &&
            m_entityBodyByteStream.size() >= m_contentLength) {

          endMessage();
        }
      }

      public void record() {
        m_request.setHeaders(
          m_commonHeadersMap.extractCommonHeaders(m_headers));

        if (hasBody()) {
          final BodyType body = m_request.addNewBody();
          body.setContentType(m_contentType);

          if ("application/x-www-form-urlencoded".equals(m_contentType)) {
            try {
              final NameValue[] formNameValuePairs;

              try {
                formNameValuePairs = parseNameValueString(
                  m_entityBodyByteStream.toString("ISO8859_1"));
              }
              catch (UnsupportedEncodingException e) {
                throw new AssertionError(e);
              }

              final FormDataType formData = FormDataType.Factory.newInstance();

              for (int i = 0; i < formNameValuePairs.length; ++i) {
                final FormFieldType formField = formData.addNewFormField();
                formField.setName(formNameValuePairs[i].getName());
                formField.setValue(formNameValuePairs[i].getValue());
              }

              body.setForm(formData);
            }
            catch (ParseException e) {
              // Failed to parse form data as name-value pairs, we'll
              // treat it as raw data instead.
              body.setBinary(m_entityBodyByteStream.toByteArray());
            }
          }
        }

        synchronized (m_recordingDocument) {
          m_recordingDocument.getHttpRecording().addNewRequest().set(m_request);
        }
      }

      private NameValue[] parseNameValueString(String input)
        throws ParseException {

        final NVPair[] pairs = Codecs.query2nv(input);

        final NameValue[] result = new NameValue[pairs.length];

        for (int i = 0; i < pairs.length; ++i) {
          result[i] = new NameValue(pairs[i].getName(), pairs[i].getValue());
        }

        return result;
      }
    }
  }

  /**
   * Map of {@link ConnectionDetails} to handlers.
   */
  private class HandlerMap {
    private final Map m_handlers = new HashMap();

    public Handler getHandler(ConnectionDetails connectionDetails) {
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

    public void closeHandler(ConnectionDetails connectionDetails) {

      final Handler handler;

      synchronized (m_handlers) {
        handler = (Handler)m_handlers.remove(connectionDetails);
      }

      if (handler == null) {
        throw new IllegalArgumentException(
          "Unknown connection " + connectionDetails);
      }

      handler.endMessage();
    }

    public void closeAllHandlers() {
      synchronized (m_handlers) {
        final Iterator iterator = m_handlers.values().iterator();

        while (iterator.hasNext()) {
          final Handler handler = (Handler)iterator.next();
          handler.endMessage();
        }
      }
    }
  }

  private static final class NameValue {
    private final String m_name;
    private final String m_value;

    public NameValue(String s1, String s2) {
      m_name = s1;
      m_value = s2;
    }

    public String getName() {
      return m_name;
    }

    public String getValue() {
      return m_value;
    }

    public String toString() {
      return getName() + "='" + getValue() + "'";
    }

    public int hashCode() {
      return m_name.hashCode() ^ m_value.hashCode();
    }

    public boolean equals(Object o) {
      if (o == this) {
        return true;
      }

      if (!(o instanceof NameValue)) {
        return false;
      }

      final NameValue other = (NameValue)o;

      return
        getName().equals(other.getName()) &&
        getValue().equals(other.getValue());
    }
  }

  private final class CommonHeadersMap {
    private Map m_map = new HashMap();
    private UniqueIDGenerator m_idGenerator = new UniqueIDGenerator();

    public RequestHeadersType extractCommonHeaders(
      RequestHeadersType requestHeaders) {
      final CommonHeadersType commonHeaders =
        CommonHeadersType.Factory.newInstance();
      final RequestHeadersType newRequestHeaders =
        RequestHeadersType.Factory.newInstance();

      final HeaderType[] headers = requestHeaders.getHeaderArray();

      String idPrefix = "headers-";

      for (int i = 0; i < headers.length; ++i) {
        if (COMMON_HEADERS.contains(headers[i].getName())) {
          commonHeaders.addNewHeader().set(headers[i]);
        }
        else {
          newRequestHeaders.addNewHeader().set(headers[i]);
        }

        if ("Accept".equals(headers[i].getName())) {
          final String acceptValue = headers[i].getValue();
          final int comma = acceptValue.indexOf(',');

          if (comma > 0) {
            idPrefix = "headers-" + acceptValue.substring(0, comma) + "-";
          }
          else {
            idPrefix = "headers-" + acceptValue + "-";
          }
        }
      }

      // Key that ignores ID.
      final Object key =
        Arrays.asList(commonHeaders.getHeaderArray()).toString();

      synchronized (m_map) {
        final CommonHeadersType existing = (CommonHeadersType)m_map.get(key);

        if (existing != null) {
          newRequestHeaders.setExtends(existing.getHeadersID());
        }
        else {
          commonHeaders.setHeadersID(m_idGenerator.next(idPrefix));

          synchronized (m_recordingDocument) {
            m_recordingDocument.getHttpRecording().addNewCommonHeaders()
            .set(commonHeaders);
          }

          m_map.put(key, commonHeaders);
          newRequestHeaders.setExtends(commonHeaders.getHeadersID());
        }
      }

      return newRequestHeaders;
    }
  }

  private static final class UniqueIDGenerator {
    private final Map m_nextValues = new HashMap();

    private String next(String key) {
      final IntGenerator intGenerator;
      synchronized (m_nextValues) {
        final IntGenerator existing = (IntGenerator)m_nextValues.get(key);

        if (existing != null) {
          intGenerator = existing;
        }
        else {
          intGenerator = new IntGenerator();
          m_nextValues.put(key, intGenerator);
        }
      }

      return key + intGenerator.next();
    }
  }

  private static final class IntGenerator {
    private int m_value = -1;

    public synchronized int next() {
      return ++m_value;
    }
  }
}
