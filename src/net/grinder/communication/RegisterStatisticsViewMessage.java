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

import java.io.Serializable;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import net.grinder.common.GrinderException;
import net.grinder.statistics.StatisticsView;


/**
 * Message used to register statistics views with Console.
 *
 * @author Philip Aston
 * @version $Revision$
 */
public class RegisterStatisticsViewMessage extends Message
{
    private static final long serialVersionUID = -7078786346425431655L;

    private StatisticsView m_statisticsView;

    /**
     * Constructor.
     *
     * @param tests The test set to register.
     **/
    public RegisterStatisticsViewMessage(StatisticsView statisticsView)
    {
	m_statisticsView = statisticsView;
    }

    /**
     * Get the statistics view.
     **/
    public StatisticsView getStatisticsView()
    {
	return m_statisticsView;
    }
}
