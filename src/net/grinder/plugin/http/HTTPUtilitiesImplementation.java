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

import net.grinder.common.GrinderException;
import net.grinder.plugininterface.PluginProcessContext;
import net.grinder.util.URIParser;
import net.grinder.util.URIParserImplementation;
import HTTPClient.Codecs;
import HTTPClient.HTTPResponse;
import HTTPClient.NVPair;


/**
 * {@link HTTPUtilities} implementation.
 *
 * @author Philip Aston
 * @version $Revision$
 */
class HTTPUtilitiesImplementation implements HTTPUtilities {

  private final URIParser m_uriParser = new URIParserImplementation();
  private final PluginProcessContext m_processContext;

  public HTTPUtilitiesImplementation(PluginProcessContext processContext) {
    m_processContext = processContext;
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

  public String valueFromLocationURI(final String tokenName)
    throws GrinderException {

    final HTTPResponse response = getLastResponse();

    if (response == null) {
      return null;
    }

    final String location;

    try {
      location = response.getHeader("Location");
    }
    catch (Exception e) {
      throw new AssertionError("HTTPResponse not initialised (" + e + ")");
    }

    if (location != null) {
      final String[] result = new String[1];

      m_uriParser.parse(location, new URIParser.AbstractParseListener() {
        public boolean pathParameterNameValue(String name, String value) {
          if (name.equals(tokenName)) {
            result[0] = value;
            return false;
          }

          return true;
        }

        public boolean queryStringNameValue(String name, String value) {
          if (name.equals(tokenName)) {
            result[0] = value;
            return false;
          }

          return true;
        }
      });

      return result[0];
    }

    return null;
  }

  public String valueFromBodyURI(String tokenName) {
    // TODO Auto-generated method stub
    return null;
  }
}
