// The Grinder
// Copyright (C) 2000, 2001  Paco Gomez
// Copyright (C) 2000, 2001  Philip Aston
// Copyright (C) 2000, 2001  Dirk Feufel

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

package net.grinder.communication;

import java.io.Serializable;

import net.grinder.common.ProcessStatus;


/**
 * Message for informing the Console of worker process status.
 *
 * @author Dirk Feufel
 * @author Philip Aston
 **/
public final class ReportStatusMessage extends Message
    implements Serializable, ProcessStatus
{
    private static final long serialVersionUID = 111833598590121547L;

    private final short m_state;
    private final short m_totalNumberOfThreads;
    private final short m_numberOfRunningThreads;

    /**
     * Creates a new <code>ReportStatusMessage</code> instance.
     *
     * @param state The process state. See {@link
     * net.grinder.common.ProcessStatus}.
     * @param totalThreads The total number of threads.
     * @param runningThreads The number of threads that are still running.
     **/
    public ReportStatusMessage(short state, short runningThreads,
			       short totalThreads)
    {
	m_state = state;
        m_numberOfRunningThreads = runningThreads;
        m_totalNumberOfThreads = totalThreads;
    }

    public final String getName()
    {
	return super.getSenderGrinderID();
    }

    public final short getState()
    {
	return m_state;
    }

    public final short getNumberOfRunningThreads()
    {
	return m_numberOfRunningThreads;
    }

    public final short getTotalNumberOfThreads()
    {
	return m_totalNumberOfThreads;
    }
}
