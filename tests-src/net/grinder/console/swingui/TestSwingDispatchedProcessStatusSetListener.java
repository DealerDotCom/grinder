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

import javax.swing.SwingUtilities;

import net.grinder.common.ProcessStatus;
import net.grinder.console.model.ProcessStatusSetListener;


/**
 * @author Philip Aston
 * @version $Revision$
 */
public class TestSwingDispatchedProcessStatusSetListener extends TestCase
{
    public static void main(String[] args)
    {
	TestRunner.run(TestSwingDispatchedProcessStatusSetListener.class);
    }

    public TestSwingDispatchedProcessStatusSetListener(String name)
    {
	super(name);
    }

    private Runnable m_voidRunnable = new Runnable() { public void run() {} };

    public void testDispatch() throws Exception
    {
	final MyProcessStatusSetListener listener =
	    new MyProcessStatusSetListener();

	final ProcessStatusSetListener swingDispatchedListener =
	    new SwingDispatchedProcessStatusSetListener(listener);

	final ProcessStatus[] data = new ProcessStatus[0];
	final int running = 1;
	final int total = 2;

	listener.update(data, running, total);

	// Wait for a dummy event to be processed by the swing event
	// queue.
	SwingUtilities.invokeAndWait(m_voidRunnable);

	assert(listener.m_updateCalled);
	assertEquals(data, listener.m_updateData);
	assertEquals(running, listener.m_updateRunning);
	assertEquals(total, listener.m_updateTotal);
    }

    private class MyProcessStatusSetListener
	implements ProcessStatusSetListener
    {
	public boolean m_updateCalled = false;
	public ProcessStatus[] m_updateData;
	public int m_updateRunning;
	public int m_updateTotal;

	public void update(ProcessStatus[] data, int running, int total)
	{
	    m_updateCalled = true;
	    m_updateData = data;
	    m_updateRunning = running;
	    m_updateTotal = total;
	}
    }
}

