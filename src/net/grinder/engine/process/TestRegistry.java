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
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import net.grinder.common.GrinderException;
import net.grinder.communication.RegisterTestsMessage;
import net.grinder.communication.Sender;
import net.grinder.script.InvokeableTest;
import net.grinder.statistics.TestStatisticsMap;


/**
 * @author Philip Aston
 * @version $Revision$
 */
final class TestRegistry
{
    private final Sender m_consoleSender;

    /**
     * A map of InvokeableTest to TestData's. (TestData is the class
     * this package uses to store information about InvokeableTests).
     * Synchronize on instance when accessesing.
     **/
    private final Map m_testMap = new TreeMap();

    private InvokeableTest[] m_tests;

    /**
     * A map of InvokeableTests to Statistics for passing elsewhere.
     **/
    private final TestStatisticsMap m_testStatisticsMap =
	new TestStatisticsMap();

    public TestRegistry(Sender consoleSender)
    {
	m_consoleSender = consoleSender;
    }

    public final TestData registerTest(InvokeableTest test)
	throws GrinderException
    {
	final TestData newTestData;
	
	synchronized (this) {
	    final TestData existing = (TestData)m_testMap.get(test);

	    if (existing != null) {
		return existing;
	    }
	    else {
		newTestData = new TestData(test);
		m_testMap.put(test, newTestData);
		m_testStatisticsMap.put(test, newTestData.getStatistics());

		m_tests = null;
	    }
	}
	
	m_consoleSender.queue(
	    new RegisterTestsMessage(Collections.singleton(test)));

	return newTestData;
    }

    public final synchronized InvokeableTest[] getTests()
    {
	if (m_tests == null) {
	    m_tests =
		(InvokeableTest[])
		m_testMap.keySet().toArray(new InvokeableTest[0]);
	}

	return m_tests;
    }

    public final TestStatisticsMap getTestStatisticsMap()
    {
	return m_testStatisticsMap;
    }
}
