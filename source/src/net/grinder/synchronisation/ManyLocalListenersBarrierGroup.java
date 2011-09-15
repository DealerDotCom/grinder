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

import net.grinder.synchronisation.BarrierGroup.Listener;


/**
 * {@link BarrierGroup} decorator that manages multiple local listeners, and
 * dispatches a single awake callback to all listeners.
 *
 * @author Philip Aston
 * @version $Revision:$
 */
public class ManyLocalListenersBarrierGroup
  extends AbstractBarrierGroup implements Listener {

  private final BarrierGroup m_delegate;

  /**
   * Constructor.
   *
   * @param delegate Delegate barrier group.
   */
  public ManyLocalListenersBarrierGroup(BarrierGroup delegate) {
    m_delegate = delegate;
    m_delegate.addListener(this);
  }

  /**
   * {@inheritDoc}
   */
  public void addBarrier() {
    m_delegate.addBarrier();
  }

  /**
   * {@inheritDoc}
   */
  public void removeBarriers(int n) {
    m_delegate.removeBarriers(n);
  }

  /**
   * {@inheritDoc}
   */
  public void addWaiter(BarrierIdentity barrierIdentity) {
    m_delegate.addWaiter(barrierIdentity);
  }

  /**
   * {@inheritDoc}
   */
  public void cancelWaiter(BarrierIdentity barrierIdentity) {
    m_delegate.cancelWaiter(barrierIdentity);
  }

  /**
   * {@inheritDoc}
   */
  public String getName() {
    return m_delegate.getName();
  }
}
