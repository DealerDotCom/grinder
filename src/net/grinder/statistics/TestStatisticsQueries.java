// Copyright (C) 2000, 2001, 2002, 2003, 2004, 2005 Philip Aston
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

package net.grinder.statistics;


/**
 * Common queries against the standard statistics.
 *
 * @author Philip Aston
 * @version $Revision$
 */
public final class TestStatisticsQueries {

  private static final StatisticsIndexMap.LongIndex s_errorsIndex;
  private static final StatisticsIndexMap.LongIndex s_untimedTestsIndex;
  private static final StatisticsIndexMap.LongSampleIndex s_timedTestsIndex;

  static {
    final StatisticsIndexMap indexMap = StatisticsIndexMap.getInstance();

    s_errorsIndex = indexMap.getLongIndex("errors");
    s_untimedTestsIndex = indexMap.getLongIndex("untimedTests");
    s_timedTestsIndex = indexMap.getLongSampleIndex("timedTests");
  }

  private static final TestStatisticsQueries s_instance =
    new TestStatisticsQueries();

  /**
   * Singleton accessor.
   *
   * @return The singleton.
   */
  public static TestStatisticsQueries getInstance() {
    return s_instance;
  }

  /**
   * Constructor.
   */
  private TestStatisticsQueries() {
  }

  /**
   * Return the number of tests. This is equal to the sum of the
   * <em>timedTests</em> <em>count</em> value and the
   * <em>untimedTests</em> value.
   *
   * @param statistics The statistics to query.
   * @return a <code>long</code> value
   */
  public long getNumberOfTests(StatisticsSet statistics) {
    return
      statistics.getCount(s_timedTestsIndex) +
      statistics.getValue(s_untimedTestsIndex);
  }

  /**
   * Return the value of the <em>errors</em> statistic.
   *
   * @param statistics The statistics to query.
   * @return a <code>long</code> value
   */
  public long getNumberOfErrors(StatisticsSet statistics) {
    return statistics.getValue(s_errorsIndex);
  }

  /**
   * Return the value obtained by dividing the <em>timedTests</em> sample
   * statistics <em>total</em> attribute by its <em>count</em> attribute.
   *
   * @param statistics The statistics to query.
   * @return a <code>double</code> value
   */
  public double getAverageTestTime(StatisticsSet statistics) {
    final long count = statistics.getCount(s_timedTestsIndex);

    return
      count == 0 ?
      Double.NaN : statistics.getSum(s_timedTestsIndex) / (double)count;
  }
}
