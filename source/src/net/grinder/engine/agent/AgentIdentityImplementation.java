// Copyright (C) 2005 - 2008 Philip Aston
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

package net.grinder.engine.agent;

import net.grinder.common.AgentIdentity;
import net.grinder.common.WorkerIdentity;
import net.grinder.util.UniqueIdentityGenerator;


/**
 * Agent process identity.
 *
 * Non-final so unit tests can extend.
 *
 * @author Philip Aston
 * @version $Revision$
 */
class AgentIdentityImplementation
  extends AbstractProcessIdentityImplementation implements AgentIdentity {

  private static final long serialVersionUID = -2217064199726714227L;

  private static final UniqueIdentityGenerator s_identityGenerator =
    new UniqueIdentityGenerator();

  private int m_nextWorkerNumber = 0;

  /**
   * Constructor.
   *
   * @param name The public name of the agent.
   */
  AgentIdentityImplementation(String name) {
    super(s_identityGenerator.createUniqueString(name), name);
  }

  /**
   * Create a worker identity.
   *
   * @param agentID The console allocated agent ID.
   * @return The worker identity.
   */
  WorkerIdentity createWorkerIdentity(int agentID) {
    return new WorkerIdentityImplementation(
      getName() + "-" + m_nextWorkerNumber++, agentID);
  }

  private final class WorkerIdentityImplementation
    extends AbstractProcessIdentityImplementation
    implements WorkerIdentity {

    private static final long serialVersionUID = 2;
    private final int m_agentID;

    private WorkerIdentityImplementation(String name, int agentID) {
      super(s_identityGenerator.createUniqueString(name), name);
      m_agentID = agentID;
    }

    public AgentIdentity getAgentIdentity() {
      return AgentIdentityImplementation.this;
    }

    public int getAgentID() {
      return m_agentID;
    }
  }
}
