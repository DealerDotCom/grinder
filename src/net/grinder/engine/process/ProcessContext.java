// Copyright (C) 2000 Paco Gomez
// Copyright (C) 2000, 2001, 2002, 2003, 2004 Philip Aston
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

package net.grinder.engine.process;

import java.io.PrintWriter;

import net.grinder.common.GrinderException;
import net.grinder.common.GrinderProperties;
import net.grinder.common.Logger;
import net.grinder.communication.QueuedSender;
import net.grinder.communication.ReportStatusMessage;
import net.grinder.script.Grinder;
import net.grinder.statistics.CommonStatisticsViews;
import net.grinder.statistics.ExpressionView;
import net.grinder.util.Sleeper;


/**
 * @author Philip Aston
 * @version $Revision$
 * @stereotype singleton
 */
final class ProcessContext {
  private final String m_grinderID;
  private final String m_uniqueProcessID;
  private final GrinderProperties m_properties;
  private final boolean m_recordTime;
  private final LoggerImplementation m_loggerImplementation;
  private final QueuedSender m_consoleSender;
  private final PluginRegistry m_pluginRegistry;
  private final TestRegistry m_testRegistry;
  private final Grinder.ScriptContext m_scriptContext;
  private final Sleeper m_sleeper;

  private long m_executionStartTime;
  private boolean m_shutdown;

  ProcessContext(String grinderID, GrinderProperties properties,
                 LoggerImplementation loggerImplementation,
                 QueuedSender consoleSender)
    throws GrinderException {

    m_grinderID = grinderID;
    m_uniqueProcessID = grinderID + ":" + System.currentTimeMillis();
    m_properties = properties;

    m_recordTime = properties.getBoolean("grinder.recordTime", true);

    m_loggerImplementation = loggerImplementation;

    m_consoleSender = consoleSender;

    m_pluginRegistry = new PluginRegistry(this);
    m_testRegistry = new TestRegistry();

    final Logger externalLogger =
      new ExternalLogger(m_loggerImplementation.getProcessLogger());

    m_sleeper = new Sleeper(
      properties.getDouble("grinder.sleepTimeFactor", 1.0d),
      properties.getDouble("grinder.sleepTimeVariation", 0.2d),
      externalLogger);

    m_scriptContext = new ScriptContextImplementation(this,
                                                      externalLogger,
                                                      m_sleeper);

    Grinder.grinder = m_scriptContext;
    m_shutdown = false;
  }

  public void initialiseDataWriter() {

    final PrintWriter dataWriter = m_loggerImplementation.getDataWriter();

    dataWriter.print("Thread, Run, Test, Milliseconds since start");

    final ExpressionView[] detailExpressionViews =
      CommonStatisticsViews.getDetailStatisticsView().getExpressionViews();

    for (int i = 0; i < detailExpressionViews.length; ++i) {
      dataWriter.print(", " + detailExpressionViews[i].getDisplayName());
    }

    dataWriter.println();
  }

  public QueuedSender getConsoleSender() {
    return m_consoleSender;
  }

  public ReportStatusMessage createStatusMessage(
    short state, short numberOfThreads, short totalNumberOfThreads) {

    return new ReportStatusMessage(m_uniqueProcessID, getGrinderID(), state,
                                   numberOfThreads, totalNumberOfThreads);
  }

  public LoggerImplementation getLoggerImplementation() {
    return m_loggerImplementation;
  }

  public Logger getProcessLogger() {
    return m_loggerImplementation.getProcessLogger();
  }

  public PluginRegistry getPluginRegistry() {
    return m_pluginRegistry;
  }

  public TestRegistry getTestRegistry() {
    return m_testRegistry;
  }

  public String getGrinderID() {
    return m_grinderID;
  }

  public GrinderProperties getProperties() {
    return m_properties;
  }

  public boolean getRecordTime() {
    return m_recordTime;
  }

  public Grinder.ScriptContext getScriptContext() {
    return m_scriptContext;
  }

  /**
   * Unsynchronised for efficiency. {@link GrinderProcess} calls
   * {@link #setExecutionStartTime} just before launching threads,
   * after which it is never called again.
   *
   * @param startTime Start of execution, in milliseconds since Epoch.
   */
  public void setExecutionStartTime(long startTime) {
    m_executionStartTime = startTime;
  }

  /**
   * Unsynchronised for efficiency. {@link GrinderProcess} calls
   * {@link #setExecutionStartTime} just before launching threads,
   * after which it is never called again.
   *
   * @return Start of execution, in milliseconds since Epoch.
   */
  public long getExecutionStartTime() {
    return m_executionStartTime;
  }

  public boolean getShutdown() {
    return m_shutdown;
  }

  public void shutdown() {
    // Interrupt any sleepers.
    Sleeper.shutdownAllCurrentSleepers();

    // Worker threads poll this before each test execution.
    m_shutdown = true;
  }

  public Sleeper getSleeper() {
    return m_sleeper;
  }
}
