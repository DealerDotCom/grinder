// Copyright (C) 2000, 2001, 2002, 2003, 2004 Philip Aston
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

package net.grinder.plugin.http;

import junit.framework.TestCase;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

import HTTPClient.HTTPResponse;
import HTTPClient.NVPair;

import net.grinder.plugininterface.PluginProcessContext;
import net.grinder.plugininterface.PluginThreadContext;
import net.grinder.plugininterface.PluginThreadListener;
import net.grinder.script.Grinder.ScriptContext;
import net.grinder.script.Statistics;
import net.grinder.testutility.RandomStubFactory;


/**
 * Unit test case for <code>HTTPRequest</code>.
 *
 * @author Philip Aston
 * @version $Revision$
 */
public class TestHTTPRequest extends TestCase {

  public void testSetHeaders() {
    final HTTPRequest httpRequest = new HTTPRequest();

    assertEquals(0, httpRequest.getHeaders().length);

    final NVPair[] newHeaders = new NVPair[] {
      new NVPair("name", "value"),
      new NVPair("another name", "another value"),
    };
    
    httpRequest.setHeaders(newHeaders);
    assertArraysEqual(newHeaders, httpRequest.getHeaders());
  }

  public void testAddHeader() {

    final HTTPRequest httpRequest = new HTTPRequest();

    final NVPair[] newHeaders = new NVPair[] {
      new NVPair("name", "value"),
      new NVPair("another name", "another value"),
    };

    httpRequest.setHeaders(newHeaders);
    
    httpRequest.addHeader("name", "value");
    httpRequest.addHeader("foo", "bah");

    assertArraysEqual(new NVPair[] {
                        new NVPair("name", "value"),
                        new NVPair("another name", "another value"),
                        new NVPair("name", "value"),
                        new NVPair("foo", "bah"),
                      },
                      httpRequest.getHeaders());
  }

  public void testDeleteHeader() {

    final HTTPRequest httpRequest = new HTTPRequest();

    final NVPair[] newHeaders = new NVPair[] {
      new NVPair("name", "value"),
      new NVPair("another name", "another value"),
      new NVPair("name", "value"),
      new NVPair("some more stuff", "value"),
    };

    httpRequest.setHeaders(newHeaders);
    
    httpRequest.deleteHeader("name");

    assertArraysEqual(new NVPair[] {
                        new NVPair("another name", "another value"),
                        new NVPair("some more stuff", "value"),
                      },
                      httpRequest.getHeaders());

    httpRequest.deleteHeader("some more stuff");

    assertArraysEqual(new NVPair[] {
                        new NVPair("another name", "another value"),
                      },
                      httpRequest.getHeaders());
  }

  private static void assertArraysEqual(NVPair[] a, NVPair[] b) {
    
    assertTrue("Arrays of equal length", a.length == b.length);

    for (int i=0; i<a.length; ++i) {
      assertEquals("NVPair " + i + " name matches", 
                   a[i].getName(), b[i].getName());
      assertEquals("NVPair " + i + " value matches",
                   a[i].getValue(), b[i].getValue());
    }
  }

  /**
   * Active class that accepts a connection on a socket, reads an HTTP
   * request, and returns a response who's body is the text of the
   * request.
   */
  private final class EchoHTTPRequest implements Runnable {

    private final ServerSocket m_serverSocket;

    public EchoHTTPRequest() throws Exception {
      m_serverSocket = new ServerSocket(0);

      new Thread(this, getClass().getName()).start();
    }

    public String getServerAndPort() {
      return "localhost:" + m_serverSocket.getLocalPort();
    }

    public void run() {
      try {
        final Socket localSocket = m_serverSocket.accept();

        final InputStream in = localSocket.getInputStream();
        final OutputStream out = localSocket.getOutputStream();

        final StringBuffer response = new StringBuffer();
        response.append("HTTP/1.0 200 OK\r\n");
        response.append("\r\n");
        out.write(response.toString().getBytes());

        final byte[] buffer = new byte[1000];
        int n;

        READ_REQUEST:
        while ((n = in.read(buffer, 0, buffer.length)) != -1) {
          out.write(buffer, 0, n);

          // Can't be bothered to parse Content-Length header. For
          // now, quit when header boundary is read.
          for (int i=0; i<n-3; ++i) {
            if (buffer[i] == '\r' &&
                buffer[i+1] == '\n' &&
                buffer[i+2] == '\r' &&
                buffer[i+3] == '\n') {
              break READ_REQUEST;
            }
          }
        }

        out.flush();

        localSocket.close();
        m_serverSocket.close();
      }
      catch (Exception e) {
        System.err.println(e);
      }
    }
  }

  public static class StatisticsStubFactory extends RandomStubFactory {

    public StatisticsStubFactory() {
      super(Statistics.class);
    }

    public final Statistics getStatistics() {
      return (Statistics)getStub();
    }

    public final boolean override_availableForUpdate(Object proxy) {
      return false;
    }
  }

  public static class ScriptContextStubFactory
    extends RandomStubFactory {

    private final Statistics m_statistics;

    public ScriptContextStubFactory(Statistics statistics) {
      super(ScriptContext.class);
      m_statistics = statistics;
    }

    public final ScriptContext getScriptContext() {
      return (ScriptContext)getStub();
    }

    public final Statistics override_getStatistics(Object proxy) {
      return m_statistics;
    }
  }

  public static class PluginProcessContextStubFactory
    extends RandomStubFactory {

    private final HTTPPluginThreadState m_threadState;
    private final ScriptContext m_scriptContext;

    PluginProcessContextStubFactory(HTTPPluginThreadState threadState,
                                    ScriptContext scriptContext) {
      super(PluginProcessContext.class);
      m_threadState = threadState;
      m_scriptContext = scriptContext;
    }

    public final PluginProcessContext getPluginProcessContext() {
      return (PluginProcessContext)getStub();
    }

    public PluginThreadListener override_getPluginThreadListener(
      Object proxy) {
      return m_threadState;
    }

    public ScriptContext override_getScriptContext(Object proxy) {
      return m_scriptContext;
    }
  }

  public void testFoo() throws Exception {

    final PluginThreadContext threadContext =
      (PluginThreadContext)
      (new RandomStubFactory(PluginThreadContext.class)).getStub();

    final HTTPPluginThreadState threadState =
      new HTTPPluginThreadState(threadContext);

    final Statistics statistics = new StatisticsStubFactory().getStatistics();

    final ScriptContextStubFactory scriptContextStubFactory =
      new ScriptContextStubFactory(statistics);

    final PluginProcessContextStubFactory pluginProcessContextStubFactory =
      new PluginProcessContextStubFactory(
        threadState,
        scriptContextStubFactory.getScriptContext());

    HTTPPlugin.getPlugin().initialize(
      pluginProcessContextStubFactory.getPluginProcessContext());

    final EchoHTTPRequest echo = new EchoHTTPRequest();

    final HTTPRequest request = new HTTPRequest();

    HTTPResponse response = request.GET("http://" + echo.getServerAndPort());

    System.out.println(response.getText());
  }

}
