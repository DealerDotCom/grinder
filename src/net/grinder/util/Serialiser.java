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
    private final byte[] m_byteBuffer = new byte[8];

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
	m_byteBuffer[0] = input.readByte();

	if (m_byteBuffer[0] >= 0) {
	    return m_byteBuffer[0];
	}
	else {
	    input.readFully(m_byteBuffer, 1, 7);

	    return - (((long)(m_byteBuffer[0] & 0xFF) << 56) |
		      ((long)(m_byteBuffer[1] & 0xFF) << 48) |
		      ((long)(m_byteBuffer[2] & 0xFF) << 40) |
		      ((long)(m_byteBuffer[3] & 0xFF) << 32) |
		      ((long)(m_byteBuffer[4] & 0xFF) << 24) |
		      ((long)(m_byteBuffer[5] & 0xFF) << 16) |
		      ((long)(m_byteBuffer[6] & 0xFF) << 8) |
		      ((long)(m_byteBuffer[7] & 0xFF)));
	}
    }

    /**
     * Write a <code>long</code> to a stream in such a way it can be
     * read by {@link #readLong}. 
     *
     * <p>Efficient for small, positive longs. Values less than 16
     * take one byte. The worst cases (including all negative values)
     * take nine bytes.</p>
     *
     * @param output The stream.
     * @param long The value.
     * @throws IOException If the stream raises an error.
     **/
    public final void writeLong(DataOutput output, long l)
	throws IOException
    {
	boolean startedToWrite = false;

	for (byte i=(byte)(m_byteBuffer.length-1); i>=0; --i) {
	    final byte b = (byte)((l >>> i*8) & 0xFF);

	    if (startedToWrite) {
		output.writeByte(b);
	    }
	    else {
		if ((b & 0xFF) != 0) {
		    if ((b & 0xF0) != 0) {
			// Write length.
			output.writeByte((i+1) << 4);
			output.writeByte(b);
		    }
		    else {
			// Special case, byte will fit in one nibble.
			// Combine with length.
			output.writeByte(b | (i << 4));
		    }

		    startedToWrite = true;
		}
	    }
	}

	if (!startedToWrite) {
	    output.writeByte(0);
	}
    }


    /**
     * Read a <code>long</code> written by {@link #writeLong}.
     **/
    public final long readLong(DataInput input)
	throws IOException
    {
	byte b = input.readByte();
	long length = (b & 0xF0) >>> 4;
	long result = (long)(b & 0x0F);

	while (length-- > 0) {
	    result = (result << 8) | input.readUnsignedByte();
	}

	return result;
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
