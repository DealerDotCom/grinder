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

import java.io.IOException;
import java.io.ObjectInput;

import net.grinder.common.GrinderException;
import net.grinder.util.Serialiser;


/**
 * @author Philip Aston
 * @version $Revision$
 **/
class TestStatisticsImplementation
    extends RawStatisticsImplementation implements TestStatistics
{
    private final static StatisticsIndexMap.LongIndex s_errorsIndex;
    private final static StatisticsIndexMap.LongIndex s_timedTransactionsIndex;
    private final static StatisticsIndexMap.LongIndex
	s_untimedTransactionsIndex;
    private final static StatisticsIndexMap.LongIndex s_totalTimeIndex;

    static
    {
	final StatisticsIndexMap indexMap =
	    TestStatisticsFactory.getInstance().getIndexMap();

	try {
	    s_errorsIndex = indexMap.getIndexForLong("errors");
	    s_timedTransactionsIndex =
		indexMap.getIndexForLong("timedTransactions");
	    s_untimedTransactionsIndex =
		indexMap.getIndexForLong("untimedTransactions");
	    s_totalTimeIndex = indexMap.getIndexForLong("totalTime");
	}
	catch (GrinderException e) {
	    throw new RuntimeException(
		"Assertion failure, " +
		"TestStatisticsImplementation could not initialise: " +
		e.getMessage());
	}
    }

    /**
     * Creates a new <code>TestStatisticsImplementation</code> instance.
     **/
    public TestStatisticsImplementation()
    {
    }

    /**
     * Efficient externalisation method used by {@link
     * TestStatisticsFactory#writeStatisticsExternal}.
     *
     * @param out Handle to the output stream.
     * @param serialiser <code>Serialiser</code> helper object.
     * @exception IOException If an error occurs.
     **/
    public TestStatisticsImplementation(ObjectInput in, Serialiser serialiser)
	throws IOException
    {
	super(in, serialiser);
    }

    public final void addError()
    {
	addValue(s_errorsIndex, 1);
    }
    
    public final void addTransaction()
    {
	addValue(s_untimedTransactionsIndex, 1);
    }
    
    public final void addTransaction(long time)
    {
	addValue(s_timedTransactionsIndex, 1);
	addValue(s_totalTimeIndex, time);
    }

    public final long getTransactions()
    {
	return
	    getValue(s_timedTransactionsIndex) +
	    getValue(s_untimedTransactionsIndex);
    }

    public final long getErrors()
    {
	return getValue(s_errorsIndex);
    }

    public final double getAverageTransactionTime()
    {
	final long timedTransactions = getValue(s_timedTransactionsIndex);

	return
	    timedTransactions == 0 ?
	    Double.NaN : getValue(s_totalTimeIndex)/(double)timedTransactions;
    }
}
