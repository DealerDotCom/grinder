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

import net.grinder.common.GrinderException;


/**
 * @author Philip Aston
 * @version $Revision$
 * @stereotype singleton
 **/
public final class CommonStatistics
{
    private static CommonStatistics s_instance;

    /**
     * @link aggregation
     * @supplierCardinality 1 
     */
    private final ProcessStatisticsIndexMap m_indexMap =
	new ProcessStatisticsIndexMap();

    private final int m_errorsIndex;
    private final int m_timedTransactionsIndex;
    private final int m_untimedTransactionsIndex;
    private final int m_totalTimeIndex;

    /**
     * @link aggregation
     * @supplierCardinality 1 
     */
    private final StatisticsView m_statisticsView = new StatisticsView();

    public final synchronized static CommonStatistics getInstance()
	throws GrinderException
    {
	if (s_instance == null) {
	    s_instance = new CommonStatistics();
	}

	return s_instance;
    }

    private CommonStatistics() throws GrinderException
    {
	m_errorsIndex = m_indexMap.getIndexFor("errors");
	m_timedTransactionsIndex = m_indexMap.getIndexFor("timedTransactions");
	m_untimedTransactionsIndex =
	    m_indexMap.getIndexFor("untimedTransactions");
	m_totalTimeIndex = m_indexMap.getIndexFor("totalTime");

	final ExpressionView[] expressionViews = {
	    new ExpressionView("Transactions", "statistic.transactions", 
			       "(+ timedTransactions untimedTransactions)",
			       m_indexMap),
	    new ExpressionView("Errors", "statistic.errors", "errors",
			       m_indexMap),
	    new ExpressionView("Average Response Time (ms)",
			       "statistic.averageResponseTime",
			       "(/ totalTime timedTransactions)", m_indexMap),
	};

	final StatisticsView statisticsView = new StatisticsView();

	for (int i=0; i<expressionViews.length; ++i) {
	    statisticsView.add(expressionViews[i]);
	}
    }

    public final ProcessStatisticsIndexMap getIndexMap()
    {
	return m_indexMap;
    }

    public final StatisticsView getStatisticsView()
    {
	return m_statisticsView;
    }

    public final class TestStatistics extends RawStatistics
    {
	public final void addError()
	{
	    addValue(m_errorsIndex, 1);
	}
    
	public final void addTransaction()
	{
	    addValue(m_untimedTransactionsIndex, 1);
	}
    
	public final void addTransaction(long time)
	{
	    addValue(m_timedTransactionsIndex, 1);
	    addValue(m_totalTimeIndex, time);
	}
    }
}
