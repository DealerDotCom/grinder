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

    private final GrinderProperties m_properties;
    private final PropertyChangeSupport m_changeSupport =
	new PropertyChangeSupport(this);

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
	m_properties = new GrinderProperties(file.getPath());

	m_collectSampleCount =
	    m_properties.getInt(COLLECT_SAMPLES_PROPERTY, 0);
	m_ignoreSampleCount = m_properties.getInt(IGNORE_SAMPLES_PROPERTY, 1);
	m_sampleInterval = m_properties.getInt(SAMPLE_INTERVAL_PROPERTY, 1000);
	m_significantFigures = m_properties.getInt(SIG_FIG_PROPERTY, 3);

	m_multicastAddress =
	    m_properties.getProperty(MULTICAST_ADDRESS_PROPERTY,
				     CommunicationDefaults.MULTICAST_ADDRESS);

	m_consolePort =
	    m_properties.getInt(CONSOLE_PORT_PROPERTY,
				CommunicationDefaults.CONSOLE_PORT);

	m_grinderPort =
	    m_properties.getInt(GRINDER_PORT_PROPERTY,
				CommunicationDefaults.GRINDER_PORT);
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

    public void save() throws GrinderException
    {
	m_properties.setInt(COLLECT_SAMPLES_PROPERTY, m_collectSampleCount);
	m_properties.setInt(IGNORE_SAMPLES_PROPERTY, m_ignoreSampleCount);
	m_properties.setInt(SAMPLE_INTERVAL_PROPERTY, m_sampleInterval);
	m_properties.setInt(SIG_FIG_PROPERTY, m_significantFigures);
	m_properties.setProperty(MULTICAST_ADDRESS_PROPERTY,
				 m_multicastAddress);
	m_properties.setInt(CONSOLE_PORT_PROPERTY, m_consolePort);
	m_properties.setInt(GRINDER_PORT_PROPERTY, m_grinderPort);

	m_properties.save();
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
