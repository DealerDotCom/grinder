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
import java.util.Map;
import java.util.TreeMap;

import net.grinder.common.GrinderException;
import net.grinder.common.GrinderProperties;
import net.grinder.common.Test;
import net.grinder.util.Serialiser;


/**
 * A map of test numbers to {@link RawStatistics}s.
 *
 * Unsynchronised.
 *
 * @author Philip Aston
 * @version $Revision$
 **/
public class TestStatisticsMap implements java.io.Externalizable
{
    /**
     * Use a TreeMap so we store in test number order.
     *
     * @supplierCardinality 0..*
     * @link aggregation 
     * @associates <{net.grinder.statistics.RawStatistics}>
     **/
    private final Map m_data = new TreeMap();

    /**
     * Put a new {test, statistics} pair in the map.
     *
     * @param test A test.
     * @param statistics The test's statistics.
     **/
    public final void put(Test test, RawStatistics statistics)
    {
	m_data.put(test, statistics);
    }

    /**
     * Return a <code>TestStatisticsMap</code> representing the change
     * since the last snapshot.
     *
     * @param updateSnapshot <code>true</code> => update the snapshot.
     * @return A <code>TestStatisticsMap</code> representing the
     * difference between our values and the snapshot's values.
     **/
    public final TestStatisticsMap getDelta(boolean updateSnapshot)
    {
	final TestStatisticsMap result = new TestStatisticsMap();

	final Iterator iterator = new Iterator();

	while (iterator.hasNext()) {

	    final Pair pair = iterator.next();

	    result.put(pair.getTest(),
		       pair.getStatistics().getDelta(updateSnapshot));
	}

	return result;
    }

    /**
     * Get a new <code>RawStatistics</code> containing the totals of
     * all our entries.
     *
     * @return The totals <code>RawStatistics</code>.
     **/
    public final RawStatistics getTotal()
    {
	final RawStatistics result = new RawStatistics();

	final java.util.Iterator iterator = m_data.values().iterator();

	while (iterator.hasNext()) {
	    result.add((RawStatistics)iterator.next());
	}

	return result;
    }

    /**
     * Return the number of entries in the
     * <code>TestStatisticsMap</code>.
     *
     * @return an <code>int</code> value
     **/
    public final int size()
    {
	return m_data.size();
    }

    /**
     * Add <code>operand</code> to our values.
     *
     * @param operand The <code>TestStatisticsMap</code> containing
     * values to add.
     **/
    public final void add(TestStatisticsMap operand)
    {
	final Iterator iterator = operand.new Iterator();

	while (iterator.hasNext()) {
	    final Pair pair = iterator.next();

	    final Test test = pair.getTest();
	    final RawStatistics statistics =
		(RawStatistics)m_data.get(pair.getTest());

	    if (statistics == null) {
		final RawStatistics newStatistics = new RawStatistics();
		newStatistics.add(pair.getStatistics());

		put(test, newStatistics);
	    }
	    else {
		statistics.add(pair.getStatistics());
	    }
	}
    }

    /**
     * Implement value based equality. Mainly used by unit tests.
     *
     * @param o <code>Object</code> to compare to.
     * @return <code>true</code> if and only if the two objects are equal.
     **/
    public final boolean equals(Object o)
    {
	if (o == this) {
	    return true;
	}
	
	if (!(o instanceof TestStatisticsMap)) {
	    return false;
	}

	final TestStatisticsMap otherMap = (TestStatisticsMap)o;

	if (size() != otherMap.size()) {
	    return false;
	}

	final Iterator iterator = new Iterator();
	final Iterator otherIterator = otherMap.new Iterator();

	while (iterator.hasNext()) {
	    final Pair pair = iterator.next();
	    final Pair otherPair = otherIterator.next();

	    if (!pair.getTest().equals(otherPair.getTest()) ||
		!pair.getStatistics().equals(otherPair.getStatistics())) {
		return false;
	    }
	}

	return true;
    }

    /**
     * Return a <code>String</code> representation of this
     * <code>TestStatisticsMap</code>.
     *
     * @return The <code>String</code>
     **/
    public String toString()
    {
	final StringBuffer result = new StringBuffer();

	result.append("TestStatisticsMap = {");

	final Iterator iterator = new Iterator();

	while (iterator.hasNext()) {
	    final Pair pair = iterator.next();

	    result.append("(");
	    result.append(pair.getTest());
	    result.append(", ");
	    result.append(pair.getStatistics());
	    result.append(")");
	}

	result.append("}");

	return result.toString();
    }

    /**
     * Efficient externalisation method.
     *
     * @param out Handle to the output stream.
     * @exception IOException If an I/O error occurs.
     **/
    public void writeExternal(ObjectOutput out) throws IOException
    {
	out.writeInt(m_data.size());

	final Serialiser serialiser = new Serialiser();

	final Iterator iterator = new Iterator();

	while (iterator.hasNext()) {
	    final Pair pair = iterator.next();

	    out.writeInt(pair.getTest().getNumber());
	    pair.getStatistics().myWriteExternal(out, serialiser);
	}
    }

    /**
     * Efficient externalisation method.
     *
     * @param in Handle to the input stream.
     * @exception IOException If an I/O error occurs.
     **/
    public void readExternal(ObjectInput in) throws IOException
    {
	final int n = in.readInt();

	m_data.clear();

	final Serialiser serialiser = new Serialiser();

	for (int i=0; i<n; i++) {
	    m_data.put(new LightweightTest(in.readInt()),
		       new RawStatistics(in, serialiser));
	}
    }

    /**
     * Light weight test implementation that the console uses.
     **/
    private final static class LightweightTest implements Test
    {
	private final int m_number;

	public LightweightTest(int number)
	{
	    m_number = number;
	}

	public final int getNumber()
	{
	    return m_number;
	}

	public final String getDescription()
	{
	    throw new UnsupportedOperationException(
		getClass().getName() +
		".LightweightTest.getDescription() should never be called");	    
	}

	public final GrinderProperties getParameters()
	{
	    throw new UnsupportedOperationException(
		getClass().getName() + ".LightweightTest.getParameters()");
	}

	public final int compareTo(Object o) 
	{
	    final int other = ((Test)o).getNumber();
	    return m_number<other ? -1 : (m_number==other ? 0 : 1);
	}

	/**
	 * The test number is used as the hash code. Wondered whether
	 * it was worth distributing the hash codes more evenly across
	 * the range of an int, but using the value is good enough for
	 * <code>java.lang.Integer</code> so its good enough for us.
	 **/
	public final int hashCode()
	{
	    return m_number;
	}

	public final boolean equals(Object o)
	{
	    if (o instanceof Test) {
		return m_number == ((Test)o).getNumber();
	    }

	    return false;
	}

	public final String toString()
	{
	    return "Test " + getNumber();
	}
    }

    /**
     * A type safe iterator.
     **/
    public final class Iterator
    {
	private final java.util.Iterator m_iterator;

	/**
	 * Creates a new <code>Iterator</code> instance.
	 **/
	public Iterator()
	{
	    m_iterator = m_data.entrySet().iterator();
	}

	/**
	 * Check whether we are at the end of the {@link
	 * TestStatisticsMap}.
	 *
	 * @return <code>true</code> if there is a next {@link Pair}.
	 *
	 */
	public final boolean hasNext()
	{
	    return m_iterator.hasNext();
	}

	/**
	 * Get the next {@link Pair} from the {@link
	 * TestStatisticsMap}.
	 *
	 * @return The next {@link Pair}.
	 *
	 */
	public final Pair next()
	{
	    final Map.Entry entry = (Map.Entry)m_iterator.next();
	    final Test test = (Test)entry.getKey();
	    final RawStatistics statistics = (RawStatistics)entry.getValue();

	    return new Pair(test, statistics);
	}
    }

    /**
     * A type safe pair of a {@link net.grinder.common.Test} and a
     * {@link RawStatistics}.
     **/
    public final class Pair
    {
	private final Test m_test;
	private final RawStatistics m_statistics;

	private Pair(Test test, RawStatistics statistics)
	{
	    m_test = test;
	    m_statistics = statistics;
	}

	/**
	 * Get the {@link net.grinder.common.Test}.
	 *
	 * @return  The {@link net.grinder.common.Test}.
	 */
	public final Test getTest()
	{
	    return m_test;
	}

	/**
	 * Get the {@link RawStatistics}.
	 *
	 * @return The {@link RawStatistics}.
	 */
	public final RawStatistics getStatistics()
	{
	    return m_statistics;
	}
    }
}
