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

package net.grinder.synchronisation.messages;

import net.grinder.synchronisation.BarrierGroup.BarrierIdentity;


/**
 * Barrier group message requesting that a waiter be removed.
 *
 * @author Philip Aston
 * @version $Revision:$
 */
public class CancelWaiterMessage extends AbstractBarrierGroupMessage {

  private static final long serialVersionUID = 1L;

  private final BarrierIdentity m_barrierIdentity;

  /**
   * Constructor.
   *
   * @param name
   *          Barrier name.
   * @param barrierIdentity
   *          The identity of the waiter.
   */
  public CancelWaiterMessage(String name,
                             BarrierIdentity barrierIdentity) {
    super(name);
    m_barrierIdentity = barrierIdentity;
  }

  /**
   * Identifies the waiter.
   *
   * @return The identity of the waiter.
   */
  public BarrierIdentity getBarrierIdentity() {
    return m_barrierIdentity;
  }
}
