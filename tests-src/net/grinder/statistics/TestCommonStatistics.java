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

import net.grinder.common.GrinderException;


/**
 * Unit test case for <code>CommonStatistics</code>.
 *
 * @author Philip Aston
 * @version $Revision$
 * @see RawStatistics
 */
public class TestCommonStatistics extends TestCase
{
    public static void main(String[] args)
    {
	TestRunner.run(TestCommonStatistics.class);
    }

    public TestCommonStatistics(String name)
    {
	super(name);
    }

    public void testCreation() throws Exception
    {
	final CommonStatistics commonStatistics =
	    CommonStatistics.getInstance();

	assertSame(commonStatistics, CommonStatistics.getInstance());
	assert(commonStatistics.getIndexMap() != null);
	assert(commonStatistics.getStatisticsView() != null);
    }

    public void testTestStatisticsImplementation() throws Exception
    {
	final CommonStatistics commonStatistics =
	    CommonStatistics.getInstance();

	final TestStatistics testStatistics1 =
	    commonStatistics.new TestStatisticsImplementation();

	assertEquals(0, testStatistics1.getErrors());
	assertEquals(0, testStatistics1.getTransactions());
	assert(Double.isNaN(testStatistics1.getAverageTransactionTime()));

	final TestStatistics testStatistics2 =
	    commonStatistics.new TestStatisticsImplementation();

	assert(testStatistics1 != testStatistics2);

	assertEquals(testStatistics1, testStatistics2);

	testStatistics1.addError();
	assertEquals(1, testStatistics1.getErrors());
	assert(!testStatistics1.equals(testStatistics2));

	testStatistics2.addError();
	assertEquals(testStatistics1, testStatistics2);

	testStatistics1.addTransaction();
	assertEquals(1, testStatistics1.getTransactions());
	assert(!testStatistics1.equals(testStatistics2));

	testStatistics2.addTransaction();
	assertEquals(testStatistics1, testStatistics2);

	testStatistics1.addTransaction(5);
	testStatistics2.addTransaction(10);
	assertEquals(2, testStatistics1.getTransactions());
	assert(!testStatistics1.equals(testStatistics2));

	testStatistics1.addTransaction(10);
	testStatistics2.addTransaction(5);
	assertEquals(testStatistics1, testStatistics2);
    }
}
