// The Grinder
// Copyright (C) 2001  Paco Gomez
// Copyright (C) 2001  Philip Aston

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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import net.grinder.common.GrinderException;
import net.grinder.common.GrinderProperties;
import net.grinder.common.TestImplementation;
import net.grinder.plugininterface.GrinderPlugin;
import net.grinder.plugininterface.PluginProcessContext;


/**
 * This class contains properties parsing logic that is required from
 * more than one place.
 *
 * @author Philip Aston
 * @version $Revision$
 */
public class PropertiesHelper
{
    private static final String DEFAULT_FILENAME = "grinder.properties";
    private static final String TEST_PREFIX = "grinder.test";

    private final GrinderProperties m_properties;

    public PropertiesHelper() throws GrinderException
    {
	this((String)null);
    }

    public PropertiesHelper(String filename) throws GrinderException
    {
	m_properties = new GrinderProperties();

	try {
	    final InputStream propertiesInputStream =
		new FileInputStream(
		    filename == null ? DEFAULT_FILENAME : filename);

	    m_properties.load(propertiesInputStream);
	}
	catch (Exception e) {
	    throw new GrinderException(
		"Error loading properties file '" + filename + "'", e);
	}

	// Allow overriding on command line.
	m_properties.putAll(System.getProperties());
    }

    /** Constructor for tests. **/
    PropertiesHelper(GrinderProperties properties)
    {
	m_properties = properties;
    }

    public GrinderProperties getProperties()
    {
	return m_properties;
    }

    public GrinderPlugin instantiatePlugin(PluginProcessContext processContext)
	throws GrinderException
    {
	final String pluginClassName =
	    m_properties.getMandatoryProperty("grinder.plugin");

	try {
	    final Class pluginClass = Class.forName(pluginClassName);

	    if (!GrinderPlugin.class.isAssignableFrom(pluginClass)) {
		throw new GrinderException(
		    "The specified plugin class ('" + pluginClass.getName() +
		    "') does not implement the interface '" +
		    GrinderPlugin.class.getName() + "'");
	    }

	    final GrinderPlugin plugin =
		(GrinderPlugin)pluginClass.newInstance();

	    plugin.initialize(processContext, getPropertiesTestSet());

	    return plugin;
	}
	catch(ClassNotFoundException e){
	    throw new GrinderException(
		"The specified plug-in class was not found.", e);
	}
	catch (Exception e){
	    throw new GrinderException(
		"An instance of the specified plug-in class " +
		"could not be created.", e);
	}
    }

    private Set getPropertiesTestSet() throws GrinderException
    {
	final Map tests = new HashMap();

	final Iterator nameIterator = m_properties.keySet().iterator();

	while (nameIterator.hasNext()) {
	    final String name = (String)nameIterator.next();
		
	    if (!name.startsWith(TEST_PREFIX)) {
		continue;	// Not a test property.
	    }

	    final int nextSeparator = name.indexOf('.', TEST_PREFIX.length());

	    final int testNumber;

	    try {
		testNumber =
		    Integer.parseInt(name.substring(TEST_PREFIX.length(),
						    nextSeparator));
	    }
	    catch (Exception e) {
		throw new GrinderException(
		    "Could not resolve test number from property '" +
		    name + ".");
	    }

	    final Integer testNumberInteger = new Integer(testNumber);

	    if (tests.containsKey(testNumberInteger)) {
		continue;	// Already parsed.
	    }

	    final String description =
		m_properties.getProperty(
		    getTestPropertyName(testNumber, "description"), null);

	    final GrinderProperties parameters =
		m_properties.getPropertySubset(
		    getTestPropertyName(testNumber, "parameter") + '.');

	    tests.put(testNumberInteger, new TestImplementation(testNumber,
								description,
								parameters));
	}

	return new HashSet(tests.values());
    }

    public static String getTestPropertyName(int testNumber,
					     String unqualifiedName)
    {
	return TEST_PREFIX + testNumber + '.' + unqualifiedName;
    }
}
