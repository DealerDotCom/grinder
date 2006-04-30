// Copyright (C) 2003, 2004, 2005, 2006 Philip Aston
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

import net.grinder.common.GrinderException;
import net.grinder.communication.QueuedSender;
import net.grinder.console.messages.RegisterStatisticsViewMessage;
import net.grinder.script.InvalidContextException;
import net.grinder.script.NoSuchStatisticException;
import net.grinder.script.Statistics;
import net.grinder.statistics.StatisticsServices;
import net.grinder.statistics.StatisticsView;
import net.grinder.statistics.StatisticsIndexMap.DoubleIndex;
import net.grinder.statistics.StatisticsIndexMap.LongIndex;


/**
 * Implement the script statistics interface.
 *
 * <p>Package scope.
 *
 * @author Philip Aston
 * @version $Revision$
 */
final class ScriptStatisticsImplementation implements Statistics {

  private final ThreadContextLocator m_threadContextLocator;
  private final TestStatisticsHelper m_testStatisticsHelper;
  private final StatisticsServices m_statisticsServices;
  private final QueuedSender m_consoleSender;

  public ScriptStatisticsImplementation(
    ThreadContextLocator threadContextLocator,
    TestStatisticsHelper testStatisticsHelper,
    StatisticsServices statisticsServices,
    QueuedSender consoleSender) {

    m_threadContextLocator = threadContextLocator;
    m_testStatisticsHelper = testStatisticsHelper;
    m_statisticsServices = statisticsServices;
    m_consoleSender = consoleSender;
  }

  public void setDelayReports(boolean b) throws InvalidContextException {
    getThreadContext().setDelayReports(b);
  }

  public void report() throws InvalidContextException {
    getThreadContext().flushPendingDispatchContext();
  }

  private ThreadContext getThreadContext()
    throws InvalidContextException {
    final ThreadContext threadContext = m_threadContextLocator.get();

    if (threadContext == null) {
      throw new InvalidContextException(
        "Statistics interface is only supported for worker threads.");
    }

    return threadContext;
  }

  private DispatchContext getDispatchContext() throws InvalidContextException {
    final DispatchContext dispatchContext =
      getThreadContext().getDispatchContext();

    if (dispatchContext == null) {
      throw new InvalidContextException(
        "Found no statistics for the last test performed by this thread. " +
        "Perhaps you should have called setDelayReports(true)?");
    }

    return dispatchContext;
  }

  public boolean availableForUpdate() {
    final ThreadContext threadContext = m_threadContextLocator.get();

    return
      threadContext != null &&
      threadContext.getDispatchContext() != null;
  }

  public void setLong(String statisticName, long value)
    throws InvalidContextException, NoSuchStatisticException {

    getDispatchContext().getStatistics().setValue(
      getLongIndex(statisticName), value);
  }

  public void setDouble(String statisticName, double value)
    throws InvalidContextException, NoSuchStatisticException {

    getDispatchContext().getStatistics().setValue(
      getDoubleIndex(statisticName), value);
  }

  public void addLong(String statisticName, long value)
    throws InvalidContextException, NoSuchStatisticException {

    getDispatchContext().getStatistics().addValue(
      getLongIndex(statisticName), value);
  }

  public void addDouble(String statisticName, double value)
  throws InvalidContextException, NoSuchStatisticException {

    getDispatchContext().getStatistics().addValue(
      getDoubleIndex(statisticName), value);
  }

  public long getLong(String statisticName)
    throws InvalidContextException, NoSuchStatisticException {

    return getDispatchContext().getStatistics().getValue(
      getLongIndex(statisticName));
  }

  public double getDouble(String statisticName)
    throws InvalidContextException, NoSuchStatisticException {

    return getDispatchContext().getStatistics().getValue(
      getDoubleIndex(statisticName));
  }

  public void setSuccess(boolean success) throws InvalidContextException {
    m_testStatisticsHelper.setSuccess(
      getDispatchContext().getStatistics(), success);
  }

  public boolean getSuccess() throws InvalidContextException {
    return m_testStatisticsHelper.getSuccess(
      getDispatchContext().getStatistics());
  }

  public long getTime() throws InvalidContextException {
    return getDispatchContext().getElapsedTime();
  }

  private LongIndex getLongIndex(String statisticName)
    throws NoSuchStatisticException {

    final LongIndex index =
      m_statisticsServices.getStatisticsIndexMap().getLongIndex(statisticName);

    if (index == null) {
      throw new NoSuchStatisticException(
        "'" + statisticName + "' is not a basic long statistic.");
    }

    return index;
  }

  private DoubleIndex getDoubleIndex(String statisticName)
    throws NoSuchStatisticException {

    final DoubleIndex index =
      m_statisticsServices.getStatisticsIndexMap().getDoubleIndex(
        statisticName);

    if (index == null) {
      throw new NoSuchStatisticException(
        "'" + statisticName + "' is not a basic double statistic.");
    }

    return index;
  }

  public void registerSummaryStatisticsView(StatisticsView statisticsView)
  throws GrinderException {
    m_statisticsServices.getSummaryStatisticsView().add(statisticsView);

    // Queue up, will get flushed with next process status or
    // statistics report.
    m_consoleSender.queue(new RegisterStatisticsViewMessage(statisticsView));
  }

  public void registerDetailStatisticsView(StatisticsView statisticsView)
    throws GrinderException {

    if (m_threadContextLocator.get() != null) {
      throw new InvalidContextException(
        "registerDetailStatisticsView() is not supported from worker threads");
    }

    // DetailStatisticsViews are only for the data logs, so we don't
    // register the view with the console.
    m_statisticsServices.getDetailStatisticsView().add(statisticsView);
  }
}
