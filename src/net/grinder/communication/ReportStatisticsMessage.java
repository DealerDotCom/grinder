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
import net.grinder.statistics.TestStatisticsMap;


/**
 * Message used to report test statistics to the console.
 *
 * @author Philip Aston
 * @version $Revision$
 */
public class ReportStatisticsMessage extends Message implements Serializable
{
    private static final long serialVersionUID = 171863391515128541L;

    private transient TestStatisticsMap m_statisticsDelta;

    /**
     * Constructor.
     *
     * @param statisticsDelta The test statistics.
     **/
    public ReportStatisticsMessage(TestStatisticsMap statisticsDelta)
    {
	m_statisticsDelta = statisticsDelta;
    }

    /**
     * Get the test statistics.
     **/
    public TestStatisticsMap getStatisticsDelta()
    {
	return m_statisticsDelta;
    }

    /**
     * Customise serialisation for efficiency.
     *
     * @param in The stream to write our data to.
     **/
    private void writeObject(ObjectOutputStream out)
	throws IOException
    {
	out.defaultWriteObject();
	m_statisticsDelta.writeExternal(out);
    }

    /**
     * Customise serialisation for efficiency.
     *
     * @param in The stream to read our data from.
     **/
    private void readObject(ObjectInputStream in)
	throws IOException, ClassNotFoundException
    {
	in.defaultReadObject();

	try {
	    m_statisticsDelta = new TestStatisticsMap();
	    m_statisticsDelta.readExternal(in);
	}
	catch (GrinderException e) {
	    throw new IOException("Could instantiate a TestStatisticsMap");
	}
    }
}
