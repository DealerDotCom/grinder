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

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import net.grinder.common.GrinderException;
import net.grinder.util.Serialiser;


/**
 * @author Philip Aston
 * @version $Revision$
 * @stereotype singleton
 **/
public final class TestStatisticsFactory
{
    private static TestStatisticsFactory s_instance;

    private final Serialiser m_serialiser = new Serialiser();

    /**
     * @link aggregation
     * @supplierCardinality 1 
     */
    private final StatisticsView m_statisticsView = new StatisticsView();

    /** @link dependency 
     * @stereotype instantiate*/
    /*#TestStatisticsImplementation lnkTestStatistics;*/

    public final synchronized static TestStatisticsFactory getInstance()
    {
	if (s_instance == null) {
	    s_instance = new TestStatisticsFactory();
	}

	return s_instance;
    }

    private TestStatisticsFactory()
    {
	try {
	    final ExpressionView[] expressionViews = {
		new ExpressionView("Transactions", "statistic.transactions", 
				   "(+ timedTransactions untimedTransactions)"
				   ),
		new ExpressionView("Errors", "statistic.errors", "errors"),
		new ExpressionView("Average Response Time (ms)",
				   "statistic.averageResponseTime",
				   "(/ totalTime timedTransactions)"),
	    };

	    for (int i=0; i<expressionViews.length; ++i) {
		m_statisticsView.add(expressionViews[i]);
	    }
	}
	catch (GrinderException e) {
	    throw new RuntimeException(
		"Assertion failure, " +
		"TestStatisticsFactory could not initialise: " +
		e.getMessage());
	}
    }

    public final StatisticsView getStatisticsView()
    {
	return m_statisticsView;
    }

    public final TestStatistics create()
    {
	return createImplementation();
    }

    /**
     * Package scope factory method that returns instances of our implementation type.
     * @see #create
     **/
    final TestStatisticsImplementation createImplementation()
    {
	return new TestStatisticsImplementation();
    }

    final void writeStatisticsExternal(ObjectOutput out,
				       TestStatisticsImplementation statistics)
	throws IOException
    {
	statistics.myWriteExternal(out, m_serialiser);
    }

    final TestStatistics readStatisticsExternal(ObjectInput in)
	throws IOException
    {
	return new TestStatisticsImplementation(in, m_serialiser);
    }
}
