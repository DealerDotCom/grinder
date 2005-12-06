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


/**
 * Object used for synchronisation.
 *
 * <p>
 * We have a general policy of ignoring {@link InterruptedException}s
 * throughout The Grinder code base as
 * <ul>
 * <li>They cause checked exception stupidity.</li>
 * <li>We can't hope to correctly handle {@link InterruptedException}s, or
 * exceptions thrown because of them, such that threads are truly interruptible.
 * Even if we could do this for The Grinder, we can't control third party
 * libraries.</li>
 * <li>We don't call {@link Thread#interrupt} anywhere.</li>
 * </ul>
 * </p>
 *
 * @author Philip Aston
 * @version $Revision$
 */
public class Monitor {

  /**
   * Wait until we are notified, or receive an {@link InterruptedException}.
   *
   * @see Object#wait
   */
  public void waitNoInterrruptException() {
    try {
      super.wait();
    }
    catch (InterruptedException e) {
      // Swallow.
    }
  }

  /**
   * Wait until we are notified, time out, or receive an
   * {@link InterruptedException}.
   *
   * @param timeout
   *          the maximum time to wait in milliseconds.
   * @see Object#wait(long)
   */
  public void waitNoInterrruptException(long timeout) {
    try {
      super.wait(timeout);
    }
    catch (InterruptedException e) {
      // Swallow.
    }
  }
}
