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

package net.grinder.statistics;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.text.DecimalFormat;

import net.grinder.common.GrinderException;
import net.grinder.common.Test;
import net.grinder.util.FixedWidthFormatter;


/**
 * Package scope
 *
 * @author Philip Aston
 * @version $Revision$
 */
public class StatisticsTable
{
    /**
     * @supplierCardinality 1
     **/
    private final TestStatisticsMap m_testStatisticsMap;
    private final RawStatistics m_totals;
    private final DecimalFormat m_twoDPFormat = new DecimalFormat("0.00");
    private final int m_columnWidth = 12;
    private final String m_columnSeparator = " ";

    private final FixedWidthFormatter m_headingFormatter =
	new FixedWidthFormatter(FixedWidthFormatter.ALIGN_LEFT,
				FixedWidthFormatter.FLOW_WORD_WRAP,
				m_columnWidth);

    private final FixedWidthFormatter m_rowLabelFormatter =
	new FixedWidthFormatter(FixedWidthFormatter.ALIGN_LEFT,
				FixedWidthFormatter.FLOW_TRUNCATE,
				m_columnWidth);

    private final FixedWidthFormatter m_rowCellFormatter =
	new FixedWidthFormatter(FixedWidthFormatter.ALIGN_LEFT,
				FixedWidthFormatter.FLOW_TRUNCATE,
				m_columnWidth);

    /**
     * @supplierCardinality 1
     **/
    private final StatisticsView m_statisticsView;
    
    public StatisticsTable(StatisticsView statisticsView,
			   TestStatisticsMap testStatisticsMap)
    {
	m_statisticsView = statisticsView;
	m_testStatisticsMap = testStatisticsMap;

	m_totals = m_testStatisticsMap.getTotal();
    }

    public final void print(PrintStream out) throws GrinderException
    {
	final PrintWriter writer = new PrintWriter(out);
	print(writer);
	writer.flush();
    }

    public final void print(PrintWriter out) throws GrinderException
    {
	final ExpressionView[] expressionViews =
	    m_statisticsView.getExpressionViews();

	final int numberOfHeaderColumns = expressionViews.length + 1;

	StringBuffer[] cells = new StringBuffer[numberOfHeaderColumns];
	StringBuffer[] remainders = new StringBuffer[numberOfHeaderColumns];

	for (int i=0; i<numberOfHeaderColumns; i++) {
	    cells[i] = new StringBuffer(
		i == 0 ? "" : expressionViews[i-1].getDisplayName());

	    remainders[i] = new StringBuffer();
	}

	boolean wrapped = false;

	do {
	    wrapped = false;

	    for (int i=0; i<numberOfHeaderColumns; ++i) {
		remainders[i].setLength(0);
		m_headingFormatter.transform(cells[i], remainders[i]);

		out.print(cells[i].toString());
		out.print(m_columnSeparator);

		if (remainders[i].length() > 0) {
		    wrapped = true;
		}
	    }

	    out.println();

	    final StringBuffer[] otherArray = cells;
	    cells = remainders;
	    remainders = otherArray;
	}
	while (wrapped);

	final TestStatisticsMap.Iterator iterator =
	    m_testStatisticsMap.new Iterator();

	while (iterator.hasNext()) {
	    final TestStatisticsMap.Pair pair = iterator.next();

	    final Test test = pair.getTest();

	    StringBuffer output = formatLine("Test " + test.getNumber(),
					     pair.getStatistics(),
					     expressionViews);

	    final String testDescription = test.getDescription();

	    if (testDescription != null) {
		output.append(" \"" + testDescription + "\"");
	    }

	    out.println(output.toString());
	}

	out.println();
	out.println(formatLine("Totals", m_totals, expressionViews));
    }

    private StringBuffer formatLine(String rowLabel,
				    RawStatistics rawStatistics,
				    ExpressionView[] expressionViews)
	throws GrinderException
    {
	final StringBuffer result = new StringBuffer();

	final StringBuffer cell = new StringBuffer(rowLabel);
	final StringBuffer remainder = new StringBuffer();

	m_rowLabelFormatter.transform(cell, remainder);
	result.append(cell.toString());
	result.append(m_columnSeparator);

	for (int i=0; i<expressionViews.length; ++i) {

	    final StatisticExpression expression =
		expressionViews[i].getExpression();

	    final String text;

	    if (expression.isDouble()) {
		text = m_twoDPFormat.format(expression.getDoubleValue(
						rawStatistics));
	    }
	    else {
		text = String.valueOf(expression.getLongValue(rawStatistics));
	    }

	    cell.replace(0, cell.length(), text);
	    m_rowCellFormatter.transform(cell, remainder);
	    result.append(cell.toString());
	    result.append(m_columnSeparator);
	}

	return result;
    }
}
