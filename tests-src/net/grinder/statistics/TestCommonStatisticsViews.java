// The Grinder
// Copyright (C) 2001  Paco Gomez
// Copyright (C) 2001  Philip Aston

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

import junit.framework.TestCase;
import junit.swingui.TestRunner;
//import junit.textui.TestRunner;


/**
 * Unit test case for <code>CommonStatisticsViews</code>.
 *
 * @author Philip Aston
 * @version $Revision$
 * @see RawStatistics
 */
public class TestCommonStatisticsViews extends TestCase
{
    public static void main(String[] args)
    {
	TestRunner.run(TestCommonStatisticsViews.class);
    }

    public TestCommonStatisticsViews(String name)
    {
	super(name);
    }

    public void testGetViews() throws Exception
    {
	final StatisticsView detail =
	    CommonStatisticsViews.getDetailStatisticsView();

	final ExpressionView[] detailExpressionViews =
	    detail.getExpressionViews();

	assert(detailExpressionViews.length > 0);

	final StatisticsView summary =
	    CommonStatisticsViews.getSummaryStatisticsView();

	final ExpressionView[] summaryExpressionViews =
	    summary.getExpressionViews();

	assert(summaryExpressionViews.length > 0);
    }
}
