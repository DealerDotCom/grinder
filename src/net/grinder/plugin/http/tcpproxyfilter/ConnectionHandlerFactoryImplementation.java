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


/**
 * Factory for {@link ConnectionHandlers}.
 *
 * @author Philip Aston
 * @version $Revision$
 */
public final class ConnectionHandlerFactoryImplementation
  implements ConnectionHandlerFactory {

  private final Logger m_logger;
  private final HTTPRecording m_httpRecording;
  private final RegularExpressions m_regularExpressions =
    new RegularExpressions();

  /**
   * Constructor.
   *
   * @param httpRecording Common recording state.
   * @param logger Logger.
   */
  public ConnectionHandlerFactoryImplementation(HTTPRecording httpRecording,
                                                Logger logger) {
    m_logger = logger;
    m_httpRecording = httpRecording;
  }

  /**
   * Factory method.
   *
   * @param connectionDetails Connection details.
   * @return A new ConnectionHandler.
   */
  public ConnectionHandler create(ConnectionDetails connectionDetails) {
    return new ConnectionHandlerImplementation(
        m_httpRecording, m_logger, m_regularExpressions, connectionDetails);
  }
}
