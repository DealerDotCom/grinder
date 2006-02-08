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

import java.io.File;

import net.grinder.common.LoggerStubFactory;
import net.grinder.plugin.http.xml.FormFieldType;
import net.grinder.plugin.http.xml.ParsedTokenReferenceType;
import net.grinder.plugin.http.xml.RequestType;
import net.grinder.plugin.http.xml.TokenType;
import net.grinder.testutility.AbstractFileTestCase;
import net.grinder.testutility.FileUtilities;
import net.grinder.testutility.RandomStubFactory;
import net.grinder.tools.tcpproxy.ConnectionDetails;
import net.grinder.tools.tcpproxy.EndPoint;
import net.grinder.util.URIParser;
import net.grinder.util.URIParserImplementation;


/**
 * Unit tests for {@link ConnectionHandlerImplementation}.
 *
 * @author Philip Aston
 * @version $Revision$
 */
public class TestConnectionHandlerImplementation extends AbstractFileTestCase {

  private final RandomStubFactory m_httpRecordingStubFactory =
    new RandomStubFactory(HTTPRecording.class);
  private final HTTPRecording m_httpRecording =
    (HTTPRecording) m_httpRecordingStubFactory.getStub();

  private final LoggerStubFactory m_loggerStubFactory =
    new LoggerStubFactory();

  private final RegularExpressions m_regularExpressions =
    new RegularExpressionsImplementation();

  final URIParser m_uriParser = new URIParserImplementation();

  private final ConnectionDetails m_connectionDetails =
    new ConnectionDetails(
      new EndPoint("hostA", 80),
      new EndPoint("hostB", 80),
      false);

  public void testRequestWithGet() throws Exception {
    final ConnectionHandler handler =
      new ConnectionHandlerImplementation(
        m_httpRecording, m_loggerStubFactory.getLogger(), m_regularExpressions,
        m_uriParser, m_connectionDetails);

    final RequestType request = RequestType.Factory.newInstance();
    request.addNewHeaders();
    request.setMethod(RequestType.Method.Enum.forString("GET"));

    final TokenType token = TokenType.Factory.newInstance();
    token.setName("query");
    token.setTokenId("tokenID");

    m_httpRecordingStubFactory.setResult("addRequest", request);
    m_httpRecordingStubFactory.setResult("addNameValueToken", token);

    final String message = "GET /something?query=whatever HTTP/1.0\r\n\r\n";
    final byte[] buffer = message.getBytes();
    handler.handleRequest(buffer, buffer.length);

    m_httpRecordingStubFactory.assertSuccess("addRequest",
      m_connectionDetails, "GET", "/something?query=whatever");
    m_httpRecordingStubFactory.assertNoMoreCalls();

    final String response = "HTTP/1.0 200 OK\r\n";
    final byte[] responseBuffer = response.getBytes();

    handler.handleResponse(responseBuffer, responseBuffer.length);

    m_httpRecordingStubFactory.assertSuccess("markLastResponseTime");
    m_httpRecordingStubFactory.assertNoMoreCalls();

    handler.newRequestMessage();

    m_httpRecordingStubFactory.assertNoMoreCalls();
    m_loggerStubFactory.assertNoMoreCalls();
  }

  public void testAuthorization() throws Exception {
    final ConnectionHandler handler =
      new ConnectionHandlerImplementation(
        m_httpRecording, m_loggerStubFactory.getLogger(), m_regularExpressions,
        m_uriParser, m_connectionDetails);

    final RequestType request = RequestType.Factory.newInstance();
    request.addNewHeaders();
    request.setMethod(RequestType.Method.Enum.forString("GET"));

    m_httpRecordingStubFactory.setResult("addRequest", request);

    final String message =
      "GET / HTTP/1.1\r\n" +
      "Authorization: Basic aBcD\r\n";
    final byte[] buffer = message.getBytes();
    handler.handleRequest(buffer, buffer.length);

    m_httpRecordingStubFactory.assertSuccess("addRequest",
      m_connectionDetails, "GET", "/");

    // Bad authorization header.
    m_loggerStubFactory.assertSuccess("error", String.class);
    assertEquals(0, request.getHeaders().sizeOfAuthorizationArray());

    final String message2 =
      "GET / HTTP/1.1\r\n" +
      "Authorization: Basic dDpyYXVtc2NobWllcmU=\r\n";
    final byte[] buffer2 = message2.getBytes();
    handler.handleRequest(buffer2, buffer2.length);

    m_httpRecordingStubFactory.assertSuccess("addRequest",
      m_connectionDetails, "GET", "/");
    assertEquals("t", request.getHeaders().getAuthorizationArray(0).getBasic().getUserid());
    assertEquals("raumschmiere", request.getHeaders().getAuthorizationArray(0).getBasic().getPassword());

    m_httpRecordingStubFactory.assertNoMoreCalls();
    m_loggerStubFactory.assertNoMoreCalls();
  }

  public void testRequestWithPost() throws Exception {
    final ConnectionHandler handler =
      new ConnectionHandlerImplementation(
        m_httpRecording, m_loggerStubFactory.getLogger(), m_regularExpressions,
        m_uriParser, m_connectionDetails);

    final RequestType request = RequestType.Factory.newInstance();
    request.addNewHeaders();
    request.setMethod(RequestType.Method.Enum.forString("POST"));

    m_httpRecordingStubFactory.setResult("addRequest", request);

    final String message =
      "POST /something HTTP/1.0\r\n" +
      "Content-Length: 10\r\n" +
      "Content-Type: bah\r\n" +
      "\r\n" +
      "0123456789";

    final byte[] buffer = message.getBytes();
    handler.handleRequest(buffer, buffer.length);

    m_httpRecordingStubFactory.assertSuccess("addRequest",
      m_connectionDetails, "POST", "/something");
    m_httpRecordingStubFactory.assertNoMoreCalls();

    final String response = "HTTP/1.0 200 OK\r\n";
    final byte[] responseBuffer = response.getBytes();

    handler.handleResponse(responseBuffer, responseBuffer.length);

    m_httpRecordingStubFactory.assertSuccess("markLastResponseTime");
    m_httpRecordingStubFactory.assertNoMoreCalls();

    final String message2 =
      "POST /more HTTP/1.0\r\n" +
      "Content-Length: 10\r\n" +
      "\r\n";

    final byte[] buffer2 = message2.getBytes();
    handler.handleRequest(buffer2, buffer2.length);

    m_httpRecordingStubFactory.assertSuccess("addRequest",
      m_connectionDetails, "POST", "/more");

    final String message3 = "0123456789";
    final byte[] buffer3 = message3.getBytes();
    handler.handleRequest(buffer3, buffer3.length);

    final String message4 =
      "POST /evenmore HTTP/1.0\r\n";

    final byte[] buffer4 = message4.getBytes();
    handler.handleRequest(buffer4, buffer4.length);

    m_httpRecordingStubFactory.assertSuccess("addRequest",
      m_connectionDetails, "POST", "/evenmore");

    final String message5 =
      "Content-Length: 0\r\n" +
      "\r\n";

    final byte[] buffer5 = message5.getBytes();
    handler.handleRequest(buffer5, buffer5.length);

    m_httpRecordingStubFactory.assertNoMoreCalls();
    m_loggerStubFactory.assertNoMoreCalls();
  }

  public void testResponseMessage1() throws Exception {
    final ConnectionHandler handler =
      new ConnectionHandlerImplementation(
        m_httpRecording, m_loggerStubFactory.getLogger(), m_regularExpressions,
        m_uriParser, m_connectionDetails);

    final RequestType request = RequestType.Factory.newInstance();
    request.addNewHeaders();
    request.setMethod(RequestType.Method.Enum.forString("HEAD"));

    m_httpRecordingStubFactory.setResult("addRequest", request);

    final String message = "HEAD / HTTP/1.1\r\n";
    final byte[] buffer = message.getBytes();
    handler.handleRequest(buffer, buffer.length);

    m_httpRecordingStubFactory.assertSuccess("addRequest",
      m_connectionDetails, "HEAD", "/");

    final String response =
      "HTTP/1.0 302 Redirect\r\n" +
      "Hello: world\r\n" +
      "Location: http://somewhere/;a=b?c=d\r\n";

    final byte[] responseBuffer = response.getBytes();
    handler.handleResponse(responseBuffer, responseBuffer.length);

    m_httpRecordingStubFactory.assertSuccess("markLastResponseTime");
    m_httpRecordingStubFactory.assertSuccess(
      "addNameValueTokenReference",
      String.class,
      String.class,
      ParsedTokenReferenceType.class);
    m_httpRecordingStubFactory.assertSuccess(
      "addNameValueTokenReference",
      String.class,
      String.class,
      ParsedTokenReferenceType.class);

    m_httpRecordingStubFactory.assertNoMoreCalls();
    m_loggerStubFactory.assertNoMoreCalls();
  }

  public void testResponseMessage2() throws Exception {
    final ConnectionHandler handler =
      new ConnectionHandlerImplementation(
        m_httpRecording, m_loggerStubFactory.getLogger(), m_regularExpressions,
        m_uriParser, m_connectionDetails);

    final RequestType request = RequestType.Factory.newInstance();
    request.addNewHeaders();
    request.setMethod(RequestType.Method.Enum.forString("GET"));

    m_httpRecordingStubFactory.setResult("addRequest", request);

    final String message = "GET / HTTP/1.1\r\n";
    final byte[] buffer = message.getBytes();
    handler.handleRequest(buffer, buffer.length);

    m_httpRecordingStubFactory.assertSuccess("addRequest",
      m_connectionDetails, "GET", "/");

    final String response =
      "HTTP/1.0 200 OK\r\n" +
      "Content-Length:10\r\n\r\n" +
      "0123456789";

    final byte[] responseBuffer = response.getBytes();
    handler.handleResponse(responseBuffer, responseBuffer.length);

    m_httpRecordingStubFactory.assertSuccess("markLastResponseTime");

    m_httpRecordingStubFactory.assertNoMoreCalls();
    m_loggerStubFactory.assertNoMoreCalls();
  }

  public void testResponseMessage3() throws Exception {
    final ConnectionHandler handler =
      new ConnectionHandlerImplementation(
        m_httpRecording, m_loggerStubFactory.getLogger(), m_regularExpressions,
        m_uriParser, m_connectionDetails);

    final RequestType request = RequestType.Factory.newInstance();
    request.addNewHeaders();
    request.setMethod(RequestType.Method.Enum.forString("GET"));

    m_httpRecordingStubFactory.setResult("addRequest", request);

    final String message = "GET / HTTP/1.1\r\n";
    final byte[] buffer = message.getBytes();
    handler.handleRequest(buffer, buffer.length);

    m_httpRecordingStubFactory.assertSuccess("addRequest",
      m_connectionDetails, "GET", "/");

    final String response =
      "HTTP/1.0 304 Not Modified\r\n";

    final byte[] responseBuffer = response.getBytes();
    handler.handleResponse(responseBuffer, responseBuffer.length);

    m_httpRecordingStubFactory.assertSuccess("markLastResponseTime");

    m_httpRecordingStubFactory.assertNoMoreCalls();
    m_loggerStubFactory.assertNoMoreCalls();
  }

  public void testResponseMessageWithTokensInLinks() throws Exception {
    final ConnectionHandler handler =
      new ConnectionHandlerImplementation(
        m_httpRecording, m_loggerStubFactory.getLogger(), m_regularExpressions,
        m_uriParser, m_connectionDetails);

    final RequestType request = RequestType.Factory.newInstance();
    request.addNewHeaders();
    request.setMethod(RequestType.Method.Enum.forString("GET"));

    m_httpRecordingStubFactory.setResult("addRequest", request);

    final String message = "GET / HTTP/1.0\r\n";
    final byte[] buffer = message.getBytes();
    handler.handleRequest(buffer, buffer.length);

    m_httpRecordingStubFactory.assertSuccess("addRequest",
      m_connectionDetails, "GET", "/");

    final String response =
      "HTTP/1.0 200 OK\r\n" +
      "\r\n" +
      "<html>" +
      "<body><a href='./foo;session=57?token=1'>Hello world</a>" +
      "<a href=\"http://grinder.sourceforge.net/?token=1\">something else</a>";

    final byte[] responseBuffer = response.getBytes();
    handler.handleResponse(responseBuffer, responseBuffer.length);

    m_httpRecordingStubFactory.assertSuccess("markLastResponseTime");

    final Object[] parameters =
      m_httpRecordingStubFactory.assertSuccess("addNameValueTokenReference",
        String.class, String.class, ParsedTokenReferenceType.class).getParameters();
    assertEquals("session", parameters[0]);
    assertEquals("57", parameters[1]);
    assertEquals(ParsedTokenReferenceType.Source.BODY_URI_PATH_PARAMETER,
      ((ParsedTokenReferenceType)parameters[2]).getSource());
    m_httpRecordingStubFactory.assertSuccess("addNameValueTokenReference",
      String.class, String.class, ParsedTokenReferenceType.class);
    m_httpRecordingStubFactory.assertSuccess("addNameValueTokenReference",
      String.class, String.class, ParsedTokenReferenceType.class);

    m_loggerStubFactory.assertNoMoreCalls();

    final String response2 =
      "<a href=\"http://grinder.sourceforge.net/?token=2\">something else</a>" +
      "</body>";
    final byte[] responseBuffer2 = response2.getBytes();
    handler.handleResponse(responseBuffer2, responseBuffer2.length);
    m_httpRecordingStubFactory.assertSuccess("addNameValueTokenReference",
      String.class, String.class, ParsedTokenReferenceType.class);

    // Differing token values.
    m_loggerStubFactory.assertSuccess("error", String.class);

    m_httpRecordingStubFactory.assertNoMoreCalls();
    m_loggerStubFactory.assertNoMoreCalls();
  }

  public void testRequestStringBody() throws Exception {
    final ConnectionHandler handler =
      new ConnectionHandlerImplementation(
        m_httpRecording, m_loggerStubFactory.getLogger(), m_regularExpressions,
        m_uriParser, m_connectionDetails);

    final RequestType request = RequestType.Factory.newInstance();
    request.addNewHeaders();
    request.setMethod(RequestType.Method.Enum.forString("POST"));

    m_httpRecordingStubFactory.setResult("addRequest", request);

    final String message =
      "POST /something HTTP/1.0\r\n" +
      "Content-Length: 9\r\n" +
      "Content-Type: bah\r\n" +
      "\r\n" +
      "0123456789";

    final byte[] buffer = message.getBytes();
    handler.handleRequest(buffer, buffer.length);

    m_httpRecordingStubFactory.assertSuccess("addRequest",
      m_connectionDetails, "POST", "/something");
    m_httpRecordingStubFactory.assertNoMoreCalls();

    // Content length exceeded.
    m_loggerStubFactory.assertSuccess("error", String.class);
    m_loggerStubFactory.assertNoMoreCalls();

    handler.newRequestMessage(); // Force body to be flushed.

    assertEquals("bah", request.getBody().getContentType());
    assertEquals("012345678", request.getBody().getString());
    assertFalse(request.getBody().isSetBinary());
    assertFalse(request.getBody().isSetFile());
    assertFalse(request.getBody().isSetForm());

    m_loggerStubFactory.assertNoMoreCalls();
  }

  public void testRequestBinaryBody() throws Exception {
    final ConnectionHandler handler =
      new ConnectionHandlerImplementation(
        m_httpRecording, m_loggerStubFactory.getLogger(), m_regularExpressions,
        m_uriParser, m_connectionDetails);

    final RequestType request = RequestType.Factory.newInstance();
    request.addNewHeaders();
    request.setMethod(RequestType.Method.Enum.forString("POST"));

    m_httpRecordingStubFactory.setResult("addRequest", request);

    final String message =
      "POST /something HTTP/1.0\r\n" +
      "Content-Length: 100\r\n" +
      "Content-Type: bah\r\n" +
      "\r\n";

    final byte[] buffer = message.getBytes();
    handler.handleRequest(buffer, buffer.length);

    m_httpRecordingStubFactory.assertSuccess("addRequest",
      m_connectionDetails, "POST", "/something");
    m_httpRecordingStubFactory.assertNoMoreCalls();

    final byte[] buffer2 = new byte[50];

    for (int i = 0; i < buffer2.length; i++) {
      buffer2[i] = (byte) i;
    }

    handler.handleRequest(buffer2, 50);
    handler.handleRequest(buffer2, 50);

    handler.newRequestMessage(); // Force body to be flushed.

    assertEquals("bah", request.getBody().getContentType());
    assertEquals(100, request.getBody().getBinary().length);
    assertFalse(request.getBody().isSetString());
    assertFalse(request.getBody().isSetFile());
    assertFalse(request.getBody().isSetForm());

    m_loggerStubFactory.assertNoMoreCalls();
  }

  public void testRequestFormBody() throws Exception {
    final ConnectionHandler handler =
      new ConnectionHandlerImplementation(
        m_httpRecording, m_loggerStubFactory.getLogger(), m_regularExpressions,
        m_uriParser, m_connectionDetails);

    final RequestType request = RequestType.Factory.newInstance();
    request.addNewHeaders();
    request.setMethod(RequestType.Method.Enum.forString("POST"));

    m_httpRecordingStubFactory.setResult("addRequest", request);

    final String message =
      "POST /something HTTP/1.0\r\n" +
      "Content-Type: application/x-www-form-urlencoded\r\n" +
      "\r\n" +
      "red=5&blue=10";

    final byte[] buffer = message.getBytes();
    handler.handleRequest(buffer, buffer.length);

    m_httpRecordingStubFactory.assertSuccess("addRequest",
      m_connectionDetails, "POST", "/something");
    m_httpRecordingStubFactory.assertNoMoreCalls();

    handler.newRequestMessage(); // Force body to be flushed.

    final FormFieldType[] formFieldArray =
      request.getBody().getForm().getFormFieldArray();
    assertEquals(2, formFieldArray.length);
    assertEquals("red", formFieldArray[0].getName());
    assertEquals("10", formFieldArray[1].getValue());
    assertFalse(request.getBody().isSetBinary());
    assertFalse(request.getBody().isSetString());
    assertFalse(request.getBody().isSetFile());

    m_loggerStubFactory.assertNoMoreCalls();
  }

  public void testRequestFileBody() throws Exception {
    final ConnectionHandler handler =
      new ConnectionHandlerImplementation(
        m_httpRecording, m_loggerStubFactory.getLogger(), m_regularExpressions,
        m_uriParser, m_connectionDetails);

    final RequestType request = RequestType.Factory.newInstance();
    request.addNewHeaders();
    request.setMethod(RequestType.Method.Enum.forString("POST"));

    final File file = new File(getDirectory(), "formData");

    m_httpRecordingStubFactory.setResult("addRequest", request);
    m_httpRecordingStubFactory.setResult("createBodyDataFileName", file);

    final String message =
      "POST /something HTTP/1.0\r\n" +
      "\r\n";

    final byte[] buffer = message.getBytes();
    handler.handleRequest(buffer, buffer.length);

    m_httpRecordingStubFactory.assertSuccess("addRequest",
      m_connectionDetails, "POST", "/something");
    m_httpRecordingStubFactory.assertNoMoreCalls();

    final byte[] buffer2 = new byte[0x10000];

    for (int i = 0; i < buffer2.length; i++) {
      buffer2[i] = (byte) i;
    }

    handler.handleRequest(buffer2, buffer2.length);

    handler.newRequestMessage(); // Force body to be flushed.

    m_httpRecordingStubFactory.assertSuccess("createBodyDataFileName");

    assertEquals(file.getPath(), request.getBody().getFile());
    assertTrue(file.exists());
    assertTrue(file.canRead());
    assertEquals(0x10000, file.length());
    assertFalse(request.getBody().isSetBinary());
    assertFalse(request.getBody().isSetForm());
    assertFalse(request.getBody().isSetString());
  }

  public void testRequestFileBody2() throws Exception {
    final ConnectionHandler handler =
      new ConnectionHandlerImplementation(
        m_httpRecording, m_loggerStubFactory.getLogger(), m_regularExpressions,
        m_uriParser, m_connectionDetails);

    final RequestType request = RequestType.Factory.newInstance();
    request.addNewHeaders();
    request.setMethod(RequestType.Method.Enum.forString("POST"));

    final File file = new File(getDirectory(), "formData");
    file.createNewFile();
    FileUtilities.setCanAccess(file, false);

    m_httpRecordingStubFactory.setResult("addRequest", request);
    m_httpRecordingStubFactory.setResult("createBodyDataFileName", file);

    final String message =
      "POST /something HTTP/1.0\r\n" +
      "\r\n";

    final byte[] buffer = message.getBytes();
    handler.handleRequest(buffer, buffer.length);

    m_httpRecordingStubFactory.assertSuccess("addRequest",
      m_connectionDetails, "POST", "/something");
    m_httpRecordingStubFactory.assertNoMoreCalls();

    final byte[] buffer2 = new byte[0x10000];

    for (int i = 0; i < buffer2.length; i++) {
      buffer2[i] = (byte) i;
    }

    handler.handleRequest(buffer2, buffer2.length);

    handler.newRequestMessage(); // Force body to be flushed.

    assertFalse(request.getBody().isSetFile());
    assertFalse(request.getBody().isSetBinary());
    assertFalse(request.getBody().isSetForm());
    assertFalse(request.getBody().isSetString());

    // Failed to write body.
    m_loggerStubFactory.assertSuccess("error", String.class);
    m_loggerStubFactory.assertSuccess("getErrorLogWriter");
    m_loggerStubFactory.assertNoMoreCalls();
  }

  public void testWithBadRequestMessages() throws Exception {
    final ConnectionHandler handler =
      new ConnectionHandlerImplementation(
        m_httpRecording, m_loggerStubFactory.getLogger(), m_regularExpressions,
        m_uriParser, m_connectionDetails);

    handler.handleRequest(new byte[0], 0);
    m_loggerStubFactory.assertSuccess("error", String.class);

    final String message = "   GET blah HTTP/1.1";
    final byte[] buffer = message.getBytes();

    handler.handleRequest(buffer, buffer.length);
    m_loggerStubFactory.assertSuccess("error", String.class);

    m_httpRecordingStubFactory.assertNoMoreCalls();
    m_loggerStubFactory.assertNoMoreCalls();
  }

  public void testWithBadResponseMessages() throws Exception {
    final ConnectionHandler handler =
      new ConnectionHandlerImplementation(
        m_httpRecording, m_loggerStubFactory.getLogger(), m_regularExpressions,
        m_uriParser, m_connectionDetails);

    // Response with no request.
    handler.handleResponse(new byte[0], 0);
    m_loggerStubFactory.assertSuccess("error", String.class);

    final RequestType request = RequestType.Factory.newInstance();
    request.addNewHeaders();
    request.setMethod(RequestType.Method.Enum.forString("GET"));

    m_httpRecordingStubFactory.setResult("addRequest", request);

    final String message = "GET / HTTP/1.1\r\n";
    final byte[] buffer = message.getBytes();
    handler.handleRequest(buffer, buffer.length);

    m_httpRecordingStubFactory.assertSuccess("addRequest",
      m_connectionDetails, "GET", "/");

    // Responses that don't start with a standard response line are ignored.
    handler.handleResponse(new byte[0], 0);

    m_httpRecordingStubFactory.assertNoMoreCalls();
    m_loggerStubFactory.assertNoMoreCalls();
  }
}
