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

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
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
    public final static String COLLECT_SAMPLES_PROPERTY = "numberToCollect";
    public final static String IGNORE_SAMPLES_PROPERTY = "numberToIgnore";
    public final static String SAMPLE_INTERVAL_PROPERTY = "sampleInterval";
    public final static String SIG_FIG_PROPERTY = "significantFigures";
    public final static String MULTICAST_ADDRESS_PROPERTY = "multicastAddress";
    public final static String CONSOLE_PORT_PROPERTY = "consolePort";
    public final static String GRINDER_PORT_PROPERTY = "grinderPort";

    private final PropertyChangeSupport m_changeSupport =
	new PropertyChangeSupport(this);
    private final File m_file;

    private int m_collectSampleCount;
    private int m_ignoreSampleCount;
    private int m_sampleInterval;
    private int m_significantFigures;

    private String m_multicastAddress;
    private int m_consolePort;
    private int m_grinderPort;

    /**
     * Construct a ConsoleProperties backed by the file .grinder_console in the user's home directory.
     **/
    public ConsoleProperties() throws GrinderException
    {
	this(new File(getHomeDirectory(), ".grinder_console"));
    }

    /**
     * Return the user's home directory, or the location of the Java
     * installation on platforms that do not support the concept.
     **/
    private final static String getHomeDirectory()
    {
	final String home = System.getProperty("user.home");
	return home != null ? home : System.getProperty("java.home");
    }
    
    /**
     * Construct a ConsoleProperties backed by the given file.
     * @param file The properties file.
     **/
    public ConsoleProperties(File file) throws GrinderException
    {
	m_file = file;

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
	    properties.getProperty(MULTICAST_ADDRESS_PROPERTY,
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

    public void addPropertyChangeListener(PropertyChangeListener listener)
    {
	m_changeSupport.addPropertyChangeListener(listener);
    }

    public void addPropertyChangeListener(String property,
					  PropertyChangeListener listener)
    {
	m_changeSupport.addPropertyChangeListener(property, listener);
    }

    /**
     * Assignment.
     **/
    public final void set(final ConsoleProperties consoleProperties)
    {
	setCollectSampleCount(consoleProperties.m_collectSampleCount);
	setIgnoreSampleCount(consoleProperties.m_ignoreSampleCount);
	setSampleInterval(consoleProperties.m_sampleInterval);
	setSignificantFigures(consoleProperties.m_significantFigures);
	setMulticastAddress(consoleProperties.m_multicastAddress);
	setConsolePort(consoleProperties.m_consolePort);
	setGrinderPort(consoleProperties.m_grinderPort);
    }

    public void save() throws IOException
    {
	final GrinderProperties properties = new GrinderProperties();

	properties.setInt(COLLECT_SAMPLES_PROPERTY, m_collectSampleCount);
	properties.setInt(IGNORE_SAMPLES_PROPERTY, m_ignoreSampleCount);
	properties.setInt(SAMPLE_INTERVAL_PROPERTY, m_sampleInterval);
	properties.setInt(SIG_FIG_PROPERTY, m_significantFigures);

	properties.setProperty(MULTICAST_ADDRESS_PROPERTY, m_multicastAddress);
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
	if (i != m_collectSampleCount) {
	    final int old = m_collectSampleCount;
	    m_collectSampleCount = i;
	    m_changeSupport.firePropertyChange(COLLECT_SAMPLES_PROPERTY,
					       old, m_collectSampleCount);
	}
    }

    public final int getIgnoreSampleCount()
    {
	return m_ignoreSampleCount;
    }

    public final void setIgnoreSampleCount(int i)
    {
	if (i != m_ignoreSampleCount) {
	    final int old = m_ignoreSampleCount;
	    m_ignoreSampleCount = i;
	    m_changeSupport.firePropertyChange(IGNORE_SAMPLES_PROPERTY,
					       old, m_ignoreSampleCount);
	}
    }

    public final int getSampleInterval()
    {
	return m_sampleInterval;
    }

    public final void setSampleInterval(int i)
    {
	if (i != m_sampleInterval) {
	    final int old = m_sampleInterval;
	    m_sampleInterval = i;
	    m_changeSupport.firePropertyChange(SAMPLE_INTERVAL_PROPERTY,
					       old, m_sampleInterval);
	}
    }

    public final int getSignificantFigures()
    {
	return m_significantFigures;
    }

    public final void setSignificantFigures(int i)
    {
	if (i != m_significantFigures) {
	    final int old = m_significantFigures;
	    m_significantFigures = i;
	    m_changeSupport.firePropertyChange(SIG_FIG_PROPERTY,
					       old, m_significantFigures);
	}
    }

    public final String getMulticastAddress()
    {
	return m_multicastAddress;
    }

    public final void setMulticastAddress(String s)
    {
	if (s != m_multicastAddress) {
	    final String old = m_multicastAddress;
	    m_multicastAddress = s;
	    m_changeSupport.firePropertyChange(MULTICAST_ADDRESS_PROPERTY,
					       old, m_multicastAddress);
	}
    }

    public final int getConsolePort()
    {
	return m_consolePort;
    }

    public final void setConsolePort(int i)
    {
	if (i != m_consolePort) {
	    final int old = m_consolePort;
	    m_consolePort = i;
	    m_changeSupport.firePropertyChange(CONSOLE_PORT_PROPERTY,
					       old, m_consolePort);
	}
    }

    public final int getGrinderPort()
    {
	return m_grinderPort;
    }

    public final void setGrinderPort(int i)
    {
	if (i != m_grinderPort) {
	    final int old = m_grinderPort;
	    m_grinderPort = i;
	    m_changeSupport.firePropertyChange(GRINDER_PORT_PROPERTY,
					       old, m_grinderPort);
	}
    }
}
