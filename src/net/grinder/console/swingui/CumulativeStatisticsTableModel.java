// Copyright (C) 2000 Paco Gomez
// Copyright (C) 2000, 2001, 2002 Philip Aston
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
