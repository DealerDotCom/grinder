// The Grinder
// Copyright (C) 2001  Paco Gomez
// Copyright (C) 2001  Philip Aston

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

import junit.framework.TestCase;
import junit.swingui.TestRunner;
//import junit.textui.TestRunner;

import java.util.SortedSet;
import java.util.TreeSet;

import net.grinder.common.GrinderException;
import net.grinder.statistics.StatisticsIndexMap;
import net.grinder.statistics.RawStatistics;


/**
 * Unit test case for <code>StatisticsIndexMap</code>.
 *
 * @author Philip Aston
 * @version $Revision$
 * @see RawStatistics
 */
public class TestStatisticsIndexMap extends TestCase
{
    public static void main(String[] args)
    {
	TestRunner.run(TestStatisticsIndexMap.class);
    }

    public TestStatisticsIndexMap(String name)
    {
	super(name);
    }

    private final StatisticsIndexMap m_indexMap = new StatisticsIndexMap();

    public void testLongs() throws Exception
    {
	final String[] data = {
	    "key 1",
	    "Key 1",
	    "",
	    "something",
	};

	final StatisticsIndexMap.LongIndex[] longResults =
	    new StatisticsIndexMap.LongIndex[data.length];

	for (int i=0; i<data.length; i++) {
	    longResults[i] = m_indexMap.getIndexForLong(data[i]);

	    assert(m_indexMap.isLongIndex(data[i]));
	    assert(!m_indexMap.isDoubleIndex(data[i]));

	    for (int j=0; j<i; ++j) {
		assert(longResults[i].getValue() != longResults[j].getValue());
	    }
	}

	for (int i=0; i<data.length; i++) {
	    assertEquals(longResults[i].getValue(),
			 m_indexMap.getIndexForLong(data[i]).getValue());
	}
    }

    public void testDoubles() throws Exception
    {
	final String[] data = {
	    "key 1",
	    "Key 1",
	    "",
	    "something",
	};

	final StatisticsIndexMap.DoubleIndex[] doubleResults =
	    new StatisticsIndexMap.DoubleIndex[data.length];

	for (int i=0; i<data.length; i++) {
	    doubleResults[i] = m_indexMap.getIndexForDouble(data[i]);

	    assert(m_indexMap.isDoubleIndex(data[i]));
	    assert(!m_indexMap.isLongIndex(data[i]));

	    for (int j=0; j<i; ++j) {
		assert(doubleResults[i].getValue() !=
		       doubleResults[j].getValue());
	    }
	}

	for (int i=0; i<data.length; i++) {
	    assertEquals(doubleResults[i].getValue(),
			 m_indexMap.getIndexForDouble(data[i]).getValue());
	}
    }

    public void testExceptions() throws Exception
    {
	m_indexMap.getIndexForLong("abc");
	m_indexMap.getIndexForDouble("def");

	try {
	    m_indexMap.getIndexForDouble("abc");
	    fail("Expected GrinderException");
	}
	catch (GrinderException e) {
	}

	try {
	    m_indexMap.getIndexForLong("def");
	    fail("Expected GrinderException");
	}
	catch (GrinderException e) {
	}
    }
}
