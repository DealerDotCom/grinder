// The Grinder
// Copyright (C) 2000  Paco Gomez
// Copyright (C) 2000, 2001  Philip Aston

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

import net.grinder.common.Test;
import net.grinder.util.GrinderProperties;


/**
 * This interface defines the callbacks that an individual Grinder
 * thread can make on a plugin.
 *
 * @author Philip Aston
 * @version $Revision$
 */ 
public interface ThreadCallbacks
{
    /**
     * This method is executed when the thread starts. It is only
     * executed once per thread.
     */
    public void initialize(PluginThreadContext pluginThreadContext)
	throws PluginException;
    
    /**
     * This method is executed at the beginning of evey cycle.
     */
    public void beginCycle() throws PluginException;

    /**
     * This is called for each method name in grinder.plugin.methods.
     */
    public boolean doTest(Test testDefinition)
	throws PluginException;
    
    /**
     * This method is executed at the end of every cycle.
     */  
    public void endCycle() throws PluginException;
}
