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

package net.grinder.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.Writer;
import java.io.IOException;


/**
 * FileWriter that doesn't create a file until a write occurs.
 *
 * @author Philip Aston
 * @version $Revision$
 */
public class DelayedCreationFileWriter extends Writer
{
    private final File m_file;
    private final boolean m_append;

    private Writer m_delegate = null;

    public DelayedCreationFileWriter(File file, boolean append)
    {
	m_file = file;
	m_append = append;

	if (!append) {
	    // Delete the old file. Well it would get trashed anyway
	    // if you used a standard FileWriter, so stop
	    // complaining, ok?
	    m_file.delete();
	}
    }

    public void close() throws IOException
    {
	synchronized(this) {
	    if (m_delegate == null) {
		return;
	    }
	}

	m_delegate.close();
    }

    public void flush() throws IOException
    {
	synchronized(this) {
	    if (m_delegate == null) {
		return;
	    }
	}

	m_delegate.flush();
    }

    private synchronized void checkOpen() throws IOException
    {
	if (m_delegate == null) {
	    try {
		m_delegate = new FileWriter(m_file.getPath(), m_append);
	    }
	    catch (FileNotFoundException e) {
		throw new IOException(e.getMessage());
	    }
	}
    }

    public void write(int i)
	throws IOException
    {
	checkOpen();
	m_delegate.write(i);
    }

    public void write(char[] bytes, int offset, int length)
	throws IOException
    {
	checkOpen();
	m_delegate.write(bytes, offset, length);
    }
}
