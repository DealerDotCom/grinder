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

import net.grinder.util.ListenerSupport;


/**
 * Common listener support for {@link BarrierGroup} implementations.
 *
 * @author Philip Aston
 * @version $Revision:$
 */
abstract class AbstractBarrierGroup implements BarrierGroup {

  private final ListenerSupport<Listener> m_listeners =
    new ListenerSupport<Listener>();

  public AbstractBarrierGroup() {
    super();
  }

  /**
   * {@inheritDoc}
   */
  public void addListener(Listener listener) {
    m_listeners.add(listener);
  }

  /**
   * {@inheritDoc}
   */
  public void removeListener(Listener listener) {
    m_listeners.remove(listener);
  }

  /**
   * {@inheritDoc}
   */
  public void awaken() {
    m_listeners.apply(new ListenerSupport.Informer<Listener>() {
      public void inform(Listener listener) { listener.awaken(); }
    });
  }
}
