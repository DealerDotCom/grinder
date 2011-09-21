// Copyright (C) 2011 Philip Aston
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
// COPYRIGHT HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
// INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
// (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
// SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
// HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
// STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
// ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
// OF THE POSSIBILITY OF SUCH DAMAGE.

package net.grinder;

import static net.grinder.testutility.AssertUtilities.assertContains;
import static net.grinder.testutility.AssertUtilities.assertContainsPattern;
import junit.framework.TestCase;
import net.grinder.common.Logger;
import net.grinder.common.LoggerStubFactory;
import net.grinder.plugin.http.tcpproxyfilter.HTTPRequestFilter;
import net.grinder.plugin.http.tcpproxyfilter.HTTPResponseFilter;
import net.grinder.tools.tcpproxy.EchoFilter;
import net.grinder.tools.tcpproxy.UpdatableCommentSource;

import org.picocontainer.PicoContainer;


/**
 * Unit tests for {@link TestTCPProxy}.
 *
 * @author Philip Aston
 * @version $Revision:$
 */
public class TestTCPProxy extends TestCase {

  private final LoggerStubFactory m_loggerStubFactory = new LoggerStubFactory();
  private final Logger m_logger = m_loggerStubFactory.getLogger();

  public void setUp() {
    m_loggerStubFactory.setIgnoreCallOrder(true);
    m_loggerStubFactory.setIgnoreMethod("getOutputLogWriter");
    m_loggerStubFactory.setIgnoreMethod("getErrorLogWriter");
  }

  public void testDefaultOptions() throws Exception {

    final TCPProxy tcpProxy = new TCPProxy(new String[0], m_logger);

    final String message =
      m_loggerStubFactory.assertSuccess("error", String.class)
      .getParameters()[0].toString();
    assertContains(message, "HTTP/HTTPS proxy");
    assertContainsPattern(message, "Request filters:\\s+EchoFilter\\s*\n");
    assertContainsPattern(message, "Response filters:\\s+EchoFilter\\s*\n");

    m_loggerStubFactory.assertErrorMessageContains("listening on port 8001");
    m_loggerStubFactory.assertNoMoreCalls();

    final PicoContainer filterContainer = tcpProxy.getFilterContainer();

    assertNotNull(filterContainer.getComponent(UpdatableCommentSource.class));
    assertNotNull(filterContainer.getComponent(Logger.class));
    assertEquals(2, filterContainer.getComponents(EchoFilter.class).size());
    assertEquals(4, filterContainer.getComponents().size());
  }

  public void testHTTP() throws Exception {

    final String[] arguments = { "-http", };

    final TCPProxy tcpProxy = new TCPProxy(arguments, m_logger);

    final String message =
      m_loggerStubFactory.assertSuccess("error", String.class)
      .getParameters()[0].toString();
    assertContains(message, "HTTP/HTTPS proxy");
    assertContainsPattern(message,
                          "Request filters:\\s+HTTPRequestFilter\\s*\n");
    assertContainsPattern(message,
                          "Response filters:\\s+HTTPResponseFilter\\s*\n");

    m_loggerStubFactory.assertErrorMessageContains("listening on port 8001");
    m_loggerStubFactory.assertNoMoreCalls();

    final PicoContainer filterContainer = tcpProxy.getFilterContainer();

    assertNotNull(filterContainer.getComponent(UpdatableCommentSource.class));
    assertNotNull(filterContainer.getComponent(Logger.class));
    assertNotNull(filterContainer.getComponent(HTTPRequestFilter.class));
    assertNotNull(filterContainer.getComponent(HTTPResponseFilter.class));
  }

  public void testInvalidFilter() throws Exception {
    final String[] arguments = { "-responsefilter", "XXX" };

    try {
      new TCPProxy(arguments, m_logger);
      fail("Expected exception");
    }
    catch (Exception e) {
    }

    m_loggerStubFactory.assertErrorMessageContains("'XXX' not found");
    m_loggerStubFactory.assertNoMoreCalls();
  }

  public void testInvalidFilter2() throws Exception {
    final String[] arguments = { "-requestfilter", "java.lang.Object" };

    try {
      new TCPProxy(arguments, m_logger);
      fail("Expected exception");
    }
    catch (Exception e) {
    }

    m_loggerStubFactory.assertErrorMessageContains("does not implement");
    m_loggerStubFactory.assertNoMoreCalls();
  }
}
