// Copyright (C) 2001, 2002 Philip Aston
// All rights reserved.
//
// This file is part of The Grinder software distribution. Refer to
// the file LICENSE which is part of The Grinder distribution for
// licensing details. The Grinder distribution is available on the
// Internet at http://grinder.sourceforge.net/
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
// "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
// LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
// FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
// REGENTS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
// INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
// (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
// SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
// HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
// STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
// ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
// OF THE POSSIBILITY OF SUCH DAMAGE.

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
