// Copyright (C) 2003, 2004 Philip Aston
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

package net.grinder.script;

import net.grinder.statistics.StatisticsIndexMap;


/**
 * Script statistics query and reporting API.
 *
 * <p>
 * An implementation of this interface can be obtained by calling {@link
 * Grinder.ScriptContext#getStatistics getStatistics} on the
 * {@link Grinder#grinder grinder} context object. This can be used in a script
 * to query the result of a test. For example:
 *
 * <blockquote>
 *
 * <pre>
 *                   result1 = test1.doSomething()
 *                   timeTaken1 = grinder.statistics.time
 *
 *                   if grinder.statistics.success:
 *                     # ...
 * </pre>
 *
 * </blockquote>
 *
 * <p>
 * If called within code wrapped by a {@link Test} proxy, the query methods (the
 * methods having names that start <code>get...</code>) provide information
 * about the test in progress. This information may be partially complete.
 * Otherwise, the query methods provide information about the last test
 * performed.
 * </p>
 *
 * <p>
 * The other methods allow the statistics to be updated. By default, test
 * statistics reports are automatically sent to the console and data log when
 * the test proxy call completes, so the script cannot modify the test
 * statistics after the call. By using {@link #setDelayReports}, scripts can
 * turn off this automatic reporting for the current worker thread. Having done
 * this, the script can modify or set the statistics before they are sent to the
 * log and the console. The statistics reports are sent at a later time as
 * described in {@link #setDelayReports}below. For example:
 *
 * <blockquote>
 *
 * <pre>
 *   grinder.statistics.delayReports = 1
 *
 *   result1 = test1.doSomething()
 *
 *   if isFailed(result1):
 *
 *      # Mark test as failure. The appropriate failure detection
 *      # depends on the type of test.
 *      grinder.statistics.success = 0
 * </pre>
 *
 * </blockquote>
 *
 * <p>
 * It is possible to set the statistics from within test implementation itself.
 * This is more useful for user statistics than for the standard statistics (e.g
 * <em>untimedTests</em>,<em>errors</em>) as the standard statistics may
 * be overridden by The Grinder engine when the test finishes.
 * </p>
 *
 * <p>
 * For a list of the standard statistics, see {@link StatisticsIndexMap}.
 *
 * <p>
 * There is currently no script interface for updating sample statistics.
 * Currently the only sample statistic is <em>timedTests</em>.
 * </p>
 *
 *
 * @author Philip Aston
 * @version $Revision$
 * @see net.grinder.statistics.StatisticsView
 */
public interface Statistics  {

  /**
   * Use to delay reporting of the last test statistics to the log and the
   * console so that the script can modify them. Normally test statistics are
   * reported automatically when the test implementation returns. Only affects
   * reporting for the calling worker thread.
   *
   * <p>
   * With this set to <code>true</code> the test statistics will be reported
   * at the following times:
   * <ol>
   * <li>When the next test is invoked.</li>
   * <li>When the current run completes.</li>
   * <li>When the script calls {@link #report}.</li>
   * <li>When the script calls <code>setDelayReports(false)</code>.</li>
   * </ol>
   * </p>
   *
   * @param b
   *          <code>false</code> => enable automatic reporting when tests
   *          return (the default behaviour); <code>true</code> => delay
   *          reporting.
   * @throws InvalidContextException
   *           InvalidContextException If called from a different thread to the
   *           thread in which the <code>Statistics</code> was was acquired.
   * @see #report
   */
  void setDelayReports(boolean b) throws InvalidContextException;

  /**
   * Send any pending statistics for the last completed test to the data log and
   * the console.
   *
   * <p>
   * Calling this does nothing if there are no pending statistics to report.
   * This will be the case if {@link #setDelayReports} has not been called to
   * delay reporting.
   * </p>
   *
   * @exception InvalidContextException
   *              If called from a different thread to the thread in which the
   *              <code>Statistics</code> was was acquired, or before the
   *              first test.
   * @see #availableForUpdate
   * @see #report
   */
  void report() throws InvalidContextException;

  /**
   * Return whether the statistics for the current test are available
   * for update. If this returns <code>true</code>, then other methods
   * will not throw {@link InvalidContextException}.
   * @return Whether the statistics for the current test are available
   * for update.
   */
  boolean availableForUpdate();

  /**
   * Sets the long statistic with index <code>index</code> to the
   * specified <code>value</code> for the last test.
   *
   * @param index The statistic index. See {@link #getStatisticsIndexMap()}.
   * @param value The value.
   * @exception InvalidContextException If called from a different
   * thread to the thread in which the <code>Statistics</code> was was
   * acquired.
   * @exception InvalidContextException If called before the first test.
   * @exception InvalidContextException If called when the statistics
   * have already been sent for the last test performed by this thread
   * - see {@link #setDelayReports} and {@link #availableForUpdate}.
   */
  void setValue(StatisticsIndexMap.LongIndex index, long value)
    throws InvalidContextException;

  /**
   * Sets the double statistic with index <code>index</code> to the
   * specified <code>value</code> for the last test .
   *
   * @param index The statistic index. See {@link #getStatisticsIndexMap()}.
   * @param value The value.
   * @exception InvalidContextException If called from a different
   * thread to the thread in which the <code>Statistics</code> was was
   * acquired.
   * @exception InvalidContextException If called before the first test.
   * @exception InvalidContextException If called when the statistics
   * have already been sent for the last test performed by this thread
   * - see {@link #setDelayReports} and {@link #availableForUpdate}.
   */
  void setValue(StatisticsIndexMap.DoubleIndex index, double value)
    throws InvalidContextException;

  /**
   * Add <code>value</code> to the long statistic with index
   * <code>index</code> for the last test.
   *
   * @param index The statistic index. See {@link #getStatisticsIndexMap()}.
   * @param value The value.
   * @exception InvalidContextException If called from a different
   * thread to the thread in which the <code>Statistics</code> was was
   * acquired.
   * @exception InvalidContextException If called before the first test.
   * @exception InvalidContextException If called when the statistics
   * have already been sent for the last test performed by this thread
   * - see {@link #setDelayReports} and {@link #availableForUpdate}.
   */
  void addValue(StatisticsIndexMap.LongIndex index, long value)
    throws InvalidContextException;

  /**
   * Add <code>value</code> to the double statistic with index
   * <code>index</code> for the last test.
   *
   * @param index The statistic index. See {@link #getStatisticsIndexMap()}.
   * @param value The value.
   * @exception InvalidContextException If called from a different
   * thread to the thread in which the <code>Statistics</code> was was
   * acquired.
   * @exception InvalidContextException If called before the first test.
   * @exception InvalidContextException If called when the statistics
   * have already been sent for the last test performed by this thread
   * - see {@link #setDelayReports} and {@link #availableForUpdate}.
   */
  void addValue(StatisticsIndexMap.DoubleIndex index, double value)
    throws InvalidContextException;

  /**
   * Return the long value specified by <code>index</code> for the test in
   * progress, or if the last test completed if there is no test in progress.
   *
   * @param index
   *          The statistic index. See {@link #getStatisticsIndexMap()}.
   * @return The value.
   * @throws InvalidContextException
   *           If called from a different thread to the thread in which the
   *           <code>Statistics</code> was was acquired, or before the first
   *           test.
   */
  long getValue(StatisticsIndexMap.LongIndex index)
    throws InvalidContextException;

  /**
   * Return the double value specified by <code>index</code> for the test in
   * progress, or if the last test completed if there is no test in progress.
   *
   * @param index
   *          The statistic index. See {@link #getStatisticsIndexMap()}.
   * @return The value.
   * @throws InvalidContextException
   *           If called from a different thread to the thread in which the
   *           <code>Statistics</code> was was acquired, or before the first
   *           test.
   */
  double getValue(StatisticsIndexMap.DoubleIndex index)
    throws InvalidContextException;

  /**
   * Convenience method that sets whether the last test should be considered a
   * success or not.
   *
   * <p>
   * When the statistics are reported (see {@link #report()}):
   * </p>
   *
   * <ul>
   * <li>If <em>errors</em> is <code>0</code> and the process is recording
   * time, the elapsed time of the test is added to the <em>timedTest</em>
   * statistic.</li>
   * <li>If <em>errors</em> is <code>0</code> and the process is not
   * recording time, <em>untimedTests</em> is set to <code>1</code>.</li>
   * <li>If <em>errors</em> is <code>1</code> the <em>timedTests</em> and
   * <em>untimedTests</em> statistics are not altered.</li>
   * </ul>
   *
   * @param success
   *          If <code>true</code> <em>errors</em> is set to <code>0</code>,
   *          otherwise <em>errors</em> is set to <code>1</code>.
   * @exception InvalidContextException
   *              If called from a different thread to the thread in which the
   *              <code>Statistics</code> was was acquired.
   * @exception InvalidContextException
   *              If called before the first test.
   * @exception InvalidContextException
   *              If called when the statistics have already been sent for the
   *              last test performed by this thread - see
   *              {@link #setDelayReports}and {@link #availableForUpdate}.
   */
  void setSuccess(boolean success) throws InvalidContextException;

  /**
   * Convenience method that returns whether the test in progress (or the last
   * test if there is no test in progress) was a success (<em>errors</em> is
   * zero) or not.
   *
   * @return Whether the test was a success.
   * @throws InvalidContextException
   *           If called from a different thread to the thread in which the
   *           <code>Statistics</code> was was acquired, or before the first
   *           test.
   */
  boolean getSuccess() throws InvalidContextException;

  /**
   * Convenience method that returns the elapsed time since the test in progress
   * was started, or the time taken by the last test if there is no test in
   * progress.
   *
   * @return The elapsed time for the test.
   * @throws InvalidContextException
   *           If called from a different thread to the thread in which the
   *           <code>Statistics</code> was was acquired, or before the first
   *           test.
   */
  long getTime() throws InvalidContextException;

  /**
   * The {@link StatisticsIndexMap} is a registry of statistic index objects.
   *
   * @return The registry.
   */
  StatisticsIndexMap getStatisticsIndexMap();
}
