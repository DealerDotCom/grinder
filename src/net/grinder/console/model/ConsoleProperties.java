// The Grinder
// Copyright (C) 2000  Paco Gomez
// Copyright (C) 2000  Philip Aston

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

package net.grinder.console.model;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import net.grinder.common.GrinderException;
import net.grinder.common.GrinderProperties;
import net.grinder.communication.CommunicationDefaults;


/**
 * @author Philip Aston
 * @version $Revision$
 */
public class ConsoleProperties
{
    private final GrinderProperties m_properties;
    private final File m_file;

    private int m_collectSampleCount;
    private int m_ignoreSampleCount;
    private int m_sampleInterval;
    private int m_significantFigures;

    private String m_multicastAddress;
    private int m_consolePort;
    private int m_grinderPort;

    public ConsoleProperties() throws GrinderException
    {
	String home = System.getProperty("user.home");

	if (home == null) {
	    home = System.getProperty("java.home");
	}

	m_file = new File(home, ".grinder_console");

	m_properties = new GrinderProperties();
	load();

	m_collectSampleCount = m_properties.getInt("numberToCollect", 0);
	m_ignoreSampleCount = m_properties.getInt("numberToIgnore", 1);
	m_sampleInterval = m_properties.getInt("sampleInterval", 1000);
	m_significantFigures = m_properties.getInt("significantFigures", 3);

	m_multicastAddress =
	    m_properties.getProperty("multicastAddress",
				     CommunicationDefaults.MULTICAST_ADDRESS);

	m_consolePort =
	    m_properties.getInt("console.multicastPort",
				CommunicationDefaults.CONSOLE_PORT);

	m_grinderPort =
	    m_properties.getInt("grinder.multicastPort",
				CommunicationDefaults.GRINDER_PORT);
    }

    private void load() throws GrinderException
    {
	m_properties.clear();

	if (m_file.exists()) {
	    try {
		final InputStream propertiesInputStream =
		    new FileInputStream(m_file);

		m_properties.load(propertiesInputStream);
	    }
	    catch (Exception e) {
		throw new GrinderException(
		    "Error loading properties file '" + m_file + "'", e);
	    }

	    // Allow overriding on command line.
	    m_properties.putAll(System.getProperties());
	}
    }

    public int getCollectSampleCount()
    {
	return m_collectSampleCount;
    }

    public void setCollectSampleCount(int i)
    {
	m_collectSampleCount = i;
    }

    public int getIgnoreSampleCount()
    {
	return m_ignoreSampleCount;
    }

    public void setIgnoreSampleCount(int i)
    {
	m_ignoreSampleCount = i;
    }

    public int getSampleInterval()
    {
	return m_sampleInterval;
    }

    public void setSampleInterval(int i)
    {
	m_sampleInterval = i;
    }

    public int getSignificantFigures()
    {
	return m_significantFigures;
    }

    public void setSignificantFigures(int i)
    {
	m_significantFigures = i;
    }

    public String getMulticastAddress()
    {
	return m_multicastAddress;
    }

    public void setMulticastAddress(String s)
    {
	m_multicastAddress = s;
    }

    public int getConsolePort()
    {
	return m_consolePort;
    }

    public void setConsolePort(int i)
    {
	m_consolePort = i;
    }

    public int getGrinderPort()
    {
	return m_grinderPort;
    }

    public void setGrinderPort(int i)
    {
	m_grinderPort = i;
    }
}
