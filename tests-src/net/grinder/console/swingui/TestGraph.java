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

import java.awt.BorderLayout;
import java.text.DecimalFormat;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import javax.swing.JComponent;
import javax.swing.JFrame;

import net.grinder.statistics.CumulativeStatistics;
import net.grinder.statistics.IntervalStatistics;


/**
 * @author Philip Aston
 * @version $Revision$
 */
public class TestGraph extends TestCase
{
    public static void main(String[] args)
    {
	TestRunner.run(TestGraph.class);
    }

    public TestGraph(String name)
    {
	super(name);
    }

    private int m_pauseTime = 1;
    private Random s_random = new Random();
    private JFrame m_frame;

    protected void setUp() throws Exception
    {
	m_frame = new JFrame("Test Graph");
    }

    protected void tearDown() throws Exception
    {
	m_frame.dispose();
    }

    private void createUI(JComponent component) throws Exception
    {
        m_frame.getContentPane().add(component, BorderLayout.CENTER);
        m_frame.pack();
        m_frame.setVisible(true);
    }

    public void testRamp() throws Exception
    {
	final Graph graph = new Graph(25);
	createUI(graph);

	graph.setMaximum(150);

	for (int i=0; i<150; i++) {
	    graph.add(i);
	    pause();
	}
    }

    public void testRandom() throws Exception
    {
	final Graph graph = new Graph(100);
	createUI(graph);

	graph.setMaximum(1);

	for (int i=0; i<500; i++) {
	    graph.add(s_random.nextDouble());
	    pause();
	}
    }

    public void testLabelledGraph() throws Exception
    {
	final LabelledGraph labelledGraph =
	    new LabelledGraph("Test", new Resources());

	createUI(labelledGraph);

	double peak = 0d;

	final MyCumulativeStatistics myCS = new MyCumulativeStatistics();

	final DecimalFormat format = new DecimalFormat();

	for (int i=0; i<500; i++) {
	    myCS.next();
	    labelledGraph.add(myCS.getIntervalStatistics(),
			      myCS, format);
	    pause();
	}
    }

    private void pause() throws Exception
    {
	if (m_pauseTime > 0) {
	    Thread.sleep(m_pauseTime);
	}
    }

    private class MyCumulativeStatistics implements CumulativeStatistics
    {
	private long m_samples = 0;
	private long m_transactions = 0;
	private long m_errors = 0;
	private double m_totalTime = 0;
	private double m_totalTPS = 0;
	private double m_peakTPS = 0;
	private IntervalStatisticsImplementation m_intervalStatistics;

	public double getAverageTransactionTime()
	{
	    return m_totalTime/m_samples;
	}
	
	public long getTransactions()
	{
	    return m_transactions;
	}
	
	public long getErrors()
	{
	    return m_errors;
	}

	public double getTPS()
	{
	    return m_totalTPS/m_samples;
	}
	
	public double getPeakTPS()
	{
	    return m_peakTPS;
	}

	public IntervalStatistics getIntervalStatistics()
	{
	    return m_intervalStatistics;
	}

	public void next()
	{
	    m_intervalStatistics = new IntervalStatisticsImplementation();

	    double tps = m_intervalStatistics.getTPS();
	    
	    if (tps > m_peakTPS) {
		m_peakTPS = tps;
	    }

	    m_totalTPS += tps;
	    m_transactions += m_intervalStatistics.getTransactions();
	    m_errors += m_intervalStatistics.getErrors();
	    m_totalTime += m_intervalStatistics.getAverageTransactionTime();

	    ++m_samples;
	}

	private class IntervalStatisticsImplementation
	    implements IntervalStatistics
	{
	    private double m_tps = s_random.nextDouble() * 100;
	    private long m_transactions = s_random.nextInt(10);
	    private long m_errors = s_random.nextInt(20)/19;
	    private double m_averageTransactionTime =
		s_random.nextDouble() * 20 * m_transactions;

	    public double getAverageTransactionTime() 
	    {
		return m_averageTransactionTime;
	    }

	    public long getTransactions()
	    {
		return m_transactions;
	    }

	    public long getErrors()
	    {
		return m_errors;
	    }

	    public double getTPS()
	    {
		return m_tps;
	    }
	}
    }
}

