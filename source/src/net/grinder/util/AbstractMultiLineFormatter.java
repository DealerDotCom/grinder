// Copyright (C) 2007 Philip Aston
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

package net.grinder.util;


/**
 * Abstract implementation of MultiLineFormatter.
 *
 * @author Philip Aston
 * @version $Revision:$
 */
public abstract class AbstractMultiLineFormatter implements MultiLineFormatter {

  /**
   * Alter buffer to contain a single line according to the policy of
   * the formatter. Insert remaining text at the start of
   * <code>remainder</code>.
   *
   * @param buffer Buffer to transform to a single line.
   * @param remainder Leftovers.
   */
  public abstract void transform(StringBuffer buffer, StringBuffer remainder);

  /**
   * Convenience method.
   *
   * @param input Input text.
   * @return Formatted result.
   */
  public String format(String input) {
    final StringBuffer result = new StringBuffer();
    StringBuffer buffer = new StringBuffer(input);

    while (buffer.length() > 0) {
      final StringBuffer remainder = new StringBuffer();
      transform(buffer, remainder);

      if (result.length() > 0) {
        result.append("\n");
      }

      result.append(buffer.toString());

      buffer = remainder;
    }

    return result.toString();
  }
}