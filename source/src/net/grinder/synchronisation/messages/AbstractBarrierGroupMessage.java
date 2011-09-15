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

import net.grinder.common.processidentity.WorkerIdentity;
import net.grinder.communication.Message;


/**
 * Common implementation for barrier group messages.
 *
 * @author Philip Aston
 * @version $Revision:$
 */
public abstract class AbstractBarrierGroupMessage implements Message {

  private static final long serialVersionUID = 1L;

  private final WorkerIdentity m_processIdentity;
  private final String m_name;

  /**
   * Constructor.
   *
   * @param processIdentity Worker process identity.
   * @param name Barrier name.
   */
  public AbstractBarrierGroupMessage(WorkerIdentity processIdentity,
                                     String name) {
    m_processIdentity = processIdentity;
    m_name = name;
  }

  /**
   * Worker process identity.
   *
   * @return The process identity.
   */
  public WorkerIdentity getProcessIdentity() {
    return m_processIdentity;
  }

  /**
   * Barrier name.
   *
   * @return The barrier name.
   */
  public String getName() {
    return m_name;
  }
}
