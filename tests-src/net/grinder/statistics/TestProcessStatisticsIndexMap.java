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
import net.grinder.statistics.ProcessStatisticsIndexMap;
import net.grinder.statistics.RawStatistics;


/**
 * Unit test case for <code>ProcessStatisticsIndexMap</code>.
 *
 * @author Philip Aston
 * @version $Revision$
 * @see RawStatistics
 */
public class TestProcessStatisticsIndexMap extends TestCase
{
    public static void main(String[] args)
    {
	TestRunner.run(TestProcessStatisticsIndexMap.class);
    }

    public TestProcessStatisticsIndexMap(String name)
    {
	super(name);
    }

    public void testProcessStatisticsIndexMap() throws Exception
    {
	final ProcessStatisticsIndexMap indexMap =
	    new ProcessStatisticsIndexMap();

	final String[] data = {
	    "key 1",
	    "Key 1",
	    "",
	    "something",
	};

	final int[] results = new int[data.length];

	for (int i=0; i<data.length; i++) {
	    results[i] = indexMap.getIndexFor(data[i]);

	    for (int j=0; j<i; ++j) {
		assert(results[i] != results[j]);
	    }
	}

	for (int i=0; i<data.length; i++) {
	    assertEquals(results[i], indexMap.getIndexFor(data[i]));
	}
    }

    public void testSingleton() throws Exception
    {
	final ProcessStatisticsIndexMap map1 =
	    ProcessStatisticsIndexMap.getInstance();

	final ProcessStatisticsIndexMap map2 =
	    ProcessStatisticsIndexMap.getInstance();

	assertSame(map1, map2);
    }
}
