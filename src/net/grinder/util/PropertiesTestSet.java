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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import net.grinder.plugininterface.PluginException;
import net.grinder.plugininterface.TestDefinition;
import net.grinder.plugininterface.TestSetPlugin;


/**
 * Package scope.
 *
 * @author Philip Aston
 * @version $Revision$
 */ 
class PropertiesTestSet implements TestSetPlugin
{
    private class Test implements TestDefinition
    {
	private final Integer m_testNumber;
	private final String m_description;
	private final GrinderProperties m_parameters;

	public Test(Integer testNumber, String description,
		    GrinderProperties parameters)
	{
	    m_testNumber = testNumber;
	    m_description = description;
	    m_parameters = parameters;
	}

	public Integer getTestNumber()
	{
	    return m_testNumber;
	}

	public String getDescription()
	{
	    return m_description;
	}

	public GrinderProperties getParameters()
	{
	    return m_parameters;
	}
    }

    private final Map m_tests = new HashMap();
    private final static String TEST_PREFIX = "grinder.test";

    public static String getTestPropertyName(Integer testNumber,
					     String unqualifiedName)
    {
	return TEST_PREFIX + testNumber + '.' + unqualifiedName;
    }

    public PropertiesTestSet(GrinderProperties properties)
	throws PluginException
    {
	final Iterator nameIterator = properties.keySet().iterator();

	while (nameIterator.hasNext()) {
	    final String name = (String)nameIterator.next();

	    if (!name.startsWith(TEST_PREFIX)) {
		continue;	// Not a test property.
	    }

	    final int nextSeparator = name.indexOf('.',
						   TEST_PREFIX.length());

	    final Integer testNumber;

	    try {
		testNumber =
		    new Integer(name.substring(TEST_PREFIX.length(),
					       nextSeparator));
	    }
	    catch (Exception e) {
		throw new PluginException(
		    "Could not resolve test number from property '" + name +
		    ".");
	    }

	    if (m_tests.containsKey(testNumber)) {
		continue;	// Already parsed.
	    }

	    final String descriptionPropertyName =
		getTestPropertyName(testNumber, "description");

	    final String parameterPropertyName =
		getTestPropertyName(testNumber, "parameter");

	    final Test test =
		new Test(testNumber,
			 properties.getProperty(descriptionPropertyName, null),
			 properties.getPropertySubset(parameterPropertyName +
						      '.'));

	    m_tests.put(testNumber, test);
	}
    }

    public Set getTests() throws PluginException
    {
	return new HashSet(m_tests.values());
    }
}
