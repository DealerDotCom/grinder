// The Grinder
// Copyright (C) 2000, 2001 Paco Gomez
// Copyright (C) 2000, 2001 Philip Aston

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

package net.grinder.console.model;

import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import net.grinder.console.ConsoleException;
import net.grinder.plugininterface.GrinderPlugin;
import net.grinder.plugininterface.Test;
import net.grinder.statistics.CumulativeStatistics;
import net.grinder.statistics.IntervalStatistics;
import net.grinder.statistics.StatisticsImplementation;
import net.grinder.statistics.TestStatisticsMap;
import net.grinder.util.GrinderException;
import net.grinder.util.GrinderProperties;
import net.grinder.util.ProcessContextImplementation;
import net.grinder.util.PropertiesHelper;
import net.grinder.util.SignificantFigureFormat;


/**
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

    private final Test[] m_tests;
    private final Map m_testToIndex;
    private final SampleAccumulator[] m_sampleAccumulators;
    private final SampleAccumulator m_totalSampleAccumulator =
	new SampleAccumulator();

    private final Thread m_sampleThread;
    private int m_sampleInterval = 1000;
    private int m_significantFigures = 3;
    private NumberFormat m_numberFormat =
	new SignificantFigureFormat(m_significantFigures);

    private int m_ignoreSampleCount = 1;
    private int m_collectSampleCount = 0;
    private boolean m_stopSampler = false;
    private int m_state = 0;
    private long m_sampleCount = 0;
    private boolean m_receivedReport = false;
    private final List m_modelListeners = new LinkedList();

    /* System.currentTimeMillis is expensive. This is acurate to one
       sample period. */
    private long m_currentTime;

    public Model(GrinderProperties properties)
	throws GrinderException
    {
	final PropertiesHelper propertiesHelper = new PropertiesHelper();

	final GrinderPlugin grinderPlugin =
	    propertiesHelper.instantiatePlugin(
		new ProcessContextImplementation());

	final Set orderedTests = new TreeSet(grinderPlugin.getTests());

	m_tests = (Test[])orderedTests.toArray(new Test[0]);
	m_testToIndex = new HashMap();
	m_sampleAccumulators = new SampleAccumulator[m_tests.length];

	for (int i=0; i<m_tests.length; i++) {
	    m_testToIndex.put(m_tests[i], new Integer(i));
	    m_sampleAccumulators[i] = new SampleAccumulator();
	}

	setInitialState();

	m_sampleThread = new Thread(new Sampler());
	m_sampleThread.start();
    }

    public Test getTest(int testIndex)
    {
	return m_tests[testIndex];
    }

    public int getNumberOfTests()
    {
	return m_tests.length;
    }

    public CumulativeStatistics getCumulativeStatistics(int testIndex)
    {
	return m_sampleAccumulators[testIndex];
    }

    public CumulativeStatistics getTotalCumulativeStatistics()
    {
	return m_totalSampleAccumulator;
    }

    public IntervalStatistics getLastSampleStatistics(int testIndex)
    {
	return m_sampleAccumulators[testIndex].getLastSampleStatistics();
    }

    public synchronized void addModelListener(ModelListener listener)
    {
	m_modelListeners.add(listener);
    }

    private synchronized void fireModelUpdate()
    {
	final Iterator iterator = m_modelListeners.iterator();

	while (iterator.hasNext()) {
	    final ModelListener listener = (ModelListener)iterator.next();
	    listener.update();
	}
    }

    public void addSampleListener(int testIndex, SampleListener listener)
	throws ConsoleException
    {
	m_sampleAccumulators[testIndex].addSampleListener(listener);
    }

    public void addTotalSampleListener(SampleListener listener)
    {
	m_totalSampleAccumulator.addSampleListener(listener);
    }

    private void setInitialState()
    {
	if (getIgnoreSampleCount() != 0) {
	    setState(STATE_WAITING_FOR_TRIGGER);
	}
	else {
	    setState(STATE_CAPTURING);
	}
    }

    public void start()
    {
	setInitialState();
	fireModelUpdate();
    }

    public void stop()
    {
	setState(STATE_STOPPED);
	fireModelUpdate();
    }

    public void add(TestStatisticsMap testStatisticsMap)
	throws ConsoleException
    {
	m_receivedReport = true;

	if (getState() == STATE_CAPTURING) {
	    final TestStatisticsMap.Iterator iterator =
		testStatisticsMap.new Iterator();

	    while (iterator.hasNext()) {
		final TestStatisticsMap.Pair pair = iterator.next();

		final StatisticsImplementation statistics =
		    pair.getStatistics();

		final Integer index =
		    (Integer)m_testToIndex.get(pair.getTest());

		if (index == null) {
		    System.err.println("Ignoring unknown test: " +
				       pair.getTest());
		}
		else {
		    m_sampleAccumulators[index.intValue()].add(statistics);
		    m_totalSampleAccumulator.add(statistics);
		}
	    }
	}
    }

    private class IntervalStatisticsImplementation
	extends StatisticsImplementation implements IntervalStatistics
    {
	public synchronized double getTPS()
	{
	    return 1000d*getTransactions()/(double)m_sampleInterval;
	}
    }

    private class SampleAccumulator implements CumulativeStatistics
    {
	private final List m_listeners = new LinkedList();
	private IntervalStatisticsImplementation m_intervalStatistics =
	    new IntervalStatisticsImplementation();
	private IntervalStatistics m_lastSampleStatistics =
	    new IntervalStatisticsImplementation();
	private StatisticsImplementation m_total;
	private double m_tps;
	private double m_peakTPS;
	
	{
	    reset();
	}

	private synchronized void addSampleListener(SampleListener listener)
	{
	    m_listeners.add(listener);
	}

	private void add(StatisticsImplementation report)
	{
	    m_intervalStatistics.add(report);
	    m_total.add(report);
	}

	private synchronized void fireSample()
	{
	    final double tps = m_intervalStatistics.getTPS();

	    if (tps > m_peakTPS) {
		m_peakTPS = tps;
	    }

	    final double totalTime =
		(getState() == STATE_STOPPED ? m_stopTime : m_currentTime) -
		m_startTime;

	    m_tps = 1000d * m_total.getTransactions()/totalTime;

	    final Iterator iterator = m_listeners.iterator();

	    while (iterator.hasNext()) {
		final SampleListener listener =
		    (SampleListener)iterator.next();
		listener.update(m_intervalStatistics, this);
	    }

	    m_lastSampleStatistics = m_intervalStatistics;
	    m_intervalStatistics = new IntervalStatisticsImplementation();
	}

	private void reset()
	{
	    m_intervalStatistics = new IntervalStatisticsImplementation();
	    m_lastSampleStatistics = new IntervalStatisticsImplementation();
	    m_tps = 0;
	    m_peakTPS = 0;
	    m_total = new StatisticsImplementation();
	}

	public double getAverageTransactionTime()
	{
	    return m_total.getAverageTransactionTime();
	}

	public long getTransactions()
	{
	    return m_total.getTransactions();
	}

	public long getErrors()
	{
	    return m_total.getErrors();
	}

	public double getTPS()
	{
	    return m_tps;
	}

	public double getPeakTPS()
	{
	    return m_peakTPS;
	}

	public IntervalStatistics getLastSampleStatistics()
	{
	    return m_lastSampleStatistics;
	}
    }

    private class Sampler implements Runnable
    {
	public void run()
	{
	    while (!m_stopSampler) {
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

		for (int i=0; i<m_tests.length; i++) {
		    m_sampleAccumulators[i].fireSample();
		}

		m_totalSampleAccumulator.fireSample();

		final int state = getState();

		if (m_receivedReport) {
		    ++m_sampleCount;
		}
		
		if (state == STATE_CAPTURING) {
		    if (m_receivedReport) {
			final int collectSampleCount = getCollectSampleCount();

			if (collectSampleCount != 0 &&
			    m_sampleCount >= collectSampleCount) {
			    setState(STATE_STOPPED);
			}
		    }
		}
		else if (state == STATE_WAITING_FOR_TRIGGER) {
		    if (m_sampleCount >= getIgnoreSampleCount()) {
			setState(STATE_CAPTURING);
		    }
		}

		fireModelUpdate();

		m_receivedReport = false;
	    }
	}
    }

    public int getSampleInterval()
    {
	return m_sampleInterval;
    }

    /** Should really wait until the next sample boundary before
     * changing. **/
    public void setSampleInterval(int i)
    {
	m_sampleInterval = i;
	fireModelUpdate();
    }

    public int getSignificantFigures()
    {
	return m_significantFigures;
    }

    public NumberFormat getNumberFormat()
    {
	return m_numberFormat;
    }

    public void setSignificantFigures(int i)
    {
	m_significantFigures = i;
	m_numberFormat = new SignificantFigureFormat(i);

	fireModelUpdate();
    }

    public long getSampleCount()
    {
	return m_sampleCount;
    }

    /** Whether or not a report was received in the last period. */
    public boolean getReceivedReport()
    {
	return m_receivedReport;
    }

    public int getIgnoreSampleCount()
    {
	return m_ignoreSampleCount;
    }

    public void setIgnoreSampleCount(int i)
    {
	m_ignoreSampleCount = i;

	if (getState() == STATE_WAITING_FOR_TRIGGER) {
	    setInitialState();
	}

	fireModelUpdate();
    }

    public int getCollectSampleCount()
    {
	return m_collectSampleCount;
    }

    public void setCollectSampleCount(int i)
    {
	m_collectSampleCount = i;
	fireModelUpdate();
    }

    public int getState()
    {
	return m_state;
    }

    private void reset()
    {
	for (int i=0; i<m_tests.length; i++) {
	    m_sampleAccumulators[i].reset();
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
	    reset();
	}

	if (i == STATE_CAPTURING) {
	    reset();
	}

	if (m_state != STATE_STOPPED && i == STATE_STOPPED) {
	    m_stopTime = m_currentTime;
	}

	m_state = i;
	m_sampleCount = 0;
    }
}
