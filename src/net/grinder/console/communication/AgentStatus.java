// Copyright (C) 2004, 2005 Philip Aston
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

package net.grinder.console.communication;

import java.util.Set;


/**
 * Interface for enquiring about the currently connected agents.
 *
 * TODO - revise this in the light of new agent status reporting.
 *
 * @author Philip Aston
 * @version $Revision$
 */
public interface AgentStatus {

  /**
   * Get a Set&lt;ConnectionIdentity&gt; of connected agent processes.
   *
   * @return Copy of the set of connection identities.
   */
  Set getConnectedAgents();

  /**
   * Return whether there are any connected agents. Cheaper query than
   * {@link #getConnectedAgents}.
   *
   * @return Whether or not there are any connected agents.
   */
  boolean isAnAgentConnected();

  /**
   * Register a {@link ConnectionListener}.
   *
   * @param listener The listener.
   */
  void addConnectionListener(ConnectionListener listener);

  /**
   * Interface that clients can use to listen for agent connection
   * events.
   */
  interface ConnectionListener {
    /**
     * Called when one or more agents connect.
     */
    void agentConnected();

    /**
     * Called when one or more agents disconnect.
     */
    void agentDisconnected();
  }
}
