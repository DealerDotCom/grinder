// The Grinder
// Copyright (C) 2000  Paco Gomez
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

package net.grinder;

import net.grinder.util.GrinderProperties;


/**
 * This is the entry point of The Grinder Console.
 * 
 * @author Philip Aston
 * @version $Revision$
 */
public class Console
{       
    public static void main(String args[])
	throws Exception
    {
	if (args.length == 1) {
	    GrinderProperties.setPropertiesFileName(args[0]);
	}
	else if (args.length > 1) {
	    System.err.println("Usage: java " + Console.class.getName() +
			       " [alternatePropertiesFilename]");
	    System.exit(1);
	}

	if (GrinderProperties.getProperties() == null) {
	    return;
	}

	new net.grinder.console.Console().run();
    }
}
