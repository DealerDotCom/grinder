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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
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

    private final StatisticsIndexMap m_indexMap =
	StatisticsIndexMap.getProcessInstance();

    private ExpressionView[] m_views;

    protected void setUp() throws Exception
    {
	m_indexMap.getIndexForLong("one");
	m_indexMap.getIndexForLong("two");

	m_views = new ExpressionView[] {
	    new ExpressionView("One", "my.view", "(+ one two)"),
	    new ExpressionView("Two", "my.view", "one"),
	    new ExpressionView("Three", "my.view", "(+ one two)"),
	    new ExpressionView("Four", "my.view", "two"),
	};
    }

    public void testGetExpressionViews() throws Exception
    {
	final StatisticsView statisticsView = new StatisticsView();

	assertEquals(0, statisticsView.getExpressionViews().length);

	for (int i=0; i<m_views.length; ++i) {
	    statisticsView.add(m_views[i]);
	}

	final ExpressionView[] expressionViews =
	    statisticsView.getExpressionViews();

	assertEquals(m_views.length, expressionViews.length);

	assertEquals(m_views[0], expressionViews[0]);
	assertEquals(m_views[1], expressionViews[1]);
	assertEquals(m_views[2], expressionViews[2]);
	assertEquals(m_views[3], expressionViews[3]);
    }

    public void testAddStatisticsView() throws Exception
    {
	final StatisticsView statisticsView = new StatisticsView();

	statisticsView.add(statisticsView);
	assertEquals(0, statisticsView.getExpressionViews().length);

	final StatisticsView statisticsView2 = new StatisticsView();

	statisticsView.add(statisticsView);
	assertEquals(0, statisticsView.getExpressionViews().length);	

	for (int i=0; i<m_views.length; ++i) {
	    statisticsView.add(m_views[i]);
	}

	statisticsView2.add(statisticsView);
	assertEquals(m_views.length,
		     statisticsView2.getExpressionViews().length);

	statisticsView2.add(statisticsView);
	assertEquals(m_views.length,
		     statisticsView2.getExpressionViews().length);
    }

    public void testSerialisation() throws Exception
    {
	final StatisticsView original1 = new StatisticsView();

	for (int i=0; i<m_views.length; ++i) {
	    original1.add(m_views[i]);
	}

	final StatisticsView original2 = new StatisticsView();

	final ByteArrayOutputStream byteOutputStream =
	    new ByteArrayOutputStream();

	final ObjectOutputStream objectOutputStream =
	    new ObjectOutputStream(byteOutputStream);

	original1.writeExternal(objectOutputStream);
	original2.writeExternal(objectOutputStream);
	objectOutputStream.close();

	final ObjectInputStream objectInputStream =
	    new ObjectInputStream(
		new ByteArrayInputStream(byteOutputStream.toByteArray()));

	final StatisticsView received1 = new StatisticsView();
	received1.readExternal(objectInputStream);

	final StatisticsView received2 = new StatisticsView();
	received2.readExternal(objectInputStream);
	
	assertEquals(original1.getExpressionViews().length,
		     received1.getExpressionViews().length);
	assert(original1 != received1);

	assertEquals(original2.getExpressionViews().length,
		     received2.getExpressionViews().length);
	assert(original2 != received2);
    }
}
