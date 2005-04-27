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

package net.grinder.console.swingui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import net.grinder.common.AgentProcessStatus;
import net.grinder.common.ProcessStatus;
import net.grinder.common.WorkerProcessStatus;
import net.grinder.console.common.ConsoleException;
import net.grinder.console.common.Resources;
import net.grinder.console.communication.ProcessControl;
import net.grinder.console.communication.ProcessStatusListener;


/**
 * TableModel for the process status table. No need to synchronise,
 * all calls after initialisation are dispatched to us in the
 * SwingThread.
 *
 * @author Dirk Feufel
 * @author Philip Aston
 * @version $Revision$
 */
class ProcessStatusTableModel
  extends AbstractTableModel implements Table.TableModel {

  private static final int ID_COLUMN_INDEX = 0;
  private static final int TYPE_COLUMN_INDEX = 1;
  private static final int STATE_COLUMN_INDEX = 2;

  private final ProcessStatusComparator m_processStatusComparator =
    new ProcessStatusComparator();

  private final Comparator m_agentComparator = new Comparator() {
    public int compare(Object o1, Object o2) {
      return m_processStatusComparator.compare(
        ((ProcessStatusListener.AgentAndWorkers)o1).getAgentProcessStatus(),
        ((ProcessStatusListener.AgentAndWorkers)o2).getAgentProcessStatus());
    }
  };

  private final String[] m_columnHeadings;
  private final String m_totalString;
  private final String m_processesString;
  private final String m_threadsString;
  private final String m_agentString;
  private final String m_workerString;
  private final String m_stateStartedString;
  private final String m_stateRunningString;
  private final String m_stateFinishedString;
  private final String m_stateUnknownString;

  private RowData[] m_data = new RowData[0];
  private String m_totalDataString = "";

  public ProcessStatusTableModel(Resources resources,
                                 ProcessControl processControl)
    throws ConsoleException {

    m_columnHeadings = new String[3];
    m_columnHeadings[ID_COLUMN_INDEX] =
      resources.getString("processTable.idColumn.label");
    m_columnHeadings[TYPE_COLUMN_INDEX] =
      resources.getString("processTable.processTypeColumn.label");
    m_columnHeadings[STATE_COLUMN_INDEX] =
      resources.getString("processTable.stateColumn.label");

    m_totalString = resources.getString("processTable.total.label");
    m_processesString = resources.getString("processTable.processes.label");
    m_threadsString = resources.getString("processTable.threads.label");

    m_agentString = resources.getString("processTable.agentProcess.label");
    m_workerString = resources.getString("processTable.workerProcess.label");

    m_stateStartedString = resources.getString("processState.started.label");
    m_stateRunningString = resources.getString("processState.running.label");
    m_stateFinishedString = resources.getString("processState.finished.label");
    m_stateUnknownString = resources.getString("processState.unknown.label");

    processControl.addProcessStatusListener(
      new SwingDispatchedProcessStatusListener(
        new ProcessStatusListener() {
          public void update(AgentAndWorkers[] processStatuses) {
            final List rows = new ArrayList();
            int runningProcesses = 0;
            int totalProcesses = 0;
            int runningThreads = 0;
            int totalThreads = 0;

            Arrays.sort(processStatuses, m_agentComparator);

            for (int i = 0; i < processStatuses.length; ++i) {
              final AgentProcessStatus agentProcessStatus =
                processStatuses[i].getAgentProcessStatus();
              runningProcesses +=
                agentProcessStatus.getNumberOfRunningProcesses();
              totalProcesses +=
                agentProcessStatus.getMaximumNumberOfProcesses();
              rows.add(new RowData(agentProcessStatus));

              final WorkerProcessStatus[] workerProcessStatuses =
                processStatuses[i].getWorkerProcessStatuses();

              Arrays.sort(workerProcessStatuses, m_processStatusComparator);

              for (int j = 0; j < workerProcessStatuses.length; ++j) {
                runningThreads +=
                  workerProcessStatuses[i].getNumberOfRunningThreads();
                totalThreads +=
                  workerProcessStatuses[i].getMaximumNumberOfThreads();
                rows.add(new RowData(workerProcessStatuses[j]));
              }
            }

            rows.add(new RowData(runningProcesses,
                                 totalProcesses,
                                 runningThreads,
                                 totalThreads));

            m_data = (RowData[])rows.toArray(new RowData[rows.size()]);


            fireTableDataChanged();
          }
        }));
  }

  public int getColumnCount() {
    return m_columnHeadings.length;
  }

  public String getColumnName(int column) {
    return m_columnHeadings[column];
  }

  public int getRowCount() {
    return m_data.length;
  }

  public Object getValueAt(int row, int column) {

    if (row < m_data.length) {
      return m_data[row].getValueForColumn(column);

    }
    else {
      return "";
    }
  }

  public boolean isBold(int row, int column) {
    return row == m_data.length - 1;
  }

  public boolean isRed(int row, int column) {
    return false;
  }

  private final class RowData {
    private final String m_id;
    private final String m_processType;
    private final String m_state;

    public RowData(AgentProcessStatus agentProcessStatus) {
      m_id = agentProcessStatus.getName();
      m_processType = m_agentString;

      switch (agentProcessStatus.getState()) {
      case AgentProcessStatus.STATE_STARTED:
        m_state = m_stateStartedString;
        break;

      case AgentProcessStatus.STATE_RUNNING:
        m_state = m_stateRunningString + " (" +
                  agentProcessStatus.getNumberOfRunningProcesses() + "/" +
                  agentProcessStatus.getMaximumNumberOfProcesses() + " " +
                  m_processesString + ")";
        break;

      case AgentProcessStatus.STATE_FINISHED:
        m_state = m_stateFinishedString;
        break;

      case AgentProcessStatus.STATE_UNKNOWN:
      default:
        m_state = m_stateUnknownString;
        break;
      }
    }

    public RowData(WorkerProcessStatus workerProcessStatus) {
      m_id = "  " + workerProcessStatus.getName();
      m_processType = m_workerString;

      switch (workerProcessStatus.getState()) {
      case WorkerProcessStatus.STATE_STARTED:
        m_state = m_stateStartedString;
        break;

      case WorkerProcessStatus.STATE_RUNNING:
        m_state = m_stateRunningString + " (" +
                  workerProcessStatus.getNumberOfRunningThreads() + "/" +
                  workerProcessStatus.getMaximumNumberOfThreads() + " " +
                  m_threadsString + ")";
        break;

      case WorkerProcessStatus.STATE_FINISHED:
        m_state = m_stateFinishedString;
        break;

      default:
        m_state = m_stateUnknownString;
        break;
      }
    }

    public RowData(int runningProcesses,
                   int totalProcesses,
                   int runningThreads,
                   int totalThreads) {

      m_id = m_totalString;
      m_processType = "";
      m_state = "" +
                runningThreads + "/" + totalThreads +
                " " + m_threadsString + ", " +
                runningProcesses + "/" + totalProcesses +
                " " + m_processesString;
    }

    public String getValueForColumn(int column) {
      switch (column) {
      case ID_COLUMN_INDEX:
        return m_id;

      case TYPE_COLUMN_INDEX:
        return m_processType;

      case STATE_COLUMN_INDEX:
        return m_state;

      default:
        return "?";
      }
    }
  }

  private static final class ProcessStatusComparator implements Comparator {
    public int compare(Object o1, Object o2) {
      final ProcessStatus processStatus1 = (ProcessStatus)o1;
      final ProcessStatus processStatus2 = (ProcessStatus)o2;

      final int compareState =
        processStatus1.getState() - processStatus2.getState();

      if (compareState == 0) {
        return processStatus1.getName().compareTo(processStatus2.getName());
      }
      else {
        return compareState;
      }
    }
  }
}

