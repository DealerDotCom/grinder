// Copyright (C) 2000, 2001, 2002, 2003 Philip Aston
// Copyright (C) 2003 Bertrand Ave
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

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.text.DateFormat;

import org.apache.oro.text.regex.MalformedPatternException;
import org.apache.oro.text.regex.MatchResult;
import org.apache.oro.text.regex.Pattern;
import org.apache.oro.text.regex.PatternCompiler;
import org.apache.oro.text.regex.PatternMatcher;
import org.apache.oro.text.regex.Perl5Compiler;
import org.apache.oro.text.regex.Perl5Matcher;

import HTTPClient.Codecs;

import net.grinder.TCPProxy;
import net.grinder.tools.tcpproxy.ConnectionDetails;
import net.grinder.tools.tcpproxy.EchoFilter;
import net.grinder.tools.tcpproxy.TCPProxyFilter;


/**
 * {@link TCPProxyFilter} that collects data from server responses.
 * Should be installed as a response filter. Used by
 * HttpPluginTCPProxyFilter to determine things such as the basic
 * authentication realm.
 *
 * @author Philip Aston
 * @author Bertrand Ave
 * @version $Revision$
 */
public class HTTPPluginTCPProxyResponseFilter implements TCPProxyFilter
{
    private final Pattern m_wwwAuthenticateHeaderPattern;
    private final Perl5Matcher m_matcher = new Perl5Matcher();

    private static String s_lastAuthenticationRealm;

    /**
     * Constructor.
     *
     * @exception MalformedPatternException
     */
    public HTTPPluginTCPProxyResponseFilter(PrintWriter outputWriter)
	throws MalformedPatternException
    {
	final PatternCompiler compiler = new Perl5Compiler();

	m_wwwAuthenticateHeaderPattern =
	    compiler.compile(
		"^WWW-Authenticate:[ \\t]*Basic realm[  \\t]*=[ \\t]*\"([^\"]*)\".*\\r?$",
		Perl5Compiler.READ_ONLY_MASK | Perl5Compiler.MULTILINE_MASK);
    }

    /**
     * The main handler method called by the proxy engine.
     *
     * <p>NOTE, this is called for message fragments, don't assume
     * that its passed a complete HTTP message at a time.</p>
     *
     * @param connectionDetails The TCP connection.
     * @param buffer The message fragment buffer.
     * @param bytesRead The number of bytes of buffer to process.
     * @return Filters can optionally return a <code>byte[]</code>
     * which will be transmitted to the server instead of
     * <code>buffer</code.
     * @exception IOException if an error occurs
     */
    public byte[] handle(ConnectionDetails connectionDetails, byte[] buffer,
			 int bytesRead)
	throws IOException
    {
	HTTPPluginTCPProxyFilter.markLastResponseTime();

	// String used to parse headers - header names are
	// US-ASCII encoded and anchored to start of line.
	final String asciiString =
	    new String(buffer, 0, bytesRead, "US-ASCII");

	if (m_matcher.contains(asciiString, m_wwwAuthenticateHeaderPattern)) {
	    // Packet is start of new request message.

	    s_lastAuthenticationRealm = m_matcher.getMatch().group(1).trim();
	}

	return null;
    }

    /**
     * A connection has been opened.
     *
     * @param connectionDetails a <code>ConnectionDetails</code> value
     */
    public void connectionOpened(ConnectionDetails connectionDetails)
    {
    }

    /**
     * A connection has been closed.
     *
     * @param connectionDetails a <code>ConnectionDetails</code> value
     * @exception IOException if an error occurs
     */
    public void connectionClosed(ConnectionDetails connectionDetails)
	throws IOException
    {
    }

      /**
       * Called just before stop.
       */
    public final void stop() {
    }

    /**
     * Return the realm used in the last recorded authentication
     * challenge, or null if none exists.
     *
     * @return The realm from the last recorded authentication
     * challenge.
     */
    static String getLastAuthenticationRealm()
    {
	return s_lastAuthenticationRealm;
    }
}
