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
class CumulativeStatisticsTableModel extends AbstractStatisticsTableModel
{
    private final boolean m_includeTotals;
    private final String m_totalString;

    private static final String[] s_columnTitleResourceNames = {
	"table.testColumn.label",
	"table.descriptionColumn.label",
	"table.transactionColumn.label",
	"table.errorColumn.label",
	"table.averageTimeColumn.label",
	"table.tpsColumn.label",
	"table.peakTPSColumn.label",
    };

    public CumulativeStatisticsTableModel(Model model, boolean includeTotals,
					  Resources resources)
	throws ConsoleException
    {
	super(model, resources, s_columnTitleResourceNames);

	m_includeTotals = includeTotals;
	m_totalString = resources.getString("table.total.label");
    }

    public int getRowCount()
    {
	return super.getRowCount() + (m_includeTotals ? 1 : 0);
    }

    public synchronized Object getValueAt(int row, int column)
    {
	final Model model = getModel();
	final Test[] tests = getTests();

	if (row < tests.length) {
	    if (column == 0) {
		return getTestString() + tests[row].getNumber();
	    }
	    else if (column == 1) {
		return tests[row].getDescription();
	    }
	    else
	    {
		return getStatisticsField(model.getCumulativeStatistics(row),
					  column);
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
		    getStatisticsField(model.getTotalCumulativeStatistics(),
				       column);
	    }
	}
    }

    private String getStatisticsField(CumulativeStatistics statistics,
				      int column)
    {
	switch (column) {
	case 2:
	    return String.valueOf(statistics.getTransactions());

	case 3:
	    return String.valueOf(statistics.getErrors());

	case 4:
	    final double average = statistics.getAverageTransactionTime();

	    if (Double.isNaN(average)) {
		return "";
	    }
	    else {
		return getNumberFormat().format(average);
	    }

	case 5:
	    return getNumberFormat().format(statistics.getTPS());

	case 6:
	    return getNumberFormat().format(statistics.getPeakTPS());

	default:
	    return "?";
	}
    }

    public boolean isBold(int row, int column) 
    {
	return row >= getTests().length || isRed(row, column);
    }

    public boolean isRed(int row, int column)
    {
	if (column == 3) {
	    if (row < getTests().length) {
		return getModel().getCumulativeStatistics(row).getErrors() > 0;
	    }
	    else {
		return
		    getModel().getTotalCumulativeStatistics().getErrors() > 0;
	    }
	}

	return false;
    }
}
