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
import javax.swing.table.AbstractTableModel;

import net.grinder.console.model.Model;
import net.grinder.console.model.ModelListener;
import net.grinder.plugininterface.Test;
import net.grinder.statistics.CumulativeStatistics;

import net.grinder.console.ConsoleException;


/**
 * @author Philip Aston
 * @version $Revision$
 */
class StatisticsTableModel extends AbstractTableModel implements ModelListener
{
    private final Model m_model;
    private final boolean m_includeTotals;

    private final String[] m_columnLabels;
    private final String m_testString;
    private final String m_totalString;

    private final Test[] m_tests;

    private NumberFormat m_numberFormat = null;

    public StatisticsTableModel(Model model, boolean includeTotals,
				Resources resources)
	throws ConsoleException
    {
	m_model = model;
	m_tests = (Test[])model.getTests().toArray(new Test[0]);
	m_includeTotals = includeTotals;
	m_numberFormat = m_model.getNumberFormat();

	m_model.addModelListener(this);

	final String[] resourceNames = {
	    "table.testColumn.label",
	    "table.descriptionColumn.label",
	    "table.transactionColumn.label",
	    "table.errorColumn.label",
	    "table.averageTimeColumn.label",
	    "table.averageTPSColumn.label",
	    "table.peakTPSColumn.label",
	};

	m_columnLabels = new String[resourceNames.length];

	for (int i=0; i<resourceNames.length; i++) {
	    m_columnLabels[i] = resources.getString(resourceNames[i]);
	}

	m_testString = resources.getString("table.test.label") + " ";
	m_totalString = resources.getString("table.total.label");
    }

    public synchronized void update()
    {
	m_numberFormat = m_model.getNumberFormat();
	fireTableRowsUpdated(0, getRowCount());
    }

    public String getColumnName(int column)
    {
        return m_columnLabels[column]; 
    }

    public int getRowCount()
    {
	return m_tests.length + (m_includeTotals ? 1 : 0);
    }

    public int getColumnCount()
    {
	return m_columnLabels.length;
    }

    public synchronized Object getValueAt(int row, int column)
    {
	if (row < m_tests.length) {
	    if (column == 0) {
		return m_testString + m_tests[row].getTestNumber();
	    }
	    else if (column == 1) {
		return m_tests[row].getDescription();
	    }
	    else
	    {
		return getStatisticsField(m_model.getCumulativeStatistics(row),
					  column - 2);
	    }
	}
	else {
	    if (column == 0) {
		return m_totalString;
	    }
	    else if (column == 1) {
		return "";
	    }
	    else {
		return
		    getStatisticsField(m_model.getTotalCumulativeStatistics(),
				       column - 2);
	    }
	}
    }

    private String getStatisticsField(CumulativeStatistics statistics,
				      int field)
    {
	switch (field) {
	case 0:
	    return String.valueOf(statistics.getTransactions());

	case 1:
	    return String.valueOf(statistics.getErrors());

	case 2:
	    final double average = statistics.getAverageTransactionTime();

	    if (Double.isNaN(average)) {
		return "";
	    }
	    else {
		return m_numberFormat.format(average);
	    }

	case 3:
	    return m_numberFormat.format(statistics.getAverageTPS());

	case 4:
	    return m_numberFormat.format(statistics.getPeakTPS());

	default:
	    return "?";
	}
    }

    public boolean isBold(int row, int column) 
    {
	return row >= m_tests.length || isRed(row, column);
    }

    public boolean isRed(int row, int column)
    {
	if (column == 3) {
	    if (row < m_tests.length) {
		return m_model.getCumulativeStatistics(row).getErrors() > 0;
	    }
	    else {
		return m_model.getTotalCumulativeStatistics().getErrors() > 0;
	    }
	}

	return false;
    }

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
}
