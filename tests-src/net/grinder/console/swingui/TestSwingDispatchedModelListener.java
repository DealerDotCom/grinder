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

import net.grinder.console.model.ModelListener;
import net.grinder.statistics.StatisticsView;


/**
 * @author Philip Aston
 * @version $Revision$
 */
public class TestSwingDispatchedModelListener extends TestCase
{
    public static void main(String[] args)
    {
	TestRunner.run(TestSwingDispatchedModelListener.class);
    }

    public TestSwingDispatchedModelListener(String name)
    {
	super(name);
    }

    private Runnable m_voidRunnable = new Runnable() { public void run() {} };

    public void testDispatch() throws Exception
    {
	final MyModelListener listener = new MyModelListener();

	final ModelListener swingDispatchedListener =
	    new SwingDispatchedModelListener(listener);

	listener.update();

	// Wait for a dummy event to be processed by the swing event
	// queue.
	SwingUtilities.invokeAndWait(m_voidRunnable);

	assert(listener.m_updateCalled);

	final Set myTests = new HashSet();
	listener.reset(myTests);
	SwingUtilities.invokeAndWait(m_voidRunnable);
	assert(listener.m_resetCalled);
	assertSame(myTests, listener.m_resetSet);

	final StatisticsView view1 = new StatisticsView();
	final StatisticsView view2 = new StatisticsView();
	listener.newStatisticsViews(view1, view2);
	SwingUtilities.invokeAndWait(m_voidRunnable);
	assert(listener.m_updateCalled);
	assertSame(view1, listener.m_intervalStatisticsView);
	assertSame(view2, listener.m_cumulativeStatisticsView);
    }

    private class MyModelListener implements ModelListener
    {
	public boolean m_resetCalled = false;
	public Set m_resetSet;

	public boolean m_updateCalled = false;

	public boolean m_newStatisticsViewsCalled = false;
	public StatisticsView m_intervalStatisticsView;
	public StatisticsView m_cumulativeStatisticsView;
	
	public void reset(Set newTests) 
	{
	    m_resetCalled = true;
	    m_resetSet = newTests;
	}

	public void update()
	{
	    m_updateCalled = true;
	}

	public void newStatisticsViews(StatisticsView intervalStatisticsView,
				       StatisticsView cumulativeStatisticsView)
	{
	    m_newStatisticsViewsCalled = true;
	    m_intervalStatisticsView = intervalStatisticsView;
	    m_cumulativeStatisticsView = cumulativeStatisticsView;
	}
    }
}

