// The Grinder
// Copyright (C) 2000, 2001  Paco Gomez
// Copyright (C) 2000, 2001  Philip Aston

// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.

// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.

// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.

package net.grinder.statistics;


/**
 * Provide a utility interface to a {@link RawStatistics} which
 * provides access to common values.
 *
 * @author Philip Aston
 * @version $Revision$
 **/
public interface TestStatistics extends RawStatistics
{
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
