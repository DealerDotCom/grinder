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
import net.grinder.statistics.ProcessStatisticsIndexMap;
import net.grinder.statistics.RawStatistics;


/**
 * Unit test case for <code>StatisticExpressionFactory</code>.
 *
 * @author Philip Aston
 * @version $Revision$
 * @see RawStatistics
 */
public class TestExpressionView extends TestCase
{
    public static void main(String[] args)
    {
	TestRunner.run(TestExpressionView.class);
    }

    public TestExpressionView(String name)
    {
	super(name);
    }

    private final ProcessStatisticsIndexMap m_indexMap =
	new ProcessStatisticsIndexMap();

    public void testConstruction() throws Exception
    {
	final ExpressionView view =
	    new ExpressionView("My view", "my.view", "(+ one two)",
			       m_indexMap);

	assertEquals("My view", view.getDisplayName());
	assertEquals("my.view", view.getDisplayNameResourceKey());
	assert(view.getExpression() != null);
    }

    public void testEquality() throws Exception
    {
	final ExpressionView[] views = {
	    new ExpressionView("My view", "my.view", "(+ one two)",
			       m_indexMap),
	    new ExpressionView("My view", "my.view", "(+ one two)",
			       m_indexMap),
	    new ExpressionView("My view", "my.view", "(+ one three)",
			       m_indexMap),
	    new ExpressionView("My View", "my.view", "(+ one two)",
			       m_indexMap),
	    new ExpressionView("My view", "my view", "(+ one two)",
			       m_indexMap),
	};
	

	assertEquals(views[0], views[1]);
	assertEquals(views[1], views[0]);
	assert(!views[0].equals(views[2]));
	assert(!views[1].equals(views[3]));
	assert(!views[1].equals(views[4]));
    }

    public void testOrdering() throws Exception
    {
	final ExpressionView[] views = {
	    new ExpressionView("One", "my.view", "(+ one two)", m_indexMap),
	    new ExpressionView("Two", "my.view", "one", m_indexMap),
	    new ExpressionView("Three", "my.view", "(+ one two)", m_indexMap),
	    new ExpressionView("Four", "my.view", "two", m_indexMap),
	};

	final SortedSet sortedSet = new TreeSet();

	for (int i=0; i<views.length; ++i) { sortedSet.add(views[i]); }

	final ExpressionView[] sorted =
	    (ExpressionView[])sortedSet.toArray(new ExpressionView[0]);

	assertEquals("Two", sorted[0].getDisplayName());
	assertEquals("Four", sorted[1].getDisplayName());
	assertEquals("One", sorted[2].getDisplayName());
	assertEquals("Three", sorted[3].getDisplayName());
    }    
}
