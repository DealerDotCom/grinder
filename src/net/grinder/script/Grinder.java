// Copyright (C) 2001, 2002, 2003 Philip Aston
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

package net.grinder.script;

import net.grinder.common.FilenameFactory;
import net.grinder.common.GrinderException;
import net.grinder.common.GrinderProperties;
import net.grinder.common.Logger;
import net.grinder.statistics.StatisticsView;


/**
 * Various methods that provide contextual information for scripts.
 *
 * @author Philip Aston
 * @version $Revision$
 */ 
public class Grinder {

  /**
   * Get an unique ID value for this worker process.
   *
   * @return The id.
   */
  public static final String getGrinderID() {
    return ScriptContext.getImplementation().getGrinderID();
  }

  /**
   * Return the thread ID, or -1 if not called from a worker thread.
   * @return The thread ID.
   */
  public static final int getThreadID() {
    return ScriptContext.getImplementation().getThreadID();
  }

  /**
   * Return the current run number, or -1 if not called from a
   * worker thread.
   *
   * @return An <code>int</code> value.
   */
  public static final int getRunNumber() {
    return ScriptContext.getImplementation().getRunNumber();
  }

  /**
   * Get an appropriate {@link net.grinder.common.Logger}
   * implementation. The value returned when invoked from script
   * initialisation differs from the value returned when called from
   * a worker thread, so its best not to keep references to the
   * result.
   *
   * @return A <code>Logger</code>.
   */
  public static final Logger getLogger() {
    return ScriptContext.getImplementation().getLogger();
  }

  /**
   * Sleep for a time based on the meanTime parameter. The actual
   * time may be greater or less than meanTime, and is distributed
   * according to a pseudo normal distribution.
   *
   * @param meanTime Mean time in milliseconds.
   * @exception GrinderException If the sleep failed.
   * @exception InvalidContextException If called from a non-worker
   * thread.
   */
  public static final void sleep(long meanTime)
    throws GrinderException, InvalidContextException {
    ScriptContext.getImplementation().sleep(meanTime);
  }

  /**
   * Sleep for a time based on the meanTime parameter. The actual
   * time may be greater or less than meanTime, and is distributed
   * according to a pseudo normal distribution.
   *
   * @param meanTime Mean time in milliseconds.
   * @param sigma The standard deviation, in milliseconds.
   * @exception GrinderException If the sleep failed.
   * @exception InvalidContextException If called from a non-worker
   * thread.
   **/
  public static final void sleep(long meanTime, long sigma)
    throws GrinderException, InvalidContextException {
    ScriptContext.getImplementation().sleep(meanTime, sigma);
  }

  /**
   * Get a {@link net.grinder.common.FilenameFactory} that can be used
   * to create unique filenames. The value returned when invoked from
   * script initialisation differs from the value returned when called
   * from a worker thread, so its best not to keep references to the
   * result.
   *
   * @return A <code>FilenameFactory</code>.
   */
  public static final FilenameFactory getFilenameFactory() {
    return ScriptContext.getImplementation().getFilenameFactory();
  }

  /**
   * Get the global properties for this agent/worker process set.
   *
   * @return The properties.
   */
  public static final GrinderProperties getProperties() {
    return ScriptContext.getImplementation().getProperties();
  }
  
  /**
   * Register a new "summary" statistics view. These views appear in
   * the worker process output log summaries and are displayed in the
   * console.
   *
   * @param statisticsView The new statistics view.
   * @exception GrinderException If the view could not be registered.
   */
  public static final void registerSummaryStatisticsView(
    StatisticsView statisticsView)
    throws GrinderException {
    ScriptContext.getImplementation().registerSummaryStatisticsView(
      statisticsView);
  }

  /**
   * Register a new "detail" statistics view which appears in the
   * worker process data logs. Each test invocation will have an entry
   * displayed for the detail statistics views.
   *
   * @param statisticsView The new statistics view.
   * @exception GrinderException If the view could not be registered.
   */
  public static final void registerDetailStatisticsView(
    StatisticsView statisticsView)
    throws GrinderException {
    ScriptContext.getImplementation().registerDetailStatisticsView(
      statisticsView);
  }

  /**
   * Get the Statistics for the calling worker thread. This provides
   * access to the statistics of the last test invoked by the thread.
   *
   * @return The statistics.
   * @exception InvalidContextException If called from a non-worker
   * thread.
   */
  public static final Statistics getStatistics()
    throws InvalidContextException {
    return ScriptContext.getImplementation().getStatistics();
  }
}
