// Copyright (C) 2003 Philip Aston
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

import org.apache.oro.text.regex.MalformedPatternException;
import org.apache.oro.text.regex.Pattern;
import org.apache.oro.text.regex.PatternCompiler;
import org.apache.oro.text.regex.PatternMatcher;
import org.apache.oro.text.regex.Perl5Compiler;
import org.apache.oro.text.regex.Perl5Matcher;
import org.apache.oro.text.regex.Perl5Substitution;
import org.apache.oro.text.regex.Util;


/**
 * <p>{@link TCPProxyFilter} decorator that recognises HTTP request
 * messages and converts any absolute URLs in the method line to
 * relative URLs before passing to the filter.</p>
 *
 * <p>This is used in HTTP proxy mode for two reasons.
 *
 * <ol>
 *
 * <li>We want the URL format that filters have to parse to be
 * independent of whether the TCPProxy is being used as in HTTP proxy
 * mode or port forwarding mode.</li>
 *
 * <li>When the sucessor server is not an HTTP proxy, we don't want to
 * pass absolute URL's as HTTP 1.0 servers don't know what to do with
 * them. When the sucessor server is an HTTP proxy, we use a {@link
 * HTTPMethodAbsoluteURIFilterDecorator} as well.</li>
 *
 * </ol>
 * </p>
 *
 * @author Philip Aston
 * @version $Revision$
 */
class HTTPMethodRelativeURIFilterDecorator implements TCPProxyFilter {

  private static final Pattern s_httpMethodLine;
  private static final Perl5Substitution s_substition =
    new Perl5Substitution("$1 $2");

  static {
    final PatternCompiler compiler = new Perl5Compiler();

    try {
      s_httpMethodLine =
        compiler.compile("^([A-Z]+)[ \\t]+http://[^/:]+:?\\d*(/.*)",
                         Perl5Compiler.MULTILINE_MASK  |
                         Perl5Compiler.READ_ONLY_MASK);
    }
    catch (MalformedPatternException e) {
      throw new ExceptionInInitializerError(e);
    }
  }

  private final TCPProxyFilter m_delegate;
  private final PatternMatcher m_matcher = new Perl5Matcher();

  /**
   * Constructor.
   *
   * @param delegate Filter to decorate.
   */
  public HTTPMethodRelativeURIFilterDecorator(TCPProxyFilter delegate) {
    m_delegate = delegate;
  }

  /**
   * A new connection has been opened.
   *
   * @param connectionDetails Describes the connection.
   * @exception Exception If an error occurs.
   */
  public void connectionOpened(ConnectionDetails connectionDetails)
    throws Exception {
    m_delegate.connectionOpened(connectionDetails);
  }

  /**
   * A connection has been closed.
   *
   * @param connectionDetails Describes the connection.
   * @exception Exception If an error occurs.
   */
  public void connectionClosed(ConnectionDetails connectionDetails)
    throws Exception {
    m_delegate.connectionClosed(connectionDetails);
  }

  /**
   * Called just before stop.
   */
  public void stop() {
    m_delegate.stop();
  }

  /**
   * Handle a message fragment from the stream.
   *
   * @param connectionDetails Describes the connection.
   * @param buffer Contains the data.
   * @param bytesRead How many bytes of data in <code>buffer</code>.
   * @return Filters can optionally return a <code>byte[]</code>
   * which will be transmitted to the server instead of
   * <code>buffer</code>.
   * @exception Exception If an error occurs.
   */
  public byte[] handle(ConnectionDetails connectionDetails, byte[] buffer,
                       int bytesRead)
    throws Exception {

    final String original = new String(buffer, 0, bytesRead, "US-ASCII");

    final String result =
      Util.substitute(m_matcher, s_httpMethodLine, s_substition, original);

    // Yes, I mean reference identity.
    if (result != original) {
      final byte[] resultBytes = result.getBytes();

      final byte[] delegateResult =
        m_delegate.handle(connectionDetails, resultBytes, resultBytes.length);

      return delegateResult != null ? delegateResult : resultBytes;
    }
    else {
      return m_delegate.handle(connectionDetails, buffer, bytesRead);
    }
  }
}
