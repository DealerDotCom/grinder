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

package net.grinder.communication;

import net.grinder.statistics.TestStatisticsMap;


/**
 * @author Philip Aston
 * @version $Revision$
 */
public class ReportStatisticsMessage implements Message
{
    final String m_hostID;
    final String m_processID;
    final TestStatisticsMap m_statisticsDelta;

    public ReportStatisticsMessage(String hostID, String processID,
				   TestStatisticsMap statisticsDelta)
    {
	m_hostID = hostID;
	m_processID = processID;
	m_statisticsDelta = statisticsDelta;
    }

    public String getHostID()
    {
	return m_hostID;
    }

    public String getProcessID()
    {
	return m_processID;
    }

    public TestStatisticsMap getStatisticsDelta()
    {
	return m_statisticsDelta;
    }
}
