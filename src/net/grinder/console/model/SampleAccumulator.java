// Copyright (C) 2003 Philip Aston
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

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import net.grinder.statistics.PeakStatisticExpression;
import net.grinder.statistics.StatisticsIndexMap;
import net.grinder.statistics.TestStatistics;
import net.grinder.statistics.TestStatisticsFactory;


/**
 * Manages the cumulative statistics for  a single test or set of tests.
 *
 * @author Philip Aston
 * @version $Revision$
 */
final class SampleAccumulator {

  private final List m_listeners = new LinkedList();

  private final PeakStatisticExpression m_peakTPSExpression;
  private final StatisticsIndexMap.LongIndex m_periodIndex;

  private final TestStatistics m_cumulativeStatistics;
  private TestStatistics m_intervalStatistics;
  private TestStatistics m_lastSampleStatistics;

  public SampleAccumulator(PeakStatisticExpression peakTPSExpression,
			   StatisticsIndexMap.LongIndex periodIndex) {

    m_peakTPSExpression = peakTPSExpression;
    m_periodIndex = periodIndex;

    final TestStatisticsFactory testStatisticsFactory =
      TestStatisticsFactory.getInstance();

    m_cumulativeStatistics = testStatisticsFactory.create();
    m_intervalStatistics = testStatisticsFactory.create();
    m_lastSampleStatistics = testStatisticsFactory.create();
  }

  public final synchronized void addSampleListener(SampleListener listener) {
    m_listeners.add(listener);
  }

  public final void add(TestStatistics report) {
    m_intervalStatistics.add(report);
    m_cumulativeStatistics.add(report);
  }

  public final synchronized void fireSample(long sampleInterval, long period) {

    m_intervalStatistics.setValue(m_periodIndex, sampleInterval);
    m_cumulativeStatistics.setValue(m_periodIndex, period);

    m_peakTPSExpression.update(m_intervalStatistics, m_cumulativeStatistics);

    final Iterator iterator = m_listeners.iterator();

    while (iterator.hasNext()) {
      final SampleListener listener = (SampleListener)iterator.next();
      listener.update(m_intervalStatistics, m_cumulativeStatistics);
    }

    m_lastSampleStatistics = m_intervalStatistics;

    // We create new statistics each time to ensure that
    // m_lastSampleStatistics is always valid and fixed.
    m_intervalStatistics = TestStatisticsFactory.getInstance().create();
  }

  public final void zero() {
    m_intervalStatistics.reset();
    m_lastSampleStatistics.reset();
    m_cumulativeStatistics.reset();
  }

  public final TestStatistics getLastSampleStatistics() {
    return m_lastSampleStatistics;
  }

  public final TestStatistics getCumulativeStatistics() {
    return m_cumulativeStatistics;
  }
}
