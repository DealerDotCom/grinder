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

package net.grinder.util.thread;

import java.io.InterruptedIOException;

import net.grinder.common.UncheckedGrinderException;


/**
 * Make {@link InterruptedException}s and {@link InterruptedIOException}s
 * easier to propagate.
 *
 * <p>
 * Our policy on interrupt handling:
 *
 * <ul>
 * <li>{@link Thread#interrupt()} and {@link ThreadGroup#interrupt()} are used
 * in shut down code. We can't simply swallow {@link InterruptedException}s.
 * </li>
 *
 * <li>Whenever core code receives an {@link InterruptedException} which it
 * doesn't know how to handle, it should rethrow it in an
 * {@link UncheckedInterruptedException}.</li>
 *
 * <li>{@link InterruptibleRunnable#run()} implementations are carefully
 * reviewed to ensure that they do not ignore the interrupt condition and will
 * exit whenever {@link InterruptedException} and
 * {@link InterruptedIOException}s are received. They exit cleanly, handling
 * {@link UncheckedInterruptedException}s. We only interrupt code that
 * implements {@link InterruptibleRunnable#run()}.</li>
 *
 * <li>Other code may exit cleanly or may ignore the interrupt condition due to
 * third-party libraries swallowing {@link InterruptedException}s. This doesn't
 * matter as we should never interrupt this code.</li>
 * </ul>
 * </p>
 *
 * @author Philip Aston
 * @version $Revision$
 */
public class UncheckedInterruptedException extends UncheckedGrinderException {

  /**
   * Constructor.
   *
   * @param e The original InterruptedException.
   */
  public UncheckedInterruptedException(InterruptedException e) {
    super("Thread interrupted", e);
  }

  /**
   * Constructor.
   *
   * @param e The original InterruptedIOException.
   */
  public UncheckedInterruptedException(InterruptedIOException e) {
    super("Thread interrupted", e);
  }
}
