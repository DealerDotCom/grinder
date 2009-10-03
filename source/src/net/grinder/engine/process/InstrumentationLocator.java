// Copyright (C) 2008 Philip Aston
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

package net.grinder.engine.process;


/**
 * Static methods that weaved code uses to dispatch enter and exit calls to the
 * appropriate {@link ScriptEngine.TestInstrumentation}.
 *
 * @author Philip Aston
 * @version $Revision:$
 */
public class InstrumentationLocator {

  /**
   * Called when a weaved method is entered.
   *
   * @param target
   *          The reference used to call the method.
   * @param locationID
   *          Unique identity generated when the method was instrumented.
   */
  public static void enter(Object target, String locationID) {
    System.out.printf("PRE(%s, %s)%n", target, locationID);
  }

  /**
   * Called when a weaved method is exited.
   *
   * @param target
   *          The reference used to call the method.
   * @param locationID
   *          Unique identity generated when the method was instrumented.
   * @param success
   *          {@code true} if the exit was a normal return, {code false} if an
   *          exception was thrown.
   */
  public static void exit(Object target, String locationID, boolean success) {
    System.out.printf("POST(%s, %s, %s)%n", target, locationID, success);
  }
}
