// The Grinder
// Copyright (C) 2000  Paco Gomez
// Copyright (C) 2000  Philip Aston

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

package net.grinder.engine.process;

import java.io.PrintStream;
import java.text.DecimalFormat;
import java.util.Iterator;
import java.util.Map;

import net.grinder.engine.EngineException;


/**
 * Package scope
 *
 * @author Philip Aston
 * @version $Revision$
 */
class TestStatisticsTable
{
    private final Map m_tests;
    private final TestStatistics m_totals = new TestStatistics();
    private DecimalFormat m_twoDPFormat = new DecimalFormat("0.00");
    
    public TestStatisticsTable(Map tests)
    {
	m_tests = tests;

	final Iterator testIterator = m_tests.values().iterator();

	while (testIterator.hasNext()) {
	    final Test test = (Test)testIterator.next();
	    m_totals.add(test.getStatistics());
	}
    }

    public void print(PrintStream out)
    {
	final StringBuffer heading = new StringBuffer();
	heading.append(formatField(""));
	heading.append(formatField("Transactions"));
	heading.append(formatField("Failures"));
	heading.append(formatField("Total (ms)"));
	heading.append(formatField("Average (ms)"));
	
	out.println(heading.toString());

	final Iterator testIterator = m_tests.entrySet().iterator();

	while (testIterator.hasNext()) {
	    final Map.Entry entry = (Map.Entry)testIterator.next();
	    final Integer testNumber = (Integer)entry.getKey();
	    final Test test = (Test)entry.getValue();

	    StringBuffer output = formatLine(test.toString(),
					     test.getStatistics());

	    final String testDescription = test.getDescription();

	    if (testDescription != null) {
		output.append(" (\"" + testDescription + "\")");
	    }

	    out.println(output.toString());
	}

	out.println();
	out.println(formatLine("Totals", m_totals));
    }

    private String formatField(String string)
    {
	final int width = 14;

	if (string.length() >= width) {
	    return string.substring(0, width);
	}
	else {
	    StringBuffer result = new StringBuffer(string);

	    final int padding = width - string.length();

	    for (int i=0; i<padding; i++) {
		result.append(" ");
	    }

	    return result.toString();
	}
    }

    private StringBuffer formatLine(String methodName,
				    TestStatistics methodStatistics)
    {
	final StringBuffer result = new StringBuffer();

	result.append(formatField(methodName));

	result.append(
	    formatField(String.valueOf(methodStatistics.getTransactions())));

	result.append(
	    formatField(String.valueOf(methodStatistics.getErrors())));

	result.append(
	    formatField(String.valueOf(methodStatistics.getTotalTime())));

	result.append(
	    formatField(m_twoDPFormat.format(
			    methodStatistics.getAverageTransactionTime())));

	return result;
    }
}
