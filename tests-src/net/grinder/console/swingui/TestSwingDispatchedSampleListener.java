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

package net.grinder.console.swingui;

import junit.framework.TestCase;
import junit.swingui.TestRunner;
//import junit.textui.TestRunner;

import java.util.HashSet;
import java.util.Set;

import javax.swing.SwingUtilities;

import net.grinder.console.model.SampleListener;
import net.grinder.statistics.TestStatistics;
import net.grinder.statistics.TestStatisticsFactory;


/**
 * @author Philip Aston
 * @version $Revision$
 */
public class TestSwingDispatchedSampleListener extends TestCase
{
    public static void main(String[] args)
    {
	TestRunner.run(TestSwingDispatchedSampleListener.class);
    }

    public TestSwingDispatchedSampleListener(String name)
    {
	super(name);
    }

    private Runnable m_voidRunnable = new Runnable() { public void run() {} };

    public void testDispatch() throws Exception
    {
	final MySampleListener listener = new MySampleListener();

	final SampleListener swingDispatchedListener =
	    new SwingDispatchedSampleListener(listener);

	final TestStatisticsFactory testStatisticsFactory =
	    TestStatisticsFactory.getInstance();

	final TestStatistics intervalStatistics =
	    testStatisticsFactory.create();

	final TestStatistics cumulativeStatistics =
	    testStatisticsFactory.create();

	listener.update(intervalStatistics, cumulativeStatistics);

	// Wait for a dummy event to be processed by the swing event
	// queue.
	SwingUtilities.invokeAndWait(m_voidRunnable);

	assertSame(intervalStatistics, listener.m_intervalStatistics);
	assertSame(cumulativeStatistics, listener.m_cumulativeStatistics);
    }

    private class MySampleListener implements SampleListener
    {
	public TestStatistics m_intervalStatistics;
	public TestStatistics m_cumulativeStatistics;
	
	public void update(TestStatistics intervalStatistics,
			   TestStatistics cumulativeStatistics)
	{
	    m_intervalStatistics = intervalStatistics;
	    m_cumulativeStatistics = cumulativeStatistics;
	}
    }
}

