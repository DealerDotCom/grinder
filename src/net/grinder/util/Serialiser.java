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

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;


/**
 * Utility methods for efficient serialisation.
 *
 * @author Philip Aston
 * @version $Revision$
 */
public class Serialiser
{
    private final byte[] m_readBuffer = new byte[8];

    /**
     * Write a <code>long</code> to a stream in such a way it can be
     * read by {@link #readUnsignedLong}. The value of the
     * <code>long<code> must be greater than zero. Values between 0
     * and 127 inclusive require only one byte. Other values require
     * eight bytes.
     *
     * @param output The stream.
     * @param long The value.
     * @throws IOException If the stream raises an error.
     * @throws IOException If the value is negative.
     **/
    public final void writeUnsignedLong(DataOutput output, long l)
	throws IOException
    {
	if (l < 0) {
	    throw new IOException("Negative argument");
	}

	if (l < 0x80) {
	    output.writeByte((byte)l);
	}
	else {
	    output.writeLong(- l);
	}
    }

    /**
     * Read a <code>long</code> written by {@link #writeUnsignedLong}.
     **/
    public final long readUnsignedLong(DataInput input)
	throws IOException
    {
	m_readBuffer[0] = input.readByte();

	if (m_readBuffer[0] >= 0) {
	    return m_readBuffer[0];
	}
	else {
	    input.readFully(m_readBuffer, 1, 7);

	    return - (((long)(m_readBuffer[0] & 0xFF) << 56) |
		      ((long)(m_readBuffer[1] & 0xFF) << 48) |
		      ((long)(m_readBuffer[2] & 0xFF) << 40) |
		      ((long)(m_readBuffer[3] & 0xFF) << 32) |
		      ((long)(m_readBuffer[4] & 0xFF) << 24) |
		      ((long)(m_readBuffer[5] & 0xFF) << 16) |
		      ((long)(m_readBuffer[6] & 0xFF) << 8) |
		      ((long)(m_readBuffer[7] & 0xFF)));
	}
    }

    /**
     * Write a <code>long</code> to a stream in such a way it can be
     * read by {@link #readLong}. 
     *
     * @param output The stream.
     * @param long The value.
     * @throws IOException If the stream raises an error.
     **/
    public final void writeLong(DataOutput output, long l)
	throws IOException
    {
	// One day we'll make this efficient again.
	output.writeLong(l);
    }

    /**
     * Read a <code>long</code> written by {@link #writeLong}.
     **/
    public final long readLong(DataInput input)
	throws IOException
    {
	return input.readLong();
    }

    /**
     * Write a <code>double</code> to a stream in such a way it can be
     * read by {@link #readDouble}. 
     *
     * @param output The stream.
     * @param double The value.
     * @throws IOException If the stream raises an error.
     **/
    public final void writeDouble(DataOutput output, double l)
	throws IOException
    {
	// One day we'll make this efficient again.
	output.writeDouble(l);
    }

    /**
     * Read a <code>double</code> written by {@link #writeDouble}.
     **/
    public final double readDouble(DataInput input)
	throws IOException
    {
	return input.readDouble();
    }
}
