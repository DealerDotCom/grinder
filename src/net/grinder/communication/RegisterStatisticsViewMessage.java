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
import net.grinder.statistics.StatisticsIndexMap;
import net.grinder.statistics.StatisticsView;


/**
 * Message used to register statistics views with Console. Also used
 * to inform the Console of changes to the local StatisticsIndexMap.
 *
 * @author Philip Aston
 * @version $Revision$
 */
public class RegisterStatisticsViewMessage extends Message
{
    private static final long serialVersionUID = -7078786346425431655L;

    private transient StatisticsIndexMap m_statisticsIndexMap =
	StatisticsIndexMap.getProcessInstance();

    private transient StatisticsView m_statisticsView;

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

    /**
     * Get the <code>StatisticsIndexMap</code> that this message was
     * created against.
     **/
    public StatisticsIndexMap getStatisticsIndexMap()
    {
	return m_statisticsIndexMap;
    }

    /**
     * Customise serialisation.
     *
     * @param in The stream to write our data to.
     **/
    private void writeObject(ObjectOutputStream out)
	throws IOException
    {
	out.defaultWriteObject();

	// Write out our statistics index map so that the receiver
	// knows what we're talking about.
	out.writeObject(m_statisticsIndexMap);

	m_statisticsView.writeExternal(out);
    }

    /**
     * Customise serialisation.
     *
     * @param in The stream to read our data from.
     **/
    private void readObject(ObjectInputStream in)
	throws IOException, ClassNotFoundException
    {
	in.defaultReadObject();

	m_statisticsIndexMap = (StatisticsIndexMap)in.readObject();

	try {
	    // Add any new statistics keys to our process map before
	    // we read the statistics view.
	    StatisticsIndexMap.getProcessInstance().add(m_statisticsIndexMap);
	}
	catch (GrinderException e) {
	    throw new IOException("Incompatible statistics views");
	}

	m_statisticsView = new StatisticsView();
	m_statisticsView.readExternal(in);
    }
}
