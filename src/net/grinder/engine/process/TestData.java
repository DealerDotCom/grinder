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

package net.grinder.engine.process;

import net.grinder.common.Test;
import net.grinder.statistics.TestStatistics;
import net.grinder.statistics.TestStatisticsFactory;


/**
 * Represents an individual test. Holds configuration information and
 * the tests statistics.
 *
 * Package scope.
 * 
 * @author Philip Aston
 * @version $Revision$
 **/
final class TestData
{
    private final Test m_test;
    private final TestStatistics m_statistics;

    TestData(Test testDefinition)
    {
	m_test = testDefinition;
	m_statistics = TestStatisticsFactory.getInstance().create();
    }

    final Test getTest()
    {
	return m_test;
    }

    final TestStatistics getStatistics() 
    {
	return m_statistics;
    }
}
