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

package net.grinder.console.model;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.NumberFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import net.grinder.common.GrinderException;
import net.grinder.common.Test;
import net.grinder.console.common.ConsoleException;
import net.grinder.statistics.CommonStatisticsViews;
import net.grinder.statistics.ExpressionView;
import net.grinder.statistics.PeakStatisticExpression;
import net.grinder.statistics.StatisticExpression;
import net.grinder.statistics.StatisticExpressionFactory;
import net.grinder.statistics.StatisticsIndexMap;
import net.grinder.statistics.StatisticsView;
import net.grinder.statistics.TestStatistics;
import net.grinder.statistics.TestStatisticsFactory;
import net.grinder.statistics.TestStatisticsMap;
import net.grinder.util.SignificantFigureFormat;


/**
 * The console model.
 *
 * <p>This class uses synchronisation sparingly, in particular it is
 * not used to protect accessor methods across a model structure
 * change. Instead clients should implement {@link ModelListener}, and
 * should not call any of the following methods in between receiving a
 * {@link ModelListener#reset} and a {@link ModelListener#update}.
 * <ul>
 * <li>{@link #getCumulativeStatistics}</li>
 * <li>{@link #getLastSampleStatistics}</li>
 * <li>{@link #getNumberOfTests}</li>
 * <li>{@link #getTest}</li>
 * </ul>
 * These methods will throw a {@link IllegalStateException} if called between a 
 * @link ModelListener#reset} and a {@link ModelListener#update}.
 * </p>
 *
 * @author Philip Aston
 * @version $Revision$
 */
public class Model
{
    public final static int STATE_WAITING_FOR_TRIGGER = 0;
    public final static int STATE_STOPPED = 1;
    public final static int STATE_CAPTURING = 2;

    private long m_startTime;
    private long m_stopTime;

    private final TestStatisticsFactory m_testStatisticsFactory =
	TestStatisticsFactory.getInstance();

    /**
     * The current test set. A TreeSet is used to maintain the test
     * order.
     **/
    private final Set m_tests = new TreeSet();

    /** A {@link SampleAccumulator} for each test. **/
    private final Map m_accumulators = new HashMap();

    /** Index into m_tests by test index. **/
    private Test[] m_testArray;

    /** Index into m_accumulators by test index. **/
    private SampleAccumulator[] m_accumulatorArray;

    /** true => m_testArray and m_accumulatorArray are valid. **/
    private boolean m_indicesValid = false;

    private final SampleAccumulator m_totalSampleAccumulator =
	new SampleAccumulator();

    private final ConsoleProperties m_properties;
    private int m_sampleInterval;
    private NumberFormat m_numberFormat;

    private int m_state = 0;
    private long m_sampleCount = 0;
    private boolean m_receivedReport = false;
    private boolean m_receivedReportInLastInterval = false;
    private final List m_modelListeners = new LinkedList();

    private final StatisticsIndexMap.LongIndex m_periodIndex;
    private final StatisticExpression m_tpsExpression;
    private final ExpressionView m_tpsExpressionView;
    private final PeakStatisticExpression m_peakTPSExpression;
    private final ExpressionView m_peakTPSExpressionView;
    private final StatisticsView m_intervalStatisticsView =
	new StatisticsView();
    private final StatisticsView m_cumulativeStatisticsView =
	new StatisticsView();

    private final ProcessStatusSet m_processStatusSet =
	new ProcessStatusSet();

    /**
     * System.currentTimeMillis() is expensive. This is acurate to one
     * sample interval.
     **/
    private long m_currentTime;

    public Model(ConsoleProperties properties) throws GrinderException
    {
	final StatisticExpressionFactory statisticExpressionFactory =
	    StatisticExpressionFactory.getInstance();

	final StatisticsIndexMap indexMap = StatisticsIndexMap.getInstance();

	m_periodIndex = indexMap.getIndexForLong("period");

	m_tpsExpression =
	    statisticExpressionFactory.createExpression(
		"(* 1000 (/ (+ untimedTransactions timedTransactions) period))"
		);
	
	m_tpsExpressionView =
	    new ExpressionView("TPS", "statistic.tps", m_tpsExpression);

	m_peakTPSExpression =
	    statisticExpressionFactory.createPeak(
		indexMap.getIndexForDouble("peakTPS"), m_tpsExpression);
	
	m_peakTPSExpressionView =
	    new ExpressionView("Peak TPS", "statistic.peakTPS",
			       m_peakTPSExpression);

	final StatisticsView summaryStatisticsView =
	    CommonStatisticsViews.getSummaryStatisticsView();

	m_intervalStatisticsView.add(summaryStatisticsView);
	m_intervalStatisticsView.add(m_tpsExpressionView);

	m_cumulativeStatisticsView.add(summaryStatisticsView);
	m_cumulativeStatisticsView.add(m_tpsExpressionView);
	m_cumulativeStatisticsView.add(m_peakTPSExpressionView);

	m_properties = properties;

	m_sampleInterval = m_properties.getSampleInterval();
	m_numberFormat =
	    new SignificantFigureFormat(m_properties.getSignificantFigures());

	m_testArray = new Test[0];
	m_accumulatorArray = new SampleAccumulator[0];
	m_indicesValid = true;

	setState(STATE_WAITING_FOR_TRIGGER);

	new Thread(new Sampler()).start();

	m_properties.addPropertyChangeListener(
	    ConsoleProperties.SIG_FIG_PROPERTY,
	    new PropertyChangeListener() {
		public void propertyChange(PropertyChangeEvent event) {
		    m_numberFormat =
			new SignificantFigureFormat(
			    ((Integer)event.getNewValue()).intValue());
		}
	    });

	m_properties.addPropertyChangeListener(
	    ConsoleProperties.SAMPLE_INTERVAL_PROPERTY,
	    new PropertyChangeListener() {
		public void propertyChange(PropertyChangeEvent event) {
		    m_sampleInterval =
			((Integer)event.getNewValue()).intValue();
		}
	    });
    }

    public final ProcessStatusSet getProcessStatusSet()
    {
	return m_processStatusSet;
    }

    public ExpressionView getTPSExpressionView()
    {
	return m_tpsExpressionView;
    }

    public StatisticExpression getTPSExpression()
    {
	return m_tpsExpression;
    }

    public ExpressionView getPeakTPSExpressionView()
    {
	return m_peakTPSExpressionView;
    }

    public StatisticExpression getPeakTPSExpression()
    {
	return m_peakTPSExpression;
    }

    public synchronized void registerTests(Set tests)
    {
	// Need to copy collection, might be immutable.
	final HashSet newTests = new HashSet(tests);

	newTests.removeAll(m_tests);

	if (newTests.size() > 0) {
	    m_tests.addAll(newTests);

	    final Iterator newTestIterator = newTests.iterator();

	    while (newTestIterator.hasNext()) {
		m_accumulators.put((Test)newTestIterator.next(),
				   new SampleAccumulator());
	    }	

	    fireModelReset(Collections.unmodifiableSet(newTests));

	    m_indicesValid = false;

	    m_testArray = (Test[])m_tests.toArray(new Test[0]);
	    m_accumulatorArray = new SampleAccumulator[m_testArray.length];

	    for (int i=0; i<m_accumulatorArray.length; i++) {
		m_accumulatorArray[i] =
		    (SampleAccumulator)m_accumulators.get(m_testArray[i]);
	    }

	    m_indicesValid = true;

	    fireModelUpdate();
	}
    }

    public void registerStatisticsViews(
	StatisticsView intervalStatisticsView,
	StatisticsView cumulativeStatisticsView)
    {
	m_intervalStatisticsView.add(intervalStatisticsView);
	m_cumulativeStatisticsView.add(cumulativeStatisticsView);

	fireModelNewViews(intervalStatisticsView, cumulativeStatisticsView);
    }

    /**
     * See note on sychronisation in {@link Model} class
     * description. 
     * @throws IllegalStateException if called when model structure is changing.
     **/
    public Test getTest(int testIndex)
    {
	assertIndiciesValid();
	return m_testArray[testIndex];
    }

    /**
     * See note on sychronisation in {@link Model} class
     * description. 
     * @throws IllegalStateException if called when model structure is changing.
     **/
    public int getNumberOfTests()
    {
	assertIndiciesValid();
	return m_testArray.length;
    }

    /**
     * See note on sychronisation in {@link Model} class
     * description. 
     * @throws IllegalStateException if called when model structure is changing.
     **/
    public TestStatistics getCumulativeStatistics(int testIndex)
    {
	assertIndiciesValid();
	return m_accumulatorArray[testIndex].getCumulativeStatistics();
    }

    public TestStatistics getTotalCumulativeStatistics()
    {
	return m_totalSampleAccumulator.getCumulativeStatistics();
    }

    /**
     * See note on sychronisation in {@link Model} class
     * description. 
     * @throws IllegalStateException if called when model structure is changing.
     **/
    public TestStatistics getLastSampleStatistics(int testIndex)
    {
	assertIndiciesValid();
	return m_accumulatorArray[testIndex].getLastSampleStatistics();
    }

    public final StatisticsView getCumulativeStatisticsView()
    {
	return m_cumulativeStatisticsView;
    }

    public final StatisticsView getIntervalStatisticsView()
    {
	return m_intervalStatisticsView;
    }

    public synchronized void addModelListener(ModelListener listener)
    {
	m_modelListeners.add(listener);
    }

    public void addSampleListener(Test test, SampleListener listener)
    {
	((SampleAccumulator)m_accumulators.get(test))
	    .addSampleListener(listener);
    }

    public void addTotalSampleListener(SampleListener listener)
    {
	m_totalSampleAccumulator.addSampleListener(listener);
    }

    private void assertIndiciesValid()
    {
	if (!m_indicesValid) {
	    throw new IllegalStateException("Invalid model state");
	}
    }

    private synchronized void fireModelReset(Set newTests)
    {
	final Iterator iterator = m_modelListeners.iterator();

	while (iterator.hasNext()) {
	    final ModelListener listener = (ModelListener)iterator.next();
	    listener.reset(newTests);
	}
    }

    private synchronized void fireModelUpdate()
    {
	final Iterator iterator = m_modelListeners.iterator();

	while (iterator.hasNext()) {
	    final ModelListener listener = (ModelListener)iterator.next();
	    listener.update();
	}
    }

    private synchronized void fireModelNewViews(
	StatisticsView intervalStatisticsView,
	StatisticsView cumulativeStatisticsView)
    {
	final Iterator iterator = m_modelListeners.iterator();

	while (iterator.hasNext()) {
	    final ModelListener listener = (ModelListener)iterator.next();
	    listener.newStatisticsViews(intervalStatisticsView,
					cumulativeStatisticsView);
	}
    }

    public void start()
    {
	setState(STATE_WAITING_FOR_TRIGGER);
	fireModelUpdate();
    }

    public void stop()
    {
	setState(STATE_STOPPED);
	fireModelUpdate();
    }

    public void addTestReport(TestStatisticsMap testStatisticsMap)
	throws ConsoleException
    {
	m_receivedReport = true;

	if (getState() == STATE_CAPTURING) {
	    final TestStatisticsMap.Iterator iterator =
		testStatisticsMap.new Iterator();

	    while (iterator.hasNext()) {
		final TestStatisticsMap.Pair pair = iterator.next();

		final TestStatistics statistics = pair.getStatistics();

		final SampleAccumulator sampleAccumulator =
		    (SampleAccumulator)m_accumulators.get(pair.getTest());

		if (sampleAccumulator == null) {
		    System.err.println("Ignoring unknown test: " +
				       pair.getTest());
		}
		else {
		    sampleAccumulator.add(statistics);
		    m_totalSampleAccumulator.add(statistics);
		}
	    }
	}
    }

    private class SampleAccumulator
    {
	private final List m_listeners = new LinkedList();

	private TestStatistics m_intervalStatistics =
	    m_testStatisticsFactory.create();
	private TestStatistics m_lastSampleStatistics =
	    m_testStatisticsFactory.create();
	private TestStatistics m_cumulativeStatistics =
	    m_testStatisticsFactory.create();

	{
	    reset();
	}

	private synchronized void addSampleListener(SampleListener listener)
	{
	    m_listeners.add(listener);
	}

	private void add(TestStatistics report)
	{
	    m_intervalStatistics.add(report);
	    m_cumulativeStatistics.add(report);
	}

	private synchronized void fireSample()
	{
	    m_intervalStatistics.setValue(m_periodIndex, m_sampleInterval);

	    m_cumulativeStatistics.setValue(m_periodIndex, 
					    (getState() == STATE_STOPPED ?
					     m_stopTime : m_currentTime) - 
					    m_startTime);

	    m_peakTPSExpression.update(m_intervalStatistics,
				       m_cumulativeStatistics);

	    final Iterator iterator = m_listeners.iterator();

	    while (iterator.hasNext()) {
		final SampleListener listener =
		    (SampleListener)iterator.next();
		listener.update(m_intervalStatistics, m_cumulativeStatistics);
	    }

	    m_lastSampleStatistics = m_intervalStatistics;

	    // We create new statistics each time to ensure that
	    // m_lastSampleStatistics is always valid and fixed.
	    m_intervalStatistics = m_testStatisticsFactory.create();
	}

	private void reset()
	{
	    m_intervalStatistics.reset();
	    m_lastSampleStatistics.reset();
	    m_cumulativeStatistics.reset();
	}

	private TestStatistics getLastSampleStatistics()
	{
	    return m_lastSampleStatistics;
	}

	private TestStatistics getCumulativeStatistics()
	{
	    return m_cumulativeStatistics;
	}
    }

    /**
     * I've thought a couple of times about replacing this with a
     * java.util.TimerTask, and giving Model a Timer thread which
     * things like ProcessStatusSet could share. Its not as nice as it
     * first seems though because you have to deal with cancelling and
     * rescheduling the TimerTask when the sample period is changed.
     **/
    private class Sampler implements Runnable
    {
	public void run()
	{
	    while (true) {
		m_currentTime = System.currentTimeMillis();
		
		final long wakeUpTime = m_currentTime + m_sampleInterval;

		while (m_currentTime < wakeUpTime) {
		    try {
			Thread.sleep(wakeUpTime - m_currentTime);
			m_currentTime = wakeUpTime;
		    }
		    catch(InterruptedException e) {
			m_currentTime = System.currentTimeMillis();
		    }
		}

		for (int i=0; i<m_accumulatorArray.length; i++) {
		    m_accumulatorArray[i].fireSample();
		}

		m_totalSampleAccumulator.fireSample();

		final int state = getState();

		if (m_receivedReport) {
		    ++m_sampleCount;
		    m_receivedReportInLastInterval = true;
		}
		else {
		    m_receivedReportInLastInterval = false;
		}
		
		if (state == STATE_CAPTURING) {
		    if (m_receivedReport) {
			final int collectSampleCount =
			    m_properties.getCollectSampleCount();

			if (collectSampleCount != 0 &&
			    m_sampleCount >= collectSampleCount) {
			    setState(STATE_STOPPED);
			}
		    }
		}
		else if (state == STATE_WAITING_FOR_TRIGGER) {
		    if (m_sampleCount >= m_properties.getIgnoreSampleCount()) {
			setState(STATE_CAPTURING);
		    }
		}

		fireModelUpdate();

		m_receivedReport = false;
	    }
	}
    }

    /**
     * Return our {@link ConsoleProperties} by reference.
     **/
    public ConsoleProperties getProperties()
    {
	return m_properties;
    }

    public NumberFormat getNumberFormat()
    {
	return m_numberFormat;
    }

    public long getSampleCount()
    {
	return m_sampleCount;
    }

    /** Whether or not a report was received in the last interval. */
    public boolean getReceivedReport()
    {
	return m_receivedReportInLastInterval;
    }

    public int getState()
    {
	return m_state;
    }

    private void reset()
    {
	for (int i=0; i<m_accumulatorArray.length; i++) {
	    m_accumulatorArray[i].reset();
	}

	m_totalSampleAccumulator.reset();

	m_startTime = m_currentTime;

	fireModelUpdate();
    }

    private void setState(int i)
    {
	if (i != STATE_WAITING_FOR_TRIGGER &&
	    i != STATE_STOPPED &&
	    i != STATE_CAPTURING) {
	    throw new IllegalArgumentException("Unknown state: " + i);
	}

	if (i == STATE_WAITING_FOR_TRIGGER) {
	    // Zero everything because it looks pretty.
	    reset();
	}

	if (i == STATE_CAPTURING) {
	    reset();
	}

	if (m_state != STATE_STOPPED && i == STATE_STOPPED) {
	    m_stopTime = m_currentTime;
	}

	m_sampleCount = 0;
	m_state = i;
    }
}
