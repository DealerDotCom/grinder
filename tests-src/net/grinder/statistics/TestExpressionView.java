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

    private final StatisticsIndexMap m_indexMap = new StatisticsIndexMap();

    protected void setUp() throws Exception
    {
	m_indexMap.getIndexForLong("one");
	m_indexMap.getIndexForLong("two");
	m_indexMap.getIndexForLong("three");
    }

    public void testConstruction() throws Exception
    {
	final ExpressionView view =
	    new ExpressionView("My view", "my.view", "(+ one two)",
			       m_indexMap);

	assertEquals("My view", view.getDisplayName());
	assertEquals("my.view", view.getDisplayNameResourceKey());
	assert(view.getExpression() != null);

	final StatisticExpressionFactory statisticExpressionFactory =
	    StatisticExpressionFactory.getInstance();
	final ExpressionView view2 =
	    new ExpressionView("My view2", "my.view", 
			       statisticExpressionFactory.createExpression(
				   "one", m_indexMap));

	assertEquals("My view2", view2.getDisplayName());
	assertEquals("my.view", view2.getDisplayNameResourceKey());
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

	assertEquals("One", sorted[0].getDisplayName());
	assertEquals("Two", sorted[1].getDisplayName());
	assertEquals("Three", sorted[2].getDisplayName());
	assertEquals("Four", sorted[3].getDisplayName());
    }   

    public void testExternalisation() throws Exception
    {
	final ExpressionView original =
	    new ExpressionView("My view", "my.view", "(+ one two)",
			       m_indexMap);

	final ByteArrayOutputStream byteOutputStream =
	    new ByteArrayOutputStream();

	final ObjectOutputStream objectOutputStream =
	    new ObjectOutputStream(byteOutputStream);

	original.myWriteExternal(objectOutputStream);
	objectOutputStream.close();

	final ObjectInputStream objectInputStream =
	    new ObjectInputStream(
		new ByteArrayInputStream(byteOutputStream.toByteArray()));

	final ExpressionView received = 
	    new ExpressionView(objectInputStream, m_indexMap);

	assertEquals(original, received);

	final StatisticExpressionFactory statisticExpressionFactory =
	    StatisticExpressionFactory.getInstance();
	final ExpressionView cantStreamThis =
	    new ExpressionView("My view2", "my.view", 
			       statisticExpressionFactory.createExpression(
				   "one", m_indexMap));

	try {
	    cantStreamThis.myWriteExternal(objectOutputStream);
	    fail("Expected an IOException");
	}
	catch (IOException e) {
	}
    }
}
