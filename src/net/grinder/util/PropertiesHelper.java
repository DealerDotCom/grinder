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

import net.grinder.plugininterface.TestSetPlugin;


/**
 * This class contains properties parsing logic that is required from
 * more than one place.
 *
 * @author Philip Aston
 * @version $Revision$
 */
public class PropertiesHelper
{
    private final GrinderProperties m_properties;

    public PropertiesHelper(GrinderProperties properties)
    {
	m_properties = properties;
    }

    public TestSetPlugin getTestSetPlugin() throws GrinderException
    {
	final String testSetClassName =
	    m_properties.getProperty("grinder.testSetPlugin");

	if (testSetClassName != null) {
	    try{
		final Class testSetClass = Class.forName(testSetClassName);

		if (!TestSetPlugin.class.isAssignableFrom(testSetClass)) {
		    throw new GrinderException(
			"The specified test set plug-in class ('" +
			testSetClass.getName() +
			"') does not implement the interface: '" +
			TestSetPlugin.class.getName() + "'");
		}

		return (TestSetPlugin)testSetClass.newInstance();
	    }
	    catch (Exception e){
		throw new GrinderException(
		    "An instance of the specified plug-in class " +
		    "could not be created.", e);
	    }
	}
	else {
	    return new PropertiesTestSet(m_properties);
	}
    }

    public static String getTestPropertyName(Integer testNumber,
					     String unqualifiedName)
    {
	return PropertiesTestSet.getTestPropertyName(testNumber,
						     unqualifiedName);
    }
}
