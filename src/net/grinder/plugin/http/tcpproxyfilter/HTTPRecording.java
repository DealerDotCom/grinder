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

import java.io.File;

import net.grinder.plugin.http.xml.RequestType;
import net.grinder.plugin.http.xml.TokenReferenceType;
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
   * The request is returned to allow the caller to add things it doesn't know
   * yet, e.g. headers, body, response.
   * </p>
   *
   * @param connectionDetails
   *          The connection used to make the request.
   * @param method
   *          The HTTP method.
   * @param relativeURI
   *          The URI.
   * @param relativeURI
   * @return The request.
   */
  RequestType addRequest(
    ConnectionDetails connectionDetails, String method, String relativeURI);

  /**
   * Called when a complete request message has been read.
   *
   * @param request The request.
   */
  void endRequest(RequestType request);

  /**
   * Called when a response message starts. Because the test script represents a
   * single thread of control we need to calculate the sleep deltas using the
   * last time any response was received on any connection.
   */
  void markLastResponseTime();

  /**
   * Add a new name-value token, or update an existing one.
   *
   * @param name The name.
   * @param value The new value.
   * @param tokenReference This reference is updated with the appropriate
   * token ID, and the value if it has changed.
   */
  void addNameValueTokenReference(
    String name, String value, TokenReferenceType tokenReference);

  /**
   * Create a new file name for body data.
   *
   * @return The file name.
   */
  File createBodyDataFileName();
}
