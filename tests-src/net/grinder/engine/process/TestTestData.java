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

package net.grinder.engine.process;

import junit.framework.TestCase;
import junit.swingui.TestRunner;
//import junit.textui.TestRunner;

import net.grinder.common.GrinderProperties;
import net.grinder.common.Test;
import net.grinder.common.TestImplementation;
import net.grinder.statistics.StatisticsImplementation;


/**
 * @author Philip Aston
 * @version $Revision$
 */
public class TestTestData extends TestCase
{
    public static void main(String[] args)
    {
	TestRunner.run(TestTestData.class);
    }

    public TestTestData(String name)
    {
	super(name);
    }

    protected void setUp()
    {
    }

    public void test0() throws Exception
    {
	final Test test = new TestImplementation(99, "Some stuff",
						 new GrinderProperties());
	
	final long sleepTime = 1234;
	final StatisticsImplementation statistics =
	    new StatisticsImplementation();

	final TestData testData = new TestData(test, sleepTime, statistics);

	assertEquals(test, testData.getTest());
	assertEquals(sleepTime, testData.getSleepTime());
	assertNotNull(testData.getStatistics());
    }

    public void test1() throws Exception
    {
	final GrinderProperties properties = new GrinderProperties();
	properties.put("Something", "blah");

	final Test test = new TestImplementation(-33, "", properties);
	final long sleepTime = 1234;
	final StatisticsImplementation statistics =
	    new StatisticsImplementation();

	final TestData testData = new TestData(test, sleepTime, statistics);

	assertEquals(test, testData.getTest());
	assertEquals(sleepTime, testData.getSleepTime());
	assertNotNull(testData.getStatistics());
    }
}
