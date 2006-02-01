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
import net.grinder.plugin.http.xml.NameValueType;
import net.grinder.plugin.http.xml.RequestType;
import net.grinder.plugin.http.xml.TokenType;
import net.grinder.testutility.AbstractFileTestCase;
import net.grinder.testutility.AssertUtilities;
import net.grinder.testutility.FileUtilities;
import net.grinder.testutility.RandomStubFactory;
import net.grinder.testutility.XMLBeansUtilities;
import net.grinder.tools.tcpproxy.ConnectionDetails;
import net.grinder.tools.tcpproxy.EndPoint;


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
    new RegularExpressions();

  private final ConnectionDetails m_connectionDetails =
    new ConnectionDetails(
      new EndPoint("hostA", 80),
      new EndPoint("hostB", 80),
      false);

  protected void setUp() throws Exception {
    super.setUp();
    System.setProperty(
      ConnectionHandlerImplementation.OUTPUT_DIRECTORY_PROPERTY,
      getDirectory().getAbsolutePath());
  }

  protected void tearDown() throws Exception {
    super.tearDown();
    // Can't set a system property to null.
    System.setProperty(
      ConnectionHandlerImplementation.OUTPUT_DIRECTORY_PROPERTY, "./");
  }

  public void testRequestWithGet1() throws Exception {
    final ConnectionHandler handler =
      new ConnectionHandlerImplementation(
        m_httpRecording, m_loggerStubFactory.getLogger(), m_regularExpressions,
        m_connectionDetails);

    final TokenType token = TokenType.Factory.newInstance();
    token.setName("query");
    token.setTokenId("tokenID");

    m_httpRecordingStubFactory.setResult("addNameValueToken", token);

    final String message = "GET /something?query=whatever HTTP/1.0\r\n\r\n";
    final byte[] buffer = message.getBytes();

    m_httpRecordingStubFactory.setResult("getLastResponseTime",
      new Long(System.currentTimeMillis() + 1000));

    handler.handleRequest(buffer, buffer.length);

    final String response = "HTTP/1.0 200 OK\r\n";
    final byte[] responseBuffer = response.getBytes();

    m_httpRecordingStubFactory.assertSuccess("getLastResponseTime");

    handler.handleResponse(responseBuffer, responseBuffer.length);

    m_httpRecordingStubFactory.assertSuccess("markLastResponseTime");
    m_httpRecordingStubFactory.assertNoMoreCalls();

    handler.newRequestMessage();

    final Object[] parameters =
      m_httpRecordingStubFactory.assertSuccess("addRequest",
        ConnectionDetails.class, RequestType.class).getParameters();
    assertSame(m_connectionDetails, parameters[0]);

    final RequestType request = (RequestType)parameters[1];
    XMLBeansUtilities.validate(request);

    assertEquals("GET", request.getMethod().toString());
    assertEquals("/something", request.getUri().getPath().getTextArray(0));
    assertEquals("tokenID", request.getUri().getQueryString().getTokenReferenceArray(0).getTokenId());
    assertEquals("GET something", request.getDescription());
    assertEquals(0, request.getHeaders().getHeaderArray().length);
    assertFalse(request.toString(), request.isSetSleepTime());
    assertEquals(200, request.getResponse().getStatusCode());

    m_httpRecordingStubFactory.assertNoMoreCalls();
    m_loggerStubFactory.assertNoMoreCalls();
  }

  public void testRequestWithGet2() throws Exception {
    final ConnectionHandler handler =
      new ConnectionHandlerImplementation(
        m_httpRecording, m_loggerStubFactory.getLogger(), m_regularExpressions,
        m_connectionDetails);

    final String message =
      "GET blah HTTP/1.0\r\n" +
      "Accept: *\r\n" +
      "Foo: bah\r\n" +
      "Authorization: Basic dXNlcjpwYXNz\r\n" + // base64("user:pass")
      "\r\n\r\n";
    final byte[] buffer = message.getBytes();

    m_httpRecordingStubFactory.setResult("getLastResponseTime",
      new Long(System.currentTimeMillis() - 100));

    handler.handleRequest(buffer, buffer.length);

    m_httpRecordingStubFactory.assertSuccess("getLastResponseTime");


    final String response = "HTTP/1.0 200 OK\r\n";
    final byte[] responseBuffer = response.getBytes();

    handler.handleResponse(responseBuffer, responseBuffer.length);

    m_httpRecordingStubFactory.assertSuccess("markLastResponseTime");
    m_httpRecordingStubFactory.assertNoMoreCalls();

    handler.newRequestMessage();

    final Object[] parameters =
      m_httpRecordingStubFactory.assertSuccess("addRequest",
        ConnectionDetails.class, RequestType.class).getParameters();
    assertSame(m_connectionDetails, parameters[0]);

    final RequestType request = (RequestType)parameters[1];
    XMLBeansUtilities.validate(request);

    assertEquals("GET", request.getMethod().toString());
    assertEquals("blah", request.getUri().getPath().getTextArray(0));
    assertFalse(request.getUri().isSetQueryString());
    assertEquals("GET blah", request.getDescription());
    assertEquals(1, request.getHeaders().getHeaderArray().length);
    assertEquals(1, request.getHeaders().getAuthorizationArray().length);
    assertEquals("user", request.getHeaders().getAuthorizationArray(0).getBasic().getUserid());
    assertEquals("pass", request.getHeaders().getAuthorizationArray(0).getBasic().getPassword());
    final long sleepTime = request.getSleepTime();
    assertTrue(request.toString(), sleepTime >= 100);
    assertTrue(request.toString(), sleepTime <= 1000);

    m_httpRecordingStubFactory.assertNoMoreCalls();
    m_loggerStubFactory.assertNoMoreCalls();
  }

  public void testRequestWIthGet3() throws Exception {
    final ConnectionHandler handler =
      new ConnectionHandlerImplementation(
        m_httpRecording, m_loggerStubFactory.getLogger(), m_regularExpressions,
        m_connectionDetails);

    final String message =
      "GET /blah?unparsablequerystring HTTP/1.0\r\n" +
      "Authorization: Basic aGVsbG93b3JsZA==\r\n" + // base64("helloworld")
      "\r\n\r\n";
    final byte[] buffer = message.getBytes();

    handler.handleRequest(buffer, buffer.length);

    AssertUtilities.assertContains(
      (String)
      m_loggerStubFactory.assertSuccess("error", String.class).getParameters()[0],
      "Authorization header");

    m_httpRecordingStubFactory.assertSuccess("getLastResponseTime");
    m_httpRecordingStubFactory.assertNoMoreCalls();

    final String response = "HTTP/1.0 301 Moved Permanently\r\n";
    final byte[] responseBuffer = response.getBytes();

    handler.handleResponse(responseBuffer, responseBuffer.length);

    m_httpRecordingStubFactory.assertSuccess("markLastResponseTime");

    handler.newRequestMessage();

    final Object[] parameters =
      m_httpRecordingStubFactory.assertSuccess("addRequest",
        ConnectionDetails.class, RequestType.class).getParameters();
    assertSame(m_connectionDetails, parameters[0]);

    final RequestType request = (RequestType)parameters[1];
    XMLBeansUtilities.validate(request);

    assertEquals("GET", request.getMethod().toString());
    assertEquals("/blah", request.getUri().getPath().getTextArray(0));
    assertEquals("unparsablequerystring", request.getUri().getQueryString().getTextArray(0));
    assertEquals("GET blah", request.getDescription());
    assertEquals(0, request.getHeaders().getHeaderArray().length);
    assertEquals(0, request.getHeaders().getAuthorizationArray().length);
    assertEquals(301, request.getResponse().getStatusCode());
    assertEquals("Moved Permanently", request.getResponse().getReasonPhrase());

    m_httpRecordingStubFactory.assertNoMoreCalls();
    m_loggerStubFactory.assertNoMoreCalls();
  }

  public void testRequestWithPost1() throws Exception {
    final ConnectionHandler handler =
      new ConnectionHandlerImplementation(
        m_httpRecording, m_loggerStubFactory.getLogger(), m_regularExpressions,
        m_connectionDetails);

    final String message =
      "POST / HTTP/1.0\r\n" +
      "\r\n" +
      "Hello World";
    final byte[] buffer = message.getBytes();

    handler.handleRequest(buffer, buffer.length);

    m_httpRecordingStubFactory.assertSuccess("getLastResponseTime");
    m_httpRecordingStubFactory.assertNoMoreCalls();

    final String response = "HTTP/1.0 200 OK\r\n";
    final byte[] responseBuffer = response.getBytes();

    handler.handleResponse(responseBuffer, responseBuffer.length);

    m_httpRecordingStubFactory.assertSuccess("markLastResponseTime");
    m_httpRecordingStubFactory.assertNoMoreCalls();

    handler.newRequestMessage();

    final Object[] parameters =
      m_httpRecordingStubFactory.assertSuccess("addRequest",
        ConnectionDetails.class, RequestType.class).getParameters();
    assertSame(m_connectionDetails, parameters[0]);

    final RequestType request = (RequestType)parameters[1];
    XMLBeansUtilities.validate(request);

    assertEquals("POST", request.getMethod().toString());
    assertEquals("/", request.getUri().getPath().getTextArray(0));
    assertFalse(request.getUri().isSetQueryString());
    assertEquals("POST /", request.getDescription());
    assertEquals(0, request.getHeaders().getHeaderArray().length);
    assertEquals(0, request.getHeaders().getAuthorizationArray().length);
    assertEquals("Hello World", request.getBody().getString());

    m_httpRecordingStubFactory.assertNoMoreCalls();
    m_loggerStubFactory.assertNoMoreCalls();
  }

  public void testRequestWithPost2() throws Exception {
    final ConnectionHandler handler =
      new ConnectionHandlerImplementation(
        m_httpRecording, m_loggerStubFactory.getLogger(), m_regularExpressions,
        m_connectionDetails);

    final String message1 = "POST /blah?query=lah HTTP/1.0\r\n";
    final byte[] buffer1 = message1.getBytes();

    handler.handleRequest(buffer1, buffer1.length);

    m_httpRecordingStubFactory.assertSuccess("getLastResponseTime");
    m_httpRecordingStubFactory.assertNoMoreCalls();

    final String message2 =
      "Content-Type: application/x-www-form-urlencoded\r\n" +
      "\r\n" +
      "foo=bah&x=y";
    final byte[] buffer2 = message2.getBytes();

    handler.handleRequest(buffer2, buffer2.length);

    m_httpRecordingStubFactory.assertNoMoreCalls();

    final String response = "HTTP/1.0 200 OK\r\n";
    final byte[] responseBuffer = response.getBytes();
    handler.handleResponse(responseBuffer, responseBuffer.length);

    m_httpRecordingStubFactory.assertSuccess("markLastResponseTime");
    m_httpRecordingStubFactory.assertNoMoreCalls();

    handler.newRequestMessage();

    final Object[] parameters =
      m_httpRecordingStubFactory.assertSuccess("addRequest",
        ConnectionDetails.class, RequestType.class).getParameters();
    assertSame(m_connectionDetails, parameters[0]);

    final RequestType request = (RequestType)parameters[1];
    XMLBeansUtilities.validate(request);

    assertEquals("POST", request.getMethod().toString());
    assertEquals("/blah", request.getUri().getPath().getTextArray(0));
    assertEquals("query=lah", request.getUri().getQueryString());
    assertEquals("POST blah", request.getDescription());
    assertEquals(1, request.getHeaders().getHeaderArray().length);
    assertEquals(0, request.getHeaders().getAuthorizationArray().length);
    final FormFieldType[] form = request.getBody().getForm().getFormFieldArray();
    assertEquals(2, form.length);
    assertEquals("foo", form[0].getName());
    assertEquals("bah", form[0].getValue());
    assertEquals("x", form[1].getName());
    assertEquals("y", form[1].getValue());
    assertEquals("application/x-www-form-urlencoded",
                 request.getBody().getContentType());

    m_httpRecordingStubFactory.assertNoMoreCalls();
    m_loggerStubFactory.assertNoMoreCalls();
  }

  public void testRequestWithPost3() throws Exception {
    final ConnectionHandler handler =
      new ConnectionHandlerImplementation(
        m_httpRecording, m_loggerStubFactory.getLogger(), m_regularExpressions,
        m_connectionDetails);

    final String message1 =
      "POST /blah HTTP/1.1\r\n" +
      "Content-Type: mybinary\r\n" +
      "Content-Length: 256\r\n" +
      "\r\n";
    final byte[] buffer1 = message1.getBytes();

    handler.handleRequest(buffer1, buffer1.length);

    m_httpRecordingStubFactory.assertSuccess("getLastResponseTime");
    m_httpRecordingStubFactory.assertNoMoreCalls();

    final byte[] buffer2 = new byte[300];

    for (int i=0; i<buffer2.length; ++i) {
      buffer2[i] = (byte)i;
    }

    handler.handleRequest(buffer2, buffer2.length);

    AssertUtilities.assertContains(
      (String)
      m_loggerStubFactory.assertSuccess("error", String.class).getParameters()[0],
      "content length exceeded");
    m_loggerStubFactory.assertNoMoreCalls();

    final String response = "HTTP/1.0 200 OK\r\n";
    final byte[] responseBuffer = response.getBytes();

    handler.handleResponse(responseBuffer, responseBuffer.length);

    m_httpRecordingStubFactory.assertSuccess("markLastResponseTime");
    m_httpRecordingStubFactory.assertNoMoreCalls();

    handler.newRequestMessage();

    final Object[] parameters =
      m_httpRecordingStubFactory.assertSuccess("addRequest",
        ConnectionDetails.class, RequestType.class).getParameters();
    assertSame(m_connectionDetails, parameters[0]);

    final RequestType request = (RequestType)parameters[1];
    XMLBeansUtilities.validate(request);

    assertEquals("POST", request.getMethod().toString());
    assertEquals("/blah", request.getUri().getPath().getTextArray(0));
    assertFalse(request.getUri().isSetQueryString());
    assertEquals("POST blah", request.getDescription());
    assertEquals(1, request.getHeaders().getHeaderArray().length);
    assertEquals(0, request.getHeaders().getAuthorizationArray().length);
    assertEquals("mybinary", request.getBody().getContentType());

    final byte[] bytes = request.getBody().getBinary();
    assertEquals(256, bytes.length);
    for (int i=0; i<bytes.length; ++i) {
      assertEquals((byte)i, bytes[i]);
    }

    m_httpRecordingStubFactory.assertNoMoreCalls();
    m_loggerStubFactory.assertNoMoreCalls();
  }

  public void testRequestWithPost4() throws Exception {
    final ConnectionHandler handler =
      new ConnectionHandlerImplementation(
        m_httpRecording, m_loggerStubFactory.getLogger(), m_regularExpressions,
        m_connectionDetails);

    final String message1 =
      "POST /blah HTTP/1.1\r\n" +
      "\r\n";
    final byte[] buffer1 = message1.getBytes();

    handler.handleRequest(buffer1, buffer1.length);

    m_httpRecordingStubFactory.assertSuccess("getLastResponseTime");
    m_httpRecordingStubFactory.assertNoMoreCalls();

    final byte[] buffer2 = new byte[20000];

    handler.handleRequest(buffer2, buffer2.length);

    final String response = "HTTP/1.0 200 OK\r\n";
    final byte[] responseBuffer = response.getBytes();
    handler.handleResponse(responseBuffer, responseBuffer.length);

    m_httpRecordingStubFactory.assertSuccess("markLastResponseTime");
    m_httpRecordingStubFactory.assertNoMoreCalls();

    handler.newRequestMessage();

    final Object[] parameters =
      m_httpRecordingStubFactory.assertSuccess("addRequest",
        ConnectionDetails.class, RequestType.class).getParameters();
    assertSame(m_connectionDetails, parameters[0]);

    final RequestType request1 = (RequestType)parameters[1];
    XMLBeansUtilities.validate(request1);

    assertEquals("POST", request1.getMethod().toString());
    assertEquals("/blah", request1.getUri().getPath().getTextArray(0));
    assertFalse(request1.getUri().isSetQueryString());
    assertEquals("POST blah", request1.getDescription());
    assertEquals(0, request1.getHeaders().getHeaderArray().length);
    assertEquals(0, request1.getHeaders().getAuthorizationArray().length);
    assertEquals("http-data-0.dat", request1.getBody().getFile());

    // Test with a file to which we can't write.
    final File f = new File(getDirectory(), "http-data-1.dat");
    f.createNewFile();
    FileUtilities.setCanAccess(f, false);

    handler.handleRequest(buffer1, buffer1.length);
    m_httpRecordingStubFactory.assertSuccess("getLastResponseTime");
    m_httpRecordingStubFactory.assertNoMoreCalls();
    handler.handleRequest(buffer2, buffer2.length);
    handler.handleResponse(responseBuffer, responseBuffer.length);
    m_httpRecordingStubFactory.assertSuccess("markLastResponseTime");
    handler.newRequestMessage();

    AssertUtilities.assertContains(
      (String)
      m_loggerStubFactory.assertSuccess("error", String.class).getParameters()[0],
      "Failed to write body data");
    m_loggerStubFactory.assertSuccess("getErrorLogWriter");

    final Object[] parameters2 =
      m_httpRecordingStubFactory.assertSuccess("addRequest",
        ConnectionDetails.class, RequestType.class).getParameters();
    assertSame(m_connectionDetails, parameters2[0]);

    final RequestType request2 = (RequestType)parameters2[1];
    XMLBeansUtilities.validate(request2);

    assertEquals("POST", request2.getMethod().toString());
    assertEquals("/blah", request2.getUri().getPath().getTextArray(0));
    assertFalse(request2.getUri().isSetQueryString());
    assertEquals("POST blah", request2.getDescription());
    assertEquals(0, request2.getHeaders().getHeaderArray().length);
    assertEquals(0, request2.getHeaders().getAuthorizationArray().length);
    assertEquals("http-data-1.dat", request2.getBody().getFile());

    m_httpRecordingStubFactory.assertNoMoreCalls();
    m_loggerStubFactory.assertNoMoreCalls();
  }

  public void testRequestWIthBadMessages() throws Exception {
    final ConnectionHandler handler =
      new ConnectionHandlerImplementation(
        m_httpRecording, m_loggerStubFactory.getLogger(), m_regularExpressions,
        m_connectionDetails);

    handler.handleRequest(new byte[0], 0);
    m_loggerStubFactory.assertSuccess("error", String.class);

    final String message = "   GET blah HTTP/1.1";
    final byte[] buffer = message.getBytes();

    handler.handleRequest(buffer, buffer.length);
    m_loggerStubFactory.assertSuccess("error", String.class);

    handler.newRequestMessage();

    m_httpRecordingStubFactory.assertNoMoreCalls();
    m_loggerStubFactory.assertNoMoreCalls();

  }
}
