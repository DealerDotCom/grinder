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

import net.grinder.testutility.RandomStubFactory;
import net.grinder.tools.tcpproxy.ConnectionDetails;
import net.grinder.tools.tcpproxy.EndPoint;
import junit.framework.TestCase;


/**
 * Unit tests for {@link HTTPResponseFilter}.
 *
 * @author Philip Aston
 * @version $Revision$
 */
public class TestHTTPResponseFilter extends TestCase {

  public void testHTTPResponseFilter() throws Exception {
    final RandomStubFactory httpRecordingStubFactory =
      new RandomStubFactory(HTTPRecording.class);
    final HTTPRecording httpRecording =
      (HTTPRecording) httpRecordingStubFactory.getStub();

    final HTTPResponseFilter filter =
      new HTTPResponseFilter(httpRecording);

    final EndPoint endPoint1 = new EndPoint("hostA", 80);
    final EndPoint endPoint2 = new EndPoint("hostB", 80);
    final ConnectionDetails connectionDetails =
      new ConnectionDetails(endPoint1, endPoint2, false);

    filter.connectionOpened(connectionDetails);
    httpRecordingStubFactory.assertNoMoreCalls();

    final byte[] buffer = new byte[100];
    filter.handle(connectionDetails, buffer, buffer.length);
    httpRecordingStubFactory.assertSuccess("markLastResponseTime");
    httpRecordingStubFactory.assertNoMoreCalls();

    filter.connectionClosed(connectionDetails);
    httpRecordingStubFactory.assertNoMoreCalls();
  }

}
