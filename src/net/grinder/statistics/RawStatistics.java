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


/**
 * Store an array of raw statistics as unsigned long values. Clients
 * can access individual values using a process specific index
 * obtained from a {@link ProcessStatisticsIndexMap}. Effectively a
 * cheap array list.
 *
 * @author Philip Aston
 * @version $Revision$
 **/
public interface RawStatistics
{
    /**
     * Add the values of another <code>RawStatistics</code> to ours.
     * Assumes we don't need to synchronise access to operand.
     * @param operand The <code>RawStatistics</code> value to add.
     **/
    void add(RawStatistics operand);

    /**
     * Add <code>value</code> to the value specified by
     * <code>processStatisticsIndex</code>.
     *
     * @param processStatisticsIndex The process specific index.
     * @param value The value.
     * @throws IllegalArgumentException If the <code>processStatisticsIndex</code> is negative. 
     * @throws IllegalArgumentException If the <code>value</code> is negative. 
     **/
    void addValue(int processStatisticsIndex, long value);

    /**
     * Equivalent to <code>addValue(processStatisticsIndex, 1)</code>.
     *
     * @param processStatisticsIndex The process specific index.
     * @exception IllegalArgumentException If the <code>processStatisticsIndex</code> is negative. 
     *
     * @see {@link #addValue}
     */
    void incrementValue(int processStatisticsIndex);

    /**
     * Return the value specified by
     * <code>processStatisticsIndex</code>.
     *
     * @param processStatisticsIndex The process specific index.
     * @return The value.
     * @throws IllegalArgumentException If the <code>processStatisticsIndex</code> is negative. 
     */
    long getValue(int processStatisticsIndex);

    /**
     * Return a <code>RawStatistics</code> representing the change
     * since the last snapshot.
     *
     * @param updateSnapshot <code>true</code> => update the snapshot.
     * @return A <code>RawStatistics</code> representing the
     * difference between our values and the snapshot's values.
     **/
    RawStatistics getDelta(boolean updateSnapshot);
}
