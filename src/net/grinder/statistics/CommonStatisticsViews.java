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
 * Provides references to commonly used {@link StatisticsView}s.
 *
 * @author Philip Aston
 * @version $Revision$
 * @stereotype singleton
 **/
public final class CommonStatisticsViews
{
    private final static CommonStatisticsViews s_instance =
	new CommonStatisticsViews();

    /**
     * @supplierCardinality 1
     * @link aggregation
     * @clientRole detailStatisticsView 
     **/
    private final StatisticsView m_detailStatisticsView = new StatisticsView();

    /**
     * @supplierCardinality 1
     * @link aggregation
     * @clientRole summaryStatisticsView 
     **/
    private final StatisticsView m_summaryStatisticsView =
	new StatisticsView();

    private CommonStatisticsViews()
    {
	try {
	    final ExpressionView[] detailExpressionViews = {
		new ExpressionView("Transaction time",
				   "statistic.transactionTime", 
				   "timedTransactionTime"),
		new ExpressionView("Errors", "statistic.errors", "errors"),
	    };

	    for (int i=0; i<detailExpressionViews.length; ++i) {
		m_detailStatisticsView.add(detailExpressionViews[i]);
	    }

	    final ExpressionView[] summaryExpressionViews = {
		new ExpressionView("Transactions", "statistic.transactions", 
				   "(+ timedTransactions untimedTransactions)"
				   ),
		new ExpressionView("Errors", "statistic.errors", "errors"),
		new ExpressionView(
		    "Average Response Time (ms)",
		    "statistic.averageResponseTime",
		    "(/ timedTransactionTime timedTransactions)"),
	    };

	    for (int i=0; i<summaryExpressionViews.length; ++i) {
		m_summaryStatisticsView.add(summaryExpressionViews[i]);
	    }
	}
	catch (GrinderException e) {
	    throw new RuntimeException(
		"Assertion failure, " +
		"CommonStatisticsViews could not initialise: " +
		e.getMessage());
	}
    }

    /**
     * Get the detail {@link StatisticsView}.
     *
     * @return The {@link StatisticsView}.
     **/
    public final static StatisticsView getDetailStatisticsView()
    {
	return s_instance.m_detailStatisticsView;
    }

    /**
     * Get the summary {@link StatisticsView}.
     *
     * @return The {@link StatisticsView}.
     **/
    public final static StatisticsView getSummaryStatisticsView()
    {
	return s_instance.m_summaryStatisticsView;
    }
}
