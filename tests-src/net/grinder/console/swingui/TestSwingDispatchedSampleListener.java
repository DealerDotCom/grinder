// Copyright (C) 2000 Paco Gomez
// Copyright (C) 2000, 2001, 2002 Philip Aston
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

