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
 * Scripts can get contextual information through a global
 * <code>grinder</code> object that supports this interface.
 *
 * @author Philip Aston
 * @version $Revision$
 */ 
public interface ScriptContext {

  /**
   * Get an unique ID value for this worker process.
   *
   * @return The id.
   */
  String getGrinderID();

  /**
   * Return the thread ID, or -1 if not called from a worker thread.
   * @return The thread ID.
   */
  int getThreadID();

  /**
   * Return the current run number, or -1 if not called from a
   * worker thread.
   *
   * @return An <code>int</code> value.
   */
  int getRunNumber();

  /**
   * Get an appropriate {@link net.grinder.common.Logger}
   * implementation. The value returned when invoked from script
   * initialisation differs from the value returned when called from
   * a worker thread, so its best not to keep references to the
   * result.
   *
   * @return A <code>Logger</code>.
   */
  Logger getLogger();

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
  void sleep(long meanTime) throws GrinderException, InvalidContextException;

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
  void sleep(long meanTime, long sigma)
    throws GrinderException, InvalidContextException;

  /**
   * Get a {@link net.grinder.common.FilenameFactory} that can be
   * used to create unique filenames. The value returned when invoked from script
   * initialisation differs from the value returned when called from
   * a worker thread, so its best not to keep references to the
   * result.
   *
   * @return A <code>FilenameFactory</code>.
   */
  FilenameFactory getFilenameFactory();

  /**
   * Get the global properties for this agent/worker process set.
   *
   * @return The properties.
   */
  GrinderProperties getProperties();
  
  /**
   * Register a new "summary" statistics view. These views appear in
   * the worker process output log summaries and are displayed in the
   * console.
   *
   * @param statisticsView The new statistics view.
   * @exception GrinderException If the view could not be registered.
   */
  void registerSummaryStatisticsView(StatisticsView statisticsView)
    throws GrinderException;

  /**
   * Register a new "detail" statistics view which appears in the
   * worker process data logs. Each test invocation will have an entry
   * displayed for the detail statistics views.
   *
   * @param statisticsView The new statistics view.
   * @exception GrinderException If the view could not be registered.
   */
  void registerDetailStatisticsView(StatisticsView statisticsView)
    throws GrinderException;

  /**
   * Get the Statistics the calling worker thread. This holds the
   * statistics of the last test invoked by the thread.
   *
   * @return The statistics.
   * @exception InvalidContextException If called from a non-worker
   * thread.
   */
  Statistics getStatistics() throws InvalidContextException;
}
