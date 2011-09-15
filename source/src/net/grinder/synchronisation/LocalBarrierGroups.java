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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import net.grinder.synchronisation.BarrierGroup.BarrierIdentity;
import net.grinder.synchronisation.BarrierGroup.BarrierIdentityGenerator;


/**
 * {@link BarrierGroups} implementation for use within the local JVM.
 *
 * @author Philip Aston
 * @version $Revision:$
 */
public class LocalBarrierGroups implements BarrierGroups {

  // Guarded by self.
  private final Map<String, BarrierGroup> m_groups =
    new HashMap<String, BarrierGroup>();

  private final BarrierIdentityGenerator m_identityGenerator =
    new LocalIdentityGenerator();

  /**
   * {@inheritDoc}
   */
  public BarrierGroup getGroup(String name) {
    synchronized (m_groups) {
      final BarrierGroup existing = m_groups.get(name);

      if (existing != null) {
        return existing;
      }

      final BarrierGroup newInstance = new BarrierGroupImplementation(name);
      m_groups.put(name, newInstance);

      return newInstance;
    }
  }

  /**
   * {@inheritDoc}
   */
  public BarrierIdentityGenerator getIdentityGenerator() {
    return m_identityGenerator;
  }

  private void removeBarrierGroup(BarrierGroup barrierGroup) {
    synchronized (m_groups) {
      m_groups.remove(barrierGroup.getName());
    }
  }

  /**
   * Basic {@link BarrierGroup} implementation.
   */
  class BarrierGroupImplementation extends AbstractBarrierGroup {

    private final String m_name;

    // Guarded by this. Negative <=> the group is invalid.
    private long m_barriers = 0;

    // Guarded by this.
    private final Set<BarrierIdentity> m_waiters =
      new HashSet<BarrierIdentity>();

    /**
     * Constructor.
     *
     * @param name Barrier group name.
     */
    public BarrierGroupImplementation(String name) {
      m_name = name;
    }

    private void checkValid() {
      if (m_barriers < 0) {
        throw new IllegalStateException("BarrierGroup is invalid");
      }
    }

    /**
     * {@inheritDoc}
     */
    public void addBarrier() {
      synchronized (this) {
        checkValid();

        ++m_barriers;
      }
    }

    /**
     * {@inheritDoc}
     */
    public void removeBarriers(int n) {
      final boolean wakeListeners;

      synchronized (this) {
        checkValid();

        if (n > m_barriers - m_waiters.size()) {
          throw new IllegalStateException(
            "Can't remove " + n + " barriers from " +
            m_barriers + " barriers, " + m_waiters.size() + " waiters");
        }

        m_barriers -= n;

        if (m_barriers == 0) {
          wakeListeners = false;
          removeBarrierGroup(this);
          m_barriers = -1;
        }
        else {
          wakeListeners = checkCondition();
        }
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
        checkValid();

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

  private class LocalIdentityGenerator implements BarrierIdentityGenerator {

    private final AtomicInteger m_next = new AtomicInteger();

    public BarrierIdentity next() {
      return new LocalBarrierIdentity(m_next.getAndIncrement());
    }
  }

  private static final class LocalBarrierIdentity implements BarrierIdentity {

    private final int m_value;

    public LocalBarrierIdentity(int value) {
      m_value = value;
    }

    @Override public int hashCode() {
      return m_value;
    }

    @Override public boolean equals(Object o) {
      if (this == o) {
        return true;
      }

      if (o == null || getClass() != o.getClass()) {
        return false;
      }

      return m_value == ((LocalBarrierIdentity) o).m_value;
    }
  }
}
