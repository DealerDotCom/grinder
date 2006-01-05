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

import java.io.IOException;
import java.util.Calendar;

import net.grinder.common.LoggerStubFactory;
import net.grinder.plugin.http.xml.BasicAuthorizationHeaderType;
import net.grinder.plugin.http.xml.CommonHeadersType;
import net.grinder.plugin.http.xml.HTTPRecordingType;
import net.grinder.plugin.http.xml.HeaderType;
import net.grinder.plugin.http.xml.HeadersType;
import net.grinder.plugin.http.xml.HttpRecordingDocument;
import net.grinder.plugin.http.xml.PageType;
import net.grinder.plugin.http.xml.RequestType;
import net.grinder.plugin.http.xml.HTTPRecordingType.Metadata;
import net.grinder.testutility.RandomStubFactory;
import net.grinder.testutility.XMLBeansUtilities;
import net.grinder.tools.tcpproxy.ConnectionDetails;
import net.grinder.tools.tcpproxy.EndPoint;

import junit.framework.TestCase;


/**
 * Unit tests for {@link HTTPRecordingImplementation}.
 *
 * @author Philip Aston
 * @version $Revision$
 */
public class TestHTTPRecordingImplementation extends TestCase {

  private final RandomStubFactory m_resultProcessorStubFactory =
    new RandomStubFactory(HTTPRecordingResultProcessor.class);
  private final HTTPRecordingResultProcessor m_resultProcessor =
    (HTTPRecordingResultProcessor)m_resultProcessorStubFactory.getStub();

  public void testConstructorAndDispose() throws Exception {
    final LoggerStubFactory loggerStubFactory = new LoggerStubFactory();

    final HTTPRecordingImplementation httpRecording =
      new HTTPRecordingImplementation(m_resultProcessor,
                                      loggerStubFactory.getLogger());

    m_resultProcessorStubFactory.assertNoMoreCalls();

    httpRecording.dispose();

    final HttpRecordingDocument recording =
      (HttpRecordingDocument)
      m_resultProcessorStubFactory.assertSuccess("process",
      HttpRecordingDocument.class).getParameters()[0];

    XMLBeansUtilities.validate(recording);

    httpRecording.dispose();

    final HttpRecordingDocument recording2 =
      (HttpRecordingDocument)
      m_resultProcessorStubFactory.assertSuccess("process",
      HttpRecordingDocument.class).getParameters()[0];

    XMLBeansUtilities.validate(recording2);

    assertNotSame("We get a copy", recording, recording2);

    final Metadata metadata = recording.getHttpRecording().getMetadata();
    assertTrue(metadata.getVersion().length() > 0);
    assertNotNull(metadata.getTime());
    assertEquals(0, recording.getHttpRecording().getCommonHeadersArray().length);
    assertEquals(0, recording.getHttpRecording().getBaseUrlArray().length);
    assertEquals(0, recording.getHttpRecording().getPageArray().length);
    m_resultProcessorStubFactory.assertNoMoreCalls();

    final IOException exception = new IOException("Eat me");
    m_resultProcessorStubFactory.setThrows("process", exception);

    httpRecording.dispose();

    m_resultProcessorStubFactory.assertException(
      "process",
      new Class[] { HttpRecordingDocument.class},
      exception);
    loggerStubFactory.assertSuccess("error", exception.getMessage());
    loggerStubFactory.assertSuccess("getErrorLogWriter");
    loggerStubFactory.assertNoMoreCalls();

    m_resultProcessorStubFactory.assertNoMoreCalls();
  }

  public void testMarkLastResponseTime() throws Exception {
    final HTTPRecordingImplementation httpRecording =
      new HTTPRecordingImplementation(m_resultProcessor, null);

    final long before = System.currentTimeMillis();
    httpRecording.markLastResponseTime();
    final long after = System.currentTimeMillis();
    final long value = httpRecording.getLastResponseTime();
    assertTrue(before >= value);
    assertTrue(value <= after);
  }

  private RequestType createRequest(String path) {
    final RequestType result = RequestType.Factory.newInstance();
    result.setMethod(RequestType.Method.Enum.forString("GET"));
    result.addNewUrl().setPath(path);
    result.addNewHeaders();
    result.setTime(Calendar.getInstance());
    result.setDescription("GET " + path);

    XMLBeansUtilities.validate(result);

    return result;
  }

  public void testAddRequest() throws Exception {
    final HTTPRecordingImplementation httpRecording =
      new HTTPRecordingImplementation(m_resultProcessor, null);

    final EndPoint endPoint1 = new EndPoint("hostA", 80);
    final EndPoint endPoint2 = new EndPoint("hostB", 80);
    final EndPoint endPoint3 = new EndPoint("hostC", 80);

    // Request 1
    final RequestType request1 = createRequest("/foo");
    final ConnectionDetails connectionDetails1 =
      new ConnectionDetails(endPoint1, endPoint2, false);

    httpRecording.addRequest(connectionDetails1, request1);

    // Request 2
    final RequestType request2 = createRequest("/foo");
    final ConnectionDetails connectionDetails2 =
      new ConnectionDetails(endPoint1, endPoint2, true);

    httpRecording.addRequest(connectionDetails2, request2);

    // Request 3
    final RequestType request3 = createRequest("bah.gif");
    final ConnectionDetails connectionDetails3 =
      new ConnectionDetails(endPoint3, endPoint2, false);

    httpRecording.addRequest(connectionDetails3, request3);

    httpRecording.dispose();

    final HttpRecordingDocument recording =
      (HttpRecordingDocument)
      m_resultProcessorStubFactory.assertSuccess("process",
      HttpRecordingDocument.class).getParameters()[0];

    XMLBeansUtilities.validate(recording);

    m_resultProcessorStubFactory.assertNoMoreCalls();

    final HTTPRecordingType result = recording.getHttpRecording();
    assertEquals(1, result.getCommonHeadersArray().length);

    assertEquals(2, result.getBaseUrlArray().length);
    assertEquals("hostb", result.getBaseUrlArray(0).getHost().toString());
    assertEquals("https", result.getBaseUrlArray(1).getScheme().toString());

    assertEquals(2, result.getPageArray().length);

    final PageType page0 = result.getPageArray(0);
    assertEquals(2, page0.getRequestArray().length);
    assertEquals(result.getBaseUrlArray(0).getUrlId(),
                 page0.getRequestArray(1).getUrl().getExtends());
    assertEquals("bah.gif", page0.getRequestArray(1).getUrl().getPath());

    final PageType page1 = result.getPageArray(1);
    assertEquals(1, page1.getRequestArray().length);
    assertEquals(0, page1.getRequestArray(0).getHeaders().sizeOfHeaderArray());
  }

  private void addHeader(RequestType request, String name, String value) {
    final HeaderType header = request.getHeaders().addNewHeader();
    header.setName(name);
    header.setValue(value);
  }

  public void testAddRequestWithHeaders() throws Exception {
    final HTTPRecordingImplementation httpRecording =
      new HTTPRecordingImplementation(m_resultProcessor, null);

    final EndPoint endPoint1 = new EndPoint("hostA", 80);
    final EndPoint endPoint2 = new EndPoint("hostB", 80);

    // Request 1
    final RequestType request1 = createRequest("/foo");
    addHeader(request1, "foo", "bah");
    addHeader(request1, "User-Agent", "x");
    addHeader(request1, "Accept", "*");
    final ConnectionDetails connectionDetails1 =
      new ConnectionDetails(endPoint1, endPoint2, false);

    httpRecording.addRequest(connectionDetails1, request1);

    // Request 2
    final RequestType request2 = createRequest("/foo");
    addHeader(request2, "User-Agent", "y");
    addHeader(request2, "Accept", "*");

    final BasicAuthorizationHeaderType basic =
      request2.getHeaders().addNewAuthorization().addNewBasic();
    basic.setUserid("user");
    basic.setPassword("S3cret");

    httpRecording.addRequest(connectionDetails1, request2);

    // Request 3 - shares common headers with request 2
    final RequestType request3 = createRequest("/foo");
    addHeader(request3, "User-Agent", "y");
    addHeader(request3, "Accept", "*");

    httpRecording.addRequest(connectionDetails1, request3);

    // Request 4
    final RequestType request4 = createRequest("/foo");
    addHeader(request4, "User-Agent", "y");
    addHeader(request4, "Accept-Language", "en-gb");
    addHeader(request4, "Accept", "*");

    httpRecording.addRequest(connectionDetails1, request4);

    httpRecording.dispose();

    final HttpRecordingDocument recording =
      (HttpRecordingDocument)
      m_resultProcessorStubFactory.assertSuccess("process",
      HttpRecordingDocument.class).getParameters()[0];

    final CommonHeadersType[] commonHeaders =
      recording.getHttpRecording().getCommonHeadersArray();

    assertEquals(4, commonHeaders.length);

    assertEquals("defaultHeaders", commonHeaders[0].getHeadersId());
    assertFalse(commonHeaders[0].isSetExtends());
    assertEquals(1, commonHeaders[0].getHeaderArray().length);
    assertEquals("Accept", commonHeaders[0].getHeaderArray(0).getName());
    assertEquals(0, commonHeaders[0].getAuthorizationArray().length);

    assertEquals("defaultHeaders", commonHeaders[1].getExtends());
    assertEquals(1, commonHeaders[1].getHeaderArray().length);
    assertEquals(0, commonHeaders[1].getAuthorizationArray().length);
    assertEquals("User-Agent", commonHeaders[1].getHeaderArray(0).getName());
    assertEquals("x", commonHeaders[1].getHeaderArray(0).getValue());

    assertEquals("defaultHeaders", commonHeaders[2].getExtends());
    assertEquals(1, commonHeaders[2].getHeaderArray().length);
    assertEquals(0, commonHeaders[2].getAuthorizationArray().length);
    assertEquals("User-Agent", commonHeaders[2].getHeaderArray(0).getName());
    assertEquals("y", commonHeaders[2].getHeaderArray(0).getValue());

    assertEquals("defaultHeaders", commonHeaders[3].getExtends());
    assertEquals(2, commonHeaders[3].getHeaderArray().length);
    assertEquals(0, commonHeaders[2].getAuthorizationArray().length);

    final HeadersType request1Headers =
      recording.getHttpRecording().getPageArray(0).getRequestArray(0).getHeaders();
    assertEquals(commonHeaders[1].getHeadersId(), request1Headers.getExtends());
    assertEquals(1, request1Headers.getHeaderArray().length);
    assertEquals("foo", request1Headers.getHeaderArray(0).getName());
    assertEquals("bah", request1Headers.getHeaderArray(0).getValue());
    assertEquals(0, request1Headers.getAuthorizationArray().length);

    final HeadersType request2Headers =
      recording.getHttpRecording().getPageArray(1).getRequestArray(0).getHeaders();
    assertEquals(commonHeaders[2].getHeadersId(), request2Headers.getExtends());
    assertEquals(0, request2Headers.getHeaderArray().length);
    assertEquals(1, request2Headers.getAuthorizationArray().length);
    assertEquals("user", request2Headers.getAuthorizationArray(0).getBasic().getUserid());
    assertEquals("S3cret", request2Headers.getAuthorizationArray(0).getBasic().getPassword());

    final HeadersType request3Headers =
      recording.getHttpRecording().getPageArray(2).getRequestArray(0).getHeaders();
    assertEquals(commonHeaders[2].getHeadersId(), request3Headers.getExtends());
    assertEquals(0, request3Headers.getHeaderArray().length);
    assertEquals(0, request3Headers.getAuthorizationArray().length);

    final HeadersType request4Headers =
      recording.getHttpRecording().getPageArray(3).getRequestArray(0).getHeaders();
    assertEquals(commonHeaders[3].getHeadersId(), request4Headers.getExtends());
    assertEquals(0, request4Headers.getHeaderArray().length);
    assertEquals(0, request4Headers.getAuthorizationArray().length);
  }
}
