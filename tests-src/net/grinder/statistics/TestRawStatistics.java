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
 * Unit test case for <code>RawStatistics</code>.
 *
 * @author Philip Aston
 * @version $Revision$
 * @see RawStatistics
 */
public class TestRawStatistics extends TestCase
{
    public static void main(String[] args)
    {
	TestRunner.run(TestRawStatistics.class);
    }

    public TestRawStatistics(String name)
    {
	super(name);
    }

    public void testCreation() 
    {
	final RawStatistics statistics = new RawStatistics();

	assertEquals(0, statistics.getValue(10));
    }

    public void testAddValueIncrementAndEquals() 
    {
	final RawStatistics statistics0 = new RawStatistics();
	final RawStatistics statistics1 = new RawStatistics();

	assertEquals(statistics0, statistics0);
	assertEquals(statistics0, statistics1);
	
	final int indexA = 1;

	statistics0.addValue(indexA, 700);
	statistics0.addValue(indexA, 300);
	assert(!statistics0.equals(statistics1));
	statistics1.addValue(indexA, 500);
	assert(!statistics0.equals(statistics1));
	statistics1.addValue(indexA, 500);
	assertEquals(statistics0, statistics1);

	final int indexB = 3;

	statistics0.incrementValue(indexB);
	assert(!statistics0.equals(statistics1));
	statistics1.incrementValue(indexB);
	assertEquals(statistics0, statistics1);

	assertEquals(statistics0, statistics0);
	assertEquals(statistics1, statistics1);

	try {
	    statistics0.addValue(-1, 1);
	    fail("Expected IllegalArgumentException");
	}
	catch (IllegalArgumentException e) {
	}

	try {
	    statistics0.incrementValue(-1);
	    fail("Expected IllegalArgumentException");
	}
	catch (IllegalArgumentException e) {
	}

	try {
	    statistics0.addValue(1, -1);
	    fail("Expected IllegalArgumentException");
	}
	catch (IllegalArgumentException e) {
	}
    }

    public void testAdd() throws Exception
    {
	final RawStatistics statistics0 = new RawStatistics();
	final RawStatistics statistics1 = new RawStatistics();

	// 0 + 0 = 0
	statistics0.add(statistics1);
	assertEquals(statistics0, statistics1);

	// 0 + 1 = 1
	statistics0.addValue(0, 100);
	statistics0.addValue(3, 55);
	statistics1.add(statistics0);
	assertEquals(statistics0, statistics1);

	// 1 + 1 != 1
	statistics1.add(statistics0);
	assert(!statistics0.equals(statistics1));

	// 1 + 1 = 2
	statistics0.add(statistics0); // Test add to self.
	assertEquals(statistics0, statistics1);

	assertEquals(200, statistics0.getValue(0));
	assertEquals(110, statistics0.getValue(3));
    }

    public void testGetDelta() throws Exception
    {
	final RawStatistics statistics0 = new RawStatistics();
	statistics0.addValue(0, 1234);

	final RawStatistics statistics1 = statistics0.getDelta(false);
	assertEquals(statistics0, statistics1);

	final RawStatistics statistics2 = statistics0.getDelta(true);
	assertEquals(statistics0, statistics1);
	assertEquals(statistics0, statistics2);

	final RawStatistics statistics3 = statistics0.getDelta(false);
	assert(!statistics0.equals(statistics3));

	statistics0.addValue(1, 2345);
	final RawStatistics statistics4 = statistics0.getDelta(true);

	assertEquals(0, statistics4.getValue(0));
	assertEquals(2345, statistics4.getValue(1));

	statistics0.addValue(0, 5678);

	final RawStatistics statistics5 = statistics0.getDelta(true);
	assertEquals(5678, statistics5.getValue(0));
	assertEquals(0, statistics5.getValue(1));
    }

    public void testSerialisation() throws Exception
    {
	final Random random = new Random();

	final RawStatistics original0 = new RawStatistics();
	original0.addValue(0, Math.abs(random.nextLong()));
	original0.addValue(1, Math.abs(random.nextLong()));
	original0.addValue(5, Math.abs(random.nextLong()));

	final RawStatistics original1 = new RawStatistics();

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

	final RawStatistics received0 =
	    new RawStatistics(objectInputStream, serialiser);

	final RawStatistics received1 =
	    new RawStatistics(objectInputStream, serialiser);

	assertEquals(original0, received0);
	assertEquals(original1, received1);
    }
	
}
