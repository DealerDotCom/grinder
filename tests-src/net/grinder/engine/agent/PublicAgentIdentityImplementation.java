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

package net.grinder.engine.agent;

import net.grinder.common.AgentIdentity;
import net.grinder.common.WorkerIdentity;

/**
 * An AgentIdentityImplementation that is public so unit tests from other
 * packages can use it.
 *
 * @author Philip Aston
 * @version $Revision$
 */
public final class PublicAgentIdentityImplementation implements AgentIdentity {

  private final AgentIdentityImplementation m_agentIdentityImplementation;

  public PublicAgentIdentityImplementation(String name) {
    m_agentIdentityImplementation = new AgentIdentityImplementation(name);
  }

  public String getName() {
    return m_agentIdentityImplementation.getName();
  }

  public WorkerIdentity createWorkerIdentity() {
    return m_agentIdentityImplementation.createWorkerIdentity();
  }

  public int hashCode() {
    return m_agentIdentityImplementation.hashCode();
  }

  public boolean equals(Object o) {

    if (!(o instanceof PublicAgentIdentityImplementation)) {
      return false;
    }

    final PublicAgentIdentityImplementation other =
      (PublicAgentIdentityImplementation)o;

    return m_agentIdentityImplementation.equals(
      other.m_agentIdentityImplementation);
  }
}
