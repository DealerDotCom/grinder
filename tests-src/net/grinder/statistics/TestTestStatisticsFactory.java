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
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Random;

import net.grinder.common.GrinderException;


/**
 * Unit test case for <code>TestStatisticsFactory</code>.
 *
 * @author Philip Aston
 * @version $Revision$
 * @see RawStatistics
 */
public class TestTestStatisticsFactory extends TestCase
{
    public static void main(String[] args)
    {
	TestRunner.run(TestTestStatisticsFactory.class);
    }

    public TestTestStatisticsFactory(String name)
    {
	super(name);
    }

    public void testCreation() throws Exception
    {
	final TestStatisticsFactory factory =
	    TestStatisticsFactory.getInstance();

	assertSame(factory, TestStatisticsFactory.getInstance());
	assert(factory.getIndexMap() != null);

	final StatisticsView statisticsView = factory.getStatisticsView();

	final ExpressionView[] expressionViews =
	    statisticsView.getExpressionViews();

	assert(expressionViews.length > 0);
    }

    public void testFactory() throws Exception
    {
	final TestStatisticsFactory factory =
	    TestStatisticsFactory.getInstance();

	final TestStatistics testStatistics1 = factory.createImplementation();
	assert(testStatistics1 != null);

	final TestStatistics testStatistics2 = factory.create();
	assert(testStatistics2 instanceof TestStatisticsImplementation);
    }

    public void testSerialisation() throws Exception
    {
	final TestStatisticsFactory factory =
	    TestStatisticsFactory.getInstance();

	final Random random = new Random();

	final StatisticsIndexMap indexMap = new StatisticsIndexMap();
	final StatisticsIndexMap.LongIndex aIndex =
	    indexMap.getIndexForLong("a");
	final StatisticsIndexMap.LongIndex bIndex =
	    indexMap.getIndexForLong("b");
	final StatisticsIndexMap.LongIndex cIndex =
	    indexMap.getIndexForLong("c");

	final TestStatisticsImplementation original0 =
	    factory.createImplementation();
	original0.addValue(aIndex, Math.abs(random.nextLong()));
	original0.addValue(bIndex, Math.abs(random.nextLong()));
	original0.addValue(cIndex, Math.abs(random.nextLong()));

	final TestStatisticsImplementation original1 =
	    factory.createImplementation();

	final ByteArrayOutputStream byteOutputStream =
	    new ByteArrayOutputStream();

	final ObjectOutputStream objectOutputStream =
	    new ObjectOutputStream(byteOutputStream);

	factory.writeStatisticsExternal(objectOutputStream, original0);
	factory.writeStatisticsExternal(objectOutputStream, original1);

	objectOutputStream.close();

	final ObjectInputStream objectInputStream =
	    new ObjectInputStream(
		new ByteArrayInputStream(byteOutputStream.toByteArray()));

	final TestStatistics received0 =
	    factory.readStatisticsExternal(objectInputStream);

	final TestStatistics received1 =
	    factory.readStatisticsExternal(objectInputStream);

	assertEquals(original0, received0);
	assertEquals(original1, received1);
    }
}
