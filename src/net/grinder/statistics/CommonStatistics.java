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

    private final int m_errorsIndex;
    private final int m_timedTransactionsIndex;
    private final int m_untimedTransactionsIndex;
    private final int m_totalTimeIndex;

    private final StatisticsView m_statisticsView = new StatisticsView();

    public final synchronized static CommonStatistics
	getInstance(ProcessStatisticsIndexMap indexMap) throws GrinderException
    {
	if (s_instance == null) {
	    s_instance = new CommonStatistics(indexMap);
	}

	return s_instance;
    }

    private CommonStatistics(ProcessStatisticsIndexMap indexMap)
	throws GrinderException
    {
	m_errorsIndex = indexMap.getIndexFor("errors");
	m_timedTransactionsIndex = indexMap.getIndexFor("timedTransactions");
	m_untimedTransactionsIndex =
	    indexMap.getIndexFor("untimedTransactions");
	m_totalTimeIndex = indexMap.getIndexFor("totalTime");

	final ExpressionView[] expressionViews = {
	    new ExpressionView("Transactions", "statistic.transactions", 
			       "(+ timedTransactions untimedTransactions)",
			       indexMap),
	    new ExpressionView("Errors", "statistic.errors", "errors",
			       indexMap),
	    new ExpressionView("Average Response Time (ms)",
			       "statistic.averageResponseTime",
			       "(/ totalTime timedTransactions)", indexMap),
	};

	final StatisticsView statisticsView = new StatisticsView();

	for (int i=0; i<expressionViews.length; ++i) {
	    statisticsView.add(expressionViews[i]);
	}
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
