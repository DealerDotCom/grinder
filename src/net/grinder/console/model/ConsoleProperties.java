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
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;

import net.grinder.common.GrinderException;
import net.grinder.common.GrinderProperties;
import net.grinder.communication.CommunicationDefaults;


/**
 * @author Philip Aston
 * @version $Revision$
 */
public class ConsoleProperties
{
    private final static String COLLECT_SAMPLES_PROPERTY = "numberToCollect";
    private final static String IGNORE_SAMPLES_PROPERTY = "numberToIgnore";
    private final static String SAMPLE_INTERVAL_PROPERTY = "sampleInterval";
    private final static String SIG_FIG_PROPERTY = "significantFigures";

    private final static String ADDRESS_PROPERTY = "multicastAddress";
    private final static String CONSOLE_PORT_PROPERTY = "consolePort";
    private final static String GRINDER_PORT_PROPERTY = "grinderPort";

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

	final GrinderProperties properties = new GrinderProperties();

	if (m_file.exists()) {
	    try {
		final InputStream inputStream = new FileInputStream(m_file);
		properties.load(inputStream);
	    }
	    catch (Exception e) {
		throw new GrinderException(
		    "Error loading properties file '" + m_file + "'", e);
	    }
	}

	// Allow overriding on command line.
	properties.putAll(System.getProperties());

	m_collectSampleCount = properties.getInt(COLLECT_SAMPLES_PROPERTY, 0);
	m_ignoreSampleCount = properties.getInt(IGNORE_SAMPLES_PROPERTY, 1);
	m_sampleInterval = properties.getInt(SAMPLE_INTERVAL_PROPERTY, 1000);
	m_significantFigures = properties.getInt(SIG_FIG_PROPERTY, 3);

	m_multicastAddress =
	    properties.getProperty(ADDRESS_PROPERTY,
				   CommunicationDefaults.MULTICAST_ADDRESS);

	m_consolePort = properties.getInt(CONSOLE_PORT_PROPERTY,
					  CommunicationDefaults.CONSOLE_PORT);

	m_grinderPort = properties.getInt(GRINDER_PORT_PROPERTY,
					  CommunicationDefaults.GRINDER_PORT);
    }

    /**
     * Copy constructor.
     **/
    public ConsoleProperties(final ConsoleProperties consoleProperties)
    {
	m_file = consoleProperties.m_file;
	set(consoleProperties);
    }

    /**
     * Assignment.
     **/
    public final void set(final ConsoleProperties consoleProperties)
    {
	m_collectSampleCount = consoleProperties.m_collectSampleCount;
	m_ignoreSampleCount = consoleProperties.m_ignoreSampleCount;
	m_sampleInterval = consoleProperties.m_sampleInterval;
	m_significantFigures = consoleProperties.m_significantFigures;
	m_multicastAddress = consoleProperties.m_multicastAddress;
	m_consolePort = consoleProperties.m_consolePort;
	m_grinderPort = consoleProperties.m_grinderPort;
    }

    public void save() throws IOException
    {
	final GrinderProperties properties = new GrinderProperties();

	properties.setInt(COLLECT_SAMPLES_PROPERTY, m_collectSampleCount);
	properties.setInt(IGNORE_SAMPLES_PROPERTY, m_ignoreSampleCount);
	properties.setInt(SAMPLE_INTERVAL_PROPERTY, m_sampleInterval);
	properties.setInt(SIG_FIG_PROPERTY, m_significantFigures);

	properties.setProperty(ADDRESS_PROPERTY, m_multicastAddress);
	properties.setInt(CONSOLE_PORT_PROPERTY, m_consolePort);
	properties.setInt(GRINDER_PORT_PROPERTY, m_grinderPort);

	final OutputStream outputStream = new FileOutputStream(m_file);
	properties.store(outputStream, "Grinder Console Properties");
    }

    public final int getCollectSampleCount()
    {
	return m_collectSampleCount;
    }

    public final void setCollectSampleCount(int i)
    {
	m_collectSampleCount = i;
    }

    public final int getIgnoreSampleCount()
    {
	return m_ignoreSampleCount;
    }

    public final void setIgnoreSampleCount(int i)
    {
	m_ignoreSampleCount = i;
    }

    public final int getSampleInterval()
    {
	return m_sampleInterval;
    }

    public final void setSampleInterval(int i)
    {
	m_sampleInterval = i;
    }

    public final int getSignificantFigures()
    {
	return m_significantFigures;
    }

    public final void setSignificantFigures(int i)
    {
	m_significantFigures = i;
    }

    public final String getMulticastAddress()
    {
	return m_multicastAddress;
    }

    public final void setMulticastAddress(String s)
    {
	m_multicastAddress = s;
    }

    public final int getConsolePort()
    {
	return m_consolePort;
    }

    public final void setConsolePort(int i)
    {
	m_consolePort = i;
    }

    public final int getGrinderPort()
    {
	return m_grinderPort;
    }

    public final void setGrinderPort(int i)
    {
	m_grinderPort = i;
    }
}
