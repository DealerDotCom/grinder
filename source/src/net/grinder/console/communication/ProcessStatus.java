// Copyright (C) 2004, 2005, 2006 Philip Aston
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

import java.util.EventListener;

import net.grinder.common.AgentProcessReport;
import net.grinder.common.WorkerProcessReport;


/**
 * Handles process status information.
 *
 * @author Dirk Feufel
 * @author Philip Aston
 * @version $Revision$
 */
public interface ProcessStatus {

  /**
   * Add a new listener.
   *
   * @param listener A listener.
   */
  void addListener(Listener listener);

  /**
   * How many agents are live?
   *
   * @return The number of agents.
   */
  int getNumberOfLiveAgents();

  /**
   * Listener interface for receiving updates about process status.
   */
  interface Listener extends EventListener {

    /**
     * Called with regular updates on process status.
     *
     * @param processReports
     *          Process status information.
     * @param newAgent
     *          A new agent has connected since the last update. This is used to
     *          invalidate the cache distribution status; it will go away when
     *          we have per-agent cache updates.
     */
    void update(ProcessReports[] processReports, boolean newAgent);
  }

  /**
   * Interface to the information the console has about an agent and its
   * worker processes.
   */
  interface ProcessReports {

    /**
     * Returns the latest agent process report.
     *
     * @return The agent process report.
     */
    AgentProcessReport getAgentProcessReport();

    /**
     * Returns the latest worker process reports.
     *
     * @return The worker process reports.
     */
    WorkerProcessReport[] getWorkerProcessReports();
  }
}
