// Copyright (C) 2001, 2002, 2003 Philip Aston
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

import net.grinder.common.FilenameFactory;
import net.grinder.common.GrinderException;
import net.grinder.common.GrinderProperties;
import net.grinder.common.Logger;
import net.grinder.script.InvalidContextException;
import net.grinder.script.Grinder;
import net.grinder.script.Statistics;
import net.grinder.statistics.CommonStatisticsViews;
import net.grinder.statistics.StatisticsView;
import net.grinder.communication.RegisterStatisticsViewMessage;



/**
 * @author Philip Aston
 * @version $Revision$
 */
final class ScriptContextImplementation implements Grinder.ScriptContext {

  private final ProcessContext m_processContext;

  public ScriptContextImplementation(ProcessContext processContext) {
    m_processContext = processContext;
  }

  public final String getGrinderID() {
    return m_processContext.getGrinderID();
  }

  public final int getThreadID() {
    final ThreadContext threadContext = ThreadContext.getThreadInstance();

    if (threadContext != null) {
      return threadContext.getThreadID();
    }

    return -1;
  }

  public final int getRunNumber() {
    final ThreadContext threadContext =
      ThreadContext.getThreadInstance();

    if (threadContext != null) {
      return threadContext.getRunNumber();
    }

    return -1;
  }

  public final Logger getLogger() {
    final ThreadContext threadContext = ThreadContext.getThreadInstance();

    if (threadContext != null) {
      return threadContext.getThreadLogger();
    }

    return m_processContext.getLogger();
  }

  public final void sleep(long meanTime)
    throws GrinderException, InvalidContextException {
    final ThreadContext threadContext = ThreadContext.getThreadInstance();

    if (threadContext == null) {
      throw new InvalidContextException(
    "sleep() is currently only supported for worker threads");
    }

    threadContext.getSleeper().sleepNormal(meanTime);
  }

  public final void sleep(long meanTime, long sigma)
    throws GrinderException, InvalidContextException {
    final ThreadContext threadContext = ThreadContext.getThreadInstance();

    if (threadContext == null) {
      throw new InvalidContextException(
    "sleep is currently only supported for worker threads");
    }

    threadContext.getSleeper().sleepNormal(meanTime, sigma);
  }

  public final FilenameFactory getFilenameFactory() {
    final ThreadContext threadContext = ThreadContext.getThreadInstance();

    if (threadContext != null) {
      return threadContext.getFilenameFactory();
    }

    return m_processContext.getLoggerImplementation().getFilenameFactory();
  }

  public final GrinderProperties getProperties() {
    return m_processContext.getProperties();
  }

  public final void registerSummaryStatisticsView(
    StatisticsView statisticsView)
    throws GrinderException {
    CommonStatisticsViews.getSummaryStatisticsView().add(statisticsView);

    // Queue up, will get flushed with next process status or
    // statistics report.
    m_processContext.getConsoleSender().queue(
      new RegisterStatisticsViewMessage(statisticsView));
  }

  public final void registerDetailStatisticsView(StatisticsView statisticsView)
    throws GrinderException {
    // DetailStatisticsViews are only for the data logs, so we don't
    // register the view with the console.
    CommonStatisticsViews.getDetailStatisticsView().add(statisticsView);
  }

  public final Statistics getStatistics() throws InvalidContextException {

    final ThreadContext threadContext = ThreadContext.getThreadInstance();

    if (threadContext == null) {
      throw new InvalidContextException(
    "getCurrentTestStatistics() is only supported for worker threads");
    }

    return threadContext.getScriptStatistics();
  }
}
