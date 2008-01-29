// Copyright (C) 2006 - 2008 Philip Aston
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

import net.grinder.common.Test;
import net.grinder.statistics.StatisticExpression;
import net.grinder.statistics.StatisticsSet;
import net.grinder.statistics.TestStatisticsMap;
import net.grinder.statistics.TestStatisticsQueries;


/**
 * Interface to {@link ModelImplementation}.
 *
 * @author Philip Aston
 * @version $Revision:$
 */
public interface Model {

  /**
   * Constant that represents the model state of <em>waiting for a
   * trigger</em>.
   * @see #getState
   */
  int STATE_WAITING_FOR_TRIGGER = 0;

  /**
   * Constant that represents the model state of <em>stopped statistics
   * capture</em>.
   * @see #getState
   */
  int STATE_STOPPED = 1;

  /**
   * Constant that represents the model state of <em>statistics capture
   * in progress</em>.
   * @see #getState
   */
  int STATE_CAPTURING = 2;


  /**
   * Reset the model.
   */
  void reset();

  /**
   * Start  the model.
   */
  void start();

  /**
   * Stop the model.
   */
  void stop();

  /**
   * Get the current model state.
   *
   * @return The model state.
   * @see #STATE_WAITING_FOR_TRIGGER
   * @see #STATE_CAPTURING
   * @see #STATE_STOPPED
   */
  int getState();

  /**
   * Whether or not a report was received in the last interval.
   * @return <code>true</code> => yes there was a report.
   */
  boolean getReportsRecentlyReceived();

  /**
   * Get the current sample count.
   *
   * @return The sample count.
   */
  long getSampleCount();

  /**
   * Get the statistics expression for TPS.
   *
   * @return The TPS expression for this model.
   */
  StatisticExpression getTPSExpression();

  /**
   * Get the expression for peak TPS.
   *
   * @return The peak TPS expression for this model.
   */
  StatisticExpression getPeakTPSExpression();

  /**
   * Return an object allowing access to common functions of test statistics.
   *
   * @return The {@link TestStatisticsQueries}.
   */
  TestStatisticsQueries getTestStatisticsQueries();

  /**
   * Get the cumulative statistics for this model.
   *
   * @return The cumulative statistics.
   */
  StatisticsSet getTotalCumulativeStatistics();

  /**
   * Add a new model listener.
   *
   * @param listener The listener.
   */
  void addModelListener(ModelListener listener);

  /**
   * Add a new total sample listener.
   *
   * @param listener The sample listener.
   */
  void addTotalSampleListener(SampleListener listener);

  /**
   * Add a new sample listener for the specific test.
   *
   * @param test The test to add the sample listener for.
   * @param listener The sample listener.
   */
  void addSampleListener(Test test, SampleListener listener);

  /**
   * Register new tests.
   *
   * @param tests The new tests.
   */
  void registerTests(Collection tests);

  /**
   * Add a new test report.
   * @param statisticsDelta The new test statistics.
   */
  void addTestReport(TestStatisticsMap statisticsDelta);
}
