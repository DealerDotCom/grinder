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

package net.grinder.engine.process;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import net.grinder.common.GrinderException;
import net.grinder.common.Test;
import net.grinder.communication.RegisterTestsMessage;
import net.grinder.communication.Sender;
import net.grinder.statistics.TestStatisticsMap;


/**
 * @author Philip Aston
 * @version $Revision$
 */
class TestRegistry
{
    private final Sender m_consoleSender;

    /**
     * A set of TestData's. (TestData is the class this package uses
     * to store information about Tests). Synchronize on instance when
     * accessesing.
     **/
    private final SortedSet m_testSet = new TreeSet();

    /**
     * A map of Tests to Statistics for passing elsewhere. Synchronize
     * on instance when accessesing.
     **/
    private final TestStatisticsMap m_testStatisticsMap =
	new TestStatisticsMap();

    TestRegistry(Sender consoleSender)
    {
	m_consoleSender = consoleSender;
    }

    void registerTests(Set tests) throws GrinderException
    {
	final Set newTests = new HashSet(tests);

	synchronized (this) {
	    newTests.removeAll(m_testSet);

	    if (newTests.size() > 0) {
		final Iterator newTestsIterator = newTests.iterator();

		while (newTestsIterator.hasNext()) {
		    final Test test = (Test)newTestsIterator.next();

		    final TestData testData = new TestData(test);
		    
		    m_testSet.add(testData);
		    m_testStatisticsMap.put(test, testData.getStatistics());
		}
	    }
	}
	
	if (m_consoleSender != null && newTests.size() > 0) {
	    m_consoleSender.send(new RegisterTestsMessage(newTests));
	}
    }

    synchronized SortedSet getTests()
    {
	return m_testSet;
    }

    synchronized TestStatisticsMap getTestStatisticsMap()
    {
	return m_testStatisticsMap;
    }
}
