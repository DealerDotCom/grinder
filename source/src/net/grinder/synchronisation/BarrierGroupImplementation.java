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

package net.grinder.synchronisation;

import java.util.HashSet;
import java.util.Set;


/**
 * Basic {@link BarrierGroup} implementation.
 *
 * @author Philip Aston
 * @version $Revision:$
 */
public class BarrierGroupImplementation extends AbstractBarrierGroup {

  private final String m_name;

  // Guarded by this.
  private long m_barriers = 0;

  // Guarded by this.
  private Set<BarrierIdentity> m_waiters = new HashSet<BarrierIdentity>();

  /**
   * Constructor.
   *
   * @param name Barrier group name.
   */
  public BarrierGroupImplementation(String name) {
    m_name = name;
  }

  /**
   * {@inheritDoc}
   */
  public void addBarrier() {
    synchronized (this) {
      ++m_barriers;
    }
  }

  /**
   * {@inheritDoc}
   */
  public void removeBarriers(int n) {
    final boolean wakeListeners;

    synchronized (this) {
      if (n > m_barriers - m_waiters.size()) {
        throw new IllegalStateException(
          "Can't remove " + n + " barriers from " +
          m_barriers + " barriers, " + m_waiters.size() + " waiters");
      }

      m_barriers -= n;

      wakeListeners = checkCondition();
    }

    if (wakeListeners) {
      awaken();
    }
  }

  /**
   * {@inheritDoc}
   */
  public void addWaiter(BarrierIdentity barrierIdentity) {
    final boolean wakeListeners;

    synchronized (this) {
      // Can only happen if m_barriers == 0.
      if (m_barriers == 0) {
        throw new IllegalStateException("Can't add waiter, no barriers");
      }

      assert m_waiters.size() < m_barriers;

      m_waiters.add(barrierIdentity);

      wakeListeners = checkCondition();
    }

    if (wakeListeners) {
      awaken();
    }
  }

  /**
   * {@inheritDoc}
   */
  public void cancelWaiter(BarrierIdentity barrierIdentity) {
    synchronized (this) {
      m_waiters.remove(barrierIdentity);
    }
  }

  /**
   * {@inheritDoc}
   */
  public String getName() {
    return m_name;
  }

  private boolean checkCondition() {
    if (m_barriers > 0 && m_barriers == m_waiters.size()) {
      m_waiters.clear();

      // The caller will notify the listeners after releasing the lock to
      // minimise the length of time it is held. Otherwise the distributed
      // nature of the communication might delay subsequent operations
      // significantly.
      // This does not cause a race from the perspective of an individual
      // waiting thread, since it cannot proceed until its barrier is woken or
      // cancelled, and once cancelled a barrier cannot be re-used.
        return true;
    }

    return false;
  }
}
