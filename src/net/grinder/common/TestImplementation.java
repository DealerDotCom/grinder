// The Grinder
// Copyright (C) 2001  Paco Gomez
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

package net.grinder.common;

import net.grinder.util.GrinderProperties;


/**
 * @author Philip Aston
 * @version $Revision$
 */
public class TestImplementation implements Test
{
    private final int m_number;
    private final String m_description;
    private transient final GrinderProperties m_parameters;

    public TestImplementation(int number, String description,
			      GrinderProperties parameters)
    {
	m_number = number;
	m_description = description;
	m_parameters = parameters;
    }

    public int getNumber()
    {
	return m_number;
    }

    public String getDescription()
    {
	return m_description;
    }

    public GrinderProperties getParameters()
    {
	return m_parameters;
    }

    public int compareTo(Object o) 
    {
	final int other = ((TestImplementation)o).m_number;
	return m_number<other ? -1 : (m_number==other ? 0 : 1);
    }

    /**
     * The test number is used as the hash code. Wondered whether it
     * was worth distributing the hash codes more evenly across the
     * range of an int, but using the value is good enough for
     * <code>java.lang.Integer</code> so its good enough for us.
     **/
    public int hashCode()
    {
	return m_number;
    }

    public boolean equals(Object o)
    {
	if (o instanceof TestImplementation) {
	    return m_number == ((TestImplementation)o).m_number;
	}

	return false;
    }

    public String toString()
    {
	final String description = getDescription();

	if (description == null) {
	    return "Test " + getNumber();
	}
	else {
	    return "Test " + getNumber() + " (" + description + ")";
	}
    }
}
