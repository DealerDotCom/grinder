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

package net.grinder.engine.process;

import junit.framework.TestCase;
import junit.swingui.TestRunner;
//import junit.textui.TestRunner;

import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import net.grinder.plugininterface.Test;
import net.grinder.util.GrinderProperties;


/**
 * @author Philip Aston
 * @version $Revision$
 */
public class TestTestStatistics extends TestCase
{
    public static void main(String[] args)
    {
	TestRunner.run(TestTestStatistics.class);
    }

    public TestTestStatistics(String name)
    {
	super(name);
    }

    protected void setUp()
    {
    }

    public void testCreation() 
    {
	final TestStatistics testStatistics = new TestStatistics();

	assertEquals(0, testStatistics.getTransactions());
	assertEquals(0, testStatistics.getTotalTime());
	assertEquals(0, testStatistics.getErrors());
	assertEquals(0, testStatistics.getAbortions());
    }

    public void testAddsAndEquals() 
    {
	final TestStatistics testStatistics0 = new TestStatistics();
	final TestStatistics testStatistics1 = new TestStatistics();

	assertEquals(testStatistics0, testStatistics0);
	assertEquals(testStatistics0, testStatistics1);
	
	testStatistics0.addTransaction(700);
	testStatistics0.addTransaction(300);
	assert(!testStatistics0.equals(testStatistics1));
	testStatistics1.addTransaction(500);
	assert(!testStatistics0.equals(testStatistics1));
	testStatistics1.addTransaction(500);
	assertEquals(testStatistics0, testStatistics1);

	testStatistics0.addError();
	assert(!testStatistics0.equals(testStatistics1));
	testStatistics1.addError();
	assertEquals(testStatistics0, testStatistics1);

	testStatistics1.addAbortion();
	assert(!testStatistics0.equals(testStatistics1));
	testStatistics0.addAbortion();
	assertEquals(testStatistics0, testStatistics1);

	assertEquals(testStatistics0, testStatistics0);
	assertEquals(testStatistics1, testStatistics1);
    }

    public void testGetClone()
    {
	final TestStatistics testStatistics0 = new TestStatistics();
	final TestStatistics testStatistics1 = testStatistics0.getClone();
	assertEquals(testStatistics0, testStatistics1);
	assert(testStatistics0 != testStatistics1);
	assert(testStatistics0.getClass() == testStatistics1.getClass());
    }

    public void testAdd()
    {
	final TestStatistics testStatistics0 = new TestStatistics();
	final TestStatistics testStatistics1 = new TestStatistics();

	// 0 + 0 = 0
	testStatistics0.add(testStatistics1);
	assertEquals(testStatistics0, testStatistics1);

	// 0 + 1 = 1
	testStatistics0.addTransaction(100);
	testStatistics0.addAbortion();
	testStatistics0.addAbortion();
	testStatistics1.add(testStatistics0);
	assertEquals(testStatistics0, testStatistics1);

	// 1 + 1 != 1
	testStatistics1.add(testStatistics0);
	assert(!testStatistics0.equals(testStatistics1));

	// 1 + 1 = 2
	testStatistics0.add(testStatistics0);
	assertEquals(testStatistics0, testStatistics1);

	assertEquals(200, testStatistics0.getTotalTime());
	assertEquals(2, testStatistics0.getTransactions());
	assertEquals(0, testStatistics0.getErrors());
	assertEquals(4, testStatistics0.getAbortions());
    }

    public void testGetDelta()
    {
	final TestStatistics testStatistics0 = new TestStatistics();
	testStatistics0.addTransaction(1234);

	final TestStatistics testStatistics1 = testStatistics0.getDelta(false);
	assertEquals(testStatistics0, testStatistics1);

	final TestStatistics testStatistics2 = testStatistics0.getDelta(true);
	assertEquals(testStatistics0, testStatistics2);

	final TestStatistics testStatistics3 = testStatistics0.getDelta(false);
	assert(!testStatistics0.equals(testStatistics3));

	testStatistics0.addError();
	testStatistics0.addAbortion();

	final TestStatistics testStatistics4 = testStatistics0.getDelta(true);
	assertEquals(0, testStatistics4.getTransactions());
	assertEquals(0, testStatistics4.getTotalTime());
	assertEquals(1, testStatistics4.getErrors());
	assertEquals(1, testStatistics4.getAbortions());

	testStatistics0.addTransaction(5678);
	final TestStatistics testStatistics5 = testStatistics0.getDelta(true);
	assertEquals(1, testStatistics5.getTransactions());
	assertEquals(5678, testStatistics5.getTotalTime());
	assertEquals(0, testStatistics5.getErrors());
	assertEquals(0, testStatistics5.getAbortions());
    }

    public void testGetAverageTransactionTime()
    {
	final TestStatistics testStatistics0 = new TestStatistics();

	assert(Double.isNaN(testStatistics0.getAverageTransactionTime()));

	final double accuracy = 1e-20;

	testStatistics0.addTransaction(1);
	assertEquals(1d, testStatistics0.getAverageTransactionTime(), accuracy);

	testStatistics0.addTransaction(1);
	assertEquals(1d, testStatistics0.getAverageTransactionTime(), accuracy);

	testStatistics0.addTransaction(2);
	assertEquals(4/3d, testStatistics0.getAverageTransactionTime(), accuracy);

	testStatistics0.addTransaction(0);
	assertEquals(1d, testStatistics0.getAverageTransactionTime(), accuracy);
    }
}
