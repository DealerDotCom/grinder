// Copyright (C) 2006 Philip Aston
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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import junit.framework.TestCase;


/**
 * Unit tests for {@link RegularExpressionsImplementation}.
 *
 * @author Philip Aston
 * @version $Revision$
 */
public class TestRegularExpressionsImplementation extends TestCase {

  public void testHyperlinkURIPattern() throws Exception {
    final RegularExpressions regularExpressions =
      new RegularExpressionsImplementation();

    final Pattern pattern = regularExpressions.getHyperlinkURIPattern();

    final String text =
      "  blah='./foo' <a href=\"http://bah\">http://blah</a>\n" +
      "<a href='#fragment'/>";

    final Matcher matcher = pattern.matcher(text);

    assertTrue(matcher.find());
    assertEquals("http://bah", matcher.group(1));

    assertTrue(matcher.find());
    assertEquals("#fragment", matcher.group(1));

    assertFalse(matcher.find());
  }
}
