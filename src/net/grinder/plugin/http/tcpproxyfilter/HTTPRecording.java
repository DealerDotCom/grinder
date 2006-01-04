// Copyright (C) 2005 Philip Aston
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

import net.grinder.plugin.http.xml.RequestType;
import net.grinder.tools.tcpproxy.ConnectionDetails;


/**
 * Interface for recording HTTP stream information.
 *
 * @author Philip Aston
 * @version $Revision$
 */
public interface HTTPRecording {

  /**
   * Add a new request to the recording.
   *
   * <p>
   * The "global information" (request ID, base URL ID, common headers, page
   * etc.) is filled in by this method.
   * </p>
   *
   * @param connectionDetails
   *          The connection used to make the request.
   * @param request
   *          The request as a disconnected element.
   */
  void addRequest(ConnectionDetails connectionDetails,
                  RequestType request);

  /**
   * Called when any response activity is detected. Because the test script
   * represents a single thread of control we need to calculate the sleep deltas
   * using the last time any activity occurred on any connection.
   */
  void markLastResponseTime();

  /**
   * Get the last response time.
   *
   * @return The last response time.
   */
  long getLastResponseTime();
}
