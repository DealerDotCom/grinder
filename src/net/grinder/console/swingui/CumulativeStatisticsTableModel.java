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

import net.grinder.console.common.ConsoleException;
import net.grinder.console.model.Model;
import net.grinder.statistics.StatisticsView;
import net.grinder.statistics.TestStatistics;


/**
 * @author Philip Aston
 * @version $Revision$
 */
final class CumulativeStatisticsTableModel extends DynamicStatisticsTableModel
{
    private final boolean m_includeTotals;
    private final String m_totalString;

    public CumulativeStatisticsTableModel(Model model, Resources resources,
					  boolean includeTotals)
	throws ConsoleException
    {
	super(model, resources, false);

	m_includeTotals = includeTotals;
	m_totalString = resources.getString("table.total.label");

	addColumns(model.getCumulativeStatisticsView());
    }

    protected final TestStatistics getStatistics(int row)
    {
	return getModel().getCumulativeStatistics(row);
    }

    public int getRowCount()
    {
	return super.getRowCount() + (m_includeTotals ? 1 : 0);
    }

    public synchronized Object getValueAt(int row, int column)
    {
	final Model model = getModel();

	if (row < model.getNumberOfTests()) {
	    return super.getValueAt(row, column);
	}
	else {
	    if (isModelInvalid()) {
		return "";
	    }
	    else {
		if (column == 0) {
		    return m_totalString;
		}
		else if (column == 1) {
		    return "";
		}
		else {
		    return getDynamicField(
			model.getTotalCumulativeStatistics(), column - 2);
		}
	    }
	}
    }

    public final boolean isBold(int row, int column) 
    {
	if (row < getModel().getNumberOfTests()) {
	    return super.isBold(row, column);
	}
	else {
	    return true;
	}
    }

    public final boolean isRed(int row, int column)
    {
	final Model model = getModel();

	if (row < model.getNumberOfTests()) {
	    return super.isRed(row, column);
	}
	else {
	    return
		column == 3 &&
		model.getTotalCumulativeStatistics().getErrors() > 0;
	}
    }
}
