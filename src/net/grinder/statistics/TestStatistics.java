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


/**
 * Provide a utility interface to a {@link RawStatistics} which
 * provides access to common values.
 *
 * @author Philip Aston
 * @version $Revision$
 **/
public interface TestStatistics extends RawStatistics {

  /**
   * Increment the <em>errors</em> statistic by one.
   **/
  void addError();

  /**
   * Increment the <em>untimedTransactions</em> statistic by one.
   * @see #addTransaction(long)
   **/
  void addTransaction();

  /**
   * Increment the <em>timedTransactions</em> statistic by one and
   * add the given <code>time</code> to the
   * <em>timedTransactionTime</em> statistic.
   *
   * @param time The transaction time.
   * @see #addTransaction()
   */
  void addTransaction(long time);

  /**
   * Return the sum of the <em>timedTransactions</em> and
   * <em>untimedTransactions</em> statistics.
   *
   * @return a <code>long</code> value
   */
  long getTransactions();

  /**
   * Return the value of the <em>errors</em> statistic.
   *
   * @return a <code>long</code> value
   */
  long getErrors();

  /**
   * Return the value obtained by dividing the
   * <em>timedTransactionTime</em> statistic by the
   * <em>timedTransactions</em> statistic.
   *
   * @return a <code>double</code> value
   */
  double getAverageTransactionTime();
}
