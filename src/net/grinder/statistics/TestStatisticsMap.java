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

import java.util.Map;
import java.util.TreeMap;

import net.grinder.plugininterface.Test;
import net.grinder.util.GrinderException;


/**
 * A map of Test's to StatisticsImplementation.
 *
 * Unsynchronised.
 *
 * @author Philip Aston
 * @version $Revision$
 */
public class TestStatisticsMap implements java.io.Serializable
{
    /** Use a TreeMap so we store in Test order. */
    private final Map m_data = new TreeMap();

    public void put(Test test, StatisticsImplementation statistics)
    {
	m_data.put(test, statistics);
    }

    public TestStatisticsMap getDelta(boolean updateSnapshot)
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

    public StatisticsImplementation getTotal()
    {
	final StatisticsImplementation result = new StatisticsImplementation();

	final java.util.Iterator iterator = m_data.values().iterator();

	while (iterator.hasNext()) {
	    result.add((StatisticsImplementation)iterator.next());
	}

	return result;
    }

    public int getSize()
    {
	return m_data.size();
    }

    public void add(TestStatisticsMap operand)
    {
	final Iterator iterator = operand.new Iterator();

	while (iterator.hasNext()) {

	    final Pair pair = iterator.next();

	    final Test test = pair.getTest();
	    final StatisticsImplementation statistics =
		(StatisticsImplementation)m_data.get(pair.getTest());

	    if (statistics == null) {
		put(test, pair.getStatistics().getClone());
	    }
	    else {
		statistics.add(pair.getStatistics());
	    }
	}
    }


    /**
     * A type safe iterator.
     */
    public class Iterator
    {
	private final java.util.Iterator m_iterator;

	public Iterator()
	{
	    m_iterator = m_data.entrySet().iterator();
	}

	public boolean hasNext()
	{
	    return m_iterator.hasNext();
	}

	public Pair next()
	{
	    final Map.Entry entry = (Map.Entry)m_iterator.next();
	    final Test test = (Test)entry.getKey();
	    final StatisticsImplementation statistics =
		(StatisticsImplementation)entry.getValue();

	    return new Pair(test, statistics);
	}
    }

    public class Pair
    {
	private final Test m_test;
	private final StatisticsImplementation m_statistics;

	private Pair(Test test, StatisticsImplementation statistics)
	{
	    m_test = test;
	    m_statistics = statistics;
	}

	public Test getTest()
	{
	    return m_test;
	}

	public StatisticsImplementation getStatistics()
	{
	    return m_statistics;
	}
    }
}
