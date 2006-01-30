// Copyright (C) 2006 Philip Aston
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
import java.net.HttpURLConnection;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;

import net.grinder.common.Logger;
import net.grinder.plugin.http.xml.BasicAuthorizationHeaderType;
import net.grinder.plugin.http.xml.BodyType;
import net.grinder.plugin.http.xml.FormBodyType;
import net.grinder.plugin.http.xml.FormFieldType;
import net.grinder.plugin.http.xml.HeaderType;
import net.grinder.plugin.http.xml.HeadersType;
import net.grinder.plugin.http.xml.ParsedTokenType;
import net.grinder.plugin.http.xml.ParsedURIPartType;
import net.grinder.plugin.http.xml.RelativeURIType;
import net.grinder.plugin.http.xml.RequestType;
import net.grinder.plugin.http.xml.ResponseType;
import net.grinder.tools.tcpproxy.ConnectionDetails;
import net.grinder.util.URIParser;
import net.grinder.util.URIParserImplementation;
import HTTPClient.Codecs;
import HTTPClient.NVPair;
import HTTPClient.ParseException;
import HTTPClient.URI;


/**
 * Class that handles a particular connection.
 *
 * <p>Multi-threaded calls for a given connection are
 * serialised.</p>
 *
 * @author Philip Aston
 * @version $Revision$
 */
final class ConnectionHandlerImplementation implements ConnectionHandler {

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
    new RequestType.Method.Enum[] {
        RequestType.Method.OPTIONS,
        RequestType.Method.POST,
        RequestType.Method.PUT,
    }
  ));

  private final URIParser m_uriParser = new URIParserImplementation();
  private final Logger m_logger;
  private final RegularExpressions m_regularExpressions;
  private final HTTPRecording m_httpRecording;
  private final ConnectionDetails m_connectionDetails;

  private final IntGenerator m_bodyFileIDGenerator = new IntGenerator();

  // Parse data.
  private Request m_request;

  public ConnectionHandlerImplementation(
    HTTPRecording httpRecording,
    Logger logger,
    RegularExpressions regularExpressions,
    ConnectionDetails connectionDetails) {

    m_logger = logger;
    m_regularExpressions = regularExpressions;
    m_httpRecording = httpRecording;
    m_connectionDetails = connectionDetails;
  }

  public synchronized void handleRequest(byte[] buffer, int length) {

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

    if (m_request != null && m_request.isContentLengthReached()) {
      newRequestMessage();
    }

    final Matcher matcher =
      m_regularExpressions.getRequestLinePattern().matcher(asciiString);

    if (matcher.find()) {
      // Packet is start of new request message.

      newRequestMessage();

      final String method = matcher.group(1);
      final String relativeURI = matcher.group(2);

      m_request = new Request(method, relativeURI);
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
          m_regularExpressions.getMessageBodyPattern().matcher(asciiString);

        if (messageBodyMatcher.find()) {
          bodyStart = messageBodyMatcher.start(1);
          headers = asciiString.substring(0, bodyStart);
        }
      }

      final Matcher headerMatcher =
        m_regularExpressions.getHeaderPattern().matcher(headers);

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
        m_regularExpressions.getBasicAuthorizationHeaderPattern().matcher(
          headers);

      if (authorizationMatcher.find()) {
        m_request.addBasicAuthorization(authorizationMatcher.group(1));
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

  public synchronized void handleResponse(byte[] buffer, int length) {

    if (m_request == null) {
      // We don't support pipelining.
      m_logger.error("UNEXPECTED - No current request");
      return;
    }

    // See notes in handleRequest about why we use this code page.
    final String asciiString;
    try {
      asciiString = new String(buffer, 0, length, "ISO8859_1");
    }
    catch (UnsupportedEncodingException e) {
      throw new AssertionError(e);
    }

    final Matcher matcher =
      m_regularExpressions.getResponseLinePattern().matcher(asciiString);

    if (matcher.find()) {
      // Packet is start of new request message.

      m_httpRecording.markLastResponseTime();

      final ResponseType response = m_request.addNewResponse();

      response.setStatusCode(Integer.parseInt(matcher.group(1)));
      response.setReasonPhrase(matcher.group(2));

      String headers = asciiString;
      int bodyStart = -1;

      if (m_request.expectingResponseBody()) {
        final Matcher messageBodyMatcher =
          m_regularExpressions.getMessageBodyPattern().matcher(asciiString);

        if (messageBodyMatcher.find()) {
          bodyStart = messageBodyMatcher.start(1);
          headers = asciiString.substring(0, bodyStart);
        }
      }

      final Matcher headerMatcher =
        m_regularExpressions.getHeaderPattern().matcher(headers);

      while (headerMatcher.find()) {
        final String name = headerMatcher.group(1);
        final String value = headerMatcher.group(2);

        if ("Location".equals(name)) {
          m_uriParser.parse(value, new URIParser.AbstractParseListener() {

            public boolean pathParameterNameValue(String name, String value) {
              response.addNewToken().set(
                m_httpRecording.addNameValueToken(
                  name, value,
                  ParsedTokenType.Source.LOCATION_HEADER_PATH_PARAMETER));

              return true;
            }

            public boolean queryStringNameValue(String name, String value) {
              response.addNewToken().set(
                m_httpRecording.addNameValueToken(
                  name, value,
                  ParsedTokenType.Source.LOCATION_HEADER_QUERY_STRING));

              return true;
            }
          });
        }

        // TODO parse body.
      }
    }
  }

  /**
   * Called when a new request message is expected.
   */
  public synchronized void newRequestMessage() {
    if (m_request != null) {
      m_request.record();
    }

    m_request = null;
  }

  private final class Request {
    private final RequestType m_requestXML = RequestType.Factory.newInstance();
    private final HeadersType m_headers = m_requestXML.addNewHeaders();

    private int m_contentLength = -1;
    private String m_contentType = null;
    private Body m_body;
    private boolean m_contentLengthReached;

    public Request(String method, String relativeURI) {

      // Only set up direct attributes here. The "global information"
      // (request ID, base URL ID, common headers, page etc.) is filled in
      // when we record the request.

      m_requestXML.setMethod(RequestType.Method.Enum.forString(method));
      m_requestXML.setTime(Calendar.getInstance());

      String unescapedURI;

      try {
        unescapedURI = URI.unescape(relativeURI, null);
      }
      catch (ParseException e) {
        unescapedURI = relativeURI;
      }

      final Matcher lastPathElementMatcher =
        m_regularExpressions.getLastPathElementPathPattern().matcher(
          unescapedURI);

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
        description = method + " " + relativeURI;
      }

      m_requestXML.setDescription(description);

      final RelativeURIType uri = m_requestXML.addNewUri();

      final ParsedURIPartType parsedPath =
        ParsedURIPartType.Factory.newInstance();
      final ParsedURIPartType parsedQueryString =
        ParsedURIPartType.Factory.newInstance();
      final String[] fragment = new String[1];

      // Look for tokens in path parameters and query string. We create
      // references to any tokens that have been seen before in some response.
      m_uriParser.parse(relativeURI, new URIParser.AbstractParseListener() {

        public boolean path(String path) {
          parsedPath.addText(path);
          return true;
        }

        private void addNameValue(
          ParsedURIPartType part, String name, String value) {

          final String tokenID =
            m_httpRecording.getNameValueTokenID(name, value);

          if (tokenID != null) {
            // Matches a name-value token we've found before.
            part.addNewTokenParameter().setTokenId(tokenID);
          }
          else {
            part.addText(name + "=" + value);
          }
        }

        public boolean pathParameterNameValue(String name, String value) {
          addNameValue(parsedPath, name, value);
          return true;
        }

        public boolean queryString(String queryString) {
          parsedQueryString.addText(queryString);
          return true;
        }

        public boolean queryStringNameValue(String name, String value) {
          addNameValue(parsedQueryString, name, value);
          return true;
        }

        public boolean fragment(String theFragment) {
          fragment[0] = theFragment;
          return true;
        }
      });

      uri.setPath(parsedPath);

      if (parsedQueryString.getTokenParameterArray().length > 0 ||
          parsedQueryString.getTextArray().length > 0) {
        uri.setQueryString(parsedQueryString);
      }

      if (fragment[0] != null) {
        uri.setFragment(fragment[0]);
      }

      final long lastResponseTime = m_httpRecording.getLastResponseTime();

      if (lastResponseTime > 0) {
        final long time = System.currentTimeMillis() - lastResponseTime;

        if (time > 10) {
          m_requestXML.setSleepTime(time);
        }
      }
    }

    public ResponseType addNewResponse() {
      return m_requestXML.addNewResponse();
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
      return HTTP_METHODS_WITH_BODY.contains(m_requestXML.getMethod());
    }

    public boolean expectingResponseBody() {
      // RFC 2616, 4.3.
      if (m_requestXML.getMethod().equals(RequestType.Method.HEAD)) {
        return false;
      }

      final int status = m_requestXML.getResponse().getStatusCode();

      if (status < 200 ||
          status == HttpURLConnection.HTTP_NO_CONTENT ||
          status == HttpURLConnection.HTTP_NOT_MODIFIED) {
        return false;
      }

      return true;
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

        // We mark the request as finished if we've reached the specified
        // Content-Length. We rely on next message or connection close
        // event to flush the data, this allows us to parse the response. We
        // also rely on these events if no Content-Length is specified.
        if (m_contentLength != -1 &&
            m_entityBodyByteStream.size() >= m_contentLength) {

          m_request.setContentLengthReached();
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
            System.getProperty(OUTPUT_DIRECTORY_PROPERTY, null), fileName);

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

    public void setContentLengthReached() {
      m_contentLengthReached = true;
    }

    public boolean isContentLengthReached() {
      return m_contentLengthReached;
    }
  }
}
