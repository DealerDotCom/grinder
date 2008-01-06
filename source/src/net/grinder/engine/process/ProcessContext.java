// Copyright (C) 2006, 2007 Philip Aston
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
// COPYRIGHT HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
// INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
// (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
// SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
// HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
// STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
// ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
// OF THE POSSIBILITY OF SUCH DAMAGE.

package net.grinder.engine.process;

import net.grinder.common.GrinderProperties;
import net.grinder.common.Logger;
import net.grinder.communication.QueuedSender;
import net.grinder.console.messages.ReportStatisticsMessage;
import net.grinder.console.messages.WorkerProcessReportMessage;
import net.grinder.script.Grinder;
import net.grinder.statistics.StatisticsServices;
import net.grinder.statistics.TestStatisticsMap;
import net.grinder.util.Sleeper;


/**
 * Interface to worker process state.
 *
 * @author Philip Aston
 * @version $Revision$
 */
interface ProcessContext {

  QueuedSender getConsoleSender();

  WorkerProcessReportMessage createStatusMessage(short state,
    short numberOfThreads, short totalNumberOfThreads);

  ReportStatisticsMessage createReportStatisticsMessage(
    TestStatisticsMap sample);

  Logger getProcessLogger();

  TestRegistry getTestRegistry();

  GrinderProperties getProperties();

  Grinder.ScriptContext getScriptContext();

  ThreadContextLocator getThreadContextLocator();

  /**
   * Unsynchronised for efficiency. {@link GrinderProcess} calls
   * {@link #setExecutionStartTime} just before launching threads,
   * after which it is never called again.
   */
  void setExecutionStartTime();

  /**
   * Unsynchronised for efficiency. {@link GrinderProcess} calls
   * {@link #setExecutionStartTime} just before launching threads,
   * after which it is never called again.
   *
   * @return Start of execution, in milliseconds. Basis time depends
   * on the time authority in use by the process context.
   */
  long getExecutionStartTime();

  void checkIfShutdown() throws ShutdownException;

  void shutdown();

  Sleeper getSleeper();

  StatisticsServices getStatisticsServices();

  void fireThreadCreatedEvent(ThreadContext threadContext);
}
