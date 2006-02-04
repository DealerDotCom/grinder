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

import net.grinder.common.LoggerStubFactory;
import net.grinder.plugin.http.xml.RequestType;
import net.grinder.plugin.http.xml.TokenType;
import net.grinder.testutility.AbstractFileTestCase;
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

  public void testRequestWithGet1() throws Exception {
    final ConnectionHandler handler =
      new ConnectionHandlerImplementation(
        m_httpRecording, m_loggerStubFactory.getLogger(), m_regularExpressions,
        m_uriParser, m_connectionDetails);

    final RequestType request = RequestType.Factory.newInstance();
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

  public void testRequestWIthBadMessages() throws Exception {
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

    handler.newRequestMessage();

    m_httpRecordingStubFactory.assertNoMoreCalls();
    m_loggerStubFactory.assertNoMoreCalls();

  }
}
