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

import net.grinder.util.Serialiser;


/**
 * Unit test case for <code>RawStatisticsImplementation</code>.
 *
 * @author Philip Aston
 * @version $Revision$
 * @see RawStatisticsImplementation
 */
public class TestRawStatisticsImplementation extends TestCase
{
    public static void main(String[] args)
    {
	TestRunner.run(TestRawStatisticsImplementation.class);
    }

    public TestRawStatisticsImplementation(String name)
    {
	super(name);
    }

    final StatisticsIndexMap m_indexMap = new StatisticsIndexMap();

    StatisticsIndexMap.LongIndex m_longIndex0;
    StatisticsIndexMap.LongIndex m_longIndex1;
    StatisticsIndexMap.LongIndex m_longIndex2;
    StatisticsIndexMap.DoubleIndex m_doubleIndex0;
    StatisticsIndexMap.DoubleIndex m_doubleIndex1;
    StatisticsIndexMap.DoubleIndex m_doubleIndex2;

    protected void setUp() throws Exception
    {
	m_longIndex0 = m_indexMap.getIndexForLong("a");
	m_longIndex1 = m_indexMap.getIndexForLong("b");
	m_longIndex2 = m_indexMap.getIndexForLong("c");
	m_doubleIndex0 = m_indexMap.getIndexForDouble("da");
	m_doubleIndex1 = m_indexMap.getIndexForDouble("dc");
	m_doubleIndex2 = m_indexMap.getIndexForDouble("da");
    }

    public void testCreation() 
    {
	final RawStatisticsImplementation statistics =
	    new RawStatisticsImplementation();

	assertEquals(0, statistics.getValue(m_longIndex1));
	myAssertEquals(0d, statistics.getValue(m_doubleIndex2));
    }

    public void testReset()
    {
	final RawStatisticsImplementation statistics0 =
	    new RawStatisticsImplementation();

	statistics0.setValue(m_longIndex2, 700);
	statistics0.setValue(m_doubleIndex2, -0.9999);
	assertEquals(700, statistics0.getValue(m_longIndex2));
	myAssertEquals(-0.9999d, statistics0.getValue(m_doubleIndex2));

	statistics0.reset();
	assertEquals(0, statistics0.getValue(m_longIndex2));
	myAssertEquals(0d, statistics0.getValue(m_doubleIndex2));
    }

    public void testGetValueSetValueAndEquals()
    {
	final RawStatisticsImplementation statistics0 =
	    new RawStatisticsImplementation();
	final RawStatisticsImplementation statistics1 =
	    new RawStatisticsImplementation();

	assertEquals(statistics0, statistics0);
	assertEquals(statistics0, statistics1);
	
	statistics0.setValue(m_longIndex1, 700);
	assertEquals(700, statistics0.getValue(m_longIndex1));
	statistics0.setValue(m_longIndex1, -300);
	assertEquals(-300, statistics0.getValue(m_longIndex1));
	assert(!statistics0.equals(statistics1));

	statistics1.setValue(m_longIndex1, 500);
	assert(!statistics0.equals(statistics1));
	statistics1.setValue(m_longIndex1, -300);
	assertEquals(statistics0, statistics1);

	statistics0.setValue(m_longIndex0, 1);
	assert(!statistics0.equals(statistics1));
	statistics1.setValue(m_longIndex0, 1);
	assertEquals(statistics0, statistics1);

	assertEquals(statistics0, statistics0);
	assertEquals(statistics1, statistics1);

	statistics0.setValue(m_longIndex2, 0);
	assertEquals(statistics0, statistics1);	// Statistics1.getValue(m_longIndex2)
						// defaults to 0.

	statistics0.setValue(m_doubleIndex2, 7.00d);
	myAssertEquals(7.00d, statistics0.getValue(m_doubleIndex2));
	statistics0.setValue(m_doubleIndex2, 3.00d);
	myAssertEquals(3.00d, statistics0.getValue(m_doubleIndex2));
	assert(!statistics0.equals(statistics1));

	statistics1.setValue(m_doubleIndex2, 5.00d);
	assert(!statistics0.equals(statistics1));
	statistics1.setValue(m_doubleIndex2, 3.00d);
	assertEquals(statistics0, statistics1);

	statistics0.setValue(m_doubleIndex0, -1.0d);
	assert(!statistics0.equals(statistics1));
	statistics1.setValue(m_doubleIndex0, -1.0d);
	assertEquals(statistics0, statistics1);

	assertEquals(statistics0, statistics0);
	assertEquals(statistics1, statistics1);

	statistics0.setValue(m_doubleIndex1, 0);
	assertEquals(statistics0, statistics1);	// Statistics1.getValue(m_longIndex1)
						// defaults to 0.
    }

    public void testAddValueAndIncrement() 
    {
	final RawStatisticsImplementation statistics0 =
	    new RawStatisticsImplementation();
	final RawStatisticsImplementation statistics1 =
	    new RawStatisticsImplementation();
	
	statistics0.addValue(m_longIndex1, 700);
	statistics0.addValue(m_longIndex1, 300);
	assert(!statistics0.equals(statistics1));
	statistics1.addValue(m_longIndex1, 500);
	assert(!statistics0.equals(statistics1));
	statistics1.addValue(m_longIndex1, 500);
	assertEquals(statistics0, statistics1);
	
	statistics0.addValue(m_doubleIndex1, 7.00d);
	statistics0.addValue(m_doubleIndex1, 3.00d);
	assert(!statistics0.equals(statistics1));
	statistics1.addValue(m_doubleIndex1, 5.00d);
	assert(!statistics0.equals(statistics1));
	statistics1.addValue(m_doubleIndex1, 5.00d);
	assertEquals(statistics0, statistics1);


	statistics0.incrementValue(m_longIndex0);
	assert(!statistics0.equals(statistics1));
	statistics1.incrementValue(m_longIndex0);
	assertEquals(statistics0, statistics1);
    }

    public void testAdd() throws Exception
    {
	final RawStatisticsImplementation statistics0 =
	    new RawStatisticsImplementation();
	final RawStatisticsImplementation statistics1 =
	    new RawStatisticsImplementation();

	// 0 + 0 = 0
	statistics0.add(statistics1);
	assertEquals(statistics0, statistics1);

	// 0 + 1 = 1
	statistics0.addValue(m_longIndex0, 100);
	statistics0.addValue(m_doubleIndex2, -5.5);
	statistics1.add(statistics0);
	assertEquals(statistics0, statistics1);

	// 1 + 1 != 1
	statistics1.add(statistics0);
	assert(!statistics0.equals(statistics1));

	// 1 + 1 = 2
	statistics0.add(statistics0); // Test add to self.
	assertEquals(statistics0, statistics1);

	assertEquals(200, statistics0.getValue(m_longIndex0));
	myAssertEquals(-11d, statistics0.getValue(m_doubleIndex2));
    }

    public void testGetDelta() throws Exception
    {
	final RawStatisticsImplementation statistics0 =
	    new RawStatisticsImplementation();
	statistics0.addValue(m_longIndex0, 1234);
	statistics0.addValue(m_doubleIndex0, 1.234);

	final RawStatistics statistics1 = statistics0.getDelta(false);
	assertEquals(statistics0, statistics1);

	final RawStatistics statistics2 = statistics0.getDelta(true);
	assertEquals(statistics0, statistics1);
	assertEquals(statistics0, statistics2);

	final RawStatistics statistics3 = statistics0.getDelta(false);
	assert(!statistics0.equals(statistics3));

	statistics0.addValue(m_doubleIndex0, 2.345);
	final RawStatistics statistics4 = statistics0.getDelta(true);

	assertEquals(0, statistics4.getValue(m_longIndex0));
	myAssertEquals(2.345, statistics4.getValue(m_doubleIndex0));

	statistics0.addValue(m_longIndex0, 5678);

	final RawStatistics statistics5 = statistics0.getDelta(true);
	assertEquals(5678, statistics5.getValue(m_longIndex0));
	myAssertEquals(0, statistics5.getValue(m_doubleIndex0));
    }

    public void testSerialisation() throws Exception
    {
	final Random random = new Random();

	final RawStatisticsImplementation original0 =
	    new RawStatisticsImplementation();
	original0.addValue(m_longIndex0, Math.abs(random.nextLong()));
	original0.addValue(m_longIndex2, Math.abs(random.nextLong()));

	final RawStatisticsImplementation original1 =
	    new RawStatisticsImplementation();

	final ByteArrayOutputStream byteOutputStream =
	    new ByteArrayOutputStream();

	final ObjectOutputStream objectOutputStream =
	    new ObjectOutputStream(byteOutputStream);

	final Serialiser serialiser = new Serialiser();

	original0.myWriteExternal(objectOutputStream, serialiser);
	original1.myWriteExternal(objectOutputStream, serialiser);

	objectOutputStream.close();

	final ObjectInputStream objectInputStream =
	    new ObjectInputStream(
		new ByteArrayInputStream(byteOutputStream.toByteArray()));

	final RawStatisticsImplementation received0 =
	    new RawStatisticsImplementation(objectInputStream, serialiser);

	final RawStatisticsImplementation received1 =
	    new RawStatisticsImplementation(objectInputStream, serialiser);

	assertEquals(original0, received0);
	assertEquals(original1, received1);
    }	

    private void myAssertEquals(double a, double b)
    {
	assertEquals(a, b, 0.000000001d);
    }
}
