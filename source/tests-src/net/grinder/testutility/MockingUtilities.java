// Copyright (C) 2011 Philip Aston
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

package net.grinder.testutility;

import java.util.Collection;

import org.hamcrest.Description;
import org.mockito.ArgumentMatcher;
import static org.mockito.Mockito.argThat;


/**
 * Mocking utility classes for use with Mockito.
 *
 * @author Philip Aston
 * @version $Revision$
 */
public class MockingUtilities {

  /**
   * Prettier version of {@link ArgumentMatcher}.
   *
   * @author Philip Aston
   */
  public abstract static class TypedArgumentMatcher<T>
    extends ArgumentMatcher<T> {

    @SuppressWarnings("unchecked")
    @Override public final boolean matches(Object argument) {
      try {
        return argumentMatches((T) argument);
      }
      catch (ClassCastException e) {
        return false;
      }
    }

    protected abstract boolean argumentMatches(T t);
  }

  public static <T extends Collection<?>> T equalContents(final T t) {
    return argThat(
      new TypedArgumentMatcher<T>() {
        @Override protected boolean argumentMatches(T actual) {
          return actual != null &&
                 actual.containsAll(t) &&
                 t.containsAll(actual);
        }

        @Override public void describeTo(Description description) {
          description.appendText("equalContents(" + t + ")");
        }
      });
  }
}
