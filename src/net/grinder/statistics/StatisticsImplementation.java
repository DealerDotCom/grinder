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
 * Package scope
 *
 * @author Philip Aston
 * @version $Revision$
 */
public class StatisticsImplementation
    implements Statistics, Cloneable, java.io.Serializable
{
    private long m_untimedTransactions = 0;

    private long m_timedTransactions = 0;
    private long m_totalTime = 0;

    private long m_errors = 0;

    private StatisticsImplementation m_snapshot = null;

    public StatisticsImplementation()
    {
    }
    
    private StatisticsImplementation(long transactions, long timedTransactions,
				     long totalTime, long errors)
    {
	m_untimedTransactions = transactions;
	m_timedTransactions = timedTransactions;
	m_totalTime = totalTime;
	m_errors = errors;
    }

    public synchronized void addTransaction()
    {
	m_untimedTransactions++;
    }
	
    public synchronized void addTransaction(long time)
    {
	m_timedTransactions++;
	m_totalTime += time;
    }
	
    public synchronized void addError()
    {
	m_errors++;
    }

    /**
     * Protected.
     */
    protected synchronized StatisticsImplementation getClone()
    {
	try {
	    return (StatisticsImplementation)clone();
	}
	catch (CloneNotSupportedException e) {
	    throw new Error(
		"0==1,StatisticsImplementation does not support clone");
	}
    }

    /**
     * Return a StatisticsImplementation representing the change since
     * the last snapshot. This is the only method that accesses the
     * m_snapshot object so we don't worry about the synchronisation
     * to it.
     */
    public synchronized StatisticsImplementation getDelta(
	boolean updateSnapshot)
    {
	final StatisticsImplementation result;

	if (m_snapshot == null) {
	    result = getClone();
	}
	else {
	    result =
		new StatisticsImplementation(m_untimedTransactions -
					     m_snapshot.m_untimedTransactions,
					     m_timedTransactions -
					     m_snapshot.m_timedTransactions,
					     m_totalTime - 
					     m_snapshot.m_totalTime,
					     m_errors - m_snapshot.m_errors);
	}

	if (updateSnapshot) {
	    m_snapshot = null;	// Discard history.
	    m_snapshot = getClone();
	}

	return result;
    }

    /**
     * Assumes we don't need to synchronise access to operand.
     */
    public synchronized void add(StatisticsImplementation operand)
    {
	m_untimedTransactions += operand.m_untimedTransactions;
	m_timedTransactions += operand.m_timedTransactions;
	m_totalTime += operand.m_totalTime;
	m_errors += operand.m_errors;
    }

    /** Accessor. N.B. Use clone() to get a consistent snapshot of a
     * changing StatisticsImplementation */
    public long getTransactions() 
    {
	return m_untimedTransactions + m_timedTransactions;
    }

    /** Accessor. N.B. Use clone() to get a consistent snapshot of a
     * changing StatisticsImplementation */
    public long getErrors()
    {
	return m_errors;
    }

    public synchronized double getAverageTransactionTime()
    {
	if (m_timedTransactions == 0) {
	    return Double.NaN;
	}
	else {
	    return m_totalTime/(double)m_timedTransactions;
	}
    }

    public boolean equals(Object o)
    {
	if (o == this) {
	    return true;
	}
	
	if (!(o instanceof StatisticsImplementation)) {
	    return false;
	}

	final StatisticsImplementation theOther = (StatisticsImplementation)o;

	return
	    m_untimedTransactions == theOther.m_untimedTransactions &&
	    m_timedTransactions == theOther.m_timedTransactions &&
	    m_totalTime == theOther.m_totalTime &&
	    m_errors == theOther.m_errors;
    }
}
