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

package net.grinder.util;

import net.grinder.plugininterface.Test;


/**
 * @author Philip Aston
 * @version $Revision$
 */
public class TestImplementation implements Test
{
    private final int m_index;
    private final String m_name;
    private final String m_description;
    private transient final GrinderProperties m_parameters;

    public TestImplementation(int index, String name, String description,
			      GrinderProperties parameters)
    {
	m_index = index;
	m_name = name;
	m_description = description;
	m_parameters = parameters;
    }

    public TestImplementation(int index, String description,
			      GrinderProperties parameters)
    {
	this(index, Integer.toString(index), description, parameters);
    }

    public int getIndex()
    {
	return m_index;
    }

    public String getName()
    {
	return m_name;
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
	final int other = ((TestImplementation)o).m_index;
	return m_index<other ? -1 : (m_index==other ? 0 : 1);
    }
}
