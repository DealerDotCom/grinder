// Copyright (C) 2005 Philip Aston
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

package net.grinder.plugin.http.tcpproxyfilter;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;


/**
 * Helper functions for style sheets.
 *
 * @author Philip Aston
 * @version $Revision$
 */
public final class XSLTHelper {
  private static DateFormat s_iso8601DateFormat =
    new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

  /**
   * Convert an ISO 8601 date/time string to a more friendly, locale specific
   * string.
   *
   *
   * @param iso8601
   *          An extended format ISO 8601 date/time string
   * @return The formated date/time.
   * @throws ParseException
   *           If the date could not be parsed.
   */
  public String formatTime(String iso8601) throws ParseException {
    final Date date = s_iso8601DateFormat.parse(iso8601);
    return DateFormat.getDateTimeInstance().format(date);
  }

  /**
   * Turn a list of nodes with 'name' and 'value' attributes into a list
   * of NVPair scriptlet.
   *
   * @param nodes The nodes.
   * @param level Indentation level.
   * @return The scriptlet.
   */
  public String formatNVPairList(NodeList nodes, int level) {

    final StringBuffer result = new StringBuffer();

    final int n = nodes.getLength();

    result.append("(");

    for (int i = 0; i < n; ++i) {
      final NamedNodeMap attributes = nodes.item(i).getAttributes();

      if (i == 0) {
        result.append(" ");
      }
      else {
        result.append(newLineAndIndent(level + 1));
      }

      result.append("NVPair(");
      result.append(
        quoteForPython(attributes.getNamedItem("name").getNodeValue()));
      result.append(", ");
      result.append(
        quoteForPython(attributes.getNamedItem("value").getNodeValue()));
      result.append("),");
    }

    result.append(" )");

    return result.toString();
  }

  /**
   * Wrap string in appropriate quotes for Python.
   *
   * @param value The string.
   * @return The quoted string.
   */
  public String quoteForPython(String value) {
    if (value == null) {
      return "None";
    }

    final StringBuffer result = new StringBuffer();

    final String quotes = value.indexOf("\n") > -1 ? "'''" : "'";

    result.append(quotes);

    final int length = value.length();

    for (int i = 0; i < length; ++i) {
      final char c = value.charAt(i);

      switch (c) {
      case '\'':
      case '\\':
        result.append('\\');
        // fall through.
      default:
        result.append(c);
      }
    }

    result.append(quotes);

    return result.toString();
  }

  /**
   * Return an appropriately sized indent string.
   *
   * @param level Indent level.
   * @return The string.
   */
  public String indent(int level) {
    return "                ".substring(0, level * 2);
  }

  /**
   * Return a new line string.
   *
   * <p>
   * When calling this from a style sheet, don't forget the call braces or
   * you'll end up with a no-op.
   * </p>
   *
   * @return The string.
   */
  public String newLine() {
    return "\n";
  }

  /**
   * Equivalent to {@link #newLine()} followed by {@link #indent(int)).
   *
   * @param level Indent level.
   * @return The string.
   */
  public String newLineAndIndent(int level) {
    return newLine() + indent(level);
  }
}
