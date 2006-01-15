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

import net.grinder.script.InvalidContextException;
import net.grinder.script.Statistics;
import net.grinder.statistics.StatisticsIndexMap;


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

  public ScriptStatisticsImplementation(
    ThreadContextLocator threadContextLocator,
    TestStatisticsHelper testStatisticsHelper) {

    m_threadContextLocator = threadContextLocator;
    m_testStatisticsHelper = testStatisticsHelper;
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

    if (threadContext.getScriptStatistics() != this) {
      throw new InvalidContextException(
        "Statistics objects must be used only by the worker thread from " +
        "which they are acquired.");
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
      threadContext.getScriptStatistics() == this &&
      threadContext.getDispatchContext() != null;
  }

  public void setValue(StatisticsIndexMap.LongIndex index, long value)
    throws InvalidContextException {

    getDispatchContext().getStatistics().setValue(index, value);
  }

  public void setValue(StatisticsIndexMap.DoubleIndex index, double value)
    throws InvalidContextException {

    getDispatchContext().getStatistics().setValue(index, value);
  }

  public void addValue(StatisticsIndexMap.LongIndex index, long value)
    throws InvalidContextException {

    getDispatchContext().getStatistics().addValue(index, value);
  }

  public void addValue(StatisticsIndexMap.DoubleIndex index, double value)
    throws InvalidContextException {

    getDispatchContext().getStatistics().addValue(index, value);
  }

  public long getValue(StatisticsIndexMap.LongIndex index)
    throws InvalidContextException {

    return getDispatchContext().getStatistics().getValue(index);
  }

  public double getValue(StatisticsIndexMap.DoubleIndex index)
    throws InvalidContextException {
    return getDispatchContext().getStatistics().getValue(index);
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
}
