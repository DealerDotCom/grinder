// Copyright (C) 2000, 2001, 2002, 2003 Philip Aston
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

package net.grinder.console.common;


/**
 * Exception that can be displayed through the user interface.
 * Supports internationalised text through {@link Resources}.
 *
 * @author Philip Aston
 * @version $Revision$
 */
public class DisplayMessageConsoleException extends ConsoleException {

  //  private final Resources m_resources;

  /**
   * Constructor.
   *
   * @param resourceKey Resource key that specifies message.
   * @param defaultMessage Default message to use if
   * <code>resourceKey</code> not found.
   */
  public DisplayMessageConsoleException(String resourceKey,
                                        String defaultMessage) {
    super(getMessage(resourceKey, defaultMessage));
  }

  private static String getMessage(String resourceKey, String defaultMessage) {
    final Resources resources = Resources.getSingleton();

    if (resources != null) {
      final String resourceValue = resources.getString(resourceKey, false);

      if (resourceValue != null) {
        return resourceValue;
      }
    }

    return defaultMessage;
  }
}
