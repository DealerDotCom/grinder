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
import java.io.ObjectOutput;

import net.grinder.util.Serialiser;


/**
 * Store an array of raw statistics as unsigned long values. Clients
 * can access individual values using a process specific index
 * obtained from a {@link ProcessStatisticsIndexMap}. Effectively a
 * cheap array list.
 *
 * @author Philip Aston
 * @version $Revision$
 **/
class RawStatisticsImplementation implements RawStatistics
{
    // Implementation notes
    // 1. The public interface only allows the values to be increased.
    //     We rely on the fact that the values are non-negative to
    //     implement efficient serialisation.
    // 2. Our array of values grows in size to accomodate new process
    //     index values. It never shrinks.

    private final static long[] s_emptyLongArray = new long[0];

    private long[] m_data = s_emptyLongArray;

    /**
     * @clientRole snapshot 
     * @supplierCardinality 0..1
     * @link aggregation
     **/
    private transient RawStatisticsImplementation m_snapshot = null;

    /**
     * Creates a new <code>RawStatisticsImplementation</code> instance.
     **/
    public RawStatisticsImplementation()
    {
    }

    /**
     * Add the values of another <code>RawStatistics</code> to ours.
     * Assumes we don't need to synchronise access to operand.
     *
     * <p><strong>Currently the implementation assumes that the
     * argument is actually a
     * <code>RawStatisticsImplementation</code></strong>.</p>
     *
     * @param operand The <code>RawStatistics</code> value to add.
     **/
    public final synchronized void add(RawStatistics operand)
    {
	final long[] data = ((RawStatisticsImplementation)operand).m_data;

	expandToSize(data.length);

	for (int i=0; i<data.length; i++) {
	    m_data[i] += data[i];
	}
    }

    /**
     * Add <code>value</code> to the value specified by
     * <code>processStatisticsIndex</code>.
     *
     * @param processStatisticsIndex The process specific index.
     * @param value The value.
     * @throws IllegalArgumentException If the <code>processStatisticsIndex</code> is negative. 
     * @throws IllegalArgumentException If the <code>value</code> is negative. 
     **/
    public final synchronized void addValue(int processStatisticsIndex,
					    long value)
    {
	if (processStatisticsIndex < 0) {
	    throw new IllegalArgumentException("Negative value");
	}

	if (value < 0) {
	    throw new IllegalArgumentException("Negative value");
	}

	expandToSize(processStatisticsIndex + 1);
	m_data[processStatisticsIndex] += value;
    }

    /**
     * Equivalent to <code>addValue(processStatisticsIndex, 1)</code>.
     *
     * @param processStatisticsIndex The process specific index.
     * @exception IllegalArgumentException If the <code>processStatisticsIndex</code> is negative. 
     *
     * @see {@link #addValue}
     */
    public final synchronized void incrementValue(int processStatisticsIndex)
    {
	addValue(processStatisticsIndex, 1);
    }

    /**
     * Return the value specified by
     * <code>processStatisticsIndex</code>.
     *
     * @param processStatisticsIndex The process specific index.
     * @return The value.
     * @throws IllegalArgumentException If the <code>processStatisticsIndex</code> is negative. 
     */
    public final long getValue(int processStatisticsIndex)
    {
	if (processStatisticsIndex < 0) {
	    throw new IllegalArgumentException("Negative value");
	}

	expandToSize(processStatisticsIndex + 1);
	return m_data[processStatisticsIndex];
    }

    /**
     * Return a <code>RawStatistics</code> representing the change
     * since the last snapshot.
     *
     * @param updateSnapshot <code>true</code> => update the snapshot.
     * @return A <code>RawStatistics</code> representing the
     * difference between our values and the snapshot's values.
     **/
    public final synchronized RawStatistics getDelta(boolean updateSnapshot)
    {
	// This is the only method that accesses m_snapshot so we
	// don't worry about the synchronisation of it.

	final RawStatisticsImplementation result =
	    new RawStatisticsImplementation();

	result.add(this);

	if (m_snapshot != null) {
	    if (m_data.length < m_snapshot.m_data.length) {
		throw new IllegalStateException(
		    "Assertion failure: " +
		    "Snapshot data size is larger than ours");
	    }

	    final long[] data = m_snapshot.m_data;

	    for (int i=0; i<data.length; i++) {
		result.m_data[i] -= data[i];
	    }
	}

	if (updateSnapshot) {
	    m_snapshot = new RawStatisticsImplementation();
	    m_snapshot.add(this);
	}

	return result;
    }

    /**
     * Implement value based equality.
     *
     * @param o <code>Object</code> to compare to.
     * @return <code>true</code> if and only if the two objects are equal.
     **/
    public final boolean equals(Object o)
    {
	if (o == this) {
	    return true;
	}
	
	if (!(o instanceof RawStatisticsImplementation)) {
	    return false;
	}

	final RawStatisticsImplementation otherStatistics =
	    (RawStatisticsImplementation)o;

	expandToSize(otherStatistics.m_data.length);
	otherStatistics.expandToSize(m_data.length);

	final long[] otherData = otherStatistics.m_data;

	for (int i=0; i<m_data.length; i++) {
	    if (m_data[i] != otherData[i]) {
		return false;
	    }
	}

	return true;
    }

    /**
     * Return a <code>String</code> representation of this
     * <code>RawStatistics</code>.
     *
     * @return The <code>String</code>
     **/
    public final String toString()
    {
	final StringBuffer result = new StringBuffer();

	result.append("RawStatistics = {");

	for (int i=0; i<m_data.length; i++) {
	    result.append(m_data[i]);

	    if (i!= m_data.length-1) {
		result.append(", ");
	    }
	}

	result.append("}");

	return result.toString();
    }

    /**
     * Efficient externalisation method used by {@link
     * TestStatisticsFactory#writeStatisticsExternal}.
     *
     * @param out Handle to the output stream.
     * @param serialiser <code>Serialiser</code> helper object.
     * @exception IOException If an error occurs.
     **/
    final void myWriteExternal(ObjectOutput out, Serialiser serialiser)
	throws IOException
    {
	out.writeInt(m_data.length);

	for (int i=0; i<m_data.length; i++) {
	    serialiser.writeUnsignedLong(out, m_data[i]);
	}
    }

    /**
     * Efficient externalisation method used by {@link
     * TestStatisticsFactory#readStatisticsExternal}.
     *
     * @param in Handle to the input stream.
     * @param serialiser <code>Serialiser</code> helper object.
     * @exception IOException If an error occurs.
     **/
    protected RawStatisticsImplementation(ObjectInput in,
					  Serialiser serialiser)
	throws IOException
    {
	final int length = in.readInt();

	m_data = new long[length];

	for (int i=0; i<m_data.length; i++) {
	    m_data[i] = serialiser.readUnsignedLong(in);
	}
    }

    private final void expandToSize(int size)
    {
	if (m_data.length < size) {
	    final long[] newStatistics = new long[size];

	    System.arraycopy(m_data, 0, newStatistics, 0,
			     m_data.length);

	    m_data = newStatistics;
	}
    }
}
