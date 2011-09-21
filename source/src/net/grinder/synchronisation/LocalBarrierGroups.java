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

import net.grinder.communication.CommunicationException;
import net.grinder.synchronisation.BarrierGroup.BarrierIdentityGenerator;
import net.grinder.synchronisation.messages.BarrierIdentity;


/**
 * {@link BarrierGroups} implementation for use within the local JVM.
 *
 * @author Philip Aston
 */
public class LocalBarrierGroups extends AbstractBarrierGroups {

  private final BarrierIdentityGenerator m_identityGenerator =
    new IdentityGeneratorImplementation("LOCAL");

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

  // TODO: could be a wrapper. Is this really a win?
  private class LocalBarrierGroup extends AbstractBarrierGroup {
    public LocalBarrierGroup(String name) {
      super(name);
    }

    /**
     * {@inheritDoc}
     */
    @Override public void removeBarriers(long n) throws CommunicationException {
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
    @Override public void addWaiter(BarrierIdentity barrierIdentity)
      throws CommunicationException {

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
