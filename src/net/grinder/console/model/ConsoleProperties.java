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
import java.net.InetAddress;
import java.net.UnknownHostException;

import net.grinder.common.GrinderException;
import net.grinder.common.GrinderProperties;
import net.grinder.communication.CommunicationDefaults;
import net.grinder.console.common.DisplayMessageConsoleException;


/**
 * Class encapsulating the console options.
 *
 * @author Philip Aston
 * @version $Revision$
 */
public class ConsoleProperties
{
    /** Property name **/
    public final static String COLLECT_SAMPLES_PROPERTY = 
	"grinder.console.numberToCollect";

    /** Property name **/
    public final static String IGNORE_SAMPLES_PROPERTY =
	"grinder.console.numberToIgnore";

    /** Property name **/
    public final static String SAMPLE_INTERVAL_PROPERTY =
	"grinder.console.sampleInterval";

    /** Property name **/
    public final static String SIG_FIG_PROPERTY =
	"grinder.console.significantFigures";

    /** Property name **/
    public final static String MULTICAST_ADDRESS_PROPERTY =
	"grinder.multicastAddress";

    /** Property name **/
    public final static String CONSOLE_PORT_PROPERTY = 
	"grinder.console.multicastPort";

    /** Property name **/
    public final static String GRINDER_PORT_PROPERTY =
	"grinder.multicastPort";

    private final PropertyChangeSupport m_changeSupport =
	new PropertyChangeSupport(this);

    private int m_collectSampleCount;
    private int m_ignoreSampleCount;
    private int m_sampleInterval;
    private int m_significantFigures;

    /**
     *We hang onto the address as a string so we can copy and
     * externalise it reasonably. 
     **/
    private String m_multicastAddressString;
    private int m_consolePort;
    private int m_grinderPort;

    /**
     * Use to save and load properties, and to keep track of the
     * associated file.
     **/
     private final GrinderProperties m_properties;;

    /**
     * Construct a ConsoleProperties backed by the given file.
     * @param file The properties file.
     *
     * @throws GrinderException If the properties file cannot be read.
     * @throws DisplayMessageConsoleException If the properties file contains invalid data.
     **/
    public ConsoleProperties(File file) throws GrinderException
    {
	m_properties = new GrinderProperties(file);

	setCollectSampleCount(
	    m_properties.getInt(COLLECT_SAMPLES_PROPERTY, 0));
	setIgnoreSampleCount(m_properties.getInt(IGNORE_SAMPLES_PROPERTY, 1));
	setSampleInterval(m_properties.getInt(SAMPLE_INTERVAL_PROPERTY, 1000));
	setSignificantFigures(m_properties.getInt(SIG_FIG_PROPERTY, 3));

	setMulticastAddress(
	    m_properties.getProperty(MULTICAST_ADDRESS_PROPERTY,
				     CommunicationDefaults.MULTICAST_ADDRESS));

	setConsolePort(
	    m_properties.getInt(CONSOLE_PORT_PROPERTY,
				CommunicationDefaults.CONSOLE_PORT));

	setGrinderPort(
	    m_properties.getInt(GRINDER_PORT_PROPERTY,
				CommunicationDefaults.GRINDER_PORT));
    }

    /**
     * Copy constructor. Does not copy property change listeners.
     *
     * @param properties The properties to copy.
     **/
    public ConsoleProperties(ConsoleProperties properties)
    {
	m_properties = properties.m_properties;
	set(properties);
    }

    /**
     * Assignment. Does not copy property change listeners, nor the
     * associated file.
     *
     * @param properties The properties to copy.
     **/
    public void set(ConsoleProperties properties)
    {
	setCollectSampleCountNoCheck(properties.m_collectSampleCount);
	setIgnoreSampleCountNoCheck(properties.m_ignoreSampleCount);
	setSampleIntervalNoCheck(properties.m_sampleInterval);
	setSignificantFiguresNoCheck(properties.m_significantFigures);
	setMulticastAddressNoCheck(properties.m_multicastAddressString);
	setConsolePortNoCheck(properties.m_consolePort);
	setGrinderPortNoCheck(properties.m_grinderPort);
    }

    /**
     * Add a <code>PropertyChangeListener</code>.
     *
     * @param listener The listener.
     **/
    public void addPropertyChangeListener(PropertyChangeListener listener)
    {
	m_changeSupport.addPropertyChangeListener(listener);
    }

    /**
     * Add a <code>PropertyChangeListener</code> which listens to a
     * particular property.
     *
     * @param property The property.
     * @param listener The listener.
     **/
    public void addPropertyChangeListener(String property,
					  PropertyChangeListener listener)
    {
	m_changeSupport.addPropertyChangeListener(property, listener);
    }

    /**
     * Save to the associated file.
     **/
    public void save() throws GrinderException
    {
	m_properties.setInt(COLLECT_SAMPLES_PROPERTY, m_collectSampleCount);
	m_properties.setInt(IGNORE_SAMPLES_PROPERTY, m_ignoreSampleCount);
	m_properties.setInt(SAMPLE_INTERVAL_PROPERTY, m_sampleInterval);
	m_properties.setInt(SIG_FIG_PROPERTY, m_significantFigures);
	m_properties.setProperty(MULTICAST_ADDRESS_PROPERTY,
				 m_multicastAddressString);
	m_properties.setInt(CONSOLE_PORT_PROPERTY, m_consolePort);
	m_properties.setInt(GRINDER_PORT_PROPERTY, m_grinderPort);

	m_properties.save();
    }

    /**
     * Get the number of samples to collect.
     *
     * @returns The number.
     **/
    public final int getCollectSampleCount()
    {
	return m_collectSampleCount;
    }

    /**
     * Set the number of samples to collect.
     *
     * @param n The number. 0 => forever.
     * @throws DisplayMessageConsoleException If the number is negative.
     **/
    public final void setCollectSampleCount(int n)
	throws DisplayMessageConsoleException
    {
	if (n < 0) {
	    throw new DisplayMessageConsoleException(
		"collectNegativeError.text",
		"You must collect at least one sample, " +
		"zero means \"forever\"");
	}

	setCollectSampleCountNoCheck(n);
    }

    /**
     * Set the number of samples to collect. 
     *
     * @param n The number. 0 => forever.
     **/
    private final void setCollectSampleCountNoCheck(int n)
    {
	if (n != m_collectSampleCount) {
	    final int old = m_collectSampleCount;
	    m_collectSampleCount = n;
	    m_changeSupport.firePropertyChange(COLLECT_SAMPLES_PROPERTY,
					       old, m_collectSampleCount);
	}
    }

    /**
     * Get the number of samples to ignore.
     *
     * @returns The number.
     **/
    public final int getIgnoreSampleCount()
    {
	return m_ignoreSampleCount;
    }

    /**
     * Set the number of samples to collect.
     *
     * @param n The number. Must be at least 1.
     * @throws DisplayMessageConsoleException If the number is negative or zero.
     **/
    public final void setIgnoreSampleCount(int n)
	throws DisplayMessageConsoleException
    {
	if (n <= 0) {
	    throw new DisplayMessageConsoleException(
		"ignoreLessThanOneError.text",
		"You must ignore at least the first sample");
	}

	setIgnoreSampleCountNoCheck(n);
    }

    /**
     * Set the number of samples to collect.
     *
     * @param n The number. Must be at least 1.
     **/
    public final void setIgnoreSampleCountNoCheck(int n)
    {
	if (n != m_ignoreSampleCount) {
	    final int old = m_ignoreSampleCount;
	    m_ignoreSampleCount = n;
	    m_changeSupport.firePropertyChange(IGNORE_SAMPLES_PROPERTY,
					       old, m_ignoreSampleCount);
	}
    }

    /**
     * Get the sample interval.
     *
     * @returns The interval in milliseconds.
     **/
    public final int getSampleInterval()
    {
	return m_sampleInterval;
    }

    /**
     * Set the sample interval.
     *
     * @param interval The interval in milliseconds.
     * @throws DisplayMessageConsoleException If the number is negative or zero.
     **/
    public final void setSampleInterval(int interval)
	throws DisplayMessageConsoleException
    {
	if (interval <= 0) {
	    throw new DisplayMessageConsoleException(
		"intervalLessThanOneError.text",
		"Minimum sample interval is 1 ms");
	}

	setSampleIntervalNoCheck(interval);
    }

    /**
     * Set the sample interval.
     *
     * @param interval The interval in milliseconds.
     **/
    public final void setSampleIntervalNoCheck(int interval)
    {
	if (interval != m_sampleInterval) {
	    final int old = m_sampleInterval;
	    m_sampleInterval = interval;
	    m_changeSupport.firePropertyChange(SAMPLE_INTERVAL_PROPERTY,
					       old, m_sampleInterval);
	}
    }

    /**
     * Get the number of significant figures.
     *
     * @returns The number of significant figures.
     **/
    public final int getSignificantFigures()
    {
	return m_significantFigures;
    }

    /**
     * Set the number of significant figures.
     *
     * @param n The number of significant figures.
     * @throws DisplayMessageConsoleException If the number is negative.
     **/
    public final void setSignificantFigures(int n)
	throws DisplayMessageConsoleException
    {
	if (n <= 0) {
	    throw new DisplayMessageConsoleException(
		"significantFiguresNegativeError.text",
		"Number of significant figures cannot be negative");
	}

	setSignificantFiguresNoCheck(n);
    }


    /**
     * Set the number of significant figures.
     *
     * @param n The number of significant figures.
     **/
    public final void setSignificantFiguresNoCheck(int n)
    {
	if (n != m_significantFigures) {
	    final int old = m_significantFigures;
	    m_significantFigures = n;
	    m_changeSupport.firePropertyChange(SIG_FIG_PROPERTY,
					       old, m_significantFigures);
	}
    }

    /**
     * Get the multicast address as a string.
     *
     * @returns The address.
     **/
    public final String getMulticastAddress()
    {
	return m_multicastAddressString;
    }

    /**
     * Set the multicast address.
     *
     * @param String s Either a machine name or the IP address.
     * @throws DisplayMessageConsoleException If the multicast address is
     * not valid.
     **/
    public final void setMulticastAddress(String s)
	throws DisplayMessageConsoleException
    {
	final InetAddress newAddress;

	try {
	    newAddress = InetAddress.getByName(s);
	}
	catch (UnknownHostException e) {
	    throw new DisplayMessageConsoleException(
		"unknownHostError.text", "Unknown hostname");
	}

	if (!newAddress.isMulticastAddress()) {
	    throw new DisplayMessageConsoleException(
		"invalidMulticastAddressError.text",
		"Invalid multicast address");
	}

	setMulticastAddressNoCheck(s);
    }

    /**
     * Set the multicast address.
     *
     * @param String s Either a machine name or the IP address.
     **/
    public final void setMulticastAddressNoCheck(String s)
    {
	if (!s.equals(m_multicastAddressString)) {
	    final String old = m_multicastAddressString;
	    m_multicastAddressString = s;
	    m_changeSupport.firePropertyChange(MULTICAST_ADDRESS_PROPERTY,
					       old, m_multicastAddressString);
	}
    }

    /**
     * Get the Console multicast port.
     *
     * @returns The port.
     **/
    public final int getConsolePort()
    {
	return m_consolePort;
    }

    /**
     * Set the Console multicast port.
     *
     * @param port The port number.
     * @throws DisplayMessageConsoleException If the port number is not sensible.
     **/
    public final void setConsolePort(int i)
	throws DisplayMessageConsoleException
    {
	assertValidPort(i);
	setConsolePortNoCheck(i);
    }

    /**
     * Set the Console multicast port.
     *
     * @param port The port number.
     **/
    public final void setConsolePortNoCheck(int i)
    {
	if (i != m_consolePort) {
	    final int old = m_consolePort;
	    m_consolePort = i;
	    m_changeSupport.firePropertyChange(CONSOLE_PORT_PROPERTY,
					       old, m_consolePort);
	}
    }

    /**
     * Get the Grinder process multicast port.
     *
     * @returns The port.
     **/
    public final int getGrinderPort()
    {
	return m_grinderPort;
    }

    /**
     * Set the Grinder process multicast port.
     *
     * @param port The port number.
     * @throws DisplayMessageConsoleException If the port number is not sensible.
     **/
    public final void setGrinderPort(int port)
	throws DisplayMessageConsoleException
    {
	assertValidPort(port);
	setGrinderPortNoCheck(port);
    }

    /**
     * Set the Grinder process multicast port.
     *
     * @param port The port number.
     **/
    public final void setGrinderPortNoCheck(int port)
    {
	if (port != m_grinderPort) {
	    final int old = m_grinderPort;
	    m_grinderPort = port;
	    m_changeSupport.firePropertyChange(GRINDER_PORT_PROPERTY,
					       old, m_grinderPort);
	}
    }

    /**
     * Check the given port number is sensible.
     *
     * @param port The port number.
     * @throws DisplayMessageConsoleException If the port number is not sensible.
     **/
    private void assertValidPort(int port)
	throws DisplayMessageConsoleException
    {
	if (port < 0 || port > CommunicationDefaults.MAX_PORT) {
	    throw new DisplayMessageConsoleException(
		"invalidPortNumberError.text", 
		"Port numbers should be in the range [0, 65535]");
	}
    }
}
