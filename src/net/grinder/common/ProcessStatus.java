// The Grinder
// Copyright (C) 2000, 2001  Paco Gomez
// Copyright (C) 2000, 2001  Philip Aston
// Copyright (C) 2000, 2001  Dirk Feufel

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

package net.grinder.common;


/**
 * @author Philip Aston
 **/
public interface ProcessStatus
{
    public static final short STATE_STARTED = 1;
    public static final short STATE_RUNNING = 2;
    public static final short STATE_FINISHED = 3;

    /**
     * Return the process name.
     *
     * @return The process name.
     **/
    String getName();

    /**
     * Return the process status.
     *
     * @return One of {@link STATE_STARTED}, {@link
     * STATE_RUNNING}, {@link STATE_FINISHED}, {@link STATE_DEAD}.
     **/
    short getState();

    /**
     * Get the number of running threads
     *
     * @return The number of threads that are stull running.
     **/
    short getNumberOfRunningThreads();

    /**
     * Get the total number of threads
     *
     * @return The total number of threads.
     **/
    short getTotalNumberOfThreads();
}

