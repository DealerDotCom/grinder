// Copyright (C) 2000, 2001, 2002, 2003 Philip Aston
// Copyright (C) 2000, 2001 Phil Dawes
// Copyright (C) 2001 Paddy Spencer
// Copyright (C) 2003 Bertrande Ave
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

package net.grinder.tools.tcpproxy;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.oro.text.regex.MalformedPatternException;
import org.apache.oro.text.regex.MatchResult;
import org.apache.oro.text.regex.Pattern;
import org.apache.oro.text.regex.PatternCompiler;
import org.apache.oro.text.regex.PatternMatcher;
import org.apache.oro.text.regex.Perl5Compiler;
import org.apache.oro.text.regex.Perl5Matcher;
import org.apache.oro.text.regex.Perl5Substitution;
import org.apache.oro.text.regex.Util;

import net.grinder.common.GrinderBuild;
import net.grinder.util.CopyStreamRunnable;



/**
 * HTTP/HTTPS proxy implementation.
 *
 * <p>A HTTPS proxy client first send a CONNECT message to the proxy
 * port. The proxy accepts the connection responds with a 200 OK,
 * which is the client's queue to send SSL data to the proxy. The
 * proxy just forwards it on to the server identified by the CONNECT
 * message.</p>
 *
 * <p>The Java API presents a particular challenge: it allows sockets
 * to be either SSL or not SSL, but doesn't let them change their
 * stripes midstream. (In fact, if the JSSE support was stream
 * oriented rather than socket oriented, a lot of problems would go
 * away). To hack around this, we accept the CONNECT then blindly
 * proxy the rest of the stream through a special
 * TCPProxyEngineImplementation which instantiated to handle SSL.</p>
 *
 * @author Paddy Spencer
 * @author Philip Aston
 * @author Bertrand Ave
 * @version $Revision$
 */
public final class HTTPProxyTCPProxyEngine extends AbstractTCPProxyEngine {

  private final PatternMatcher m_matcher = new Perl5Matcher();
  private final Pattern m_httpsConnectPattern;

  private final ProxySSLEngine m_proxySSLEngine;

  /**
   * Static so it can be used by {@link
   * StripAbsoluteURIFilterDecorator} as well.
   */
  private static Pattern s_httpConnectPattern;

  private static synchronized Pattern getHTTPConnectPattern()
    throws MalformedPatternException {

    if (s_httpConnectPattern == null) {
      final PatternCompiler compiler = new Perl5Compiler();

      s_httpConnectPattern = compiler.compile(
        "^([A-Z]+)[ \\t]+http://([^/:]+):?(\\d*)(/.*)",
        Perl5Compiler.MULTILINE_MASK  | Perl5Compiler.READ_ONLY_MASK);
    }

    return s_httpConnectPattern;
  }

  /**
   * Constructor.
   *
   * @param plainSocketFactory Factory for plain old sockets.
   * @param sslSocketFactory Factory for SSL sockets.
   * @param requestFilter Request filter.
   * @param responseFilter Response filter.
   * @param outputWriter Writer to terminal.
   * @param localEndPoint Local host and port.
   * @param useColour Whether to use colour.
   * @param timeout Timeout in milliseconds.
   *
   * @exception IOException If an I/O error occurs
   * @exception MalformedPatternException If a regular expression
   * error occurs.
   */
  public HTTPProxyTCPProxyEngine(TCPProxyPlainSocketFactory plainSocketFactory,
                                 TCPProxySocketFactory sslSocketFactory,
                                 TCPProxyFilter requestFilter,
                                 TCPProxyFilter responseFilter,
                                 PrintWriter outputWriter,
                                 EndPoint localEndPoint,
                                 boolean useColour,
                                 int timeout)
    throws IOException, MalformedPatternException {

    // We set this engine up for handling plain HTTP and delegate
    // to a proxy for HTTPS.
    super(plainSocketFactory,
          new StripAbsoluteURIFilterDecorator(requestFilter),
          responseFilter,
          outputWriter,
          localEndPoint,
          useColour,
          timeout);

    final PatternCompiler compiler = new Perl5Compiler();

    m_httpsConnectPattern = compiler.compile(
      "^CONNECT[ \\t]+([^:]+):(\\d+)",
      Perl5Compiler.MULTILINE_MASK | Perl5Compiler.READ_ONLY_MASK);

    // When handling HTTPS proxies, we use our plain socket to accept
    // connections on. We suck the bit we understand off the front and
    // forward the rest through our proxy engine. The proxy engine
    // listens for connection attempts (which come from us), then sets
    // up a thread pair which pushes data back and forth until either
    // the server closes the connection, or we do (in response to our
    // client closing the connection). The engine handles multiple
    // connections by spawning multiple thread pairs.
    if (sslSocketFactory != null) {
      m_proxySSLEngine =
        new ProxySSLEngine(sslSocketFactory, requestFilter,
                           responseFilter, outputWriter, useColour);
      new Thread(m_proxySSLEngine, "HTTPS proxy SSL engine").start();
    }
    else {
      m_proxySSLEngine = null;
    }
  }

  /**
   * Main event loop.
   */
  public void run() {

    // Should be more than adequate.
    final byte[] buffer = new byte[4096];

    try {
      while (true) {
        final Socket localSocket = getServerSocket().accept();

        // Grab the first upstream packet and grep it for the
        // remote server and port in the method line.
        final BufferedInputStream in =
          new BufferedInputStream(localSocket.getInputStream(), buffer.length);

        in.mark(buffer.length);

        // Read a buffer full.
        final int bytesRead = in.read(buffer);

        final String line =
          bytesRead > 0 ? new String(buffer, 0, bytesRead, "US-ASCII") : "";

        if (m_matcher.contains(line, getHTTPConnectPattern())) {
          // HTTP proxy request.

          // Reset stream to beginning of request.
          in.reset();

          new Thread(
            new HTTPProxyStreamDemultiplexer(in, localSocket),
            "HTTPProxyStreamDemultiplexer for " + localSocket).start();
        }
        else if (m_matcher.contains(line, m_httpsConnectPattern)) {
          // HTTPS proxy request.

          // Discard anything else the client has to say.
          while (in.read(buffer, 0, in.available()) > 0) {
            // Skip..
          }

          final MatchResult match = m_matcher.getMatch();

          // Match.group(2) must be a port number by specification.
          final EndPoint remoteEndPoint =
            new EndPoint(match.group(1), Integer.parseInt(match.group(2)));

          // System.err.println("New HTTPS proxy connection to " +
          // remoteEndPoint);

          if (m_proxySSLEngine == null) {
            System.err.println("Specify -ssl for HTTPS proxy support");
            continue;
          }

          // Set our proxy engine up to create connections to the
          // remoteEndPoint.
          m_proxySSLEngine.proxyTo(remoteEndPoint);

          // Create a new proxy connection to the proxy engine.
          final Socket sslProxySocket =
            getSocketFactory().createClientSocket(
              new EndPoint(getLocalHost(),
                           m_proxySSLEngine.getServerSocket().getLocalPort()));

          // Now set up a couple of threads to punt
          // everything we receive over localSocket to
          // sslProxySocket, and vice versa.
          new Thread(
            new CopyStreamRunnable(in, sslProxySocket.getOutputStream()),
            "Copy to proxy engine for " + remoteEndPoint).start();

          final OutputStream out = localSocket.getOutputStream();

          new Thread(
            new CopyStreamRunnable(sslProxySocket.getInputStream(), out),
            "Copy from proxy engine for " + remoteEndPoint).start();

          // Send a 200 response to send to client. Client
          // will now start sending SSL data to localSocket.
          final StringBuffer response = new StringBuffer();
          response.append("HTTP/1. 200 OK\r\n");
          response.append("Host: ");
          response.append(remoteEndPoint);
          response.append("\r\n");
          response.append("Proxy-agent: The Grinder/");
          response.append(GrinderBuild.getVersionString());
          response.append("\r\n");
          response.append("\r\n");

          out.write(response.toString().getBytes());
          out.flush();
        }
        else {
          System.err.println(
            "Failed to determine proxy destination from message:");
          System.err.println(line);
        }
      }
    }
    catch (SocketException e) {
      // Most likely "socket closed" - ignore.
    }
    catch (InterruptedIOException e) {
      System.err.println("Listen time out");
    }
    catch (IOException e) {
      e.printStackTrace(System.err);
    }
    catch (MalformedPatternException e) {
      e.printStackTrace(System.err);
    }
  }

  /**
   * Runnable that actively reads from an Input stream, greps every
   * outgoing packet, and directs appropriately. This is necessary to
   * support HTTP/1.1 between browser and proxy.
   */
  private final class HTTPProxyStreamDemultiplexer implements Runnable {

    private final InputStream m_in;
    private final Socket m_localSocket;
    private final PatternMatcher m_matcher = new Perl5Matcher();
    private final Map m_remoteStreamMap = new HashMap();
    private OutputStreamFilterTee m_lastRemoteStream;

    HTTPProxyStreamDemultiplexer(InputStream in, Socket localSocket) {
      m_in = in;
      m_localSocket = localSocket;
    }

    public void run() {

      final byte[] buffer = new byte[4096];

      try {
        while (true) {
          // Read a buffer full. We're relying on the world conspiring
          // to place request at start of buffer.
          final int bytesRead = m_in.read(buffer);

          if (bytesRead == -1) {
            break;
          }

          final String bytesReadAsString =
            new String(buffer, 0, bytesRead, "US-ASCII");

          if (m_matcher.contains(bytesReadAsString, getHTTPConnectPattern())) {

            final MatchResult match = m_matcher.getMatch();
            final String remoteHost = match.group(2);

            int remotePort = 80;

            try {
              remotePort = Integer.parseInt(match.group(3));
            }
            catch (NumberFormatException e) {
              // remotePort = 80;
            }

            final EndPoint remoteEndPoint =
              new EndPoint(remoteHost, remotePort);

            final String key = remoteEndPoint.toString();

            m_lastRemoteStream =
              (OutputStreamFilterTee) m_remoteStreamMap.get(key);

            if (m_lastRemoteStream == null) {

              // New connection.
              final Socket remoteSocket =
                getSocketFactory().createClientSocket(remoteEndPoint);

              final ConnectionDetails connectionDetails =
                new ConnectionDetails(getLocalHost(), m_localSocket.getPort(),
                                      remoteHost, remotePort, false);

              m_lastRemoteStream =
                new OutputStreamFilterTee(connectionDetails,
                                          remoteSocket.getOutputStream(),
                                          getRequestFilter(),
                                          getRequestColour());

              m_lastRemoteStream.connectionOpened();

              m_remoteStreamMap.put(key, m_lastRemoteStream);

              // Spawn a thread to handle everything coming back from
              // the remote server.
              new FilteredStreamThread(
                remoteSocket.getInputStream(),
                new OutputStreamFilterTee(connectionDetails.getOtherEnd(),
                                          m_localSocket.getOutputStream(),
                                          getResponseFilter(),
                                          getResponseColour()));
            }
          }
          else if (m_lastRemoteStream == null) {
            throw new IOException("Assertion failure - no last stream");
          }

          // Should do filtering etc.
          m_lastRemoteStream.handle(buffer, bytesRead);
        }
      }
      catch (IOException e) {
        // Most likely SocketException("socket closed") or
        // IOException("Stream closed"). Ignore.
      }
      catch (MalformedPatternException e) {
        e.printStackTrace(System.err);
      }
      finally {
        // When exiting close all our outgoing streams. This will
        // force all the FilteredStreamThreads we've launched to
        // handle the paired streams to shut down.
        final Iterator iterator = m_remoteStreamMap.values().iterator();

        while (iterator.hasNext()) {
          ((OutputStreamFilterTee)iterator.next()).connectionClosed();
        }

        // We may not have any FilteredStreamThreads, so ensure the
        // local socket is closed. The local socket is shutdown on any
        // error, any browser using us will open up a new connection
        // for new work.
        try {
          m_localSocket.close();
        }
        catch (IOException e) {
          // Ignore.
        }
      }
    }
  }

  private final class ProxySSLEngine extends AbstractTCPProxyEngine {

    private EndPoint m_remoteEndPoint;

    ProxySSLEngine(TCPProxySocketFactory socketFactory,
                   TCPProxyFilter requestFilter,
                   TCPProxyFilter responseFilter,
                   PrintWriter outputWriter,
                   boolean useColour)
      throws IOException {
      super(socketFactory, requestFilter, responseFilter, outputWriter,
            new EndPoint(HTTPProxyTCPProxyEngine.this.getLocalHost(), 0),
            useColour, 0);
    }

    public void run() {

      while (true) {
        try {
          final Socket localSocket = getServerSocket().accept();

          // System.err.println("New proxy proxy connection to " +
          // m_remoteEndPoint);

          launchThreadPair(localSocket, localSocket.getInputStream(),
                           localSocket.getOutputStream(), m_remoteEndPoint,
                           true);
        }
        catch (IOException e) {
          e.printStackTrace(System.err);
        }
      }
    }

    /**
     * Set the ProxySSLEngine up so that the next connection will be
     * wired through to <code>remoteHost:remotePort</code>.
     */
    public void proxyTo(EndPoint remoteEndPoint) {
      m_remoteEndPoint = remoteEndPoint;
    }
  }

  /**
   * Filter decorator to convert absolute URLs in the method line as
   * HTTP 1.0 servers don't expect them.
   */
  private static class StripAbsoluteURIFilterDecorator
    implements TCPProxyFilter {

    private static final Perl5Substitution s_substition =
      new Perl5Substitution("$1 $4");

    private final TCPProxyFilter m_delegate;
    private final PatternMatcher m_matcher = new Perl5Matcher();

    public StripAbsoluteURIFilterDecorator(TCPProxyFilter delegate)
      throws MalformedPatternException {
      m_delegate = delegate;
    }

    public void connectionOpened(ConnectionDetails connectionDetails)
      throws Exception {
      m_delegate.connectionOpened(connectionDetails);
    }

    public void connectionClosed(ConnectionDetails connectionDetails)
      throws Exception {
      m_delegate.connectionClosed(connectionDetails);
    }

    public void stop() {
      m_delegate.stop();
    }

    public byte[] handle(ConnectionDetails connectionDetails,
                         byte[] buffer, int bytesRead)
      throws Exception {

      final byte[] delegateResult =
        m_delegate.handle(connectionDetails, buffer, bytesRead);

      final String original =
        new String(delegateResult != null ? delegateResult : buffer,
                   0, bytesRead, "US-ASCII");

      final String result =
        Util.substitute(m_matcher, getHTTPConnectPattern(),
                        s_substition, original);

      if (result != original) {    // Yes, I mean reference identity.
        return result.getBytes();
      }

      return null;
    }
  }
}
