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
 * JUnit test case for {@link Sleep}.
 *
 * @author Philip Aston
 * @version $Revision$
 */
public class TestSleep extends TestCase
{
    public static void main(String[] args)
    {
	TestRunner.run(TestSleep.class);
    }

    public TestSleep(String name)
    {
	super(name);
    }

    public void testConstruction() throws Exception
    {
	try {
	    new Sleep(-1, 1, null);
	    fail("IllegalArgumentException expected");
	}
	catch (IllegalArgumentException e) {
	}

	try {
	    new Sleep(1, -1, null);
	    fail("IllegalArgumentException expected");
	}
	catch (IllegalArgumentException e) {
	}

	new Sleep(1, 1, null);
    }

    public void testSleepNormal() throws Exception
    {
	// Warm up hotspot.
	final Sleep sleep0 = new Sleep(1, 0, null);

	Time time0 = new Time(0, 1000)
	{
	    void doIt() throws Exception { sleep0.sleepNormal(10); }
	};

	for (int i=0; i<10; i++) { time0.run(); }

	// Now do the tests.
	final Sleep sleep1 = new Sleep(1, 0, null);

	assert(
	    new Time(50, 70) {
		void doIt() throws Exception  { sleep1.sleepNormal(50); }
	    }.run());

	final Sleep sleep2 = new Sleep(2, 0, null);

	assert(
	    new Time(100, 120) {
		void doIt() throws Exception  { sleep2.sleepNormal(50); }
	    }.run());

	final Sleep sleep3 = new Sleep(1, 0.1, null);

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
	final Sleep sleep0 = new Sleep(1, 0, null);

	Time time0 = new Time(0, 1000)
	{
	    void doIt() throws Exception { sleep0.sleepFlat(10); }
	};

	for (int i=0; i<10; i++) { time0.run(); }

	// Now do the tests.
	final Sleep sleep1 = new Sleep(1, 0, null);

	assert(
	    new Time(0, 70) {
		void doIt() throws Exception  { sleep1.sleepFlat(50); }
	    }.run());

	final Sleep sleep2 = new Sleep(2, 0, null);

	assert(
	    new Time(0, 120) {
		void doIt() throws Exception  { sleep2.sleepFlat(50); }
	    }.run());
    }

    public void testShutdown() throws Exception
    {
	final Sleep sleep = new Sleep(1, 0, null);

	final Thread t1 = new Thread() {
		public void run()
		{
		    try {
			sleep.sleepNormal(50000);
		    }
		    catch (Sleep.ShutdownException e) {
		    }
		}
	    };
		    

	assert(
	    new Time(1000, 1100)
	    {
		void doIt() throws Exception
		{
		    t1.start();
		    sleep.sleepNormal(1000);
		    Sleep.shutdown();
		    t1.join();
		    try {
			sleep.sleepNormal(50000);
		    }
		    catch (Sleep.ShutdownException e) {
		    }
		}
	    }.run());
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
