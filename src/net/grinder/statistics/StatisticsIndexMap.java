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

import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import net.grinder.common.GrinderException;


/**
 * @author Philip Aston
 * @version $Revision$
 **/
public class StatisticsIndexMap implements Serializable
{
    private final static StatisticsIndexMap s_processInstance =
	new StatisticsIndexMap();

    private int m_nextDoubleIndex = 0;
    private int m_nextLongIndex = 0;

    private final Map m_map = new HashMap();
    private transient DoubleIndex[] m_doubleSequenceInProcessMap;
    private transient LongIndex[] m_longSequenceInProcessMap;

    public final static StatisticsIndexMap getProcessInstance()
    {
	return s_processInstance;
    }

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
	    m_doubleSequenceInProcessMap = null;
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
	    m_longSequenceInProcessMap = null;
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

    abstract static class AbstractIndex implements Serializable
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

    public final synchronized void add(StatisticsIndexMap statisticsIndexMap)
	throws GrinderException
    {
	final Iterator iterator = 
	    statisticsIndexMap.m_map.entrySet().iterator();

	while (iterator.hasNext()) {
	    final Map.Entry entry = (Map.Entry)iterator.next();
	    final String key = (String)entry.getKey();
	    final AbstractIndex index = (AbstractIndex)entry.getValue();

	    if (index instanceof DoubleIndex) {
		getIndexForDouble(key);
	    }
	    else if (index instanceof LongIndex) {
		getIndexForLong(key);
	    }
	    else {
		throw new GrinderException("Key '" + key +
					   "' is an unknown type: " +
					   index.getClass().getName());
	    }
	}
    }

    private final synchronized void updateProcessSequences()
	throws GrinderException
    {
	if (m_doubleSequenceInProcessMap == null ||
	    m_longSequenceInProcessMap == null) {

	    final StatisticsIndexMap processIndexMap = getProcessInstance();

	    m_doubleSequenceInProcessMap = new DoubleIndex[m_map.size()];
	    m_longSequenceInProcessMap = new LongIndex[m_map.size()];

	    final Iterator iterator = m_map.entrySet().iterator();

	    while (iterator.hasNext()) {
		final Map.Entry entry = (Map.Entry)iterator.next();
		final String key = (String)entry.getKey();
		final AbstractIndex index = (AbstractIndex)entry.getValue();
			
		if (index instanceof DoubleIndex) {
		    m_doubleSequenceInProcessMap[index.getValue()] =
			processIndexMap.getIndexForDouble(key);
		}
		else if (index instanceof LongIndex) {
		    m_longSequenceInProcessMap[index.getValue()] =
			processIndexMap.getIndexForLong(key);
		}
		else {
		    throw new GrinderException("Key '" + key +
					       "' is an unknown type: " +
					       index.getClass().getName());
		}
	    }
	}
    }

    final DoubleIndex[] getDoubleSequenceInProcessMap() throws GrinderException
    {
	updateProcessSequences();
	return m_doubleSequenceInProcessMap;
    }

    final LongIndex[] getLongSequenceInProcessMap() throws GrinderException
    {
	updateProcessSequences();
	return m_longSequenceInProcessMap;
    }
}
