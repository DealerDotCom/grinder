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

import net.grinder.common.FilenameFactory;
import net.grinder.common.GrinderException;
import net.grinder.common.GrinderProperties;
import net.grinder.common.Logger;
import net.grinder.statistics.StatisticsView;


/**
 * This class is used to share data about the process context between
 * the Grinder and the plugin.
 * 
 * @author Paco Gomez
 * @author Philip Aston
 * @version $Revision$
 */
public interface PluginProcessContext extends Logger
{
    /**
     * Returns the name of this Grinder Process.
     *
     * @return The name.
     **/
    String getGrinderID();

    /**
     * Return a {@link FilenameFactory} that can be used to generate
     * file names appropriate to the process context.
     *
     * @return The {@link FilenameFactory}.
     **/
    FilenameFactory getFilenameFactory();
    
    /**
     * Returns the parameters specified with "grinder.plugin.parameter="
     *
     * @return The parameters.
     **/
    GrinderProperties getPluginParameters();

    /**
     * Plugins can use this method to register a new statistics view
     * with the Grinder and the Console.
     *
     * @param view The new view.
     * @exception GrinderException If the view cannot be registered.
     **/
    void registerStatisticsView(StatisticsView view) throws GrinderException;

    /**
     * Check whether this process is reporting times to the console or
     * not. Refer to the <code>grinder.recordTime</code> property for
     * more information.
     *
     * @return <code>true => this process should report times.
     **/
    boolean getRecordTime();
}
