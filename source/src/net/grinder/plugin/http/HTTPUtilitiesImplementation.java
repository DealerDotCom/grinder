// Copyright (C) 2005 - 2009 Philip Aston
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

  private final ThreadLocal<ParsedBody> m_parsedBodyThreadLocal =
    new ThreadLocal<ParsedBody>();
  private final NameValue[] m_emptyNameValues = new NameValue[0];

  private final PluginProcessContext m_processContext;

  public HTTPUtilitiesImplementation(PluginProcessContext processContext) {
    m_processContext = processContext;
  }

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

    final HTTPResponse response = getLastResponse();

    if (response == null) {
      return "";
    }

    return getParsedBody(response).valueFromHiddenInput(tokenName, afterText);
  }

  public String valueFromBodyURI(final String tokenName)
    throws GrinderException {
    return valueFromBodyURI(tokenName, null);
  }

  public String valueFromBodyURI(final String tokenName, String afterText)
    throws GrinderException {

    final HTTPResponse response = getLastResponse();

    if (response == null) {
      return "";
    }

    return getParsedBody(response).valueFromBodyURI(tokenName, afterText);
  }

  private ParsedBody getParsedBody(HTTPResponse response) {
    final ParsedBody original = m_parsedBodyThreadLocal.get();

    if (original != null && original.isValidForResponse(response)) {
      return original;
    }

    final ParsedBody newParsedBody =
      new ParsedBody(response, this);
    m_parsedBodyThreadLocal.set(newParsedBody);
    return newParsedBody;
  }

  /**
   * Cache parse results from a HTTPResponse.
   *
   * <p>Specific to a thread, so no need to synchronise.</p>
   */
  private static final class ParsedBody {

    private final HTTPResponse m_response;
    private final String m_body;
    private final MatchList m_hiddenInputMatchList;
    private final MatchList m_bodyURIMatchList;

    public ParsedBody(HTTPResponse response,
                      HTTPUtilitiesImplementation httpUtilities) {
      m_response = response;

      try {
        // This shouldn't fail as we have already read the complete response.
        m_body = response.getText();
      }
      catch (Exception e) {
        throw new AssertionError(e);
      }

      m_hiddenInputMatchList = httpUtilities.new HiddenInputMatchList(m_body);
      m_bodyURIMatchList = httpUtilities.new BodyURIMatchList(m_body);
    }

    public boolean isValidForResponse(HTTPResponse response) {
      return m_response.equals(response);
    }

    public String valueFromHiddenInput(String tokenName, String afterText) {

      final int startFrom = getStartFrom(afterText);

      if (startFrom == -1) {
        return "";
      }

      return m_hiddenInputMatchList.getTokenValue(tokenName, startFrom);
    }

    public String valueFromBodyURI(String tokenName, String afterText) {

      final int startFrom = getStartFrom(afterText);

      if (startFrom == -1) {
        return "";
      }

      return m_bodyURIMatchList.getTokenValue(tokenName, startFrom);
    }

    private int getStartFrom(String text) {
      // afterText parameter is infrequently used, so memoizing this
      // method would cost more than it saved.

      return text == null ? 0 : m_body.indexOf(text);
    }
  }

  private static final class NameValue {
    private final String m_name;
    private final String m_value;

    public NameValue(String name, String value) {
      m_name = name;
      m_value = value;
    }

    public String getName() {
      return m_name;
    }

    public String getValue() {
      return m_value;
    }
  }

  private static class CachedValueList {
    private final List<PositionAndValue> m_valuesByPosition =
      new ArrayList<PositionAndValue>();

    public void addValue(int position, String value) {
      m_valuesByPosition.add(new PositionAndValue(position, value));
    }

    public String getValue(int startFrom) {
      for (PositionAndValue positionAndValue : m_valuesByPosition) {
        if (positionAndValue.getPosition() >= startFrom) {
          return positionAndValue.getValue();
        }
      }

      return null;
    }

    private static final class PositionAndValue {
      private final int m_position;
      private final String m_value;

      public PositionAndValue(int position, String value) {
        m_position = position;
        m_value = value;
      }

      public int getPosition() {
        return m_position;
      }

      public String getValue() {
        return m_value;
      }
    }
  }

  private interface MatchList {
    String getTokenValue(String tokenName, int startFrom);
  }

  private static class CachedValueMap {
    private final Map<String, CachedValueList> m_map =
      new HashMap<String, CachedValueList>();

    public CachedValueList get(String tokenName) {
      final CachedValueList existing = m_map.get(tokenName);

      if (existing != null) {
        return existing;
      }

      final CachedValueList newCachedValueList = new CachedValueList();
      m_map.put(tokenName, newCachedValueList);
      return newCachedValueList;
    }

  }

  private abstract static class AbstractMatchList implements MatchList {
    private final Matcher m_matcher;
    private final CachedValueMap m_cache = new CachedValueMap();

    public AbstractMatchList(Matcher matcher) {
      m_matcher = matcher;
    }

    public final String getTokenValue(String tokenName, int startFrom) {

      final CachedValueList cachedValueList = m_cache.get(tokenName);

      final String existingValue = cachedValueList.getValue(startFrom);

      if (existingValue != null) {
        return existingValue;
      }

      // Cache miss, parse more of the body.
      while (m_matcher.find()) {
        final NameValue[] nameValueArray = parseMatch();

        final int matchPosition = m_matcher.start();

        String result = null;

        for (int i = 0; i < nameValueArray.length; ++i) {
          final String name = nameValueArray[i].getName();
          final String value = nameValueArray[i].getValue();

          if (name.equals(tokenName)) {
            cachedValueList.addValue(matchPosition, value);

            if (result == null && matchPosition >= startFrom) {
              result = value;
            }
          }
          else {
            m_cache.get(name).addValue(matchPosition, value);
          }
        }

        if (result != null) {
          return result;
        }
      }

      return "";
    }

    protected final Matcher getMatcher() {
      return m_matcher;
    }

    protected abstract NameValue[] parseMatch();
  }

  private final class HiddenInputMatchList extends AbstractMatchList {

    public HiddenInputMatchList(String body) {
      super(m_regularExpressions.getHiddenInputPattern().matcher(body));
    }

    protected NameValue[] parseMatch() {
      final AttributeStringParser.AttributeMap map =
        m_attributeStringParser.parse(getMatcher().group());

      final String name = map.get("name");
      final String value = map.get("value");

      if (name != null && value != null) {
        return new NameValue[] { new NameValue(name, value) };
      }

      return m_emptyNameValues;
    }
  }

  private final class BodyURIMatchList extends AbstractMatchList {

    public BodyURIMatchList(String body) {
      super(m_regularExpressions.getHyperlinkURIPattern().matcher(body));
    }

    protected NameValue[] parseMatch() {
      final List<NameValue> result = new ArrayList<NameValue>();

      final String uri = getMatcher().group(1);

      m_uriParser.parse(uri, new URIParser.AbstractParseListener() {
        public boolean pathParameterNameValue(String name, String value) {
          result.add(new NameValue(name, value));
          return true;
        }

        public boolean queryStringNameValue(String name, String value) {
          result.add(new NameValue(name, value));
          return true;
        }
      });

      return result.toArray(new NameValue[result.size()]);
    }
  }
}
