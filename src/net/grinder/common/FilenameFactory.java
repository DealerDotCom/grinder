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

package net.grinder.util;

import java.io.File;

/**
 * @author Philip Aston
 * @version $Revision$
 */
public class FilenameFactory
{
    private final String m_logDirectory;
    private final String m_contextString;

    /** Package scope - only created by ProcessContextImplementation.
     */
    FilenameFactory(String hostID, String processID, String threadID)
    {
	final GrinderProperties properties = GrinderProperties.getProperties();

	m_logDirectory = properties.getProperty("grinder.logDirectory", ".");

	final StringBuffer buffer = new StringBuffer();

	if (hostID != null) {
	    buffer.append("_");
	    buffer.append(hostID);

	    if (processID != null) {
		buffer.append("_");
		buffer.append(processID);

		if (threadID != null) {
		    buffer.append("_");
		    buffer.append(threadID);
		}
	    }
	}

	m_contextString = buffer.toString();
    }

    public String createFilename(String prefix, String suffix)
    {
	final StringBuffer result = new StringBuffer();

	result.append(m_logDirectory);
	result.append(File.separator);
	result.append(prefix);
	result.append(m_contextString);
	result.append(suffix);

	return result.toString();
    }

    public String createFilename(String prefix)
    {
	return createFilename(prefix, ".log");
    }
}
