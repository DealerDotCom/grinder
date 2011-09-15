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

import java.util.concurrent.atomic.AtomicInteger;

import net.grinder.synchronisation.BarrierGroup.BarrierIdentity;
import net.grinder.synchronisation.BarrierGroup.BarrierIdentityGenerator;


/**
 * {@link BarrierGroups} implementation for use within the local JVM.
 *
 * @author Philip Aston
 * @version $Revision:$
 */
public class LocalBarrierGroups extends AbstractBarrierGroups {

  private final BarrierIdentityGenerator m_identityGenerator =
    new LocalIdentityGenerator();

  /**
   * {@inheritDoc}
   */
  public BarrierIdentityGenerator getIdentityGenerator() {
    return m_identityGenerator;
  }

  /**
   * {@inheritDoc}
   */
  @Override protected InternalBarrierGroup createBarrierGroup(String name) {
    return new LocalBarrierGroup(name);
  }

  private class LocalIdentityGenerator implements BarrierIdentityGenerator {

    private final AtomicInteger m_next = new AtomicInteger();

    public BarrierIdentity next() {
      return new LocalBarrierIdentity(m_next.getAndIncrement());
    }
  }

  private class LocalBarrierGroup extends AbstractBarrierGroup {
    public LocalBarrierGroup(String name) {
      super(name);
    }

    /**
     * {@inheritDoc}
     */
    public void removeBarriers(int n) {
      final boolean wakeListeners;

      synchronized (this) {
        super.removeBarriers(n);
        wakeListeners = checkConditionLocal();
      }

      if (wakeListeners) {
        fireAwaken();
      }
    }

    /**
     * {@inheritDoc}
     */
    public void addWaiter(BarrierIdentity barrierIdentity) {
      final boolean wakeListeners;

      synchronized (this) {
        super.addWaiter(barrierIdentity);
        wakeListeners = checkConditionLocal();
      }

      if (wakeListeners) {
        fireAwaken();
      }
    }
  }
}
