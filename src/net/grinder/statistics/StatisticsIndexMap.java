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

package net.grinder.statistics;

import java.util.HashMap;
import java.util.Map;

import net.grinder.common.GrinderException;


/**
 * Unsynchronised.
 *
 * @author Philip Aston
 * @version $Revision$
 **/
public class StatisticsIndexMap
{
    private int m_nextDoubleIndex = 0;
    private int m_nextLongIndex = 0;

    private final Map m_map = new HashMap();

    final boolean isDoubleIndex(String statisticKey)
    {
	return m_map.get(statisticKey) instanceof DoubleIndex;
    }

    final boolean isLongIndex(String statisticKey)
    {
	return m_map.get(statisticKey) instanceof LongIndex;
    }

    /**
     * @exception GrinderException If <code>statisticKey</code> has
     * already been registered for a non-double.
     **/
    public final synchronized DoubleIndex getIndexForDouble(
	String statisticKey)
	throws GrinderException
    {
	final Object existing = m_map.get(statisticKey);

	if (existing == null) {
	    final DoubleIndex result = new DoubleIndex(m_nextDoubleIndex++);
	    m_map.put(statisticKey, result);
	    return result;
	}
	else if (!(existing instanceof DoubleIndex)) {
	    throw new GrinderException("Key '" + statisticKey +
				       "' already reserved for a " +
				       existing.getClass().getName());
	}
	else {
	    return (DoubleIndex)existing;
	}
    }

    /**
     * @exception GrinderException If <code>statisticKey</code> has
     * already been registered for a non-long.
     **/
    public final synchronized LongIndex getIndexForLong(String statisticKey)
	throws GrinderException
    {
	final Object existing = m_map.get(statisticKey);

	if (existing == null) {
	    final LongIndex result = new LongIndex(m_nextLongIndex++);
	    m_map.put(statisticKey, result);
	    return result;
	}
	else if (!(existing instanceof LongIndex)) {
	    throw new GrinderException("Key '" + statisticKey +
				       "' already reserved for a " +
				       existing.getClass().getName());
	}
	else {
	    return (LongIndex)existing;
	}
    }

    abstract static class AbstractIndex
    {
	private final int m_value;

	protected AbstractIndex(int i)
	{
	    m_value = i;
	}

	final int getValue()
	{
	    return m_value;
	}
    }

    public final static class DoubleIndex extends AbstractIndex
    {
	private DoubleIndex(int i)
	{
	    super(i);
	}
    }

    public final static class LongIndex extends AbstractIndex
    {
	private LongIndex(int i)
	{
	    super(i);
	}
    }
}
