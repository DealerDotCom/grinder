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

package net.grinder.util;

import junit.framework.TestCase;
import junit.swingui.TestRunner;
//import junit.textui.TestRunner;


/**
 * JUnit test case for {@link Sleeper}.
 *
 * @author Philip Aston
 * @version $Revision$
 */
public class TestSleeper extends TestCase
{
    public static void main(String[] args)
    {
	TestRunner.run(TestSleeper.class);
    }

    public TestSleeper(String name)
    {
	super(name);
    }

    public void testConstruction() throws Exception
    {
	try {
	    new Sleeper(-1, 1, null);
	    fail("IllegalArgumentException expected");
	}
	catch (IllegalArgumentException e) {
	}

	try {
	    new Sleeper(1, -1, null);
	    fail("IllegalArgumentException expected");
	}
	catch (IllegalArgumentException e) {
	}

	new Sleeper(1, 1, null);
    }

    public void testSleepNormal() throws Exception
    {
	// Warm up hotspot.
	final Sleeper sleep0 = new Sleeper(1, 0, null);

	Time time0 = new Time(0, 1000)
	{
	    void doIt() throws Exception { sleep0.sleepNormal(10); }
	};

	for (int i=0; i<10; i++) { time0.run(); }

	// Now do the tests.
	final Sleeper sleep1 = new Sleeper(1, 0, null);

	assert(
	    new Time(50, 70) {
		void doIt() throws Exception  { sleep1.sleepNormal(50); }
	    }.run());

	final Sleeper sleep2 = new Sleeper(2, 0, null);

	assert(
	    new Time(100, 120) {
		void doIt() throws Exception  { sleep2.sleepNormal(50); }
	    }.run());

	final Sleeper sleep3 = new Sleeper(1, 0.1, null);

	final Time time = new Time(90, 110) {
		void doIt() throws Exception { sleep3.sleepNormal(100);}
	    };

	int in = 0;
	for (int i=0; i<30; i++) {
	    if (time.run()) {
		++in;
	    }
	}

	assert(in > 20);
    }

    public void testSleepFlat() throws Exception
    {
	// Warm up hotspot.
	final Sleeper sleep0 = new Sleeper(1, 0, null);

	Time time0 = new Time(0, 1000)
	{
	    void doIt() throws Exception { sleep0.sleepFlat(10); }
	};

	for (int i=0; i<10; i++) { time0.run(); }

	// Now do the tests.
	final Sleeper sleep1 = new Sleeper(1, 0, null);

	assert(
	    new Time(0, 70) {
		void doIt() throws Exception  { sleep1.sleepFlat(50); }
	    }.run());

	final Sleeper sleep2 = new Sleeper(2, 0, null);

	assert(
	    new Time(0, 120) {
		void doIt() throws Exception  { sleep2.sleepFlat(50); }
	    }.run());
    }

    public void testShutdown() throws Exception
    {
	final TakeFifty t1 = new TakeFifty();

	assert(
	    new Time(1000, 1100)
	    {
		void doIt() throws Exception
		{
		    t1.start();
		    Thread.sleep(1000);
		    t1.getSleeper().shutdown();
		    t1.join();
		}
	    }.run());
    }

    public void testShutdownAllCurrentSleepers() throws Exception
    {
	final Thread t1 = new TakeFifty();
	final Thread t2 = new TakeFifty();

	assert(
	    new Time(1000, 1100)
	    {
		void doIt() throws Exception
		{
		    t1.start();
		    t2.start();
		    Thread.sleep(1000);
		    Sleeper.shutdownAllCurrentSleepers();
		    t1.join();
		    t2.join();
		}
	    }.run());
    }

    private final class TakeFifty extends Thread
    {
	private final Sleeper m_sleeper;

	public TakeFifty() throws Sleeper.ShutdownException
	{
	    m_sleeper = new Sleeper(1, 0, null);
	}

	public final void run()
	{
	    try {
		m_sleeper.sleepNormal(50000);
	    }
	    catch (Sleeper.ShutdownException e) {
	    }
	}

	public final Sleeper getSleeper()
	{
	    return m_sleeper;
	}
    }

    private abstract class Time
    {
	private final double m_expectedMin;
	private final double m_expectedMax;

	public Time(double expectedMin, double expectedMax)
	{
	    m_expectedMin = expectedMin;
	    m_expectedMax = expectedMax; // A bit of leeway.
	}

	abstract void doIt() throws Exception;

	public boolean run() throws Exception
	{
	    final long then = System.currentTimeMillis();
	    doIt();
	    final long time = System.currentTimeMillis() - then;

	    return m_expectedMin <= time && m_expectedMax >= time;
	}
    }

}
