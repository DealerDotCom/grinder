// Copyright (C) 2000 Paco Gomez
// Copyright (C) 2000, 2001, 2002 Philip Aston
// Copyright (C) 2000, 2001 Phil Dawes
// Copyright (C) 2001 Paddy Spencer
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

package net.grinder.tools.proxy;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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

import net.grinder.tools.tcpsniffer.ConnectionDetails;
import net.grinder.tools.tcpsniffer.SnifferEngine;
import net.grinder.tools.tcpsniffer.SnifferEngineImplementation;
import net.grinder.tools.tcpsniffer.SnifferFilter;
import net.grinder.tools.tcpsniffer.SnifferPlainSocketFactory;
import net.grinder.tools.tcpsniffer.SnifferSocketFactory;
import net.grinder.util.CopyStreamRunnable;



/** 
 * HTTPS proxy implementation.
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
 * proxy the rest of the stream to a SnifferEngineImplementation
 * which instantiated to handle SSL.</p>
 *
 * @author Paddy Spencer
 * @author Philip Aston
 * @version $Revision$ 
 */
public class HTTPSProxySnifferEngine implements SnifferEngine
{
    private final SnifferSocketFactory m_localSocketFactory =
	new SnifferPlainSocketFactory();
    private final ServerSocket m_serverSocket;

    private final SnifferSocketFactory m_sslSocketFactory;
    private final SnifferFilter m_requestFilter;
    private final SnifferFilter m_responseFilter;
    private final String m_localHost;
    private final boolean m_useColour;
    private final int m_timeout;

    private final PatternMatcher m_matcher = new Perl5Matcher();
    private final Pattern m_connectPattern;

    public HTTPSProxySnifferEngine(SnifferSocketFactory sslSocketFactory,
				   SnifferFilter requestFilter,
				   SnifferFilter responseFilter,
				   String localHost,
				   int localPort,
				   boolean useColour,
				   int timeout)
        throws IOException, MalformedPatternException
    {
	m_sslSocketFactory = sslSocketFactory;
	m_requestFilter = requestFilter;
	m_responseFilter = responseFilter;
	m_localHost = localHost;
	m_useColour = useColour;
	m_timeout = timeout;
	
	final PatternCompiler compiler = new Perl5Compiler();

 	m_connectPattern =
 	    compiler.compile(
 		"^CONNECT[ \\t]+([^:]+):(\\d+)",
 		Perl5Compiler.MULTILINE_MASK | Perl5Compiler.READ_ONLY_MASK);

	m_serverSocket =
	    m_localSocketFactory.createServerSocket(
		m_localHost, localPort, timeout);
    }

    public void run()
    {
	// Should be more than adequate.
	final byte[] buffer = new byte[4096];

        while (true) {
            try {
                final Socket localSocket = m_serverSocket.accept();
		final InputStream in = localSocket.getInputStream();
		final OutputStream out = localSocket.getOutputStream();

		// Read a buffer full ...
		final int bytesRead = in.read(buffer);

		final String line =
		    bytesRead > 0 ?
		    new String(buffer, 0, bytesRead, "US-ASCII") : "";

		// ... and discard anything else the client has to
		// say.
		while (in.read(buffer) > -1) {
		}

		// Check whether message has a CONNECT method string.
		if (m_matcher.contains(line, m_connectPattern)) {
		    final MatchResult match = m_matcher.getMatch();
		
		    final String remoteHost = match.group(1);

		    // Must be a port number by specification.
		    final int remotePort = Integer.parseInt(match.group(2));

		    System.err.println("New proxy connection to " +
				       remoteHost + ":" + remotePort);

		    // Create and start a proxy SnifferEngine to
		    // handle the SSL connection.
		    final SnifferEngineImplementation sslEngine =
			new SnifferEngineImplementation(
			    m_sslSocketFactory,
			    m_requestFilter,
			    m_responseFilter,
			    new ConnectionDetails(
				m_localHost,
				0,	// Arbitrary port.
				remoteHost, 
				remotePort,
				true),
			    m_useColour,
			    m_timeout);

		    new Thread(sslEngine).start();

		    final Socket sslProxySocket =
			m_localSocketFactory.createClientSocket(
			    m_localHost,
			    sslEngine.getServerSocket().getLocalPort());

		    // Now set up a couple of threads to copy
		    // everything we receive over localSocket.
		    new Thread(
			new CopyStreamRunnable(
			    in, sslProxySocket.getOutputStream())).start();

		    new Thread(
			new CopyStreamRunnable(
			    sslProxySocket.getInputStream(), out)).start();

		    // Send 200 response to send to client. Client
		    // will now start sending SSL data to our socket.
		    final StringBuffer response = new StringBuffer();
		    response.append("HTTP/1. 200 OK\r\n");
		    response.append("Host: " + remoteHost + ":" +
				    remotePort + "\r\n");
		    response.append("Proxy-agent: The Grinder/@version@\r\n");
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
	    catch (Exception e) {
		e.printStackTrace();
	    }
        }
    }
}
