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

package net.grinder.plugininterface;

import net.grinder.common.FilenameFactory;
import net.grinder.common.Logger;
import net.grinder.statistics.TestStatistics;


/**
 * <p>This class is used to share thread information between the
 * Grinder and the plug-in. </p>
 *
 * <p>Note, the {@link Logger} implementation isn't guaranteed to be
 * thread safe.</p>
 * 
 * @author Paco Gomez
 * @author Philip Aston
 * @version $Revision$
 **/
public interface PluginThreadContext extends Logger, FilenameFactory
{    
    /**
     * Return the thread ID.
     */ 
    public int getThreadID();

    /**
     * Return the current run ID.
     */
    public int getCurrentRunNumber();

    public void abortRun();

    /**
     * The plug-in should call startTimer() if it wishes to have more
     * precise control over the measured section of code. The Grinder
     * automatically sets the start time before calling a method -
     * calling this method overrides the start time with the current
     * time.
     *
     * @see #stopTimer
     */
    public void startTimer();

    /**
     * The plug-in should call stopTimer() if it wishes to have more
     * precise control over the measured section of code. The Grinder
     * automatically sets the end time after calling a method unless
     * the method called stopTimer().
     *
     * @see #startTimer
     */
    public void stopTimer();

    public long getStartTime();

    public TestStatistics getCurrentTestStatistics();
}
