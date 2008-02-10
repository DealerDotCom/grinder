// Copyright (C) 2001 - 2008 Philip Aston
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
// COPYRIGHT HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
// INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
// (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
// SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
// HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
// STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
// ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
// OF THE POSSIBILITY OF SUCH DAMAGE.

package net.grinder.console.model;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeSet;

import net.grinder.common.GrinderException;
import net.grinder.common.Test;
import net.grinder.console.common.Resources;
import net.grinder.statistics.PeakStatisticExpression;
import net.grinder.statistics.StatisticExpression;
import net.grinder.statistics.StatisticExpressionFactory;
import net.grinder.statistics.StatisticsIndexMap;
import net.grinder.statistics.StatisticsServices;
import net.grinder.statistics.StatisticsSet;
import net.grinder.statistics.TestStatisticsMap;
import net.grinder.statistics.TestStatisticsQueries;
import net.grinder.util.ListenerSupport;


/**
 * Collate test reports into samples and distribute to listeners.
 *
 * <p>
 * When notifying listeners of changes to the number of tests we send copies of
 * the new index arrays. This helps because most listeners are Swing dispatched
 * and so can't guarantee the model is in a reasonable state when they call
 * back.
 * </p>
 *
 * @author Philip Aston
 * @version $Revision$
 */
public final class SampleModelImplementation implements SampleModel {

  private final ConsoleProperties m_properties;
  private final StatisticsServices m_statisticsServices;
  private final Timer m_timer;

  private final String m_stateIgnoringString;
  private final String m_stateWaitingString;
  private final String m_stateStoppedString;
  private final String m_stateCapturingString;

  /**
   * The current test set. A TreeSet is used to maintain the test
   * order. Guarded by itself.
   */
  private final Set m_tests = new TreeSet();

  private final ListenerSupport m_listeners = new ListenerSupport();

  private final StatisticsIndexMap.LongIndex m_periodIndex;
  private final StatisticExpression m_tpsExpression;
  private final PeakStatisticExpression m_peakTPSExpression;

  private final SampleAccumulator m_totalSampleAccumulator;

  /**
   * A {@link SampleAccumulator} for each test. Guarded by itself.
   */
  private final Map m_accumulators = Collections.synchronizedMap(new HashMap());

  // Guarded by this.
  private InternalState m_state;

  /**
   * Creates a new <code>SampleModelImplementation</code> instance.
   *
   * @param properties The console properties.
   * @param statisticsServices Statistics services.
   * @param timer A timer.
   * @param resources Console resources.
   *
   * @exception GrinderException if an error occurs
   */
  public SampleModelImplementation(ConsoleProperties properties,
                                   StatisticsServices statisticsServices,
                                   Timer timer,
                                   Resources resources)
    throws GrinderException {

    m_properties = properties;
    m_statisticsServices = statisticsServices;
    m_timer = timer;

    m_stateIgnoringString = resources.getString("state.ignoring.label") + ' ';
    m_stateWaitingString = resources.getString("state.waiting.label");
    m_stateStoppedString = resources.getString("state.stopped.label");
    m_stateCapturingString = resources.getString("state.capturing.label") + ' ';

    final StatisticsIndexMap indexMap =
      statisticsServices.getStatisticsIndexMap();

    m_periodIndex = indexMap.getLongIndex("period");

    final StatisticExpressionFactory statisticExpressionFactory =
      m_statisticsServices.getStatisticExpressionFactory();

    m_tpsExpression =
      statisticExpressionFactory.createExpression(
        "(* 1000 (/ (+ (count timedTests) untimedTests) period))");

    m_peakTPSExpression =
      statisticExpressionFactory.createPeak(
        indexMap.getDoubleIndex("peakTPS"), m_tpsExpression);

    m_totalSampleAccumulator =
      new SampleAccumulator(m_peakTPSExpression, m_periodIndex,
                            m_statisticsServices.getStatisticsSetFactory());

    setInternalState(new WaitingForTriggerState());
  }

  /**
   * Get the expression for TPS.
   *
   * @return The TPS expression for this model.
   */
  public StatisticExpression getTPSExpression() {
    return m_tpsExpression;
  }

  /**
   * Get the expression for peak TPS.
   *
   * @return The peak TPS expression for this model.
   */
  public StatisticExpression getPeakTPSExpression() {
    return m_peakTPSExpression;
  }

  /**
   * Return an object allowing access to common functions of test statistics.
   *
   * @return The {@link TestStatisticsQueries}.
   */
  public TestStatisticsQueries getTestStatisticsQueries() {
    return m_statisticsServices.getTestStatisticsQueries();
  }

  /**
   * Register new tests.
   *
   * @param tests The new tests.
   */
  public void registerTests(Collection tests) {
    // Need to copy collection, might be immutable.
    final Set newTests = new HashSet(tests);

    final Test[] testArray;

    synchronized (m_tests) {
      newTests.removeAll(m_tests);

      if (newTests.size() == 0) {
        // No new tests.
        return;
      }

      m_tests.addAll(newTests);

      // Create an index of m_tests sorted by test number.
      testArray = (Test[])m_tests.toArray(new Test[m_tests.size()]);
    }

    final SampleAccumulator[] accumulatorArray =
      new SampleAccumulator[testArray.length];

    final Iterator newTestIterator = newTests.iterator();

    synchronized (m_accumulators) {
      while (newTestIterator.hasNext()) {
        m_accumulators.put(newTestIterator.next(),
                           new SampleAccumulator(
                             m_peakTPSExpression,
                             m_periodIndex,
                             m_statisticsServices.getStatisticsSetFactory()));
      }

      for (int i = 0; i < accumulatorArray.length; i++) {
        accumulatorArray[i] =
          (SampleAccumulator)m_accumulators.get(testArray[i]);
      }
    }

    final ModelTestIndex modelTestIndex =
      new ModelTestIndex(testArray, accumulatorArray);

    m_listeners.apply(
      new ListenerSupport.Informer() {
        public void inform(Object listener) {
          ((ModelListener)listener).newTests(newTests, modelTestIndex);
        }
      });
  }

  /**
   * Get the cumulative statistics for this model.
   *
   * @return The cumulative statistics.
   */
  public StatisticsSet getTotalCumulativeStatistics() {
    return m_totalSampleAccumulator.getCumulativeStatistics();
  }

  /**
   * Add a new model listener.
   *
   * @param listener The listener.
   */
  public void addModelListener(ModelListener listener) {
    m_listeners.add(listener);
  }

  /**
   * Add a new sample listener for the specific test.
   *
   * @param test The test to add the sample listener for.
   * @param listener The sample listener.
   */
  public void addSampleListener(Test test, SampleListener listener) {
    final SampleAccumulator sampleAccumulator =
      (SampleAccumulator)m_accumulators.get(test);

    if (sampleAccumulator != null) {
      sampleAccumulator.addSampleListener(listener);
    }
  }

  /**
   * Add a new total sample listener.
   *
   * @param listener The sample listener.
   */
  public void addTotalSampleListener(SampleListener listener) {
    m_totalSampleAccumulator.addSampleListener(listener);
  }

  /**
   * Reset the model.
   *
   * <p>This doesn't affect our internal state, just the statistics and
   * the listeners.</p>
   */
  public void reset() {

    synchronized (m_tests) {
      m_tests.clear();
    }

    m_accumulators.clear();
    m_totalSampleAccumulator.zero();

    m_listeners.apply(
      new ListenerSupport.Informer() {
        public void inform(Object listener) {
          ((ModelListener)listener).resetTests();
        }
      });
  }

  /**
   * Start the model.
   */
  public void start() {
    getInternalState().start();
  }

  /**
   * Stop the model.
   */
  public void stop() {
    getInternalState().stop();
  }

  /**
   * Add a new test report.
   *
   * @param testStatisticsMap The new test statistics.
   */
  public void addTestReport(TestStatisticsMap testStatisticsMap) {
    getInternalState().newTestReport(testStatisticsMap);
  }

  /**
   * Get the current model state.
   *
   * @return The model state.
   */
  public State getState() {
    return getInternalState().toExternalState();
  }

  private void zero() {
    synchronized (m_accumulators) {
      final Iterator iterator = m_accumulators.values().iterator();

      while (iterator.hasNext()) {
        final SampleAccumulator sampleAccumulator =
          (SampleAccumulator)iterator.next();
        sampleAccumulator.zero();
      }
    }

    m_totalSampleAccumulator.zero();
  }

  private InternalState getInternalState() {
    synchronized (this) {
      return m_state;
    }
  }

  private void setInternalState(InternalState newState) {
    synchronized (this) {
      m_state = newState;
    }

    m_listeners.apply(
      new ListenerSupport.Informer() {
        public void inform(Object listener) {
          ((ModelListener)listener).stateChanged();
        }
      });
  }

  private interface InternalState {
    State toExternalState();

    void start();

    void stop();

    void newTestReport(TestStatisticsMap testStatisticsMap);
  }

  private abstract class AbstractInternalState
    implements InternalState, State {

    protected final boolean isActiveState() {
      return getInternalState() == this;
    }

    public State toExternalState() {
      // We don't bother cloning the state, only the description varies.
      return this;
    }

    public void start() {
      // Valid transition for all states.
      setInternalState(new WaitingForTriggerState());
    }

    public void stop() {
      // Valid transition for all states.
      setInternalState(new StoppedState());
    }

    public boolean isCapturing() {
      return false;
    }

    public boolean isStopped() {
      return false;
    }
  }

  private final class WaitingForTriggerState extends AbstractInternalState {
    public WaitingForTriggerState() {
      zero();
    }

    public void newTestReport(TestStatisticsMap testStatisticsMap) {
      if (m_properties.getIgnoreSampleCount() == 0) {
        setInternalState(new CapturingState());
      }
      else {
        setInternalState(new TriggeredState());
      }

      // Ensure the the first sample is recorded.
      getInternalState().newTestReport(testStatisticsMap);
    }

    public String getDescription() {
      return m_stateWaitingString;
    }
  }

  private final class StoppedState extends AbstractInternalState {
    public void newTestReport(TestStatisticsMap testStatisticsMap) {
    }

    public String getDescription() {
      return m_stateStoppedString;
    }

    public boolean isStopped() {
      return true;
    }
  }

  private abstract class SamplingState extends AbstractInternalState {
    // Guarded by this.
    private long m_lastTime = 0;

    private volatile long m_sampleCount = 1;

    public void newTestReport(TestStatisticsMap testStatisticsMap) {
      testStatisticsMap.new ForEach() {
        public void next(Test test, StatisticsSet statistics) {
          final SampleAccumulator sampleAccumulator =
            (SampleAccumulator)m_accumulators.get(test);

          if (sampleAccumulator == null) {
            System.err.println("Ignoring unknown test: " + test);
          }
          else {
            sampleAccumulator.addIntervalStatistics(statistics);

            if (shouldAccumulateSamples()) {
              sampleAccumulator.addCumulativeStaticstics(statistics);
            }

            if (!statistics.isComposite()) {
              m_totalSampleAccumulator.addIntervalStatistics(statistics);

              if (shouldAccumulateSamples()) {
                m_totalSampleAccumulator.addCumulativeStaticstics(statistics);
              }
            }
          }
        }
      }
      .iterate();
    }

    protected void schedule() {
      synchronized (this) {
        if (m_lastTime == 0) {
          m_lastTime = System.currentTimeMillis();
        }
      }

      m_timer.schedule(
        new TimerTask() {
          public void run() { sample(); }
        },
        m_properties.getSampleInterval());
    }

    public final void sample() {
      if (!isActiveState()) {
        return;
      }

      try {
        final long period;

        synchronized (this) {
          period = System.currentTimeMillis() - m_lastTime;
        }

        final long sampleInterval = m_properties.getSampleInterval();

        synchronized (m_accumulators) {
          final Iterator iterator = m_accumulators.values().iterator();

          while (iterator.hasNext()) {
            final SampleAccumulator sampleAccumulator =
              (SampleAccumulator)iterator.next();
            sampleAccumulator.fireSample(sampleInterval, period);
          }
        }

        m_totalSampleAccumulator.fireSample(sampleInterval, period);

        ++m_sampleCount;

        // I'm ignoring a minor race here: the model could have been stopped
        // after the task was started.
        // We call setInternalState() even if the InternalState hasn't
        // changed since we've altered the sample count.
        setInternalState(nextState());

        m_listeners.apply(
          new ListenerSupport.Informer() {
            public void inform(Object listener) {
              ((ModelListener)listener).newSample();
            }
          });
      }
      finally {
        synchronized (this) {
          if (isActiveState()) {
            schedule();
          }
        }
      }
    }

    public final long getSampleCount() {
      return m_sampleCount;
    }

    protected abstract boolean shouldAccumulateSamples();

    protected abstract InternalState nextState();
  }

  private final class TriggeredState extends SamplingState {
    public TriggeredState() {
      schedule();
    }

    protected boolean shouldAccumulateSamples() {
      return false;
    }

    protected InternalState nextState() {
      if (getSampleCount() > m_properties.getIgnoreSampleCount()) {
        return new CapturingState();
      }

      return this;
    }

    public String getDescription() {
      return m_stateIgnoringString + getSampleCount();
    }
  }

  private final class CapturingState extends SamplingState {
    public CapturingState() {
      zero();
      schedule();
    }

    protected boolean shouldAccumulateSamples() {
      return true;
    }

    protected InternalState nextState() {
      final int collectSampleCount = m_properties.getCollectSampleCount();

      if (collectSampleCount != 0 && getSampleCount() > collectSampleCount) {
        return new StoppedState();
      }

      return this;
    }

    public String getDescription() {
      return m_stateCapturingString + getSampleCount();
    }

    public boolean isCapturing() {
      return true;
    }
  }
}
