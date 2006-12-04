// Copyright (C) 2006 Philip Aston
// All rights reserved.
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

import net.grinder.common.Logger;
import net.grinder.tools.tcpproxy.ConnectionDetails;
import net.grinder.util.AttributeStringParser;
import net.grinder.util.URIParser;


/**
 * Factory for {@link ConnectionHandler}s.
 *
 * @author Philip Aston
 * @version $Revision$
 */
public final class ConnectionHandlerFactoryImplementation
  implements ConnectionHandlerFactory {

  private final Logger m_logger;
  private final HTTPRecording m_httpRecording;
  private final RegularExpressions m_regularExpressions;
  private final URIParser m_uriParser;
  private final AttributeStringParser m_attributeStringParser;

  /**
   * Constructor.
   *
   * @param httpRecording
   *          Common recording state.
   * @param logger
   *          Logger.
   * @param regularExpressions
   *          Compiled regular expressions.
   * @param uriParser
   *          A URI parser.
   * @param attributeStringParser
   *          An AttributeStringParser.
   */
  public ConnectionHandlerFactoryImplementation(
    HTTPRecording httpRecording,
    Logger logger,
    RegularExpressions regularExpressions,
    URIParser uriParser,
    AttributeStringParser attributeStringParser) {

    m_logger = logger;
    m_httpRecording = httpRecording;
    m_regularExpressions = regularExpressions;
    m_uriParser = uriParser;
    m_attributeStringParser = attributeStringParser;
  }

  /**
   * Factory method.
   *
   * @param connectionDetails Connection details.
   * @return A new ConnectionHandler.
   */
  public ConnectionHandler create(ConnectionDetails connectionDetails) {
    return new ConnectionHandlerImplementation(m_httpRecording,
                                               m_logger,
                                               m_regularExpressions,
                                               m_uriParser,
                                               m_attributeStringParser,
                                               connectionDetails);
  }
}
