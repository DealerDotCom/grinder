// Copyright (C) 2006 Philip Aston
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

package net.grinder.engine.process;

import net.grinder.statistics.ImmutableStatisticsSet;
import net.grinder.statistics.StatisticsIndexMap;
import net.grinder.statistics.StatisticsSet;
import net.grinder.statistics.TestStatisticsMap;
import net.grinder.statistics.TestStatisticsMap.Iterator;
import net.grinder.statistics.TestStatisticsMap.Pair;


/**
 * TestStatisticsHelper implementation.
 *
 * @author Philip Aston
 * @version $Revision$
 */
final class TestStatisticsHelperImplementation
  implements TestStatisticsHelper {

  private final StatisticsIndexMap.LongIndex m_errorsIndex;
  private final StatisticsIndexMap.LongIndex m_untimedTestsIndex;
  private final StatisticsIndexMap.LongSampleIndex m_timedTestsIndex;

  public TestStatisticsHelperImplementation(StatisticsIndexMap indexMap) {

    m_errorsIndex = indexMap.getLongIndex("errors");
    m_untimedTestsIndex = indexMap.getLongIndex("untimedTests");
    m_timedTestsIndex = indexMap.getLongSampleIndex("timedTests");
  }

  public boolean getSuccess(ImmutableStatisticsSet statistics) {
    return statistics.getValue(m_errorsIndex) == 0;
  }

  public void setSuccess(StatisticsSet statistics, boolean success) {
    statistics.setValue(m_errorsIndex, success ? 0 : 1);
  }

  public void recordTest(StatisticsSet statistics, long elapsedTime) {
    if (getSuccess(statistics)) {
      statistics.reset(m_timedTestsIndex);
      statistics.addSample(m_timedTestsIndex, elapsedTime);
      setSuccess(statistics, true);
    }
    else {
      // The plug-in might have set timing information etc., or set errors to
      // be greater than 1. For consistency, we override to a single error per
      // Test with no associated timing information.
      statistics.reset(m_timedTestsIndex);
      setSuccess(statistics, false);
    }

    // Should only be set for statistics sent to the console.
    statistics.setValue(m_untimedTestsIndex, 0);
  }

  public void removeTestTimeFromSample(TestStatisticsMap sample) {
    final Iterator iterator = sample.new Iterator();

    while (iterator.hasNext()) {
      final Pair next = iterator.next();
      final StatisticsSet statistics = next.getStatistics();

      statistics.addValue(m_untimedTestsIndex,
                          statistics.getCount(m_timedTestsIndex));
      statistics.reset(m_timedTestsIndex);
    }
  }
}
