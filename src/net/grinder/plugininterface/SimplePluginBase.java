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

package net.grinder.plugininterface;

import java.util.Set;

import net.grinder.util.GrinderProperties;


/**
 * Abstract base class for simple plugins that use the default test
 * set mechanism and wish to focus on implementing ThreadCallbacks.
 *
 * @author Philip Aston
 * @version $Revision$
 * @deprecated I now consider plugins that use this to be less simple
 * than those that explicitly create ThreadCallbacks objects.
 */ 
public abstract class SimplePluginBase
    implements GrinderPlugin, ThreadCallbacks
{
    private Set m_testsFromPropertiesFile;

    public void initialize(PluginProcessContext processContext,
			   Set testsFromPropertiesFile)
	throws PluginException
    {
	m_testsFromPropertiesFile = testsFromPropertiesFile;
    }

    public ThreadCallbacks createThreadCallbackHandler()
	throws PluginException
    {
	try {
	    final SimplePluginBase result =
		(SimplePluginBase)getClass().newInstance();

	    result.m_testsFromPropertiesFile = m_testsFromPropertiesFile;
	    return result;
	}
	catch (Exception e) {
	    throw new PluginException(
		"Could not create new instance of plugin class " +
		getClass().getName(), e);
	}
    }

    public Set getTests() throws PluginException
    {
	return m_testsFromPropertiesFile;
    }
}
