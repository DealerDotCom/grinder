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
import java.util.Arrays;

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
    // Implementation notes: Our arrays of values grow in size to
    // accomodate new process index values. They never shrink.

    private final static long[] s_emptyLongArray = new long[0];
    private final static double[] s_emptyDoubleArray = new double[0];

    private long[] m_longData = s_emptyLongArray;
    private double[] m_doubleData = s_emptyDoubleArray;

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
     * Reset this RawStatistics to default values. Allows instance to
     * be reused. Instance is likely to have the correct sized arrays,
     * so this prevents much resizing.
     **/
    public synchronized void reset()
    {
	Arrays.fill(m_longData, 0);
	Arrays.fill(m_doubleData, 0);
    }

    /**
     * Return the value specified by <code>index</code>.
     *
     * <p>We are working with primitive types so only need to
     * synchronise if we resize.</p>
     *
     * @param index The process specific index.
     * @return The value.
     */
    public final long getValue(StatisticsIndexMap.LongIndex index)
    {
	final int indexValue = index.getValue();

	expandLongDataToSize(indexValue + 1);
	return m_longData[indexValue];
    }

    /**
     * Return the value specified by <code>index</code>.
     *
     * <p>We are working with primitive types so only need to
     * synchronise if we resize.</p>
     *
     * @param index The process specific index.
     * @return The value.
     */
    public final double getValue(StatisticsIndexMap.DoubleIndex index)
    {
	final int indexValue = index.getValue();

	expandDoubleDataToSize(indexValue + 1);
	return m_doubleData[indexValue];
    }

   /**
     * Set the value specified by <code>index</code>.
     *
     * <p>We are working with primitive types so only need to
     * synchronise if we resize.</p>
     *
     * @param index The process specific index.
     * @param value The value.
     **/
    public final void setValue(StatisticsIndexMap.LongIndex index,
			       long value)
    {
	final int indexValue = index.getValue();

	expandLongDataToSize(indexValue + 1);
	m_longData[indexValue] = value;
    }

   /**
     * Set the value specified by <code>index</code>.
     *
     * <p>We are working with primitive types so only need to
     * synchronise if we resize.</p>
     *
     * @param index The process specific index.
     * @param value The value.
     **/
    public final void setValue(StatisticsIndexMap.DoubleIndex index,
			       double value)
    {
	final int indexValue = index.getValue();

	expandDoubleDataToSize(indexValue + 1);
	m_doubleData[indexValue] = value;
    }

    /**
     * Add <code>value</code> to the value specified by
     * <code>index</code>.
     *
     * <p>Synchronised to ensure we don't lose information.</p>.
     *
     * @param index The process specific index.
     * @param value The value.
     **/
    public final synchronized void addValue(StatisticsIndexMap.LongIndex index,
					    long value)
    {
	final int indexValue = index.getValue();

	expandLongDataToSize(indexValue + 1);
	m_longData[indexValue] += value;
    }

    /**
     * Add <code>value</code> to the value specified by
     * <code>index</code>.
     *
     * <p>Synchronised to ensure we don't lose information.</p>.
     *
     * @param index The process specific index.
     * @param value The value.
     **/
    public final synchronized void
	addValue(StatisticsIndexMap.DoubleIndex index, double value)
    {
	final int indexValue = index.getValue();

	expandDoubleDataToSize(indexValue + 1);
	m_doubleData[indexValue] += value;
    }

    /**
     * Equivalent to <code>addValue(index, 1)</code>.
     *
     * @param index The process specific index.
     *
     * @see {@link #addValue}
     */
    public final void incrementValue(StatisticsIndexMap.LongIndex index)
    {
	addValue(index, 1);
    }

    /**
     * Add the values of another <code>RawStatistics</code> to ours.
     * Assumes we don't need to synchronise access to operand.
     *
     * <p><strong>Currently the implementation assumes that the
     * argument is actually a
     * <code>RawStatisticsImplementation</code></strong>.</p>
     *
     * <p>Synchronised to ensure we don't lose information.</p>.
     *
     * @param operand The <code>RawStatistics</code> value to add.
     **/
    public final synchronized void add(RawStatistics operand)
    {
	final RawStatisticsImplementation operandImplementation =
	    (RawStatisticsImplementation)operand;

	final long[] longData = operandImplementation.m_longData;

	expandLongDataToSize(longData.length);

	for (int i=0; i<longData.length; i++) {
	    m_longData[i] += longData[i];
	}

	final double[] doubleData = operandImplementation.m_doubleData;

	expandDoubleDataToSize(doubleData.length);

	for (int i=0; i<doubleData.length; i++) {
	    m_doubleData[i] += doubleData[i];
	}
    }

    /**
     * Return a <code>RawStatistics</code> representing the change
     * since the last snapshot.
     *
     * <p>Synchronised to ensure a consistent view.</p>.
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
	    if (m_longData.length < m_snapshot.m_longData.length ||
		m_doubleData.length < m_snapshot.m_doubleData.length) {
		throw new IllegalStateException(
		    "Assertion failure: " +
		    "Snapshot data size is larger than ours");
	    }

	    final long[] longData = m_snapshot.m_longData;

	    for (int i=0; i<longData.length; i++) {
		result.m_longData[i] -= longData[i];
	    }

	    final double[] doubleData = m_snapshot.m_doubleData;

	    for (int i=0; i<doubleData.length; i++) {
		result.m_doubleData[i] -= doubleData[i];
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

	expandLongDataToSize(otherStatistics.m_longData.length);
	otherStatistics.expandLongDataToSize(m_longData.length);
	expandDoubleDataToSize(otherStatistics.m_doubleData.length);
	otherStatistics.expandDoubleDataToSize(m_doubleData.length);

	final long[] otherLongData = otherStatistics.m_longData;

	for (int i=0; i<m_longData.length; i++) {
	    if (m_longData[i] != otherLongData[i]) {
		return false;
	    }
	}

	final double[] otherDoubleData = otherStatistics.m_doubleData;

	for (int i=0; i<m_doubleData.length; i++) {
	    if (m_doubleData[i] != otherDoubleData[i]) {
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

	for (int i=0; i<m_longData.length; i++) {
	    result.append(m_longData[i]);

	    if (i!= m_longData.length-1 || m_doubleData.length > 0) {
		result.append(", ");
	    }
	}

	for (int i=0; i<m_doubleData.length; i++) {
	    result.append(m_doubleData[i]);

	    if (i!= m_doubleData.length-1) {
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
     * <p>Synchronised to ensure a consistent view.</p>.
     *
     * @param out Handle to the output stream.
     * @param serialiser <code>Serialiser</code> helper object.
     * @exception IOException If an error occurs.
     **/
    final synchronized void myWriteExternal(ObjectOutput out,
					    Serialiser serialiser)
	throws IOException
    {
	out.writeInt(m_longData.length);

	for (int i=0; i<m_longData.length; i++) {
	    serialiser.writeLong(out, m_longData[i]);
	}

	out.writeInt(m_doubleData.length);

	for (int i=0; i<m_doubleData.length; i++) {
	    serialiser.writeDouble(out, m_doubleData[i]);
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
	final int longLength = in.readInt();

	m_longData = new long[longLength];

	for (int i=0; i<m_longData.length; i++) {
	    m_longData[i] = serialiser.readLong(in);
	}

	final int doubleLength = in.readInt();

	m_doubleData = new double[doubleLength];

	for (int i=0; i<m_doubleData.length; i++) {
	    m_doubleData[i] = serialiser.readDouble(in);
	}
    }

    private final void expandLongDataToSize(int size)
    {
	if (m_longData.length < size) {
	    synchronized (this) {
		final long[] newStatistics = new long[size];

		System.arraycopy(m_longData, 0, newStatistics, 0,
				 m_longData.length);

		m_longData = newStatistics;
	    }
	}
    }

    private final void expandDoubleDataToSize(int size)
    {
	if (m_doubleData.length < size) {
	    synchronized (this) {
		final double[] newStatistics = new double[size];

		System.arraycopy(m_doubleData, 0, newStatistics, 0,
				 m_doubleData.length);

		m_doubleData = newStatistics;
	    }
	}
    }
}
