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

import java.util.Set;

import net.grinder.common.FilenameFactory;
import net.grinder.common.GrinderProperties;
import net.grinder.common.Logger;
import net.grinder.common.Test;


/**
 * <p>This class is used to share process information between the
 * Grinder and the plug-in.</p>
 * 
 * @author Paco Gomez
 * @author Philip Aston
 * @version $Revision$
 */
public interface PluginProcessContext extends Logger, FilenameFactory
{
    public String getGrinderID();

    /**
     * Returns the parameters specified with "grinder.plugin.parameter="
     */
    public GrinderProperties getPluginParameters();

    public void registerTest(Test test) throws PluginException;

    public void registerTests(Set test) throws PluginException;
}
