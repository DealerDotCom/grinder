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

package net.grinder.util;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Properties;

/**
 * @author Philip Aston
 * @version $Revision$
 */
public class GrinderProperties extends Properties
{
    private static final String PROPERTIES_FILENAME = "grinder.properties";

    private static GrinderProperties s_singleton;

    private GrinderProperties()
    {
    }

    private GrinderProperties(Properties defaults)
    {
	super(defaults);
    }

    public static GrinderProperties getProperties()
    {
	if (s_singleton == null) {
	    synchronized (GrinderProperties.class) {
		if (s_singleton == null) { // Double checked locking.
		    s_singleton =
			new GrinderProperties(getDefaultProperties());

		    InputStream propertiesInputStream = null;
	
		    try {
			propertiesInputStream =
			    new FileInputStream(PROPERTIES_FILENAME);
			s_singleton.load(propertiesInputStream);
		    }
		    catch (Exception e) {
			e.printStackTrace(System.err);
			System.err.println("Error loading properties file '" +
					   PROPERTIES_FILENAME +
					   "', using defaults.");
		    }

		    // Allow overriding on command line.
		    s_singleton.putAll(System.getProperties());
		}
	    }
	}

	return s_singleton;
    }

    public static GrinderProperties getPropertySubset(String prefix)
    {
	final GrinderProperties result = new GrinderProperties();
	final GrinderProperties allProperties = getProperties();

	final Enumeration propertyNames = allProperties.propertyNames();

	while (propertyNames.hasMoreElements()) {
	    final String name = (String)propertyNames.nextElement();

	    if (name.startsWith(prefix)) {
		result.setProperty(name.substring(prefix.length()),
				   allProperties.getProperty(name));
	    }
	}

	return result;	
    }

    public String getMandatoryProperty(String propertyName)
	throws GrinderException
    {
	final String s = getProperty(propertyName);

	if (s == null) {
	    throw new GrinderException("Mandatory property '" + propertyName +
				       "' not specified");
	}

	return s;	
    }

    public int getInt(String propertyName, int defaultValue)
    {
	final String s = getProperty(propertyName);

	if (s != null) {
	    try {
		return Integer.parseInt(s);
	    }
	    catch (NumberFormatException e) {
		System.err.println("Warning, property '" + propertyName +
				   "' does not specify an integer value");
	    }
	}

	return defaultValue;
    }

    public int getMandatoryInt(String propertyName)
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

    public short getShort(String propertyName, short defaultValue)
    {
	final String s = getProperty(propertyName);

	if (s != null) {
	    try {
		return Short.parseShort(s);
	    }
	    catch (NumberFormatException e) {
		System.err.println("Warning, property '" + propertyName +
				   "' does not specify a short value");
	    }
	}

	return defaultValue;
    }

    public short getMandatoryShort(String propertyName)
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

    public double getMandatoryDouble(String propertyName)
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

    public double getDouble(String propertyName, double defaultValue)
    {
	final String s = getProperty(propertyName);

	if (s != null) {
	    try {
		return Double.parseDouble(s);
	    }
	    catch (NumberFormatException e) {
		System.err.println("Warning, property '" + propertyName +
				   "' does not specify a double value");
	    }
	}

	return defaultValue;
    }

    public boolean getBoolean(String propertyName, boolean defaultValue)
    {
	final String s = getProperty(propertyName);

	if (s != null) {
	    return Boolean.valueOf(s).booleanValue();
	}

	return defaultValue;
    }

    public boolean getMandatoryBoolean(String propertyName)
	throws GrinderException
    {
	final String s = getProperty(propertyName);

	if (s == null) {
	    throw new GrinderException("Mandatory property '" + propertyName +
				       "' not specified");
	}

	return Boolean.valueOf(s).booleanValue();
    }

    private static Properties getDefaultProperties()
    {
	final Properties defaults = new Properties();
    
	// This really needs reviewing - not sure its a good idea.
	defaults.put("grinder.hostId", "0");
	defaults.put("grinder.cycleClass",
		       "net.grinder.plugin.simple.SimpleBmk");
	defaults.put("grinder.cycleParams",
		       "[paramA]a,[paramB]500,[paramC]10.2");
	defaults.put("grinder.jvm.path", "c:\\jdk1.2.2\\bin\\java");        
	defaults.put("grinder.jvm.args", "");
	defaults.put("grinder.jvms", "1");
	defaults.put("grinder.ms.arg", "-ms");
	defaults.put("grinder.ms", "16");
	defaults.put("grinder.mx.arg", "-mx");
	defaults.put("grinder.mx", "32");
	defaults.put("grinder.threads", "2");
	defaults.put("grinder.times", "3");
	defaults.put("grinder.initialWait", "false");
	defaults.put("grinder.multicastAddress", "228.1.1.1");
	defaults.put("grinder.multicastPort", "1234");
	defaults.put("grinder.reportToConsole", "false");
	defaults.put("grinder.console.multicastAddress", "228.1.1.2");
	defaults.put("grinder.console.multicastPort", "1234");
	defaults.put("grinder.logDirectory", ".");
	defaults.put("grinder.appendLog", "false");
	defaults.put("grinder.fileStats", "true");       
	defaults.put("grinder.sleepMillis", "0"); 
	defaults.put("grinder.initialSleepTimes", "0");

	return defaults;
    }
}
