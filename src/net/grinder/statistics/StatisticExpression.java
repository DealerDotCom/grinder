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
 * Interface to apply a statistics calculation to a {@link
 * RawStatistics}.
 *
 * @author Philip Aston
 * @version $Revision$
 * @see StatisticExpressionFactory
 **/
public interface StatisticExpression
{
    /**
     * Apply this {@link StatisticExpression} to the given {@link
     * RawStatistics} and return the result as a <code>double</code>.
     *
     * @param rawStatistics A <code>RawStatistics</code> value.
     * @return The result.
     **/
    double getDoubleValue(RawStatistics rawStatistics);

    /**
     * Apply this {@link StatisticExpression} to the given {@link
     * RawStatistics} and return the result as a <code>long</code>,
     * ronding as necessary.
     *
     * @param rawStatistics A <code>RawStatistics</code> value.
     * @return The result.
     **/
    long getLongValue(RawStatistics rawStatistics);

    /**
     * Returns <code>true</code> if the type of this {@link
     * StatisticExpression} is non integral. Callers might use this to
     * decide which accessor to call to ensure that information is not
     * lost, or how to format the result.
     *
     * @return a <code>boolean</code> value
     **/
    boolean isDouble();
}
