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

import net.grinder.common.Test;
import net.grinder.common.TestImplementation;


/**
 * Unit test case for <code>TestStatisticsMap</code>.
 *
 * @author Philip Aston
 * @version $Revision$
 * @see TestStatisticsMap
 */
public class TestTestStatisticsMap extends TestCase
{
    public static void main(String[] args)
    {
	TestRunner.run(TestTestStatisticsMap.class);
    }

    public TestTestStatisticsMap(String name)
    {
	super(name);
    }

    private final Test m_test0 = new TestImplementation(0, "");
    private final Test m_test1 = new TestImplementation(1, "");
    private TestStatistics m_statistics0;
    private TestStatistics m_statistics1;
    private StatisticsIndexMap.LongIndex m_index;

    protected void setUp() throws Exception
    {
	final TestStatisticsFactory factory =
	    TestStatisticsFactory.getInstance();

	m_statistics0 = factory.create();
	m_statistics1 = factory.create();

	m_index =
	    StatisticsIndexMap.getInstance().getIndexForLong("userLong0");

	m_statistics0.addValue(m_index, 10);
    }

    public void testPut() throws Exception
    {
	final TestStatisticsMap map = new TestStatisticsMap();
	assertEquals(0, map.size());

	map.put(m_test0, m_statistics0);
	assertEquals(1, map.size());

	map.put(m_test0, m_statistics1);
	assertEquals(1, map.size());

	map.put(m_test1, m_statistics1);
	assertEquals(2, map.size());
    }

    public void testEquals() throws Exception
    {
	final TestStatisticsMap map0 = new TestStatisticsMap();
	final TestStatisticsMap map1 = new TestStatisticsMap();

	assertEquals(map0, map0);
	assertEquals(map0, map1);

	map0.put(m_test0, m_statistics0);
	assert(!map0.equals(map1));

	map1.put(m_test1, m_statistics0);
	assert(!map0.equals(map1));

	map0.put(m_test1, m_statistics0);
	map1.put(m_test0, m_statistics0);
	assertEquals(map0, map0);
	assertEquals(map0, map1);

	map1.put(m_test0, m_statistics1);
	assert(!map0.equals(map1));
    }

    public void testAdd() throws Exception
    {
	final TestStatisticsMap map0 = new TestStatisticsMap();
	final TestStatisticsMap map1 = new TestStatisticsMap();

	// 0 + 0 = 0
	map0.add(map1);
	assertEquals(map0, map1);

	// 0 + 1 = 1
	map0.put(m_test0, m_statistics0);
	map0.put(m_test1, m_statistics1);
	map1.add(map0);
	assertEquals(map0, map1);

	// 1 + 1 != 1
	map1.add(map0);
	assert(!map0.equals(map1));

	// 1 + 1 = 2
	map0.add(map0);		// Test add to self.
	assertEquals(map0, map1);
    }

    public void testGetDelta() throws Exception
    {
	final TestStatisticsMap map0 = new TestStatisticsMap();
	map0.put(m_test0, m_statistics0);

	// map0 is now {(Test 0 (), RawStatistics = {10})}
	// snap shot is not set.

	final TestStatisticsMap map1 = map0.getDelta(false);
	assertEquals(map0, map1);

	// map0 is {(Test 0 (), RawStatistics = {10})}
	// snap shot is not set.

	final TestStatisticsMap map2 = map0.getDelta(true);
	assertEquals(map0, map1);
	assertEquals(map0, map2);

	// map0 is {(Test 0 (), RawStatistics = {10})}
	// snap shot is {(Test 0 (), RawStatistics = {10})}.

	final TestStatisticsMap map3 = map0.getDelta(false);
	assert(!map0.equals(map3));
	assertEquals(map0.size(), map3.size());

	map0.add(map0);
	map0.put(m_test1, m_statistics1);

	// map0 is {(Test 0 (), RawStatistics = {20}), (Test 1 (), RawStatistics = {0})}
	// snap shot is {(Test 0 (), RawStatistics = {10})}.

	final TestStatisticsMap map4 = map0.getDelta(true);

	// map0 is {(Test 0 (), RawStatistics = {20}), (Test 1 (), RawStatistics = {0})}
	// snap shot is  {(Test 0 (), RawStatistics = {20}), (Test 1 (), RawStatistics = {0})}

	assertEquals(2, map4.size());
	final TestStatisticsMap.Iterator iterator = map4.new Iterator();

	final TestStatisticsMap.Pair first = iterator.next();
	assertEquals(0, first.getTest().getNumber());
	assertEquals(10, first.getStatistics().getValue(m_index));

	final TestStatisticsMap.Pair second = iterator.next();
	assertEquals(1, second.getTest().getNumber());
	assertEquals(0, second.getStatistics().getValue(m_index));
    }

    public void testGetTotal() throws Exception
    {
	final TestStatisticsMap map = new TestStatisticsMap();

	assertEquals(new RawStatisticsImplementation(), map.getTotal());

	map.put(m_test0, m_statistics0);

	assertEquals(m_statistics0, map.getTotal());
	assert(m_statistics0 != map.getTotal());

	map.put(m_test1, m_statistics1);

	final RawStatistics sum = new RawStatisticsImplementation();
	sum.add(m_statistics0);
	sum.add(m_statistics1);

	assertEquals(sum, map.getTotal());
    }

    public void testIteratorAndOrder() throws Exception
    {
	final TestStatisticsMap map = new TestStatisticsMap();

	final TestStatisticsMap.Iterator iterator1 = map.new Iterator();
	assert(!iterator1.hasNext());

	map.put(m_test1, m_statistics1);

	final TestStatisticsMap.Iterator iterator2 = map.new Iterator();
	assert(iterator2.hasNext());
	assert(!iterator1.hasNext());

	final TestStatisticsMap.Pair pair1 = iterator2.next();
	assert(!iterator2.hasNext());
	assertEquals(m_test1, pair1.getTest());
	assertEquals(m_statistics1, pair1.getStatistics());

	map.put(m_test0, m_statistics0);

	final TestStatisticsMap.Iterator iterator3 = map.new Iterator();
	assert(iterator3.hasNext());

	final TestStatisticsMap.Pair pair2 = iterator3.next();
	assert(iterator3.hasNext());
	assertEquals(m_test0, pair2.getTest());
	assertEquals(m_statistics0, pair2.getStatistics());

	final TestStatisticsMap.Pair pair3 = iterator3.next();
	assert(!iterator3.hasNext());
	assertEquals(m_test1, pair3.getTest());
	assertEquals(m_statistics1, pair3.getStatistics());

	try {
	    iterator3.next();
	    fail("Expected a NoSuchElementException");
	}
	catch (java.util.NoSuchElementException e) {
	}
    }

    public void testSerialisation() throws Exception
    {
	final TestStatisticsMap original0 = new TestStatisticsMap();
	original0.put(m_test0, m_statistics0);
	original0.put(m_test1, m_statistics0);

	final TestStatisticsMap original1 = new TestStatisticsMap();

	final ByteArrayOutputStream byteOutputStream =
	    new ByteArrayOutputStream();

	final ObjectOutputStream objectOutputStream =
	    new ObjectOutputStream(byteOutputStream);

	objectOutputStream.writeObject(original0);
	objectOutputStream.writeObject(original1);

	objectOutputStream.close();

	final ObjectInputStream objectInputStream =
	    new ObjectInputStream(
		new ByteArrayInputStream(byteOutputStream.toByteArray()));

	final TestStatisticsMap received0 =
	    (TestStatisticsMap)objectInputStream.readObject();

	final TestStatisticsMap received1 =
	    (TestStatisticsMap)objectInputStream.readObject();

	assertEquals(original0, received0);
	assertEquals(original1, received1);
    }
	
}
