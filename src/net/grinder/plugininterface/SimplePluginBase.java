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
 * set mechanism. (I.E. their tests are always defined in the
 * properties file).
 *
 * @author Philip Aston
 * @version $Revision$
 */ 
public abstract class SimplePluginBase
    implements GrinderPlugin, ThreadCallbacks
{
    /**
     * This method is executed when the process starts. It is only
     * executed once.
     */
    public void initialize(PluginProcessContext processContext)
	throws PluginException
    {
    }
    

    /**
     * This method is called to create a handler for each thread.
     */
    public ThreadCallbacks createThreadCallbackHandler()
	throws PluginException
    {
	try {
	    return (ThreadCallbacks)clone();
	}
	catch (CloneNotSupportedException e) {
	    throw new PluginException("Plugin classes that implement " +
				      getClass().getName() +
				      " must support clone()", e);
	}
    }

    /**
     * Returns a Set of Tests. Returns null if the tests are to be
     * defined in the properties file.
     */
    public Set getTests() throws PluginException
    {
	return null;
    }
}
