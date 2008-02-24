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

import net.grinder.communication.Address;
import net.grinder.messages.console.AgentIdentity;
import net.grinder.messages.console.WorkerIdentity;
import net.grinder.util.AllocateLowestNumber;
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

  private static final long serialVersionUID = 2;

  private static final UniqueIdentityGenerator s_identityGenerator =
    new UniqueIdentityGenerator();

  private final transient AllocateLowestNumber m_workerProcessNumberMap;

  private int m_number = -1;
  private int m_nextWorkerNumber = 0;

  /**
   * Constructor.
   *
   * @param name The public name of the agent.
   */
  AgentIdentityImplementation(String name,
                              AllocateLowestNumber workerProcessNumberMap) {
    super(s_identityGenerator.createUniqueString(name), name);
    m_workerProcessNumberMap = workerProcessNumberMap;
  }

  /**
   * Return the console allocated agent number.
   *
   * @return The number.
   */
  public int getNumber() {
    return m_number;
  }
  /**
   * Set the console allocated agent number.
   *
   * @param number The number.
   */
  public void setNumber(int number) {
    m_number = number;
  }

  /**
   * Address ourself.
   *
   * @param address
   *            Address to compare.
   * @return <code>true</code> if and only if <code>address</code> is this
   *         object.
   */
  public boolean includes(Address address) {
    return equals(address);
  }

  /**
   * Create a worker identity.
   *
   * @param processNumber The agent allocated worker process number.
   * @return The worker identity.
   */
  WorkerIdentityImplementation createWorkerIdentity() {
    final WorkerIdentityImplementation result =
      new WorkerIdentityImplementation(getName() + "-" + m_nextWorkerNumber++);
    result.setNumber(m_workerProcessNumberMap.add(result));
    return result;
  }

  /**
   * Worker process identity.
   *
   * @author Philip Aston
   * @version $Revision$
   */
  final class WorkerIdentityImplementation
    extends AbstractProcessIdentityImplementation
    implements WorkerIdentity {

    private static final long serialVersionUID = 3;
    private int m_number;

    private WorkerIdentityImplementation(String name) {
      super(s_identityGenerator.createUniqueString(name), name);
    }

    private void setNumber(int number) {
      m_number = number;
      // We could call setName() here to ensure that our name was always
      // based on the worker process number. This might not be desriable
      // in future when worker process numbers may be reused. Deferring
      // making a decision until its important.
    }

    public AgentIdentity getAgentIdentity() {
      return AgentIdentityImplementation.this;
    }

    public int getNumber() {
      return m_number;
    }

    public void destroy() {
      assert m_workerProcessNumberMap != null;
      m_workerProcessNumberMap.remove(this);
    }
  }
}
