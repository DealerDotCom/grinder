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
import static net.grinder.testutility.SocketUtilities.findFreePort;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileWriter;
import java.io.Writer;
import java.util.Properties;

import net.grinder.common.GrinderException;
import net.grinder.common.Logger;
import net.grinder.common.LoggerStubFactory;
import net.grinder.plugin.http.tcpproxyfilter.HTTPRequestFilter;
import net.grinder.plugin.http.tcpproxyfilter.HTTPResponseFilter;
import net.grinder.testutility.TemporaryDirectory;
import net.grinder.tools.tcpproxy.EchoFilter;
import net.grinder.tools.tcpproxy.NullFilter;
import net.grinder.tools.tcpproxy.UpdatableCommentSource;

import org.junit.Before;
import org.junit.Test;
import org.picocontainer.PicoContainer;



/**
 * Unit tests for {@link TestTCPProxy}.
 *
 * @author Philip Aston
 */
public class TestTCPProxy {

  private final LoggerStubFactory m_loggerStubFactory = new LoggerStubFactory();
  private final Logger m_logger = m_loggerStubFactory.getLogger();

  @Before public void setUp() {
    m_loggerStubFactory.setIgnoreCallOrder(true);
    m_loggerStubFactory.setIgnoreMethod("getOutputLogWriter");
    m_loggerStubFactory.setIgnoreMethod("getErrorLogWriter");
  }

  @Test public void testDefaultOptions() throws Exception {

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

  @Test public void testHTTP() throws Exception {

    final int port = findFreePort();

    final String[] arguments = { "-http",
                                 "-localPort",
                                 Integer.toString(port) };

    final TCPProxy tcpProxy = new TCPProxy(arguments, m_logger);

    final String message =
      m_loggerStubFactory.assertSuccess("error", String.class)
      .getParameters()[0].toString();
    assertContains(message, "HTTP/HTTPS proxy");
    assertContainsPattern(message,
                          "Request filters:\\s+HTTPRequestFilter\\s*\n");
    assertContainsPattern(message,
                          "Response filters:\\s+HTTPResponseFilter\\s*\n");

    m_loggerStubFactory.assertErrorMessageContains("listening on port " + port);
    m_loggerStubFactory.assertNoMoreCalls();

    final PicoContainer filterContainer = tcpProxy.getFilterContainer();

    assertNotNull(filterContainer.getComponent(UpdatableCommentSource.class));
    assertNotNull(filterContainer.getComponent(Logger.class));
    assertNotNull(filterContainer.getComponent(HTTPRequestFilter.class));
    assertNotNull(filterContainer.getComponent(HTTPResponseFilter.class));
  }

  @Test public void testInvalidFilter() throws Exception {
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

  @Test public void testInvalidFilter2() throws Exception {
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

  public static class TestFilter extends NullFilter {}

  @Test public void testCustomFilter() throws Exception {
    final String[] arguments = { "-localPort",
                                 Integer.toString(findFreePort()),
                                 "-requestFilter",
                                 TestFilter.class.getName() };

    final TCPProxy tcpProxy = new TCPProxy(arguments, m_logger);
    assertNotNull(tcpProxy.getFilterContainer().getComponent(TestFilter.class));

    final String message =
      m_loggerStubFactory.assertSuccess("error", String.class)
      .getParameters()[0].toString();
    assertContainsPattern(message, "Request filters:\\s+TestFilter\\s*\n");
  }

  @Test public void testNoneFilter() throws Exception {

    final String[] arguments = { "-localPort",
                                 Integer.toString(findFreePort()),
                                 "-responseFilter",
                                 "NONE" };

    final TCPProxy tcpProxy = new TCPProxy(arguments, m_logger);
    assertNotNull(tcpProxy.getFilterContainer().getComponent(NullFilter.class));

    final String message =
      m_loggerStubFactory.assertSuccess("error", String.class)
      .getParameters()[0].toString();
    assertContainsPattern(message, "Response filters:\\s+NullFilter\\s*\n");
  }

  @Test public void testEchoFilter() throws Exception {

    final String[] arguments = { "-localPort",
                                 Integer.toString(findFreePort()),
                                 "-requestFilter",
                                 "ECHO" };

    final TCPProxy tcpProxy = new TCPProxy(arguments, m_logger);
    final PicoContainer filterContainer = tcpProxy.getFilterContainer();
    assertEquals(2, filterContainer.getComponents(EchoFilter.class).size());

    final String message =
      m_loggerStubFactory.assertSuccess("error", String.class)
      .getParameters()[0].toString();
    assertContainsPattern(message, "Request filters:\\s+EchoFilter\\s*\n");
  }

  @Test public void testChainedFilter() throws Exception {
    final String[] arguments = { "-localPort",
                                 Integer.toString(findFreePort()),
                                 "-requestFilter",
                                 TestFilter.class.getName(),
                                 "-requestFilter",
                                 "ECHO" };

    final TCPProxy tcpProxy = new TCPProxy(arguments, m_logger);
    assertNotNull(tcpProxy.getFilterContainer().getComponent(TestFilter.class));

    final String message =
      m_loggerStubFactory.assertSuccess("error", String.class)
      .getParameters()[0].toString();
    assertContainsPattern(message,
                          "Request filters:\\s+TestFilter" +
                          "\\s*,\\s*EchoFilter" +
                          "\\s*\n");
  }

  @Test public void testProperties() throws Exception {
    final TemporaryDirectory directory = new TemporaryDirectory();

    Writer out = null;

    try {
      assertNull(System.getProperty("myproperty"));

      final Properties properties = new Properties();
      properties.setProperty("myproperty", "myvalue");

      final File propertiesFile = directory.newFile("properties");
      out = new FileWriter(propertiesFile);
      properties.store(out, "");

      final String[] arguments = { "-localPort",
                                   Integer.toString(findFreePort()),
                                   "-properties",
                                   propertiesFile.getAbsolutePath() };

      new TCPProxy(arguments, m_logger);

      assertEquals("myvalue", System.getProperty("myproperty"));
    }
    finally {
      if (out != null) {
        out.close();
      }

      directory.delete();
      System.getProperties().remove("myproperty");
    }
  }

  @Test public void testBadOption() throws Exception {

    final String[] arguments = { "-http",
                                 "-foobar" };

    try {
      new TCPProxy(arguments, m_logger);
      fail("Expected exception");
    }
    catch (GrinderException e) {
    }

    m_loggerStubFactory.assertErrorMessageContains("Usage");
    m_loggerStubFactory.assertNoMoreCalls();
  }

  @Test public void testBadOption2() throws Exception {

    final String[] arguments = { "-storetype" };

    try {
      new TCPProxy(arguments, m_logger);
      fail("Expected exception");
    }
    catch (GrinderException e) {
    }

    m_loggerStubFactory.assertErrorMessageContains("Usage");
    m_loggerStubFactory.assertNoMoreCalls();
  }

  @Test public void testBadOption3() throws Exception {

    final String[] arguments = { "-properties", "nonexistent" };

    try {
      new TCPProxy(arguments, m_logger);
      fail("Expected exception");
    }
    catch (GrinderException e) {
    }

    m_loggerStubFactory.assertErrorMessageContains("No such file or directory");
    m_loggerStubFactory.assertNoMoreCalls();
  }

  @Test public void testBadOption4() throws Exception {

    final String[] arguments = { "-timeout", "blah" };

    try {
      new TCPProxy(arguments, m_logger);
      fail("Expected exception");
    }
    catch (GrinderException e) {
    }

    m_loggerStubFactory.assertErrorMessageContains("Usage");
    m_loggerStubFactory.assertNoMoreCalls();
  }

  @Test public void testBadOption5() throws Exception {

    final String[] arguments = { "-timeout", "-10" };

    try {
      new TCPProxy(arguments, m_logger);
      fail("Expected exception");
    }
    catch (GrinderException e) {
    }

    m_loggerStubFactory.assertErrorMessageContains("must be non-negative");
    m_loggerStubFactory.assertNoMoreCalls();
  }

  @Test public void testPortForwardingArguments() throws Exception {

    final int port = findFreePort();

    final String[] arguments = { "-localHost", "localhost",
                                 "-localPort", "" + port,
                                 "-remoteHost", "r",
                                 "-remotePort", "1234" };

    new TCPProxy(arguments, m_logger);

    final String message =
      m_loggerStubFactory.assertSuccess("error", String.class)
      .getParameters()[0].toString();
    assertContains(message, "TCP port forwarder");
    assertContainsPattern(message, "Remote address:\\s+r:1234");

    m_loggerStubFactory.assertErrorMessageContains("listening on port " + port);

    m_loggerStubFactory.assertNoMoreCalls();
  }

  @Test public void testBadComponent() throws Exception {

    final String[] arguments = { "-component",
                                 "***abc" };

    try {
      new TCPProxy(arguments, m_logger);
      fail("Expected exception");
    }
    catch (GrinderException e) {
    }

    m_loggerStubFactory.assertErrorMessageContains("class '***abc' not found");
    m_loggerStubFactory.assertNoMoreCalls();
  }

  public static class TestClass { }

  @Test public void testGoodComponent() throws Exception {

    final String[] arguments = { "-component",
                                 TestClass.class.getName(),
                                 "-localPort",
                                 Integer.toString(findFreePort()),};

    final TCPProxy tcpProxy = new TCPProxy(arguments, m_logger);
    assertNotNull(tcpProxy.getFilterContainer().getComponent(TestClass.class));
  }
}
