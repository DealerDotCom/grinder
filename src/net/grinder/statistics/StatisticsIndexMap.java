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

package net.grinder.statistics;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import net.grinder.common.GrinderException;


/**
 * @author Philip Aston
 * @version $Revision$
 * @stereotype singleton
 **/
public class StatisticsIndexMap implements Serializable
{
    private final static StatisticsIndexMap s_processInstance =
	new StatisticsIndexMap();

    private final Map m_map = new HashMap();
    private final int m_numberOfLongIndicies;
    private final int m_numberOfDoubleIndicies;

    public final static StatisticsIndexMap getInstance()
    {
	return s_processInstance;
    }

    private StatisticsIndexMap()
    {
	int nextLongIndex = 0;

	m_map.put("errors", new LongIndex(nextLongIndex++));
	m_map.put("timedTransactions", new LongIndex(nextLongIndex++));
	m_map.put("untimedTransactions", new LongIndex(nextLongIndex++));
	m_map.put("timedTransactionTime", new LongIndex(nextLongIndex++));
	m_map.put("period", new LongIndex(nextLongIndex++));
	m_map.put("userLong0", new LongIndex(nextLongIndex++));
	m_map.put("userLong1", new LongIndex(nextLongIndex++));
	m_map.put("userLong2", new LongIndex(nextLongIndex++));
	m_map.put("userLong3", new LongIndex(nextLongIndex++));
	m_map.put("userLong4", new LongIndex(nextLongIndex++));

	m_numberOfLongIndicies = nextLongIndex;

	int nextDoubleIndex = 0;

	m_map.put("peakTPS", new DoubleIndex(nextDoubleIndex++));
	m_map.put("userDouble0", new DoubleIndex(nextDoubleIndex++));
	m_map.put("userDouble1", new DoubleIndex(nextDoubleIndex++));
	m_map.put("userDouble2", new DoubleIndex(nextDoubleIndex++));
	m_map.put("userDouble3", new DoubleIndex(nextDoubleIndex++));
	m_map.put("userDouble4", new DoubleIndex(nextDoubleIndex++));

	m_numberOfDoubleIndicies = nextDoubleIndex;
    }

    final boolean isDoubleIndex(String statisticKey)
    {
	return m_map.get(statisticKey) instanceof DoubleIndex;
    }

    final boolean isLongIndex(String statisticKey)
    {
	return m_map.get(statisticKey) instanceof LongIndex;
    }

    final int getNumberOfLongIndicies()
    {
	return m_numberOfLongIndicies;
    }

    final int getNumberOfDoubleIndicies()
    {
	return m_numberOfDoubleIndicies;
    }

    /**
     * @exception GrinderException If <code>statisticKey</code> is not
     * registered.
     **/
    public final DoubleIndex getIndexForDouble(String statisticKey)
	throws GrinderException
    {
	final Object existing = m_map.get(statisticKey);

	if (existing == null || !(existing instanceof DoubleIndex)) {
	    throw new GrinderException("Unknown key '" + statisticKey + "'");
	}
	else {
	    return (DoubleIndex)existing;
	}
    }

    /**
     * @exception GrinderException If <code>statisticKey</code> is not
     * registered.
     **/
    public final LongIndex getIndexForLong(String statisticKey)
	throws GrinderException
    {
	final Object existing = m_map.get(statisticKey);

	if (existing == null || !(existing instanceof LongIndex)) {
	    throw new GrinderException("Unknown key '" + statisticKey + "'");
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
}
