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

import java.io.IOException;

import net.grinder.common.LoggerStubFactory;
import net.grinder.plugin.http.xml.HTTPRecordingType;
import net.grinder.plugin.http.xml.HttpRecordingDocument;
import net.grinder.plugin.http.xml.PageType;
import net.grinder.plugin.http.xml.RequestType;
import net.grinder.plugin.http.xml.HTTPRecordingType.Metadata;
import net.grinder.testutility.RandomStubFactory;
import net.grinder.testutility.XMLBeansUtilities;
import net.grinder.tools.tcpproxy.ConnectionDetails;
import net.grinder.tools.tcpproxy.EndPoint;
import net.grinder.util.URIParser;
import net.grinder.util.URIParserImplementation;

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

  private final RegularExpressions m_regularExpressions =
    new RegularExpressionsImplementation();

  private final URIParser m_uriParser = new URIParserImplementation();

  public void testConstructorAndDispose() throws Exception {
    final LoggerStubFactory loggerStubFactory = new LoggerStubFactory();

    final HTTPRecordingImplementation httpRecording =
      new HTTPRecordingImplementation(m_resultProcessor,
                                      loggerStubFactory.getLogger(),
                                      m_regularExpressions,
                                      m_uriParser);

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
    assertEquals(0, recording.getHttpRecording().getBaseUriArray().length);
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

  public void testAddRequest() throws Exception {
    final HTTPRecordingImplementation httpRecording =
      new HTTPRecordingImplementation(
        m_resultProcessor, null, m_regularExpressions, m_uriParser);

    final EndPoint endPoint1 = new EndPoint("hostA", 80);
    final EndPoint endPoint2 = new EndPoint("hostB", 80);
    final EndPoint endPoint3 = new EndPoint("hostC", 80);

    // Request 1
    final ConnectionDetails connectionDetails1 =
      new ConnectionDetails(endPoint1, endPoint2, false);

    final RequestType request1 =
      httpRecording.addRequest(connectionDetails1, "GET", "/foo");
    assertEquals("/foo", request1.getUri().getUnparsed());
    request1.addNewResponse();

    // Request 2
    final ConnectionDetails connectionDetails2 =
      new ConnectionDetails(endPoint1, endPoint2, false);

    final RequestType request2 =
      httpRecording.addRequest(connectionDetails2, "GET", "/foo.gif");
    request2.addNewResponse();

    // Request 3
    final ConnectionDetails connectionDetails3 =
      new ConnectionDetails(endPoint3, endPoint2, true);

    final RequestType request3 =
      httpRecording.addRequest(connectionDetails3, "GET", "bah.gif");
    assertEquals("bah.gif", request3.getUri().getUnparsed());
    request3.addNewResponse();

    httpRecording.dispose();

    final HttpRecordingDocument recording =
      (HttpRecordingDocument)
      m_resultProcessorStubFactory.assertSuccess("process",
      HttpRecordingDocument.class).getParameters()[0];

    XMLBeansUtilities.validate(recording);

    m_resultProcessorStubFactory.assertNoMoreCalls();

    final HTTPRecordingType result = recording.getHttpRecording();
    assertEquals(0, result.getCommonHeadersArray().length);

    assertEquals(2, result.getBaseUriArray().length);
    assertEquals("hostb", result.getBaseUriArray(0).getHost().toString());
    assertEquals("https", result.getBaseUriArray(1).getScheme().toString());

    assertEquals(2, result.getPageArray().length);

    final PageType page0 = result.getPageArray(0);
    assertEquals(2, page0.getRequestArray().length);
    assertEquals(result.getBaseUriArray(0).getUriId(),
                 page0.getRequestArray(1).getUri().getExtends());
    assertEquals("/foo.gif", page0.getRequestArray(1).getUri().getPath().getTextArray(0));

    final PageType page1 = result.getPageArray(1);
    assertEquals(1, page1.getRequestArray().length);
    assertEquals(0, page1.getRequestArray(0).getHeaders().sizeOfHeaderArray());
  }
}
