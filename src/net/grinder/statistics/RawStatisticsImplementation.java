// Copyright (C) 2000 Paco Gomez
// Copyright (C) 2000, 2001, 2002 Philip Aston
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
import java.io.ObjectOutput;
import java.util.Arrays;

import net.grinder.common.GrinderException;
import net.grinder.util.Serialiser;


/**
 * Store an array of raw statistics as unsigned long values. Clients
 * can access individual values using an index obtained from a {@link
 * StatisticsIndexMap}.
 *
 * @author Philip Aston
 * @version $Revision$
 **/
class RawStatisticsImplementation implements RawStatistics
{
    private final static double[] s_emptyDoubleArray = new double[0];

    private final long[] m_longData;

    /** Double array is allocated as necessary. **/
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
	m_longData =
	    new long[
		StatisticsIndexMap.getInstance().getNumberOfLongIndicies()];
    }

    /**
     * Reset this RawStatistics to default values. Allows instance to
     * be reused.
     *
     * Assuming the caller owns this
     * <code>RawStatisticsImplementation</code> (or they shouldn't be
     * reseting it), we don't synchronise
     **/
    public final void reset()
    {
	Arrays.fill(m_longData, 0);
	Arrays.fill(m_doubleData, 0);
    }

    /**
     * Return the value specified by <code>index</code>.
     *
     * <p>We are working with primitive types so don't need to
     * synchronise.</p>
     *
     * @param index The process specific index.
     * @return The value.
     */
    public final long getValue(StatisticsIndexMap.LongIndex index)
    {
	return m_longData[index.getValue()];
    }

    /**
     * Return the value specified by <code>index</code>.
     *
     * <p>We are working with primitive types so only need to
     * synchronise if we allocate a new array.</p>
     *
     * @param index The process specific index.
     * @return The value.
     */
    public final double getValue(StatisticsIndexMap.DoubleIndex index)
    {
	ensureDoubleDataAllocated();
	return m_doubleData[index.getValue()];
    }

   /**
     * Set the value specified by <code>index</code>.
     *
     * <p>We are working with primitive types so dont need to
     * synchronise.</p>
     *
     * @param index The process specific index.
     * @param value The value.
     **/
    public final void setValue(StatisticsIndexMap.LongIndex index, long value)
    {
	m_longData[index.getValue()] = value;
    }

   /**
     * Set the value specified by <code>index</code>.
     *
     * <p>We are working with primitive types so only need to
     * synchronise if we allocate a new array.</p>
     *
     * @param index The process specific index.
     * @param value The value.
     **/
    public final void setValue(StatisticsIndexMap.DoubleIndex index,
			       double value)
    {
	ensureDoubleDataAllocated();
	m_doubleData[index.getValue()] = value;
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
	m_longData[index.getValue()] += value;
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
	ensureDoubleDataAllocated();
	m_doubleData[index.getValue()] += value;
    }

    /**
     * Equivalent to <code>addValue(index, 1)</code>.
     *
     * @param index The process specific index.
     *
     * @see #addValue
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

	for (int i=0; i<longData.length; i++) {
	    m_longData[i] += longData[i];
	}

	final double[] doubleData = operandImplementation.m_doubleData;

	if (doubleData.length > 0) {
	    ensureDoubleDataAllocated();

	    for (int i=0; i<doubleData.length; i++) {
		m_doubleData[i] += doubleData[i];
	    }
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
	    if (m_doubleData.length < m_snapshot.m_doubleData.length) {
		throw new IllegalStateException(
		    "Assertion failure: " +
		    "Snapshot double data allocated but ours isn't");
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
     * Implement value based equality. Mainly used by unit tests.
     *
     * <p><em>Note, no <code>hashCode()</code> method is defined by
     * this class.</em></p>.
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

	final long[] otherLongData = otherStatistics.m_longData;

	for (int i=0; i<m_longData.length; i++) {
	    if (m_longData[i] != otherLongData[i]) {
		return false;
	    }
	}

	if (m_doubleData.length > 0 ||
	    otherStatistics.m_doubleData.length > 0) {
	    ensureDoubleDataAllocated();
	    otherStatistics.ensureDoubleDataAllocated();

	    for (int i=0; i<m_doubleData.length; i++) {
		if (m_doubleData[i] != otherStatistics.m_doubleData[i]) {
		    return false;
		}
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
	for (int i=0; i<m_longData.length; i++) {
	    serialiser.writeLong(out, m_longData[i]);
	}

	out.writeBoolean(m_doubleData.length > 0);

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
	this();

	for (int i=0; i<m_longData.length; i++) {
	    m_longData[i] = serialiser.readLong(in);
	}

	if (in.readBoolean()) {
	    ensureDoubleDataAllocated();

	    for (int i=0; i<m_doubleData.length; i++) {
		m_doubleData[i] = serialiser.readDouble(in);
	    }
	}
    }

    private final void ensureDoubleDataAllocated()
    {
	if (m_doubleData.length == 0) {
	    synchronized (this) {
		m_doubleData =
		    new double[
			StatisticsIndexMap.getInstance().
			getNumberOfDoubleIndicies()];
	    }
	}
    }
}
