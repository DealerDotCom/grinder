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

import net.grinder.statistics.RawStatistics;


/**
 * A {@link StatisticExpression} that tracks the peak value of another
 * {@link StatisticExpression}. The monitored {@link
 * StatisticExpression} is specified when the {@link
 * PeakStatisticExpression} is created, see {@link
 * StatisticExpressionFactory}.
 *
 * @author Philip Aston
 * @version $Revision$
 * @see StatisticExpressionFactory
 **/
public interface PeakStatisticExpression extends StatisticExpression
{
    /**
     * When called, the peak value of monitored expression applied to
     * <code>monitoredStatistics</code> is calculated and stored in the
     * given <code>peakStorageStatistics</code>.
     *
     * @param monitoredStatistics The monitored <code>RawStatistics</code>.
     * @param peakStorageStatistics The <code>RawStatistics</code> in
     * which to store the result.
     */
    void update(RawStatistics monitoredStatistics,
		RawStatistics peakStorageStatistics);
}
