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

import java.util.Random;

import net.grinder.common.GrinderException;
import net.grinder.common.Logger;


/**
 * Manage sleeping. 
 *
 * @author Philip Aston
 * @version $Revision$
 */
public class Sleeper
{
    private static boolean s_shutdown = false;

    private static Random s_random = new Random();

    private final double m_factor;
    private final double m_limit99_75Factor;
    private final Logger m_logger;

    /**
     * The constructor.
     *
     * @param factor All sleep times are modified by this factor.
     * @param limit99_75Factor See {@link #sleepNormal}.
     * @param logger  A logger to chat to. Pass <code>null</code> for no chat.
     **/        
    public Sleeper(double factor, double limit99_75Factor, Logger logger)
    {
	if (factor < 0d || limit99_75Factor < 0d) {
	    throw new IllegalArgumentException("Factors must be positive");
	}

	m_factor = factor;
	m_limit99_75Factor = limit99_75Factor;
	m_logger = logger;
    }

    /**
     * Sleep for a time based on the meanTime parameter. The actual
     * time is taken from a pseudo normal distribution. Approximately
     * 99.75% of times will be within (100* limit99_75Factor) percent
     * of the meanTime.
     *
     * @param meanTime Mean time.
     * @throws ShutdownException If {@link #shutdown} has ever been called.
     **/
    public void sleepNormal(long meanTime) throws ShutdownException
    {
	if (meanTime > 0) {
	    if (m_limit99_75Factor > 0) {
		final double sigma = (meanTime * m_limit99_75Factor)/3.0;

		doSleep(meanTime + (long)(s_random.nextGaussian() * sigma));
	    }
	    else {
		doSleep(meanTime);
	    }
	}
    }

    /**
     * Sleep for a time based on the maximumTime parameter. The actual
     * time is taken from a pseudo random flat distribution between 0
     * and maximumTime.
     *
     * @param maximumTime Maximum time.
     * @throws ShutdownException If {@link #shutdown} has ever been called.
     **/
    public void sleepFlat(long maximumTime) throws ShutdownException
    {
	if (maximumTime > 0) {
	    doSleep(Math.abs(s_random.nextLong()) % maximumTime);
	}
    }

    /**
     * Shutdown all Sleepers. Once called, all sleep method
     * invocations will throw {@link ShutdownException}, including
     * those already sleeping.
     **/
    public synchronized static void shutdown()
    {
	s_shutdown = true;
	Sleeper.class.notifyAll();
    }

    private void doSleep(long time) throws ShutdownException
    {
	if (time > 0) {
	    time = (long)(time * m_factor);

	    if (m_logger != null) {
		m_logger.logMessage("Sleeping for " + time + " ms");
	    }

	    long currentTime = System.currentTimeMillis();
	    final long wakeUpTime = currentTime + time;

	    while (currentTime < wakeUpTime) {
		if (s_shutdown) {
		    throw new ShutdownException("Shutting down");
		}

		try {
		    synchronized(Sleeper.class) {
			Sleeper.class.wait(wakeUpTime - currentTime);
		    }
		    break;
		}
		catch (InterruptedException e) {
		    currentTime = System.currentTimeMillis();
		}
	    }
	}
    }

    /**
     * Exception used to indicate that all Sleepers have been shutdown.
     **/
    public static class ShutdownException extends GrinderException
    {
	private ShutdownException(String message)
	{
	    super (message);
	}
    }
}
