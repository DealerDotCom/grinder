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

package net.grinder.tools.tcpsniffer;

import java.io.BufferedInputStream;
import java.io.InterruptedIOException;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;

import org.apache.oro.text.regex.MalformedPatternException;
import org.apache.oro.text.regex.MatchResult;
import org.apache.oro.text.regex.Pattern;
import org.apache.oro.text.regex.PatternCompiler;
import org.apache.oro.text.regex.PatternMatcher;
import org.apache.oro.text.regex.Perl5Compiler;
import org.apache.oro.text.regex.Perl5Matcher;
import org.apache.oro.text.regex.Perl5Substitution;
import org.apache.oro.text.regex.Util;


/**
 * SnifferEngine that works as an HTTP proxy.
 *
 * @author Paddy Spencer
 * @author Philip Aston
 * @version $Revision$
 */
public class HTTPProxySnifferEngine extends SnifferEngineImplementation
{
    private static Pattern s_proxyConnectPattern;

    private static synchronized final Pattern getConnectPattern()
	throws MalformedPatternException
    {
	if (s_proxyConnectPattern == null) {
	    final PatternCompiler compiler = new Perl5Compiler();

	    s_proxyConnectPattern =
		compiler.compile(
		    "^([A-Z]+)[ \\t]+http://([^/:]+):?(\\d*)(/.*)",
		    Perl5Compiler.MULTILINE_MASK |
		    Perl5Compiler.READ_ONLY_MASK);
	}

	return s_proxyConnectPattern;
    }

    private final Pattern m_proxyConnectPattern;
    private final PatternMatcher m_matcher = new Perl5Matcher();

    public HTTPProxySnifferEngine(SnifferSocketFactory socketFactory,
				  SnifferFilter requestFilter,
				  SnifferFilter responseFilter,
				  String localHost,
				  int localPort,
				  boolean useColour,
				  int timeout)
	throws IOException, MalformedPatternException
    {
	super(socketFactory,
	      new StripAbsoluteURIFilterDecorator(requestFilter),
	      responseFilter,
	      new ConnectionDetails(localHost, localPort, "", -1, false),
	      useColour, timeout);

	m_proxyConnectPattern = getConnectPattern();
    }

    public void run()
    {
	// Should be more than adequate.
	final byte[] readAheadBuffer = new byte[4096];

	while (true) {
	    final Socket localSocket;

	    try {
		localSocket = getServerSocket().accept();
	    }
	    catch (InterruptedIOException e) {
		System.err.println(ACCEPT_TIMEOUT_MESSAGE);
		return;
	    }
	    catch (IOException e) {
		e.printStackTrace(System.err);
		return;
	    }

	    try {
		// Grab the first upstream packet and grep it for the
		// remote server and port in the method line.
		final BufferedInputStream in =
		    new BufferedInputStream(localSocket.getInputStream(),
					    readAheadBuffer.length);

		in.mark(readAheadBuffer.length);

		final int bytesRead = in.read(readAheadBuffer);

		in.reset();

		final String line =
		    bytesRead > 0 ?
		    new String(readAheadBuffer, 0, bytesRead, "US-ASCII") : "";

		if (m_matcher.contains(line, m_proxyConnectPattern)) {
		    final MatchResult match = m_matcher.getMatch();

		    final String remoteHost = match.group(2);

		    int remotePort = 80;

		    try {
			remotePort = Integer.parseInt(match.group(3));
		    }
		    catch (NumberFormatException e) {
			// remotePort = 80; 
		    }

		    System.err.println("New proxy connection to " +
				       remoteHost + ":" + remotePort);

		    launchThreadPair(
			localSocket, in, localSocket.getOutputStream(),
			remoteHost, remotePort);
		}
		else {
		    System.err.println(
			"Failed to determine proxy destination from message:");
		    System.err.println(line);
		}
	    }
	    catch(IOException e) {
		e.printStackTrace(System.err);
	    }
	}
    }


    /**
     * Filter decorator to convert absolute URLs in the method line as
     * HTTP 1.0 servers don't expect them.
     */
    private static class StripAbsoluteURIFilterDecorator
	implements SnifferFilter
    {
	private static final Perl5Substitution m_substition =
	    new Perl5Substitution("$1 $4");

	private final SnifferFilter m_delegate;
	private final Pattern m_proxyConnectPattern;
	private final PatternMatcher m_matcher = new Perl5Matcher();

	public StripAbsoluteURIFilterDecorator(SnifferFilter delegate) 
	    throws MalformedPatternException
	{
	    m_delegate = delegate;
	    m_proxyConnectPattern = getConnectPattern();
	}

	public void setOutputPrintWriter(PrintWriter outputPrintWriter) 
	{
	    m_delegate.setOutputPrintWriter(outputPrintWriter);
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
		Util.substitute(m_matcher, getConnectPattern(),
				m_substition, original);

	    if (result != original) {	// Yes, I mean object identity.
		return result.getBytes();
	    }

	    return null;
	}
    }    
}
