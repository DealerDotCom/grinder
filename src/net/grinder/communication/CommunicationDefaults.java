// The Grinder
// Copyright (C) 2000, 2001  Paco Gomez
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

package net.grinder.communication;


/**
 * Default communication constants.
 *
 * @author Philip Aston
 * @version $Revision$
 */
public interface CommunicationDefaults
{
    final String CONSOLE_ADDRESS = ""; // Bind to all interfaces by default.
    final int CONSOLE_PORT = 6372;

    final String GRINDER_ADDRESS = "228.1.1.1";
    final int GRINDER_PORT = 1234;

    final int MAX_PORT = 0xFFFF;
}
