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

package net.grinder.console.model;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import net.grinder.common.ProcessStatus;


/**
 * @author Philip Aston
 **/
public final class ProcessStatusSet
{
    private final static long REFRESH_TIME = 500;
    private final static long FLUSH_TIME = 2000;

    private final Timer m_timer = new Timer(true);
    private final Map m_processes = new HashMap();
    private final Set m_scratchSet = new HashSet();
    private final Comparator m_sorter;
    private final List m_listeners = new LinkedList();

    private boolean m_newData = false;
    private int m_lastProcessEventGeneration = -1;
    private int m_currentGeneration = 0;

    public ProcessStatusSet()
    {
	m_sorter = new Comparator() {
		public int compare(Object o1, Object o2) 
		{
		    final ProcessStatus p1 = (ProcessStatus)o1;
		    final ProcessStatus p2 = (ProcessStatus)o2;

		    final int compareState = p1.getState() - p2.getState();

		    if (compareState != 0) {
			return compareState;
		    }
		    else {
			return p1.getName().compareTo(p2.getName());
		    }
		}
	    };

	m_timer.scheduleAtFixedRate(
	    new TimerTask() {
		public void run() { fireUpdate(); }
	    },
	    0, REFRESH_TIME);

	m_timer.scheduleAtFixedRate(
	    new TimerTask() {
		public void run() { flush(); }
	    },
	    0, FLUSH_TIME);
    }

    public synchronized void addListener(ProcessStatusSetListener listener)
    {
	m_listeners.add(listener);
    }

    private synchronized void fireUpdate()
    {
	if (m_newData) {
	    final ProcessStatus[] data = (ProcessStatus[])
		m_processes.values().toArray(new ProcessStatus[0]);

	    Arrays.sort(data, m_sorter);

	    int runningSum = 0;
	    int totalSum = 0;

	    for (int i=0; i<data.length; ++i) {
		runningSum += data[i].getNumberOfRunningThreads();
		totalSum += data[i].getTotalNumberOfThreads();
	    }

	    final Iterator iterator = m_listeners.iterator();

	    while (iterator.hasNext()) {
		final ProcessStatusSetListener listener =
		    (ProcessStatusSetListener)iterator.next();

		listener.update(data, runningSum, totalSum);
	    }

	    m_newData = false;
	}
    }

    public final synchronized void processEvent()
    {
	m_lastProcessEventGeneration = m_currentGeneration;
    }

    public final synchronized void addStatusReport(String uniqueID,
						   ProcessStatus processStatus)
    {
	ProcessStatusImplementation processStatusImplementation = 
	    (ProcessStatusImplementation)m_processes.get(uniqueID);

	if (processStatusImplementation == null) {
	    processStatusImplementation =
		new ProcessStatusImplementation(processStatus.getName());

	    m_processes.put(uniqueID, processStatusImplementation);
	}

	processStatusImplementation.set(processStatus);

	m_newData = true;
    }

    private final synchronized void flush()
    {
	m_scratchSet.clear();
	final Set zombies = m_scratchSet;

	final Iterator iterator = m_processes.entrySet().iterator();

	while (iterator.hasNext()) {
	    final Map.Entry entry = (Map.Entry)iterator.next();
	    final String key = (String)entry.getKey();
	    final ProcessStatusImplementation processStatusImplementation =
		(ProcessStatusImplementation)entry.getValue();

	    if (processStatusImplementation.shouldPurge()) {
		zombies.add(key);
	    }
	}

	if (zombies.size() > 0) {
	    m_processes.keySet().removeAll(zombies);
	    m_newData = true;
	}

	++m_currentGeneration;
    }

    private final class ProcessStatusImplementation implements ProcessStatus
    {
	private final String m_name;
	private short m_state;
	private short m_totalNumberOfThreads;
	private short m_numberOfRunningThreads;

	private boolean m_reapable = false;
	private int m_lastTouchedGeneration;

	public ProcessStatusImplementation(String name)
	{
	    m_name = name;
	    touch();
	}

	final void set(ProcessStatus processStatus)
	{
	    m_state = processStatus.getState();
	    m_totalNumberOfThreads = processStatus.getTotalNumberOfThreads();
	    m_numberOfRunningThreads =
		processStatus.getNumberOfRunningThreads();
	    touch();
	}

	final boolean shouldPurge()
	{
	    if (m_reapable) {
		return true;
	    }
	    else if (m_lastTouchedGeneration <= m_lastProcessEventGeneration) {
		// Processes have a short time to report after a
		// {start|reset|stop} event.
		m_reapable = true;
	    }

	    return false;
	}

	private final void touch()
	{
	    m_lastTouchedGeneration = m_currentGeneration;
	    m_reapable = false;
	}

	public final String getName()
	{
	    return m_name;
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
}
