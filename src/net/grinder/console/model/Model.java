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

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import net.grinder.console.ConsoleException;
import net.grinder.plugininterface.GrinderPlugin;
import net.grinder.plugininterface.Test;
import net.grinder.statistics.Statistics;
import net.grinder.statistics.TestStatisticsMap;
import net.grinder.util.GrinderException;
import net.grinder.util.GrinderProperties;
import net.grinder.util.ProcessContextImplementation;
import net.grinder.util.PropertiesHelper;


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

    private final Map m_tests = new TreeMap();
    private final HashMap m_samples = new HashMap();
    private final Sample m_totalSample = new Sample();
    private TestStatisticsMap m_summaryStatistics = new TestStatisticsMap();
    private final Thread m_sampleThread;
    private int m_sampleInterval = 1000;
    private int m_ignoreSampleCount = 1;
    private int m_collectSampleCount = 0;
    private boolean m_stopSampler = false;
    private int m_state = 0;
    private long m_sampleCount = 0;
    private boolean m_receivedSample = false;
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
		new ProcessContextImplementation("", ""));

	// Shove the tests into a TreeMap so that they're ordered.
	final Iterator testSetIterator =
	    propertiesHelper.getTestSet(grinderPlugin).iterator();

	while (testSetIterator.hasNext())
	{
	    final Test test = (Test)testSetIterator.next();
	    final Integer testNumber = test.getTestNumber();
	    m_tests.put(test.getTestNumber(), test);
	    m_samples.put(test.getTestNumber(), new Sample());
	}

	setInitialState();

	m_sampleThread = new Thread(new Sampler());
	m_sampleThread.start();
    }

    public Collection getTests() 
    {
	return m_tests.values();
    }

    public TestStatisticsMap getSummaryStatistics()
    {
	return m_summaryStatistics;
    }

    private Sample getSample(Integer testNumber)
	throws ConsoleException
    {
	final Sample sample =(Sample)m_samples.get(testNumber);

	if (sample == null) {
	    throw new ConsoleException("Unknown test '" + testNumber + "'");
	}

	return sample;
    }

    public synchronized void addModelListener(ModelListener listener)
	throws ConsoleException
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

    public void addSampleListener(Integer testNumber, SampleListener listener)
	throws ConsoleException
    {
	getSample(testNumber).addSampleListener(listener);
    }

    public void addTotalSampleListener(SampleListener listener)
    {
	m_totalSample.addSampleListener(listener);
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
	m_receivedSample = true;

	if (getState() == STATE_CAPTURING) {
	    final TestStatisticsMap.Iterator iterator =
		testStatisticsMap.new Iterator();

	    while (iterator.hasNext()) {
		final TestStatisticsMap.Pair pair = iterator.next();

		final Integer testNumber = pair.getTest().getTestNumber();
		final Statistics statistics = pair.getStatistics();

		getSample(testNumber).add(statistics);

		m_totalSample.add(statistics);
	    }

	    m_summaryStatistics.add(testStatisticsMap);
	}
    }

    private class Sample
    {
	private final List m_listeners = new LinkedList();
	private Statistics m_total;
	private long m_transactionsInInterval;
	private double m_peakTPS;
	
	{
	    reset();
	}

	private synchronized void addSampleListener(SampleListener listener)
	{
	    m_listeners.add(listener);
	}

	private void add(Statistics statistics)
	{
	    m_transactionsInInterval += statistics.getTransactions();
	    m_total.add(statistics);
	}

	private synchronized void fireSample()
	{
	    final double tps =
		1000d*m_transactionsInInterval/(double)m_sampleInterval;

	    final long totalTime =
		(getState() == STATE_STOPPED ? m_stopTime : m_currentTime) -
		m_startTime;

	    final double averageTPS =
		1000d*m_total.getTransactions()/(double)totalTime;

	    if (tps > m_peakTPS) {
		m_peakTPS = tps;
	    }

	    final Iterator iterator = m_listeners.iterator();

	    while (iterator.hasNext()) {
		final SampleListener listener =
		    (SampleListener)iterator.next();
		listener.update(tps, averageTPS, m_peakTPS, m_total);
	    }

	    m_transactionsInInterval = 0;
	}

	private void reset()
	{
	    m_transactionsInInterval = 0;
	    m_peakTPS = 0;
	    m_total = new Statistics();
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

		final Iterator iterator = m_samples.values().iterator();

		while (iterator.hasNext()) {
		    ((Sample)iterator.next()).fireSample();
		}

		m_totalSample.fireSample();

		final int state = getState();

		if (m_receivedSample) {
		    ++m_sampleCount;
		}
		
		if (state == STATE_CAPTURING) {
		    if (m_receivedSample) {
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

		m_receivedSample = false;
	    }
	}
    }

    public int getSampleInterval()
    {
	return m_sampleInterval;
    }

    public void setSampleInterval(int i)
    {
	m_sampleInterval = i;
	fireModelUpdate();
    }

    public long getSampleCount()
    {
	return m_sampleCount;
    }

    /** Whether or not a sample was received in the last period. */
    public boolean getRecievedSample()
    {
	return m_receivedSample;
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
	final Iterator iterator = m_samples.values().iterator();

	while (iterator.hasNext()) {
	    final Sample sample = (Sample)iterator.next();
	    sample.reset();
	}

	m_totalSample.reset();
	m_summaryStatistics = new TestStatisticsMap();

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
