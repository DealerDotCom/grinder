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

package net.grinder.common;

import java.io.PrintStream;
import java.io.PrintWriter;


/**
 * GrinderException.java
 *
 * @author Philip Aston
 * @version $Revision$
 */
public class GrinderException extends Exception
{
    private final String m_message;
    private Exception m_nestedException = null;

    public GrinderException(String message)
    {
	m_message = message;
    }

    public GrinderException(String message, Exception nestedException)
    {
	m_message = message;
	m_nestedException = nestedException;
    }
    
    public String toString()
    {
	return getClass().getName() + ": " + m_message +
	    (m_nestedException != null ? 
	     ", nested exception: " + m_nestedException : "");
    }


    public void printStackTrace(PrintWriter s)
    {
	super.printStackTrace(s);

	if (m_nestedException != null) {
	    s.print("\n\tNested exception stack trace: ");
	    m_nestedException.printStackTrace(s);
	}

	s.flush();
    }

    public void printStackTrace()
    {
	printStackTrace(System.err);
    }

    public void printStackTrace(PrintStream s)
    {
	printStackTrace(new PrintWriter(s));
    }
}
