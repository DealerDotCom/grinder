// Copyright (C) 2001, 2002, 2003, 2004 Philip Aston
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

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import net.grinder.common.ProcessStatus;


/**
 * Handles process status information.
 *
 * @author Dirk Feufel
 * @author Philip Aston
 * @version $Revision$
 */
final class ProcessStatusSet {

  /**
   * Period at which to update the listeners.
   */
  private static final long UPDATE_PERIOD = 500;

  /**
   * Every FLUSH_PERIOD, process statuses are checked. Those that
   * haven't reported since a {start|stop|reset} event are marked, and
   * are discarded if they still haven't been updated by the next
   * FLUSH_PERIOD.
   */
  private static final long FLUSH_PERIOD = 2000;

  private final Map m_processes = new HashMap();
  private final Timer m_timer = new Timer(true);
  private final Comparator m_sorter = new ProcessStatusComparator();

  /**
   * Synchronise on m_listeners before accessing.
   */
  private final List m_listeners = new LinkedList();

  // No need to synchronise access to these; operations are atomic on
  // booleans and ints.
  private boolean m_newData = false;
  private int m_lastProcessEventGeneration = -1;
  private int m_currentGeneration = 0;

  /**
   * Constructor.
   */
  public ProcessStatusSet() {
  }

  /**
   * Separate from the constructor for the unit tests.
   */
  void startProcessing() {
    m_timer.schedule(
      new TimerTask() {
        public void run() { update(); }
      },
      0, ProcessStatusSet.UPDATE_PERIOD);

    m_timer.schedule(
      new TimerTask() {
        public void run() { flush(); }
      },
      0, ProcessStatusSet.FLUSH_PERIOD);
  }

  /**
   * Add a new listener.
   *
   * @param listener A listener.
   */
  public void addListener(ProcessStatusListener listener) {
    synchronized (m_listeners) {
      m_listeners.add(listener);
    }
  }

  /**
   * Package scope for unit tests.
   */
  void update() {
    if (!m_newData) {
      return;
    }

    m_newData = false;

    final ProcessStatus[] data;

    synchronized (this) {
      data = (ProcessStatus[])
        m_processes.values().toArray(new ProcessStatus[m_processes.size()]);
    }

    Arrays.sort(data, m_sorter);

    int runningThreads = 0;
    int totalThreads = 0;

    for (int i = 0; i < data.length; ++i) {
      runningThreads += data[i].getNumberOfRunningThreads();
      totalThreads += data[i].getTotalNumberOfThreads();
    }

    synchronized (m_listeners) {
      final Iterator iterator = m_listeners.iterator();

      while (iterator.hasNext()) {
        final ProcessStatusListener listener =
          (ProcessStatusListener)iterator.next();

        listener.update(data, runningThreads, totalThreads);
      }
    }
  }

  /**
   * Use to notify this object of a start/reset/stop event.
   */
  public void processEvent() {
    m_lastProcessEventGeneration = ++m_currentGeneration;
  }

  /**
   * Add a status report.
   *
   * @param processStatus Process status.
   */
  public void addStatusReport(ProcessStatus processStatus) {

    synchronized (this) {
      ProcessStatusImplementation processStatusImplementation =
        (ProcessStatusImplementation)
        m_processes.get(processStatus.getIdentity());

      if (processStatusImplementation == null) {
        processStatusImplementation =
          new ProcessStatusImplementation(processStatus.getIdentity(),
                                          processStatus.getName());

        m_processes.put(processStatus.getIdentity(),
                        processStatusImplementation);
      }

      processStatusImplementation.set(processStatus);
    }

    m_newData = true;
  }

  /**
   * Package scope for unit tests.
   */
  void flush() {
    final Set zombies = new HashSet();

    synchronized (this) {
      final Iterator iterator = m_processes.entrySet().iterator();

      while (iterator.hasNext()) {
        final Map.Entry entry = (Map.Entry)iterator.next();
        final String key = (String)entry.getKey();
        final ProcessStatusImplementation processStatusImplementation =
          (ProcessStatusImplementation)entry.getValue();

        if (processStatusImplementation.shouldPurge()) {
          zombies.add(key);
        }
      }

      if (zombies.size() > 0) {
        m_processes.keySet().removeAll(zombies);
        m_newData = true;
      }
    }
  }

  private final class ProcessStatusImplementation implements ProcessStatus {
    private final String m_identity;
    private final String m_name;
    private short m_state;
    private short m_totalNumberOfThreads;
    private short m_numberOfRunningThreads;

    private boolean m_reapable = false;
    private int m_lastTouchedGeneration;

    ProcessStatusImplementation(String identity, String name) {
      m_identity = identity;
      m_name = name;
    }

    void set(ProcessStatus processStatus) {
      m_state = processStatus.getState();
      m_totalNumberOfThreads = processStatus.getTotalNumberOfThreads();
      m_numberOfRunningThreads =
        processStatus.getNumberOfRunningThreads();

      m_lastTouchedGeneration = m_currentGeneration;
      m_reapable = false;
    }

    boolean shouldPurge() {
      if (m_reapable) {
        return true;
      }
      else if (m_lastTouchedGeneration < m_lastProcessEventGeneration) {
        // Processes have a short time to report after an event - see
        // the javadoc for FLUSH_PERIOD.
        m_reapable = true;
      }

      return false;
    }

    public String getIdentity() {
      return m_identity;
    }

    public String getName() {
      return m_name;
    }

    public short getState() {
      return m_state;
    }

    public short getNumberOfRunningThreads() {
      return m_numberOfRunningThreads;
    }

    public short getTotalNumberOfThreads() {
      return m_totalNumberOfThreads;
    }
  }

  private static final class ProcessStatusComparator implements Comparator {
    public int compare(Object o1, Object o2) {
      final ProcessStatus p1 = (ProcessStatus)o1;
      final ProcessStatus p2 = (ProcessStatus)o2;

      final int compareState = p1.getState() - p2.getState();

      if (compareState != 0) {
        return compareState;
      }
      else {
        return p1.getName().compareTo(p2.getName());
      }
    }
  }
}
