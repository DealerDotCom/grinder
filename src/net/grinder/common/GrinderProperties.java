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

package net.grinder.common;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Enumeration;
import java.util.Properties;


/**
 * Extend {@link java.util.Properties} to add typesafe accessors.
 * Has an optional associated file.
 *
 * @author Philip Aston
 * @version $Revision$
 */
public class GrinderProperties extends Properties
{
    private static final String DEFAULT_FILENAME = "grinder.properties";

    private PrintWriter m_errorWriter = new PrintWriter(System.err, true);
    private final File m_file;

    /**
     * Construct an empty GrinderProperties with no associated file.
     **/
    public GrinderProperties()
    {
	m_file = null;
    }

    /**
     * Construct a GrinderProperties, reading initial values from the specified file. System properties
     * beginning with "<code>grinder.</code>"are also added to allow values to be overriden on the command line.
     * @param file The file to read the properties from. null => use grinder.properties.
     **/
    public GrinderProperties(File file) throws GrinderException
    {
	m_file = file != null ? file : new File(DEFAULT_FILENAME);

	if (m_file.exists()) {
	    try {
		final InputStream propertiesInputStream =
		    new FileInputStream(m_file);

		load(propertiesInputStream);
	    }
	    catch (IOException e) {
		throw new GrinderException(
		    "Error loading properties file '" + m_file.getPath() + "'",
		    e);
	    }
	}

	final Enumeration systemProperties =
	    System.getProperties().propertyNames();

	while (systemProperties.hasMoreElements()) {
	    final String name = (String)systemProperties.nextElement();

	    if (name.startsWith("grinder.")) {
		put(name, System.getProperty(name));
	    }
	}
    }

    /**
     * Save our properties to our file.
     *
     * @throws GrinderException If there is no file associated with this {@link GrinderProperties}.
     * @throws GrinderException With an nested IOException if there was an error writing to the file.
     **/
    public final void save() throws GrinderException
    {
	if (m_file == null) {
	    throw new GrinderException("No associated file");
	}

	try {
	    final OutputStream outputStream = new FileOutputStream(m_file);
	    store(outputStream, "Grinder Console Properties");
	    outputStream.close();
	}
	catch (IOException e) {
	    throw new GrinderException(
		"Error writing properties file '" + m_file.getPath() + "'", e);
	}
    }
    

    /**
     * Set a writer to report warnings to.
     **/
    public final void setErrorWriter(PrintWriter writer)
    {
	m_errorWriter = writer;
    }

    /**
     * Return a new GrinderProperties that contains the subset of our Properties which begin with the specified prefix.
     * @param prefix The prefix.
     **/
    public final synchronized GrinderProperties
	getPropertySubset(String prefix)
    {
	final GrinderProperties result = new GrinderProperties();

	final Enumeration propertyNames = propertyNames();

	while (propertyNames.hasMoreElements()) {
	    final String name = (String)propertyNames.nextElement();

	    if (name.startsWith(prefix)) {
		result.setProperty(name.substring(prefix.length()),
				   getProperty(name));
	    }
	}

	return result;	
    }

    /**
     * Get the value of the property with the given name.
     * @param propertyName The property name.
     * @throws GrinderException If a property with the given name does not exist.
     **/
    public final String getMandatoryProperty(String propertyName)
	throws GrinderException
    {
	final String s = getProperty(propertyName);

	if (s == null) {
	    throw new GrinderException("Mandatory property '" + propertyName +
				       "' not specified");
	}

	return s;	
    }

    /**
     * Get the value of the property with the given name, return the
     * value as an <code>int</code>.
     * @param propertyName The property name.
     * @param defaultValue The value to return if a property with the
     * given name does not exist or is not an integer.
     **/
    public final int getInt(String propertyName, int defaultValue)
    {
	final String s = getProperty(propertyName);

	if (s != null) {
	    try {
		return Integer.parseInt(s);
	    }
	    catch (NumberFormatException e) {
		m_errorWriter.println("Warning, property '" + propertyName +
				      "' does not specify an integer value");
	    }
	}

	return defaultValue;
    }

    /**
     * Get the value of the property with the given name, return the
     * value as an <code>int</code>.
     * @param propertyName The property name.
     * @throws GrinderException If a property with the given name does
     * not exist or is not an integer.
     **/
    public final int getMandatoryInt(String propertyName)
	throws GrinderException
    {
	final String s = getProperty(propertyName);

	if (s == null) {
	    throw new GrinderException("Mandatory property '" + propertyName +
				       "' not specified");
	}
	
	try {
	    return Integer.parseInt(s);
	}
	catch (NumberFormatException e) {
	    throw new GrinderException("Mandatory property '" + propertyName +
				       "' does not specify an integer value");
	}
    }

    /**
     * Set the property with the given name to an <code>int</code>
     * value.
     * @param propertyName The property name.
     * @param value The value to set.
     **/
    public final void setInt(String propertyName, int value)
    {
	setProperty(propertyName, Integer.toString(value));
    }


    /**
     * Get the value of the property with the given name, return the
     * value as a <code>long</code>.
     * @param propertyName The property name.
     * @param defaultValue The value to return if a property with the
     * given name does not exist or is not a long.
     **/
    public final long getLong(String propertyName, long defaultValue)
    {
	final String s = getProperty(propertyName);

	if (s != null) {
	    try {
		return Long.parseLong(s);
	    }
	    catch (NumberFormatException e) {
		m_errorWriter.println("Warning, property '" + propertyName +
				      "' does not specify an integer value");
	    }
	}

	return defaultValue;
    }

    /**
     * Get the value of the property with the given name, return the
     * value as a <code>long</code>.
     * @param propertyName The property name.
     * @throws GrinderException If a property with the given name does
     * not exist or is not a long.
     **/
    public final long getMandatoryLong(String propertyName)
	throws GrinderException
    {
	final String s = getProperty(propertyName);

	if (s == null) {
	    throw new GrinderException("Mandatory property '" + propertyName +
				       "' not specified");
	}
	
	try {
	    return Long.parseLong(s);
	}
	catch (NumberFormatException e) {
	    throw new GrinderException("Mandatory property '" + propertyName +
				       "' does not specify a long value");
	}
    }

    /**
     * Set the property with the given name to a <code>long</code>
     * value.
     * @param propertyName The property name.
     * @param value The value to set.
     **/
    public final void setLong(String propertyName, long value)
    {
	setProperty(propertyName, Long.toString(value));
    }

    /**
     * Get the value of the property with the given name, return the
     * value as a <code>short</code>.
     * @param propertyName The property name.
     * @param defaultValue The value to return if a property with the
     * given name does not exist or is not a short.
     **/
    public final short getShort(String propertyName, short defaultValue)
    {
	final String s = getProperty(propertyName);

	if (s != null) {
	    try {
		return Short.parseShort(s);
	    }
	    catch (NumberFormatException e) {
		m_errorWriter.println("Warning, property '" + propertyName +
				      "' does not specify a short value");
	    }
	}

	return defaultValue;
    }

    /**
     * Get the value of the property with the given name, return the
     * value as a <code>short</code>.
     * @param propertyName The property name.
     * @throws GrinderException If a property with the given name does
     * not exist or is not a short.
     **/
    public final short getMandatoryShort(String propertyName)
	throws GrinderException
    {
	final String s = getProperty(propertyName);

	if (s == null) {
	    throw new GrinderException("Mandatory property '" + propertyName +
				       "' not specified");
	}
	
	try {
	    return Short.parseShort(s);
	}
	catch (NumberFormatException e) {
	    throw new GrinderException("Mandatory property '" + propertyName +
				       "' does not specify a short value");
	}
    }

    /**
     * Set the property with the given name to a <code>short</code>
     * value.
     * @param propertyName The property name.
     * @param value The value to set.
     **/
    public final void setShort(String propertyName, short value)
    {
	setProperty(propertyName, Short.toString(value));
    }

    /**
     * Get the value of the property with the given name, return the
     * value as a <code>double</code>.
     * @param propertyName The property name.
     * @param defaultValue The value to return if a property with the
     * given name does not exist or is not a double.
     **/
    public final double getDouble(String propertyName, double defaultValue)
    {
	final String s = getProperty(propertyName);

	if (s != null) {
	    try {
		return Double.parseDouble(s);
	    }
	    catch (NumberFormatException e) {
		m_errorWriter.println("Warning, property '" + propertyName +
				      "' does not specify a double value");
	    }
	}

	return defaultValue;
    }

    /**
     * Get the value of the property with the given name, return the
     * value as a <code>double</code>.
     * @param propertyName The property name.
     * @throws GrinderException If a property with the given name does
     * not exist or is not a double.
     **/
    public final double getMandatoryDouble(String propertyName)
	throws GrinderException
    {
	final String s = getProperty(propertyName);

	if (s == null) {
	    throw new GrinderException("Mandatory property '" + propertyName +
				       "' not specified");
	}
	
	try {
	    return Double.parseDouble(s);
	}
	catch (NumberFormatException e) {
	    throw new GrinderException("Mandatory property '" + propertyName +
				       "' does not specify a double value");
	}
    }

    /**
     * Set the property with the given name to a <code>double</code>
     * value.
     * @param propertyName The property name.
     * @param value The value to set.
     **/
    public final void setDouble(String propertyName, double value)
    {
	setProperty(propertyName, Double.toString(value));
    }

    /**
     * Get the value of the property with the given name, return the
     * value as a <code>boolean</code>.
     * @param propertyName The property name.
     * @param defaultValue The value to return if a property with the
     * given name does not exist.
     **/
    public final boolean getBoolean(String propertyName, boolean defaultValue)
    {
	final String s = getProperty(propertyName);

	if (s != null) {
	    return Boolean.valueOf(s).booleanValue();
	}

	return defaultValue;
    }


    /**
     * Get the value of the property with the given name, return the
     * value as a <code>boolean</code>.
     * @param propertyName The property name.
     * @throws GrinderException If a property with the given name does
     * not exist.
     **/
    public final boolean getMandatoryBoolean(String propertyName)
	throws GrinderException
    {
	final String s = getProperty(propertyName);

	if (s == null) {
	    throw new GrinderException("Mandatory property '" + propertyName +
				       "' not specified");
	}

	return Boolean.valueOf(s).booleanValue();
    }

    /**
     * Set the property with the given name to a <code>boolean</code>
     * value.
     * @param propertyName The property name.
     * @param value The value to set.
     **/
    public final void setBoolean(String propertyName, boolean value)
    {
	setProperty(propertyName, new Boolean(value).toString());
    }
}
