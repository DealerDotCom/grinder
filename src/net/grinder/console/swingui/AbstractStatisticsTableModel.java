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
import java.text.DecimalFormat;
import javax.swing.table.AbstractTableModel;

import net.grinder.console.model.Model;
import net.grinder.console.model.ModelListener;
import net.grinder.plugininterface.Test;
import net.grinder.statistics.Statistics;
import net.grinder.statistics.TestStatisticsMap;


/**
 * @author Philip Aston
 * @version $Revision$
 */
class StatisticsTableModel extends AbstractTableModel implements ModelListener
{
    private String[] m_columnNames = {
	"Test",
	"Description",
	"Sucessful Transactions",
	"Errors",
	"Abortions",
	"Average (ms)",
    };

    private final Model m_model;
    private final boolean m_includeTotals;
    private Statistics m_totals = new Statistics();
    private int m_lastMapSize = 0;
    private TestStatisticsMap m_testStatisticsMap = null;
    private TestStatisticsMap.Pair[] m_testIndex =
	new TestStatisticsMap.Pair[0];

    public StatisticsTableModel(Model model, boolean includeTotals)
    {
	m_model = model;
	m_includeTotals = includeTotals;

	m_model.addModelListener(this);
    }

    public synchronized void update()
    {
	final TestStatisticsMap testStatisticsMap =
	    m_model.getSummaryStatistics();

	m_totals = testStatisticsMap.getTotal();

	final int size = testStatisticsMap.getSize();

	// Crude way of figuring out whether the TestStatisticsMap has
	// changed.
	if (size != m_lastMapSize ||
	    testStatisticsMap != m_testStatisticsMap) {
	    // Its changed, rebuild the index.
	    m_lastMapSize = size;
	    m_testStatisticsMap = testStatisticsMap;

	    m_testIndex = new TestStatisticsMap.Pair[size];

	    final TestStatisticsMap.Iterator iterator =
		m_testStatisticsMap.new Iterator();

	    int index = 0;

	    while (iterator.hasNext()) {
		m_testIndex[index++] = iterator.next();
	    }

	    fireTableDataChanged();
	}
	else {
	    fireTableRowsUpdated(0, getRowCount());
	}
    }

    public String getColumnName(int column)
    {
        return m_columnNames[column]; 
    }

    public int getRowCount()
    {
	return m_testIndex.length + (m_includeTotals ? 1 : 0);
    }

    public int getColumnCount()
    {
	return m_columnNames.length;
    }

    public synchronized Object getValueAt(int row, int column)
    {
	if (row < m_testIndex.length) {
	    if (column == 0) {
		return "Test " + m_testIndex[row].getTest().getTestNumber();
	    }
	    else if (column == 1) {
		return m_testIndex[row].getTest().getDescription();
	    }
	    else
	    {
		return getStatisticsField(m_testIndex[row].getStatistics(),
					  column - 2);
	    }
	}
	else {
	    if (column == 0) {
		return "Totals";
	    }
	    else if (column == 1) {
		return "";
	    }
	    else {
		return getStatisticsField(m_totals, column - 2);
	    }
	}
    }

    private String getStatisticsField(Statistics statistics, int field)
    {
	if (field == 0) {
	    return String.valueOf(statistics.getTransactions());
	}
	else if (field == 1) {
	    return String.valueOf(statistics.getErrors());
	}
	else if (field == 2) {
	    return String.valueOf(statistics.getAbortions());
	}
	else if (field == 3) {
	    final double average = statistics.getAverageTransactionTime();

	    if (Double.isNaN(average)) {
		return "";
	    }
	    else {
		return m_model.getNumberFormat().format(average);
	    }
	}

	return "?";
    }

    public synchronized void write(Writer writer, String columnDelimiter,
				   String lineDelimeter)
	throws IOException
    {
	final int numberOfRows = getRowCount();
	final int numberOfColumns = getColumnCount();

	for (int column=0; column<numberOfColumns; column++) {
	    writer.write(m_columnNames[column]);
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
