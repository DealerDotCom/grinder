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

package net.grinder.plugin.http;

import net.grinder.common.GrinderException;
import HTTPClient.HTTPResponse;
import HTTPClient.NVPair;


/**
 * Things that HTTP scripts find useful.
 *
 * @author Philip Aston
 * @version $Revision$
 * @see HTTPPluginControl#getHTTPUtilities()
 */
public interface HTTPUtilities {

  /**
   * Create a {@link NVPair} for an HTTP Basic Authorization header.
   *
   * @param userID
   *          The user name.
   * @param password
   *          The password.
   * @return The NVPair that can be used as a header with {@link HTTPRequest}.
   */
  NVPair basicAuthorizationHeader(String userID, String password);

  /**
   * Return the response for the last request made by the calling worker thread.
   *
   * @return The response, or <code>null</code> if the calling thread has not
   *         made any requests.
   * @throws GrinderException
   *           If not called from a worker thread.
   */
  HTTPResponse getLastResponse() throws GrinderException;

  /**
   * Return the value for a path parameter or query string name-value token with
   * the given <code>tokenName</code> in a Location header from the last
   * response. If there are multiple matches, the first value is returned.
   *
   * @param tokenName
   *          The token name.
   * @return The first value if one is found, or <code>null</code>.
   * @throws GrinderException If not called from a worker thread.
   */
  String valueFromLocationURI(String tokenName) throws GrinderException;

  /**
   * Return the value for a path parameter or query string name-value token with
   * the given <code>tokenName</code> in a URI in the body of the last
   * response. If there are multiple matches, the first value is returned.
   *
   * @param tokenName
   *          The token name.
   * @return The first value if one is found, or <code>null</code>.
   * @throws GrinderException If not called from a worker thread.
   */
  String valueFromBodyURI(String tokenName) throws GrinderException;
}
