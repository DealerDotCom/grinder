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

import net.grinder.common.GrinderException;
import net.grinder.plugin.http.tcpproxyfilter.RegularExpressions;
import net.grinder.plugin.http.tcpproxyfilter.RegularExpressionsImplementation;
import net.grinder.plugininterface.PluginProcessContext;
import net.grinder.util.AttributeStringParser;
import net.grinder.util.AttributeStringParserImplementation;
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
  private final RegularExpressions m_regularExpressions =
    new RegularExpressionsImplementation();
  private final AttributeStringParser m_attributeStringParser =
    new AttributeStringParserImplementation();
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
      return "";
    }

    final String location;

    try {
      location = response.getHeader("Location");
    }
    catch (Exception e) {
      throw new AssertionError("HTTPResponse not initialised (" + e + ")");
    }

    final String[] result = { "" };

    if (location != null) {
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
    }

    return result[0];
  }

  public String valueFromHiddenInput(final String tokenName)
    throws GrinderException {
    return valueFromHiddenInput(tokenName, null);
  }

  public String valueFromHiddenInput(String tokenName, String afterText)
    throws GrinderException {

    final String body = bodyStringAfter(afterText);

    if (body == null) {
      return "";
    }

    final Matcher matcher =
      m_regularExpressions.getHiddenInputPattern().matcher(body);

    while (matcher.find()) {
      final AttributeStringParser.AttributeMap map =
        m_attributeStringParser.parse(matcher.group());

      final String name = map.get("name");
      final String value = map.get("value");

      if (name != null && value != null && name.equals(tokenName)) {
        return value;
      }
    }

    return "";
  }

  public String valueFromBodyURI(final String tokenName)
    throws GrinderException {
    return valueFromBodyURI(tokenName, null);
  }

  public String valueFromBodyURI(final String tokenName, String afterText)
    throws GrinderException {

    final String body = bodyStringAfter(afterText);

    if (body == null) {
      return "";
    }

    final Matcher matcher =
      m_regularExpressions.getHyperlinkURIPattern().matcher(body);

    final String[] result = { "" };

    while (matcher.find()) {
      final String uri = matcher.group(1);

      m_uriParser.parse(uri, new URIParser.AbstractParseListener() {
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

      if (result[0] != "") {
        return result[0];
      }
    }

    return "";
  }

  private String bodyStringAfter(String text) throws GrinderException {

    final HTTPResponse response = getLastResponse();

    if (response == null) {
      return null;
    }

    final String body;

    try {
      // This shouldn't fail as we have already read the complete response.
      body = response.getText();
    }
    catch (Exception e) {
      throw new AssertionError(e);
    }

    if (text != null) {
      final int start = body.indexOf(text);

      if (start == -1) {
        return null;
      }

      return body.substring(start);
    }

    return body;
  }
}
