// The Grinder
// Copyright (C) 2000, 2001  Paco Gomez
// Copyright (C) 2000, 2001  Philip Aston

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

import java.io.IOException;
import java.io.Writer;
import java.text.NumberFormat;
import java.util.Set;
import javax.swing.table.AbstractTableModel;

import net.grinder.common.Test;
import net.grinder.console.model.Model;
import net.grinder.console.model.ModelListener;
import net.grinder.statistics.CumulativeStatistics;

import net.grinder.console.common.ConsoleException;


/**
 * @author Philip Aston
 * @version $Revision$
 */
abstract class AbstractStatisticsTableModel
    extends AbstractTableModel implements ModelListener
{
    private final Model m_model;
    private NumberFormat m_numberFormat;
    private boolean m_modelInvalid;

    private final String[] m_columnLabels;
    private final String m_testString;

    public AbstractStatisticsTableModel(Model model, Resources resources,
					String[] columnTitleResourceNames)
	throws ConsoleException
    {
	m_model = model;

	m_columnLabels = new String[columnTitleResourceNames.length];

	for (int i=0; i<columnTitleResourceNames.length; i++) {
	    m_columnLabels[i] =
		resources.getString(columnTitleResourceNames[i]);
	}

	m_testString = resources.getString("table.test.label") + " ";

	// May need to move these to subclass constructors if those
	// constructors ever do anything significant.
	m_modelInvalid = true;
	m_numberFormat = m_model.getNumberFormat();
	m_model.addModelListener(new SwingDispatchedModelListener(this));
    }

    public synchronized void reset(Set newTests)
    {
	m_modelInvalid = newTests.size() > 0;
    }

    public synchronized void update()
    {
	final boolean wasInvalid = m_modelInvalid;
	m_modelInvalid = false;

	m_numberFormat = m_model.getNumberFormat();

	if (wasInvalid) {
	    // We've been reset, number of rows may have changed.
	    fireTableDataChanged();
	}
	else {
	    fireTableRowsUpdated(0, getRowCount());
	}
    }

    public String getColumnName(int column)
    {
        return m_columnLabels[column]; 
    }

    public int getRowCount()
    {
	return m_model.getNumberOfTests();
    }

    public int getColumnCount()
    {
	return m_columnLabels.length;
    }

    public abstract boolean isBold(int row, int column);

    public abstract boolean isRed(int row, int column);

    public synchronized void write(Writer writer, String columnDelimiter,
				   String lineDelimeter)
	throws IOException
    {
	final int numberOfRows = getRowCount();
	final int numberOfColumns = getColumnCount();

	for (int column=0; column<numberOfColumns; column++) {
	    writer.write(m_columnLabels[column]);
	    writer.write(columnDelimiter);
	}

	writer.write(lineDelimeter);

	for (int row=0; row<numberOfRows; row++) {
	    for (int column=0; column<numberOfColumns; column++) {
		final Object o = getValueAt(row, column);
		writer.write(o != null ? o.toString() : "");
		writer.write(columnDelimiter);
	    }

	    writer.write(lineDelimeter);
	}
    }

    protected final Model getModel()
    {
	return m_model;
    }

    protected final boolean isModelInvalid()
    {
	return m_modelInvalid;
    }

    protected final NumberFormat getNumberFormat()
    {
	return m_numberFormat;
    }

    protected final String getTestString()
    {
	return m_testString;
    }
}
