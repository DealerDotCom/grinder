// The Grinder
// Copyright (C) 2000  Paco Gomez
// Copyright (C) 2000  Philip Aston

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

package net.grinder.engine.process;

/**
 * Package scope
 *
 * @author Philip Aston
 * @version $Revision$
 */
class TestStatistics implements Cloneable
{
    private long m_transactions = 0;
    private long m_totalTime = 0;
    private long m_errors = 0;
    private long m_abortions = 0;
    private TestStatistics m_snapshot = null;

    public TestStatistics()
    {
    }
    
    private TestStatistics(long transactions, long totalTime, long errors,
			   long failures)
    {
	m_transactions = transactions;
	m_totalTime = totalTime;
	m_errors = errors;
	m_abortions = failures;
    }

    public synchronized void addTransaction(long time)
    {
	m_transactions++;
	m_totalTime += time;
    }
	
    public synchronized void addError()
    {
	m_errors++;
    }

    public synchronized void addAbortion()
    {
	m_abortions++;
    }

    /**
     * Protected.
     */
    protected synchronized TestStatistics getClone()
    {
	try {
	    return (TestStatistics)clone();
	}
	catch (CloneNotSupportedException e) {
	    throw new Error("0==1,TestStatistics does not support clone");
	}
    }

    /**
     * Return a TestStatistics representing the change since the last
     * snapshot. This is the only method that accesses the m_snapshot
     * object so we don't worry about the synchronisation to it.
     */
    public synchronized TestStatistics getDelta(boolean updateSnapshot)
    {
	final TestStatistics result;

	if (m_snapshot == null) {
	    result = getClone();
	}
	else {
	    result =
		new TestStatistics(m_transactions - m_snapshot.m_transactions,
				   m_totalTime - m_snapshot.m_totalTime,
				   m_errors - m_snapshot.m_errors,
				   m_abortions - m_snapshot.m_abortions);
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
    public synchronized void add(TestStatistics operand)
    {
	m_transactions += operand.m_transactions;
	m_totalTime += operand.m_totalTime;
	m_errors += operand.m_errors;
	m_abortions += operand.m_abortions;
    }

    /** Accessor. N.B. Use clone() to get a consistent snapshot of a
     * changing TestStatistics */
    public long getTransactions() 
    {
	return m_transactions;
    }

    /** Accessor. N.B. Use clone() to get a consistent snapshot of a
     * changing TestStatistics */
    public long getTotalTime()
    {
	return m_totalTime;
    }

    /** Accessor. N.B. Use clone() to get a consistent snapshot of a
     * changing TestStatistics */
    public long getErrors()
    {
	return m_errors;
    }

    /** Accessor. N.B. Use clone() to get a consistent snapshot of a
     * changing TestStatistics */
    public long getAbortions()
    {
	return m_abortions;
    }

    public synchronized double getAverageTransactionTime()
    {
	if (m_transactions == 0) {
	    return Double.NaN;
	}
	else {
	    return m_totalTime/(double)m_transactions;
	}
    }

    public boolean equals(Object o)
    {
	if (o == this) {
	    return true;
	}
	
	if (!(o instanceof TestStatistics)) {
	    return false;
	}

	final TestStatistics theOther = (TestStatistics)o;

	return
	    m_totalTime == theOther.m_totalTime &&
	    m_transactions == theOther.m_transactions &&
	    m_errors == theOther.m_errors &&
	    m_abortions == theOther.m_abortions;
    }
}
