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


/**
 * @author Philip Aston
 * @version $Revision$
 */
public class TestImplementation extends AbstractTestSemantics
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

    public final int getNumber()
    {
	return m_number;
    }

    public final String getDescription()
    {
	return m_description;
    }

    public final GrinderProperties getParameters()
    {
	return m_parameters;
    }
}
