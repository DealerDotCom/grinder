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
import java.util.Map;
import java.util.TreeMap;

import net.grinder.common.GrinderException;
import net.grinder.common.GrinderProperties;
import net.grinder.common.Test;


/**
 * A map of test numbers to {@link TestStatistics}s.
 *
 * Unsynchronised.
 *
 * @author Philip Aston
 * @version $Revision$
 **/
public class TestStatisticsMap implements java.io.Externalizable
{
    private static final long serialVersionUID = 3781345128009455699L;

    /**
     * Use a TreeMap so we store in test number order.
     *
     * @supplierCardinality 0..*
     * @link aggregation 
     * @associates <{TestStatisticsImplementation}>
     **/
    private final Map m_data = new TreeMap();

    /**
     * @supplierCardinality 1 
     **/
    private final TestStatisticsFactory m_testStatisticsFactory	=
	TestStatisticsFactory.getInstance();

    /**
     * Creates a new <code>TestStatisticsMap</code> instance.
     **/
    public TestStatisticsMap()
    {
    }

    /**
     * Put a new {test, statistics} pair in the map.
     *
     * @param test A test.
     * @param statistics The test's statistics.
     **/
    public final void put(Test test, TestStatistics statistics)
    {
	if (!(statistics instanceof TestStatisticsImplementation)) {
	    throw new RuntimeException(
		"TestStatistics implementation not supported");
	}

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

	    final TestStatisticsImplementation testStatistics =
		m_testStatisticsFactory.createImplementation();

	    testStatistics.add(pair.getStatistics().getDelta(updateSnapshot));

	    result.put(pair.getTest(), testStatistics);
	}

	return result;
    }

    /**
     * Get a new <code>TestStatistics</code> containing the totals of
     * all our entries.
     *
     * @return The totals <code>TestStatistics</code>.
     **/
    public final TestStatistics getTotal()
    {
	final TestStatisticsImplementation result =
	    m_testStatisticsFactory.createImplementation();

	final java.util.Iterator iterator = m_data.values().iterator();

	while (iterator.hasNext()) {
	    result.add((TestStatistics)iterator.next());
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
	    final TestStatistics statistics =
		(TestStatistics)m_data.get(pair.getTest());

	    if (statistics == null) {
		final TestStatisticsImplementation newStatistics =
		    m_testStatisticsFactory.createImplementation();

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

	final Iterator iterator = new Iterator();

	while (iterator.hasNext()) {
	    final Pair pair = iterator.next();

	    out.writeInt(pair.getTest().getNumber());

	    // Its a class invariant that our TestStatistics are all
	    // TestStatisticsImplementations.
	    m_testStatisticsFactory.writeStatisticsExternal(
		out, (TestStatisticsImplementation)pair.getStatistics());
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

	for (int i=0; i<n; i++) {
	    m_data.put(new LightweightTest(in.readInt()),
		       m_testStatisticsFactory.readStatisticsExternal(in));
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
	 * @return <code>true</code> if there is a next {@link
	 * TestStatisticsMap.Pair}.
	 **/
	public final boolean hasNext()
	{
	    return m_iterator.hasNext();
	}

	/**
	 * Get the next {@link TestStatisticsMap.Pair} from the {@link
	 * TestStatisticsMap}.
	 *
	 * @return The next {@link TestStatisticsMap.Pair}.
	 * @throws java.util.NoSuchElementException If there is no next element.
	 **/
	public final Pair next()
	{
	    final Map.Entry entry = (Map.Entry)m_iterator.next();
	    final Test test = (Test)entry.getKey();
	    final TestStatistics statistics = (TestStatistics)entry.getValue();

	    return new Pair(test, statistics);
	}
    }

    /**
     * A type safe pair of a {@link net.grinder.common.Test} and a
     * {@link TestStatistics}.
     **/
    public final class Pair
    {
	private final Test m_test;
	private final TestStatistics m_statistics;

	private Pair(Test test, TestStatistics statistics)
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
	 * Get the {@link TestStatistics}.
	 *
	 * @return The {@link TestStatistics}.
	 */
	public final TestStatistics getStatistics()
	{
	    return m_statistics;
	}
    }
}
