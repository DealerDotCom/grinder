// The Grinder
// Copyright (C) 2000, 2001  Paco Gomez
// Copyright (C) 2000, 2001  Philip Aston
// Copyright (C) 2000, 2001  Dirk Feufel

// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.

// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.

// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.

package net.grinder.console.swingui;

import java.util.Set;
import javax.swing.table.AbstractTableModel;

import net.grinder.common.ProcessStatus;
import net.grinder.console.common.ConsoleException;
import net.grinder.console.model.Model;
import net.grinder.console.model.ProcessStatusSetListener;


/**
 * TableModel for the process status table. No need to synchronise,
 * all calls after initialisation are dispatched to us in the
 * SwingThread.
 *
 * @author Dirk Feufel
 * @author Philip Aston
 **/
class ProcessStatusTableModel
    extends AbstractTableModel
    implements ProcessStatusSetListener, Table.TableModel
{
    private final static int ID_COLUMN_INDEX = 0;
    private final static int STATE_COLUMN_INDEX = 1;
    private final static int THREADS_COLUMN_INDEX = 2;
    private final static int NUMBER_OF_COLUMNS = 3;

    private final String m_processIDString;
    private final String m_stateString;
    private final String m_threadsString;
    private final String m_totalString;
    private final String m_stateStartedString;
    private final String m_stateRunningString;
    private final String m_stateFinishedString;

    private ProcessStatus[] m_data = new ProcessStatus[0];
    private String m_totalDataString = "";

    public ProcessStatusTableModel(Model model, Resources resources)
	throws ConsoleException
    {
	m_processIDString = resources.getString("processTable.idColumn.label");
	m_stateString = resources.getString("processTable.stateColumn.label");
	m_threadsString =
	    resources.getString("processTable.threadsColumn.label");
	m_totalString = resources.getString("processTable.total.label");

	m_stateStartedString =
	    resources.getString("processState.started.label");
	m_stateRunningString =
	    resources.getString("processState.running.label");
	m_stateFinishedString =
	    resources.getString("processState.finished.label");

	model.getProcessStatusSet().addListener(
	    new SwingDispatchedProcessStatusSetListener(this));
    }

    public final void update(ProcessStatus[] data, int runningSum,
			     int totalSum)
    {
	m_data = data;
	m_totalDataString = formatThreadCounts(runningSum, totalSum);
	
	fireTableDataChanged();
    }

    public int getColumnCount()
    {
	return NUMBER_OF_COLUMNS;
    }

    public String getColumnName(int column)
    {
	switch (column) {
	case ID_COLUMN_INDEX:
	    return m_processIDString;

	case STATE_COLUMN_INDEX:
	    return m_stateString;

	case THREADS_COLUMN_INDEX:
	    return m_threadsString;

	default:
	    return "?";
	}
    }

    public int getRowCount()
    {
	return m_data.length + 1;
    }

    public Object getValueAt(int row, int column)
    {
	if (row < m_data.length) {
	    final ProcessStatus processStatus = m_data[row];

	    switch (column) {
	    case ID_COLUMN_INDEX:
		return processStatus.getName();

	    case STATE_COLUMN_INDEX:
		switch (processStatus.getState()) {
		case ProcessStatus.STATE_STARTED:
		    return m_stateStartedString;

		case ProcessStatus.STATE_RUNNING:
		    return m_stateRunningString;

		case ProcessStatus.STATE_FINISHED:
		    return m_stateFinishedString;

		default:
		    return "UNKNOWN STATE";
		}

	    case THREADS_COLUMN_INDEX:
		if (processStatus.getState() != ProcessStatus.STATE_FINISHED) {
		    return
			formatThreadCounts(
			    processStatus.getNumberOfRunningThreads(),
			    processStatus.getTotalNumberOfThreads());
		}
		else {
		    return "";
		}

	    default:
		return "?";
	    }
	}
	else {
	    switch (column) {
	    case ID_COLUMN_INDEX:
		return m_totalString;

	    case THREADS_COLUMN_INDEX:
		return m_totalDataString;

	    default:
		return "";
	    }
	}
    }

    private final String formatThreadCounts(int running, int total) 
    {
	return running + " / " + total;
    }

    public boolean isBold(int row, int column) 
    {
	return row >= m_data.length;
    }

    public boolean isRed(int row, int column)
    {
	return false;
    }
}

