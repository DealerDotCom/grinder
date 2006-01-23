// Copyright (C) 2005, 2006 Philip Aston
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
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
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

import net.grinder.common.Logger;
import net.grinder.plugin.http.xml.BasicAuthorizationHeaderType;
import net.grinder.plugin.http.xml.BodyType;
import net.grinder.plugin.http.xml.FormBodyType;
import net.grinder.plugin.http.xml.FormFieldType;
import net.grinder.plugin.http.xml.HeaderType;
import net.grinder.plugin.http.xml.HeadersType;
import net.grinder.plugin.http.xml.ParameterType;
import net.grinder.plugin.http.xml.ParsedQueryStringType;
import net.grinder.plugin.http.xml.ParsedURIPathType;
import net.grinder.plugin.http.xml.QueryStringType;
import net.grinder.plugin.http.xml.RelativeURIType;
import net.grinder.plugin.http.xml.RequestType;
import net.grinder.plugin.http.xml.URIPathNameValueParameterType;
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
 * TODO Session key processing.
 * TODO Avoid Jython 64K limit (does it still affect 2.2?)
 *
 * @author Philip Aston
 * @author Bertrand Ave
 * @version $Revision$
 */
public final class HTTPRequestFilter
  implements TCPProxyFilter, Disposable {

  /** Package scope for unit tests. */
  static final String OUTPUT_DIRECTORY_PROPERTY =
    "HTTPRequestFilter.outputDirectory";

  /**
   * Headers which we record.
   */
  private static final Set MIRRORED_HEADERS = new HashSet(Arrays.asList(
    new String[] {
      "Accept",
      "Accept-Charset",
      "Accept-Encoding",
      "Accept-Language",
      "Cache-Control",
      "Content-Type",
      "Content-type", // Common misspelling.
      "If-Modified-Since",
      "If-None-Match",
      "Referer", // Deliberate misspelling to match specification.
      "User-Agent",
    }
  ));

  private static final Set HTTP_METHODS_WITH_BODY = new HashSet(Arrays.asList(
    new String[] { "OPTIONS", "POST", "POST" }
  ));

  private final HTTPRecording m_httpRecording;
  private final Logger m_logger;

  private final Pattern m_basicAuthorizationHeaderPattern;
  private final Pattern m_headerPattern;
  private final Pattern m_messageBodyPattern;
  private final Pattern m_requestLinePattern;

  private final Pattern m_lastPathElementPathPattern;
  private final Pattern m_parsePathPattern;

  private final HandlerMap m_handlers = new HandlerMap();

  private final IntGenerator m_bodyFileIDGenerator = new IntGenerator();

  private final PathNameValueParameterIDMap m_pathParameterIDMap =
    new PathNameValueParameterIDMap();

  /**
   * Constructor.
   *
   * @param httpRecording
   *          Common HTTP recording state.
   * @param logger
   *          Logger to direct output to.
   */
  public HTTPRequestFilter(HTTPRecording httpRecording, Logger logger) {

    m_httpRecording = httpRecording;
    m_logger = logger;

    m_messageBodyPattern = Pattern.compile("\\r\\n\\r\\n(.*)", Pattern.DOTALL);

    // From RFC 2616:
    //
    // Request-Line = Method SP Request-URI SP HTTP-Version CRLF
    // HTTP-Version = "HTTP" "/" 1*DIGIT "." 1*DIGIT
    // http_URL = "http:" "//" host [ ":" port ] [ abs_path [ "?" query ]]
    //
    // We're flexible about SP and CRLF, see RFC 2616, 19.3.

    m_requestLinePattern = Pattern.compile(
      "^([A-Z]+)[ \\t]+" +          // Method.
      "(?:https?://[^/]+)?"  +      // Ignore scheme, host, port.
      "([^\\?]+)" +                 // Path.
      "(?:\\?(.*))?" +              // Optional query string.
      "[ \\t]+HTTP/\\d.\\d[ \\t]*\\r?\\n",
      Pattern.MULTILINE | Pattern.UNIX_LINES);

    m_headerPattern = Pattern.compile(
      "^([^:\\r\\n]*)[ \\t]*:[ \\t]*(.*?)\\r?\\n",
      Pattern.MULTILINE | Pattern.UNIX_LINES);

    m_basicAuthorizationHeaderPattern = Pattern.compile(
      "^Authorization[ \\t]*:[ \\t]*Basic[  \\t]*([a-zA-Z0-9+/]*=*).*?\\r?\\n",
      Pattern.MULTILINE | Pattern.UNIX_LINES);

    // Ignore maximum amount of stuff that's not a '?' or ';' followed by
    // a '/', then grab the next until the first '?' or ';'.
    m_lastPathElementPathPattern = Pattern.compile("^[^\\?;]*/([^\\?;]*)");

    m_parsePathPattern =
      Pattern.compile("(/?[^/;]+)(?:;([^/;=]+)(?:=([^/;]+))?)?");
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
    m_handlers.openHandler(connectionDetails);
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

        final String method = matcher.group(1);
        final String path = matcher.group(2);
        final String queryString = matcher.group(3);

        m_request = new Request(method, path, queryString);
      }

      // Stuff we do whatever.

      if (m_request == null) {
        m_logger.error("UNEXPECTED - No current request");
      }
      else if (m_request.getBody() != null) {
        m_request.getBody().write(buffer, 0, length);
      }
      else {
        // Still parsing headers.

        String headers = asciiString;
        int bodyStart = -1;

        if (m_request.expectingBody()) {
          final Matcher messageBodyMatcher =
            m_messageBodyPattern.matcher(asciiString);

          if (messageBodyMatcher.find()) {
            bodyStart = messageBodyMatcher.start(1);
            headers = asciiString.substring(0, bodyStart);
          }
        }

        final Matcher headerMatcher = m_headerPattern.matcher(headers);

        while (headerMatcher.find()) {
          final String name = headerMatcher.group(1);
          final String value = headerMatcher.group(2);

          if (MIRRORED_HEADERS.contains(name)) {
            m_request.addHeader(name, value);
          }

          if ("Content-Type".equalsIgnoreCase(name)) {
            m_request.setContentType(value);
          }

          if ("Content-Length".equalsIgnoreCase(name)) {
            m_request.setContentLength(Integer.parseInt(value));
          }
        }

        final Matcher authorizationMatcher =
          m_basicAuthorizationHeaderPattern.matcher(headers);

        if (authorizationMatcher.find()) {
          m_request.addBasicAuthorization(
            authorizationMatcher.group(1));
        }

        // Write out the body after parsing the headers as we need to
        // know the content length, and writing the body can end the
        // processing of the current request.
        if (bodyStart > -1) {
          m_request.new Body().write(
            buffer, bodyStart, buffer.length - bodyStart);
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
      private final RequestType m_requestXML =
        RequestType.Factory.newInstance();
      private final HeadersType m_headers = m_requestXML.addNewHeaders();

      private int m_contentLength = -1;
      private String m_contentType = null;
      private Body m_body;

      public Request(String method, String path, String queryString) {

        // Only set up direct attributes here. The "global information"
        // (request ID, base URL ID, common headers, page etc.) is filled in
        // when we record the request.

        m_requestXML.setMethod(RequestType.Method.Enum.forString(method));
        m_requestXML.setTime(Calendar.getInstance());

        final Matcher lastPathElementMatcher =
          m_lastPathElementPathPattern.matcher(path);

        final String description;

        if (lastPathElementMatcher.find()) {
          final String element = lastPathElementMatcher.group(1);

          if (element.trim().length() != 0) {
            description = method + " " + element;
          }
          else {
            description = method + " /";
          }
        }
        else {
          description = method + " " + path;
        }

        m_requestXML.setDescription(description);

        final RelativeURIType uri = m_requestXML.addNewUri();

        final ParsedURIPathType parsedPath =
          ParsedURIPathType.Factory.newInstance();

        boolean complexPath = false;

        final Matcher pathMatcher = m_parsePathPattern.matcher(path);

        while (pathMatcher.find()) {
          parsedPath.addSegment(pathMatcher.group(1));

          if (pathMatcher.group(2) != null) {
            complexPath = true;

            if (pathMatcher.group(3) == null) {
              parsedPath.addSimpleParameter(';' + pathMatcher.group(2));
            }
            else {
              final URIPathNameValueParameterType nameValue =
                parsedPath.addNewNameValueParameter();
              nameValue.setName(pathMatcher.group(2));
              nameValue.setValue(pathMatcher.group(3));
              nameValue.setParameterId(
                m_pathParameterIDMap.getPathID(
                  nameValue.getName(), nameValue.getValue()));
            }
          }
        }

        if (complexPath) {
          uri.setParsed(parsedPath);
        }
        else {
          uri.setUnparsed(path);
        }

        if (queryString != null) {
          final QueryStringType uriQueryString = uri.addNewQueryString();

          if (expectingBody()) {
            // Requests with bodies don't have parsed query strings.
            uriQueryString.setUnparsed(queryString);
          }
          else {
            try {
              final NVPair[] queryStringAsNameValuePairs =
                Codecs.query2nv(queryString);

              final ParsedQueryStringType parsedQuery =
                ParsedQueryStringType.Factory.newInstance();

              for (int i = 0; i < queryStringAsNameValuePairs.length; ++i) {
                final ParameterType parameter = parsedQuery.addNewParameter();
                parameter.setName(queryStringAsNameValuePairs[i].getName());
                parameter.setValue(queryStringAsNameValuePairs[i].getValue());
              }

             uriQueryString.setParsed(parsedQuery);
            }
            catch (ParseException e) {
              uriQueryString.setUnparsed(queryString);
            }
          }
        }

        final long lastResponseTime = m_httpRecording.getLastResponseTime();

        if (lastResponseTime > 0) {
          final long time = System.currentTimeMillis() - lastResponseTime;

          if (time > 10) {
            m_requestXML.setSleepTime(time);
          }
        }
      }

      public void addBasicAuthorization(String base64) {
        final String decoded = Codecs.base64Decode(base64);

        final int colon = decoded.indexOf(":");

        if (colon < 0) {
          m_logger.error("Could not decode Authorization header");
        }
        else {
          final BasicAuthorizationHeaderType basicAuthorization =
            m_headers.addNewAuthorization().addNewBasic();

          basicAuthorization.setUserid(decoded.substring(0, colon));
          basicAuthorization.setPassword(decoded.substring(colon + 1));
        }
      }

      public Body getBody() {
        return m_body;
      }

      public boolean expectingBody() {
        return HTTP_METHODS_WITH_BODY.contains(
          m_requestXML.getMethod().toString());
      }

      public void setContentType(String contentType) {
        m_contentType = contentType;
      }

      public void setContentLength(int contentLength) {
        m_contentLength = contentLength;
      }

      public void addHeader(String name, String value) {
        final HeaderType header = m_headers.addNewHeader();
        header.setName(name);
        header.setValue(value);
      }

      public void record() {
        if (getBody() != null) {
          getBody().record();
        }

        m_httpRecording.addRequest(m_connectionDetails, m_requestXML);
      }

      private class Body {
        private final ByteArrayOutputStream m_entityBodyByteStream =
          new ByteArrayOutputStream();

        public Body() {
          assert m_body == null;
          m_body = this;
        }

        public void write(byte[] bytes, int start, int length) {
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
          final BodyType body = m_requestXML.addNewBody();

          if (m_contentType != null) {
            body.setContentType(m_contentType);
          }

          final byte[] bytes = m_entityBodyByteStream.toByteArray();

          if (bytes.length > 0x4000) {
            // Large amount of data, use a file.
            final String fileName =
              "http-data-" + m_bodyFileIDGenerator.next() + ".dat";

            // Output directory is not an user option, but unit tests
            // need to control it.
            final File file = new File(
              System.getProperty(OUTPUT_DIRECTORY_PROPERTY, null),
              fileName);

            try {
              final FileOutputStream dataStream =
                new FileOutputStream(file);
              dataStream.write(bytes, 0, bytes.length);
              dataStream.close();
            }
            catch (IOException e) {
              m_logger.error("Failed to write body data to '" + file + "'");
              e.printStackTrace(m_logger.getErrorLogWriter());
            }

            body.setFile(fileName);
          }
          else {
            final String iso88591String;

            try {
              iso88591String = new String(bytes, "ISO8859_1");
            }
            catch (UnsupportedEncodingException e) {
              throw new AssertionError(e);
            }

            if ("application/x-www-form-urlencoded".equals(m_contentType)) {
              try {
                final NVPair[] formNameValuePairs =
                  Codecs.query2nv(iso88591String);

                final FormBodyType formData =
                  FormBodyType.Factory.newInstance();

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
              }
            }

            if (body.getForm() == null) {
              // Basic handling of strings; should use content type headers.
              boolean looksLikeAnExtendedASCIIString = true;

              for (int i = 0; i < bytes.length; ++i) {
                final char c = iso88591String.charAt(i);

                if (Character.isISOControl(c) && !Character.isWhitespace(c)) {
                  looksLikeAnExtendedASCIIString = false;
                  break;
                }
              }

              if (looksLikeAnExtendedASCIIString) {
                body.setString(iso88591String);
              }
              else {
                body.setBinary(bytes);
              }
            }
          }
        }
      }
    }
  }

  /**
   * Map of {@link ConnectionDetails} to handlers.
   */
  private class HandlerMap {
    private final Map m_handlers = Collections.synchronizedMap(new HashMap());

    public void openHandler(ConnectionDetails connectionDetails) {
      final Handler newHandler = new Handler(connectionDetails);

      final Handler existing =
        (Handler)m_handlers.put(connectionDetails, newHandler);

      if (existing != null) {
        throw new IllegalArgumentException(
          "Connection " + connectionDetails + " already opened");
      }
    }

    public Handler getHandler(ConnectionDetails connectionDetails) {
      final Handler handler = (Handler)m_handlers.get(connectionDetails);

      if (handler == null) {
        throw new IllegalArgumentException(
          "Unknown connection " + connectionDetails);
      }

      return handler;
    }

    public void closeHandler(ConnectionDetails connectionDetails) {

      final Handler handler =
        (Handler)m_handlers.remove(connectionDetails);

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

  private final class PathNameValueParameterIDMap {
    private final Map m_map = new HashMap();
    private final IntGenerator m_idGenerator = new IntGenerator();

    public synchronized String getPathID(String name, String value) {

      final String key = name + "/" + value;

      final String existing = (String)m_map.get(key);

      if (existing != null ) {
        return existing;
      }

      final String id = "parameter" + m_idGenerator.next();
      m_map.put(key, id);
      return id;
    }
  }
}
