// Copyright (C) 2000 Paco Gomez
// Copyright (C) 2000, 2001, 2002 Philip Aston
// All rights reserved.
//
// This file is part of The Grinder software distribution. Refer to
// the file LICENSE which is part of The Grinder distribution for
// licensing details. The Grinder distribution is available on the
// Internet at http://grinder.sourceforge.net/
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
// "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
// LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
// FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
// REGENTS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
// INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
// (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
// SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
// HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
// STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
// ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
// OF THE POSSIBILITY OF SUCH DAMAGE.

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
