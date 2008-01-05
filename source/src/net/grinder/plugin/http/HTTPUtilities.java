// Copyright (C) 2005, 2006, 2007 Philip Aston
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
// COPYRIGHT HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
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
   * <p>
   * If there is no match, an empty string is returned rather than
   * <code>null</code>. This makes scripts more robust (as they don't need to
   * check the value before using it), but they lose the ability to distinguish
   * between a missing token and an empty value.
   * </p>
   *
   * @param tokenName
   *          The token name.
   * @return The first value if one is found.
   * @throws GrinderException
   *           If not called from a worker thread.
   */
  String valueFromLocationURI(String tokenName) throws GrinderException;

  /**
   * Return the value for a hidden input token with the given
   * <code>tokenName</code> in the body of the last response. If there are
   * multiple matches, the first value is returned.
   *
   * <p>
   * If there is no match, an empty string is returned rather than
   * <code>null</code>. This makes scripts more robust (as they don't need to
   * check the value before using it), but they lose the ability to distinguish
   * between a missing token and an empty value.
   * </p>
   *
   * @param tokenName
   *          The token name.
   * @return The first value if one is found, or an empty string.
   * @throws GrinderException
   *           If not called from a worker thread.
   * @see #valueFromHiddenInput(String, String)
   */
  String valueFromHiddenInput(String tokenName) throws GrinderException;

  /**
   * Return the value for a hidden input token with the given
   * <code>tokenName</code> in the body of the last response. If there are
   * multiple matches, the first value is returned. This version of
   * <code>valueFromHiddenInput</code> only considers matches following the
   * first occurrence of the literal text <code>afterText</code>. If there
   * are multiple matches, the first value is returned.
   *
   * <p>
   * If there is no match, an empty string is returned rather than
   * <code>null</code>. This makes scripts more robust (as they don't need to
   * check the value before using it), but they lose the ability to distinguish
   * between a missing token and an empty value.
   * </p>
   *
   * @param tokenName
   *          The token name.
   * @param afterText
   *          The search begins after the first occurrence of this literal text.
   * @return The first value if one is found, or an empty string if the body
   *         does not contain <code>afterText</code> followed by a URI
   *         containing a token with name <code>tokenName</code>.
   * @throws GrinderException
   *           If not called from a worker thread.
   * @see #valueFromHiddenInput(String)
   */
  String valueFromHiddenInput(String tokenName, String afterText)
    throws GrinderException;

  /**
   * Return the value for a path parameter or query string name-value token with
   * the given <code>tokenName</code> in a URI in the body of the last
   * response. If there are multiple matches, the first value is returned.
   *
   * <p>
   * If there is no match, an empty string is returned rather than
   * <code>null</code>. This makes scripts more robust (as they don't need to
   * check the value before using it), but they lose the ability to distinguish
   * between a missing token and an empty value.
   * </p>
   *
   * @param tokenName
   *          The token name.
   * @return The first value if one is found, or an empty string.
   * @throws GrinderException If not called from a worker thread.
   * @see #valueFromBodyURI(String, String)
   */
  String valueFromBodyURI(String tokenName) throws GrinderException;

  /**
   * Return the value for a path parameter or query string name-value token with
   * the given <code>tokenName</code> in a URI in the body of the last
   * response. This version of <code>valueFromBodyURI</code> only considers
   * matches following the first occurrence of the literal text
   * <code>afterText</code>. If there are multiple matches, the first value
   * is returned.
   *
   * <p>
   * If there is no match, an empty string is returned rather than
   * <code>null</code>. This makes scripts more robust (as they don't need to
   * check the value before using it), but they lose the ability to distinguish
   * between a missing token and an empty value.
   * </p>
   *
   * @param tokenName
   *          The token name.
   * @param afterText
   *          The search begins after the first occurrence of this literal text.
   * @return The first value if one is found, or an empty string if the body
   *         does not contain <code>afterText</code> followed by a URI
   *         containing a token with name <code>tokenName</code>.
   * @throws GrinderException
   *           If not called from a worker thread.
   * @see #valueFromBodyURI(String)
   */
  String valueFromBodyURI(String tokenName, String afterText)
    throws GrinderException;
}
