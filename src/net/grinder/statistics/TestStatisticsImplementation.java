// Copyright (C) 2000, 2001, 2002, 2003 Philip Aston
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

import java.io.IOException;
import java.io.ObjectInput;

import net.grinder.common.GrinderException;
import net.grinder.util.Serialiser;


/**
 * @author Philip Aston
 * @version $Revision$
 **/
final class TestStatisticsImplementation
  extends RawStatisticsImplementation implements TestStatistics {

  private static final StatisticsIndexMap.LongIndex s_errorsIndex;
  private static final StatisticsIndexMap.LongIndex s_timedTransactionsIndex;
  private static final StatisticsIndexMap.LongIndex s_untimedTransactionsIndex;
  private static final StatisticsIndexMap.LongIndex s_totalTimeIndex;

  static {
    final StatisticsIndexMap indexMap = StatisticsIndexMap.getInstance();

    try {
      s_errorsIndex = indexMap.getIndexForLong("errors");
      s_timedTransactionsIndex =
    indexMap.getIndexForLong("timedTransactions");
      s_untimedTransactionsIndex =
    indexMap.getIndexForLong("untimedTransactions");
      s_totalTimeIndex =
    indexMap.getIndexForLong("timedTransactionTime");
    }
    catch (GrinderException e) {
      throw new ExceptionInInitializerError(
    "Assertion failure, " +
    "TestStatisticsImplementation could not initialise: " +
    e.getMessage());
    }
  }

  /**
   * Creates a new <code>TestStatisticsImplementation</code> instance.
   **/
  public TestStatisticsImplementation() {
  }

  /**
   * Efficient externalisation method used by {@link
   * TestStatisticsFactory#writeStatisticsExternal}.
   *
   * @param in Handle to the output stream.
   * @param serialiser <code>Serialiser</code> helper object.
   * @exception IOException If an error occurs.
   **/
  public TestStatisticsImplementation(ObjectInput in, Serialiser serialiser)
    throws IOException {
    super(in, serialiser);
  }

  public final void addError() {
    addValue(s_errorsIndex, 1);
  }

  public final void addTransaction() {
    addValue(s_untimedTransactionsIndex, 1);
  }

  public final void addTransaction(long time) {
    addValue(s_timedTransactionsIndex, 1);
    addValue(s_totalTimeIndex, time);
  }

  public final long getTransactions() {
    return
      getValue(s_timedTransactionsIndex) +
      getValue(s_untimedTransactionsIndex);
  }

  public final long getErrors() {
    return getValue(s_errorsIndex);
  }

  public final double getAverageTransactionTime() {
    final long timedTransactions = getValue(s_timedTransactionsIndex);

    return
      timedTransactions == 0 ?
      Double.NaN : getValue(s_totalTimeIndex) / (double)timedTransactions;
  }
}
