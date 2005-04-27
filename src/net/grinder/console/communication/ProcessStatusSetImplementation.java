// Copyright (C) 2001, 2002, 2003, 2004, 2005 Philip Aston
// Copyright (C) 2001, 2002 Dirk Feufel
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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import net.grinder.common.AgentProcessStatus;
import net.grinder.common.ProcessStatus;
import net.grinder.common.WorkerProcessStatus;
import net.grinder.util.ListenerSupport;


/**
 * Handles process status information.
 *
 * TODO change to remove reliance on timing. Listen to connection events instead.
 *
 * @author Dirk Feufel
 * @author Philip Aston
 * @version $Revision$
 */
final class ProcessStatusSetImplementation implements ProcessStatusSet {

  /**
   * Period at which to update the listeners.
   */
  private static final long UPDATE_PERIOD = 500;

  /**
   * We keep a record of processes for a few seconds after they have been
   * terminated.
   *
   * Every FLUSH_PERIOD, process statuses are checked. Those that are finished,
   * or that are running and haven't reported for a while, are marked and are
   * discarded if they still haven't been updated by the next FLUSH_PERIOD.
   */
  private static final long FLUSH_PERIOD = 2000;

  // Map of agent names to AgentAndWorkers instances. Access is synchronised on
  // the map itself.
  private final Map m_processData = new HashMap();

  private final ListenerSupport m_listeners = new ListenerSupport();

  // No need to synchronise access to these; operations are atomic on
  // booleans and ints.
  private boolean m_newData = false;
  private int m_currentGeneration = 0;

  /**
   * Constructor.
   *
   * @param timer Timer which can be used to schedule housekeeping tasks.
   */
  public ProcessStatusSetImplementation(Timer timer) {
    timer.schedule(
      new TimerTask() {
        public void run() { update(); }
      },
      0, UPDATE_PERIOD);

    timer.schedule(
      new TimerTask() {
        public void run() {
          synchronized (m_processData) {
            purge(m_processData);
          }
        }
      },
      0, FLUSH_PERIOD);
  }

  /**
   * Add a new listener.
   *
   * @param listener A listener.
   */
  public void addListener(ProcessStatusListener listener) {
    m_listeners.add(listener);
  }

  private void update() {
    if (!m_newData) {
      return;
    }

    m_newData = false;

    final AgentAndWorkers[] processStatuses;

    synchronized (m_processData) {
      processStatuses = (AgentAndWorkers[])
        m_processData.values().toArray(
          new AgentAndWorkers[m_processData.size()]);
    }

    m_listeners.apply(
      new ListenerSupport.Informer() {
        public void inform(Object listener) {
          ((ProcessStatusListener)listener).update(processStatuses);
        }
      });
  }

  private AgentAndWorkers getAgentAndWorkers(String agentName) {
    synchronized (m_processData) {
      final AgentAndWorkers existing =
        (AgentAndWorkers)m_processData.get(agentName);

      if (existing != null) {
        return existing;
      }

      final AgentAndWorkers created = new AgentAndWorkers(agentName);
      m_processData.put(agentName, created);
      return created;
    }
  }

  /**
   * Add an agent status report.
   *
   * @param agentProcessStatus Process status.
   */
  public void addAgentStatusReport(AgentProcessStatus agentProcessStatus) {

    getAgentAndWorkers(agentProcessStatus.getName())
      .setAgentProcessStatus(agentProcessStatus);

    m_newData = true;
  }

  /**
   * Add a worker status report.
   *
   * @param workerProcessStatus Process status.
   */
  public void addWorkerStatusReport(WorkerProcessStatus workerProcessStatus) {

    getAgentAndWorkers(workerProcessStatus.getAgentName())
      .setWorkerProcessStatus(workerProcessStatus);

    m_newData = true;
  }

  /**
   * Callers are responsible for synchronisation.
   */
  private void purge(Map purgableMap) {
    final Set zombies = new HashSet();

    final Iterator iterator = purgableMap.entrySet().iterator();

    while (iterator.hasNext()) {
      final Map.Entry entry = (Map.Entry)iterator.next();
      final String key = (String)entry.getKey();
      final Purgable purgable = (Purgable)entry.getValue();

      if (purgable.shouldPurge()) {
        zombies.add(key);
      }
    }

    if (zombies.size() > 0) {
      purgableMap.keySet().removeAll(zombies);
      m_newData = true;
    }

    ++m_currentGeneration;
  }

  private interface Purgable {
    boolean shouldPurge();
  }

  private abstract class AbstractTimedReference implements Purgable {
    private final int m_generation;
    private int m_purgeDelayCount;

    protected AbstractTimedReference() {
      m_generation = m_currentGeneration;
    }

    public final boolean shouldPurge() {
      // Processes have a short time to report after an event - see
      // the javadoc for FLUSH_PERIOD.
      if (m_purgeDelayCount > 1) {
        return true;
      }

      final int state = getState();

      if (state == ProcessStatus.STATE_RUNNING &&
          m_generation < m_currentGeneration - 1 ||
          state == ProcessStatus.STATE_FINISHED ||
          state == ProcessStatus.STATE_UNKNOWN) {
        ++m_purgeDelayCount;
      }

      return false;
    }

    protected abstract int getState();
  }

  private final class AgentReference extends AbstractTimedReference {
    private final AgentProcessStatus m_agentProcessStatus;

    AgentReference(AgentProcessStatus agentProcessStatus) {
      m_agentProcessStatus = agentProcessStatus;
    }

    public AgentProcessStatus getAgentProcessStatus() {
      return m_agentProcessStatus;
    }

    protected int getState() {
      return getAgentProcessStatus().getState();
    }
  }

  private final class WorkerReference extends AbstractTimedReference {
    private final WorkerProcessStatus m_workerProcessStatus;

    WorkerReference(WorkerProcessStatus workerProcessStatus) {
      m_workerProcessStatus = workerProcessStatus;
    }

    public WorkerProcessStatus getWorkerProcessStatus() {
      return m_workerProcessStatus;
    }

    protected int getState() {
      return getWorkerProcessStatus().getState();
    }
  }

  private static final class UnknownAgentProcessStatus
    implements AgentProcessStatus {

    private final String m_name;

    public UnknownAgentProcessStatus(String name) {
      m_name = name;
    }

    public String getName() {
      return m_name;
    }

    public short getState() {
      return STATE_UNKNOWN;
    }

    public int getNumberOfRunningProcesses() {
      return 0;
    }

    public int getMaximumNumberOfProcesses() {
      return 0;
    }
  }

  /**
   * Implementation of {@link ProcessStatusListener.AgentAndWorkers}.
   */
  public final class AgentAndWorkers
    implements ProcessStatusListener.AgentAndWorkers, Purgable {

    // Unsynchronised because changing the reference is atomic.
    private AgentReference m_agentStatusReference;

    // Synchronise on map before accessing.
    private final Map m_workerStatusReferences = new HashMap();

    AgentAndWorkers(String name) {
      setAgentProcessStatus(new UnknownAgentProcessStatus(name));
    }

    void setAgentProcessStatus(AgentProcessStatus agentProcessStatus) {
      m_agentStatusReference = new AgentReference(agentProcessStatus);
    }

    public AgentProcessStatus getAgentProcessStatus() {
      return m_agentStatusReference.getAgentProcessStatus();
    }

    void setWorkerProcessStatus(WorkerProcessStatus workerProcessStatus) {

      synchronized (m_workerStatusReferences) {
        m_workerStatusReferences.put(workerProcessStatus.getIdentity(),
                                     new WorkerReference(workerProcessStatus));
      }
    }

    public WorkerProcessStatus[] getWorkerProcessStatuses() {

      synchronized (m_workerStatusReferences) {
        final WorkerProcessStatus[] result =
          new WorkerProcessStatus[m_workerStatusReferences.size()];

        final Iterator iterator = m_workerStatusReferences.values().iterator();
        int i = 0;

        while (iterator.hasNext()) {
          result[i++] =
            ((WorkerReference)iterator.next()).getWorkerProcessStatus();
        }

        return result;
      }
    }

    public boolean shouldPurge() {
      synchronized (m_workerStatusReferences) {
        purge(m_workerStatusReferences);
      }

      return m_agentStatusReference.shouldPurge();
    }
  }
}
