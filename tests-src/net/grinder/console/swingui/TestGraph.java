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

import java.awt.BorderLayout;
import java.text.DecimalFormat;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import javax.swing.JComponent;
import javax.swing.JFrame;

import net.grinder.statistics.PeakStatisticExpression;
import net.grinder.statistics.StatisticExpression;
import net.grinder.statistics.StatisticExpressionFactory;
import net.grinder.statistics.StatisticsIndexMap;
import net.grinder.statistics.TestStatistics;
import net.grinder.statistics.TestStatisticsFactory;



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

	for (int i=0; i<200; i++) {
	    graph.add(s_random.nextDouble());
	    pause();
	}
    }

    public void testLabelledGraph() throws Exception
    {
	final TestStatisticsFactory testStatisticsFactory
	    = TestStatisticsFactory.getInstance();

	final StatisticsIndexMap indexMap = StatisticsIndexMap.getInstance();

	final StatisticsIndexMap.LongIndex periodIndex =
	    indexMap.getIndexForLong("period");

	final StatisticExpressionFactory statisticExpressionFactory =
	    StatisticExpressionFactory.getInstance();

	final StatisticExpression tpsExpression =
	    statisticExpressionFactory.createExpression(
		"(* 1000 (/(+ untimedTransactions timedTransactions) period))"
		);

	final PeakStatisticExpression peakTPSExpression =
	    statisticExpressionFactory.createPeak(
		indexMap.getIndexForDouble("peakTPS"), tpsExpression);

	final LabelledGraph labelledGraph =
	    new LabelledGraph("Test", new Resources(), tpsExpression,
			      peakTPSExpression);

	createUI(labelledGraph);

	double peak = 0d;

	final TestStatistics cumulativeStatistics =
	    testStatisticsFactory.create();

	final DecimalFormat format = new DecimalFormat();

	final int period = 1000;

	for (int i=0; i<200; i++) {
	    final TestStatistics intervalStatistics =
		testStatisticsFactory.create();

	    intervalStatistics.setValue(periodIndex, period);

	    while (s_random.nextInt() > 0) {
		intervalStatistics.addTransaction();
	    }

	    long time;

	    while ((time = s_random.nextInt()) > 0) {
		intervalStatistics.addTransaction(time % 10000);
	    }

	    while (s_random.nextFloat() > 0.95) {
		intervalStatistics.addError();
	    }

	    cumulativeStatistics.add(intervalStatistics);
	    cumulativeStatistics.setValue(periodIndex, (1+i)*period);

	    peakTPSExpression.update(intervalStatistics, cumulativeStatistics);
	    labelledGraph.add(intervalStatistics, cumulativeStatistics,
			      format);
	    pause();
	}
    }

    private void pause() throws Exception
    {
	if (m_pauseTime > 0) {
	    Thread.sleep(m_pauseTime);
	}
    }
}

