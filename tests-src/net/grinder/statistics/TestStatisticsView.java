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

import java.util.SortedSet;
import java.util.TreeSet;

import net.grinder.common.GrinderException;
import net.grinder.statistics.StatisticsIndexMap;
import net.grinder.statistics.RawStatistics;


/**
 * Unit test case for <code>StatisticsView</code>.
 *
 * @author Philip Aston
 * @version $Revision$
 * @see RawStatistics
 */
public class TestStatisticsView extends TestCase
{
    public static void main(String[] args)
    {
	TestRunner.run(TestStatisticsView.class);
    }

    public TestStatisticsView(String name)
    {
	super(name);
    }

    private final StatisticsIndexMap m_indexMap = new StatisticsIndexMap();

    private ExpressionView[] m_views;

    protected void setUp() throws Exception
    {
	m_indexMap.getIndexForLong("one");
	m_indexMap.getIndexForLong("two");

	m_views = new ExpressionView[] {
	    new ExpressionView("One", "my.view", "(+ one two)", m_indexMap),
	    new ExpressionView("Two", "my.view", "one", m_indexMap),
	    new ExpressionView("Three", "my.view", "(+ one two)", m_indexMap),
	    new ExpressionView("Four", "my.view", "two", m_indexMap),
	};
    }

    public void testGetExpressionViews() throws Exception
    {
	final StatisticsView statisticsView = new StatisticsView();

	for (int i=0; i<m_views.length; ++i) {
	    statisticsView.add(m_views[i]);
	}

	final ExpressionView[] expressionViews =
	    statisticsView.getExpressionViews();

	assertEquals(m_views.length, expressionViews.length);

	assertEquals(m_views[1], expressionViews[0]);
	assertEquals(m_views[3], expressionViews[1]);
	assertEquals(m_views[0], expressionViews[2]);
	assertEquals(m_views[2], expressionViews[3]);
    }
}
