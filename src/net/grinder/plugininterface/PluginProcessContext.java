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
import net.grinder.common.GrinderException;
import net.grinder.common.GrinderProperties;
import net.grinder.common.Logger;
import net.grinder.common.Test;
import net.grinder.statistics.StatisticsView;


/**
 * <p>This class is used to share process information between the
 * Grinder and the plug-in.</p>
 * 
 * @author Paco Gomez
 * @author Philip Aston
 * @version $Revision$
 **/
public interface PluginProcessContext extends Logger, FilenameFactory
{
    /**
     * Returns the name of this Grinder Process.
     *
     * @return The name.
     **/
    String getGrinderID();
    
    /**
     * Returns the parameters specified with "grinder.plugin.parameter="
     **/
    GrinderProperties getPluginParameters();

    void registerTest(Test test) throws PluginException;

    void registerTests(Set test) throws PluginException;

    /**
     * Plugins can use this method to register a new "summary"
     * statistics view. These views appear in the Grinder summary
     * table and the Console.
     *
     * @param view The new view.
     * @exception GrinderException If the view cannot be registered.
     **/
    void registerSummaryStatisticsView(StatisticsView view)
	throws GrinderException;

    /**
     * Plugins can use this method to register a new "detail"
     * statistics view. These views appear in the individual process
     * data files.
     *
     * @param view The new view.
     * @exception GrinderException If the view cannot be registered.
     **/
    void registerDetailStatisticsView(StatisticsView view)
	throws GrinderException;

    /**
     * Check whether this process is reporting times to the console or
     * not. Refer to the <code>grinder.recordTime</code> property for
     * more information.
     *
     * @return <code>true => this process should report times.
     **/
    boolean getRecordTime();
}
