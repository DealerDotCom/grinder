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

package net.grinder.common;

import java.io.PrintWriter;


/**
 * This class is used to share data between the Grinder and the 
 * plug-in.
 * 
 * @author Philip Aston
 * @version $Revision$
 */
public interface Logger
{
    public int LOG = 1 << 0;
    public int TERMINAL = 1 << 1;

    /**
     * Log a message with context information.
     */
    public void logMessage(String message);
    public void logMessage(String message, int where);

    /**
     * Log an error with context information.
     */
    public void logError(String message);
    public void logError(String message, int where);

    public PrintWriter getOutputLogWriter();
    public PrintWriter getErrorLogWriter();
}
