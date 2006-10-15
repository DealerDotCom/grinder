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

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import net.grinder.common.AgentProcessReport;
import net.grinder.common.ProcessReport;
import net.grinder.common.WorkerProcessReport;
import net.grinder.console.common.ConsoleException;
import net.grinder.console.common.Resources;
import net.grinder.console.communication.ProcessControl;
import net.grinder.console.communication.ProcessStatus;


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

  private static final int NAME_COLUMN_INDEX = 0;
  private static final int TYPE_COLUMN_INDEX = 1;
  private static final int STATE_COLUMN_INDEX = 2;

  private final ProcessReportComparator m_processReportComparator =
    new ProcessReportComparator();

  private final ProcessReportsComparator m_processReportsComparator =
    new ProcessReportsComparator();

  private final String[] m_columnHeadings;
  private final String m_workerProcessesString;
  private final String m_threadsString;
  private final String m_agentString;
  private final String m_workerString;
  private final String m_stateStartedString;
  private final String m_stateRunningString;
  private final String m_stateFinishedString;
  private final String m_stateConnectedString;
  private final String m_stateDisconnectedString;
  private final String m_stateUnknownString;

  private RowData[] m_data = new RowData[0];

  public ProcessStatusTableModel(Resources resources,
                                 ProcessControl processControl,
                                 SwingDispatcherFactory swingDispatcherFactory)
    throws ConsoleException {

    m_columnHeadings = new String[3];
    m_columnHeadings[NAME_COLUMN_INDEX] =
      resources.getString("processTable.nameColumn.label");
    m_columnHeadings[TYPE_COLUMN_INDEX] =
      resources.getString("processTable.processTypeColumn.label");
    m_columnHeadings[STATE_COLUMN_INDEX] =
      resources.getString("processTable.stateColumn.label");

    m_workerProcessesString =
      resources.getString("processTable.processes.label");
    m_threadsString = resources.getString("processTable.threads.label");

    m_agentString = resources.getString("processTable.agentProcess.label");
    m_workerString = resources.getString("processTable.workerProcess.label");

    m_stateStartedString = resources.getString("processState.started.label");
    m_stateRunningString = resources.getString("processState.running.label");
    m_stateFinishedString = resources.getString("processState.finished.label");
    m_stateConnectedString =
      resources.getString("processState.connected.label");
    m_stateDisconnectedString =
      resources.getString("processState.connected.label");
    m_stateUnknownString = resources.getString("processState.unknown.label");

    processControl.addProcessStatusListener(
      (ProcessStatus.Listener)swingDispatcherFactory.create(
        new ProcessStatus.Listener() {
          public void update(ProcessStatus.ProcessReports[] processReports,
                             boolean newAgent) {
            final List rows = new ArrayList();
            int runningThreads = 0;
            int totalThreads = 0;
            int workerProcesses = 0;

            Arrays.sort(processReports, m_processReportsComparator);

            for (int i = 0; i < processReports.length; ++i) {
              final AgentProcessReport agentProcessStatus =
                processReports[i].getAgentProcessReport();
              rows.add(new RowData(agentProcessStatus));

              final WorkerProcessReport[] workerProcessStatuses =
                processReports[i].getWorkerProcessReports();

              Arrays.sort(workerProcessStatuses, m_processReportComparator);

              for (int j = 0; j < workerProcessStatuses.length; ++j) {
                runningThreads +=
                  workerProcessStatuses[j].getNumberOfRunningThreads();
                totalThreads +=
                  workerProcessStatuses[j].getMaximumNumberOfThreads();
                rows.add(new RowData(workerProcessStatuses[j]));
              }

              workerProcesses += workerProcessStatuses.length;
            }

            rows.add(
              new RowData(runningThreads, totalThreads, workerProcesses));

            m_data = (RowData[])rows.toArray(new RowData[rows.size()]);


            fireTableDataChanged();
          }
        }
      )
    );
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

  public Color getForeground(int row, int column) {
    return null;
  }

  public Color getBackground(int row, int column) {
    return null;
  }

  private final class RowData {
    private final String m_name;
    private final String m_processType;
    private final String m_state;

    public RowData(AgentProcessReport agentProcessStatus) {
      m_name = agentProcessStatus.getAgentIdentity().getName();
      m_processType = m_agentString;

      switch (agentProcessStatus.getState()) {
      case AgentProcessReport.STATE_STARTED:
      case AgentProcessReport.STATE_RUNNING:
        m_state = m_stateConnectedString;
        break;

      case AgentProcessReport.STATE_FINISHED:
        m_state = m_stateDisconnectedString;
        break;

      case AgentProcessReport.STATE_UNKNOWN:
      default:
        m_state = m_stateUnknownString;
        break;
      }
    }

    public RowData(WorkerProcessReport workerProcessStatus) {
      m_name = "  " + workerProcessStatus.getWorkerIdentity().getName();
      m_processType = m_workerString;

      switch (workerProcessStatus.getState()) {
      case WorkerProcessReport.STATE_STARTED:
        m_state = m_stateStartedString;
        break;

      case WorkerProcessReport.STATE_RUNNING:
        m_state = m_stateRunningString + " (" +
                  workerProcessStatus.getNumberOfRunningThreads() + "/" +
                  workerProcessStatus.getMaximumNumberOfThreads() + " " +
                  m_threadsString + ")";
        break;

      case WorkerProcessReport.STATE_FINISHED:
        m_state = m_stateFinishedString;
        break;

      default:
        m_state = m_stateUnknownString;
        break;
      }
    }

    public RowData(int runningThreads, int totalThreads, int workerProcesses) {
      m_name = "";
      m_processType = "" + workerProcesses + " " + m_workerProcessesString;
      m_state =
        "" + runningThreads + "/" + totalThreads + " " + m_threadsString;
    }

    public String getValueForColumn(int column) {
      switch (column) {
      case NAME_COLUMN_INDEX:
        return m_name;

      case TYPE_COLUMN_INDEX:
        return m_processType;

      case STATE_COLUMN_INDEX:
        return m_state;

      default:
        return "?";
      }
    }
  }

  private static final class ProcessReportComparator implements Comparator {
    public int compare(Object o1, Object o2) {
      final ProcessReport processReport1 = (ProcessReport)o1;
      final ProcessReport processReport2 = (ProcessReport)o2;

      final int compareState =
        processReport1.getState() - processReport2.getState();

      if (compareState == 0) {
        return processReport1.getIdentity().getName().compareTo(
               processReport2.getIdentity().getName());
      }
      else {
        return compareState;
      }
    }
  }

  private final class ProcessReportsComparator implements Comparator {
    public int compare(Object o1, Object o2) {
      return m_processReportComparator.compare(
        ((ProcessStatus.ProcessReports)o1).getAgentProcessReport(),
        ((ProcessStatus.ProcessReports)o2).getAgentProcessReport());
    }
  }
}

