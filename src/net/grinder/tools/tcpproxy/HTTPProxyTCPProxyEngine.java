// Copyright (C) 2000 Paco Gomez
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
import java.io.InterruptedIOException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

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
public class HTTPProxyTCPProxyEngine extends TCPProxyEngineImplementation
{
    private String m_tempRemoteHost;
    private int m_tempRemotePort;

    private final PatternMatcher m_matcher = new Perl5Matcher();
    private final Pattern m_httpsConnectPattern;

    private final ProxySSLEngine m_proxySSLEngine;

    private static Pattern s_httpConnectPattern;

    private static synchronized final Pattern getHTTPConnectPattern()
    throws MalformedPatternException
    {
    if (s_httpConnectPattern == null) {
        final PatternCompiler compiler = new Perl5Compiler();

        s_httpConnectPattern =
        compiler.compile(
            "^([A-Z]+)[ \\t]+http://([^/:]+):?(\\d*)(/.*)",
            Perl5Compiler.MULTILINE_MASK |
            Perl5Compiler.READ_ONLY_MASK);
    }

    return s_httpConnectPattern;
    }

    public HTTPProxyTCPProxyEngine(
    TCPProxyPlainSocketFactory plainSocketFactory,
    TCPProxySocketFactory sslSocketFactory,
    TCPProxyFilter requestFilter,
    TCPProxyFilter responseFilter,
    PrintWriter outputWriter,
    String localHost,
    int localPort,
    boolean useColour,
    int timeout)
        throws IOException, MalformedPatternException
    {
    // We set this engine up for handling plain HTTP and delegate
    // to a proxy for HTTPS.
    super(plainSocketFactory,
          new StripAbsoluteURIFilterDecorator(requestFilter),
          responseFilter,
          outputWriter,
          new ConnectionDetails(localHost, localPort, "", -1, false),
          useColour, timeout);

    final PatternCompiler compiler = new Perl5Compiler();

     m_httpsConnectPattern =
         compiler.compile(
         "^CONNECT[ \\t]+([^:]+):(\\d+)",
         Perl5Compiler.MULTILINE_MASK | Perl5Compiler.READ_ONLY_MASK);

    // When handling HTTPS proxies, we use our plain socket to
    // accept connections on. We suck the bit we understand off
    // the front and forward the rest through our proxy engine.
    // The proxy engine listens for connection attempts (which
    // come from us), then sets up a thread pair which pushes data
    // back and forth until either the server closes the
    // connection, or we do (in response to our client closing the
    // connection). The engine handles multiple connections by
    // spawning multiple thread pairs.
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

    public void run()
    {
    // Should be more than adequate.
    final byte[] buffer = new byte[4096];

        while (true) {
            try {
                final Socket localSocket = getServerSocket().accept();

        // Grab the first upstream packet and grep it for the
        // remote server and port in the method line.
        final BufferedInputStream in =
            new BufferedInputStream(localSocket.getInputStream(),
                        buffer.length);

        in.mark(buffer.length);

        // Read a buffer full.
        final int bytesRead = in.read(buffer);

        final String line =
            bytesRead > 0 ?
            new String(buffer, 0, bytesRead, "US-ASCII") : "";

        if (m_matcher.contains(line, getHTTPConnectPattern())) {
            // HTTP proxy request.

            // Reset stream to beginning of request.
            in.reset();

            final MatchResult match = m_matcher.getMatch();
            final String remoteHost = match.group(2);

            int remotePort = 80;

            try {
            remotePort = Integer.parseInt(match.group(3));
            }
            catch (NumberFormatException e) {
            // remotePort = 80;
            }

            //System.err.println("New HTTP proxy connection to " +
            //  remoteHost + ":" + remotePort);

            launchThreadPair(localSocket, in,
                     localSocket.getOutputStream(),
                     remoteHost, remotePort);
        }
        else if (m_matcher.contains(line, m_httpsConnectPattern)) {
            // HTTPS proxy request.

            // Discard anything else the client has to say.
            while (in.read(buffer, 0, in.available()) > 0) {
            }

            final MatchResult match = m_matcher.getMatch();
            final String remoteHost = match.group(1);

            // Must be a port number by specification.
            final int remotePort = Integer.parseInt(match.group(2));

            final String target = remoteHost + ":" + remotePort;

            // System.err.println("New HTTPS proxy connection
            // to " + target);

            if (m_proxySSLEngine == null) {
            System.err.println(
                "Specify -ssl for HTTPS proxy support");
            continue;
            }

            m_tempRemoteHost = remoteHost;
            m_tempRemotePort = remotePort;

            // Create a new proxy connection to our proxy
            // engine.
            final Socket sslProxySocket =
            getSocketFactory().createClientSocket(
                getConnectionDetails().getLocalHost(),
                m_proxySSLEngine.getServerSocket().getLocalPort());

            // Now set up a couple of threads to punt
            // everything we receive over localSocket to
            // sslProxySocket, and vice versa.
            new Thread(new CopyStreamRunnable(
                   in, sslProxySocket.getOutputStream()),
                   "Copy to proxy engine for " + target).start();

            final OutputStream out = localSocket.getOutputStream();

            new Thread(new CopyStreamRunnable(
                   sslProxySocket.getInputStream(), out),
                   "Copy from proxy engine for " + target).start();

            // Send a 200 response to send to client. Client
            // will now start sending SSL data to localSocket.
            final StringBuffer response = new StringBuffer();
            response.append("HTTP/1. 200 OK\r\n");
            response.append("Host: ");
            response.append(remoteHost);
            response.append(":");
            response.append(remotePort);
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
        catch (InterruptedIOException e) {
        System.err.println(ACCEPT_TIMEOUT_MESSAGE);
        break;
        }
        catch (Exception e) {
        e.printStackTrace(System.err);
        }
        }
    }

    private class ProxySSLEngine extends TCPProxyEngineImplementation {

    ProxySSLEngine(TCPProxySocketFactory socketFactory,
               TCPProxyFilter requestFilter,
               TCPProxyFilter responseFilter,
               PrintWriter outputWriter,
               boolean useColour)
        throws IOException
    {
        super(socketFactory, requestFilter, responseFilter, outputWriter,
          new ConnectionDetails(HTTPProxyTCPProxyEngine.this.
                    getConnectionDetails().getLocalHost(),
                    0, "", -1, true),
          useColour, 0);
    }

    public void run()
    {
        while (true) {
        try {
            final Socket localSocket = this.getServerSocket().accept();

            // System.err.println("New proxy proxy connection to " +
            //   m_tempRemoteHost + ":" + m_tempRemotePort);

            this.launchThreadPair(localSocket,
                      localSocket.getInputStream(),
                      localSocket.getOutputStream(),
                      m_tempRemoteHost, m_tempRemotePort);
        }
        catch(IOException e) {
            e.printStackTrace(System.err);
        }
        }
    }
    }

    /**
     * Filter decorator to convert absolute URLs in the method line as
     * HTTP 1.0 servers don't expect them.
     */
    private static class StripAbsoluteURIFilterDecorator
    implements TCPProxyFilter
    {
    private static final Perl5Substitution m_substition =
        new Perl5Substitution("$1 $4");

    private final TCPProxyFilter m_delegate;
    private final PatternMatcher m_matcher = new Perl5Matcher();

    public StripAbsoluteURIFilterDecorator(TCPProxyFilter delegate)
        throws MalformedPatternException
    {
        m_delegate = delegate;
    }

    public void connectionOpened(ConnectionDetails connectionDetails)
        throws Exception
    {
        m_delegate.connectionOpened(connectionDetails);
    }

    public void connectionClosed(ConnectionDetails connectionDetails)
        throws Exception
    {
        m_delegate.connectionClosed(connectionDetails);
    }

      public void stop() {
    m_delegate.stop();
      }

    public byte[] handle(ConnectionDetails connectionDetails,
                 byte[] buffer, int bytesRead)
        throws Exception
    {
        final byte[] delegateResult =
        m_delegate.handle(connectionDetails, buffer, bytesRead);

        if (delegateResult != null) {
        buffer = delegateResult;
        }

        final String original =
        new String(buffer, 0, bytesRead, "US-ASCII");

        final String result =
        Util.substitute(m_matcher, getHTTPConnectPattern(),
                m_substition, original);

        if (result != original) {    // Yes, I mean reference identity.
        return result.getBytes();
        }

        return null;
    }
    }
}
