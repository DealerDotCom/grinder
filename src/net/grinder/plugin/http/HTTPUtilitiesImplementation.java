// Copyright (C) 2005, 2006 Philip Aston
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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.grinder.common.GrinderException;
import net.grinder.plugininterface.PluginProcessContext;
import HTTPClient.Codecs;
import HTTPClient.HTTPResponse;
import HTTPClient.NVPair;
import HTTPClient.ParseException;


/**
 * {@link HTTPUtilities} implementation.
 *
 * @author Philip Aston
 * @version $Revision$
 */
class HTTPUtilitiesImplementation implements HTTPUtilities {

  private final PluginProcessContext m_processContext;

  private final Pattern m_pathNameValuePattern;
  private final Pattern m_queryNameValuePattern;

  public HTTPUtilitiesImplementation(PluginProcessContext processContext) {
    m_processContext = processContext;

    m_pathNameValuePattern = Pattern.compile("(?:;([^;/\\?=;]+)=([^;/\\?]*))");
    m_queryNameValuePattern = Pattern.compile("([^&=;]+)=([^&]*)");
  }

  /**
   * Create a {@link NVPair} for an HTTP Basic Authorization header.
   *
   * @param userID
   *          The user name.
   * @param password
   *          The password.
   * @return The NVPair that can be used as a header with {@link HTTPRequest}.
   */
  public NVPair basicAuthorizationHeader(String userID, String password) {
    return new NVPair("Authorization",
                      "Basic " +
                      Codecs.base64Encode(userID + ":" + password));
  }

  public HTTPResponse getLastResponse() throws GrinderException {
    final HTTPPluginThreadState threadState =
      (HTTPPluginThreadState)m_processContext.getPluginThreadListener();

    return threadState.getLastResponse();
  }

  public String valueFromLocationHeaderURI(String tokenName)
    throws GrinderException {

    final HTTPResponse response = getLastResponse();

    if (response == null) {
      return null;
    }

    final String location;

    try {
      location = Codecs.URLDecode(response.getHeader("Location"));
    }
    catch (ParseException e) {
      // Don't throw an exception on invalid encodings.
      return null;
    }
    catch (Exception e) {
      throw new AssertionError("HTTPResponse not initialised (" + e + ")");
    }

    if (location != null) {
      final int queryIndex = location.indexOf('?');

      final String beforeQuery =
        queryIndex >= 0 ? location.substring(0, queryIndex) : location;
      final Matcher pathMatcher = m_pathNameValuePattern.matcher(beforeQuery);

      while (pathMatcher.find()) {
        if (pathMatcher.group(1).equals(tokenName)) {
          return pathMatcher.group(2);
        }
      }

      if (queryIndex == -1) {
        return null;
      }

      final String query = location.substring(queryIndex + 1);
      final Matcher queryMatcher = m_queryNameValuePattern.matcher(query);

      while (queryMatcher.find()) {
        if (queryMatcher.group(1).equals(tokenName)) {
          return queryMatcher.group(2);
        }
      }
    }

    return null;
  }

  public String valueFromBodyURI(String tokenName) {
    // TODO Auto-generated method stub
    return null;
  }
}
