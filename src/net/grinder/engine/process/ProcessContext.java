// Copyright (C) 2000 Paco Gomez
// Copyright (C) 2000, 2001, 2002, 2003 Philip Aston
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
import net.grinder.communication.CommunicationDefaults;
import net.grinder.communication.CommunicationException;
import net.grinder.communication.Message;
import net.grinder.communication.Sender;
import net.grinder.communication.UnicastSender;
import net.grinder.script.Grinder;
import net.grinder.statistics.CommonStatisticsViews;
import net.grinder.statistics.ExpressionView;
import net.grinder.util.Sleeper;


/**
 * @author Philip Aston
 * @version $Revision$
 * @stereotype singleton
 **/
class ProcessContext {
  private final String m_grinderID;
  private final GrinderProperties m_properties;
  private final boolean m_recordTime;
  private final LoggerImplementation m_loggerImplementation;
  private final Logger m_processLogger;
  private final Sender m_consoleSender;
  private final PluginRegistry m_pluginRegistry;
  private final TestRegistry m_testRegistry;
  private final Grinder.ScriptContext m_scriptContext;
  private final boolean m_receiveConsoleSignals;

  private long m_executionStartTime;
  private boolean m_shutdown;

  ProcessContext(String grinderID, GrinderProperties properties)
    throws GrinderException {

    m_grinderID = grinderID;
    m_properties = properties;

    m_recordTime = properties.getBoolean("grinder.recordTime", true);

    final int numberOfOldLogs =
      properties.getInt("grinder.numberOfOldLogs", 1);

    m_loggerImplementation =
      new LoggerImplementation(m_grinderID,
                               properties.getProperty(
                                 "grinder.logDirectory", "."),
                               properties.getBoolean(
                                 "grinder.logProcessStreams", true),
                               numberOfOldLogs);

    m_processLogger = m_loggerImplementation.getProcessLogger();

    Sender consoleSender = null;

    if (properties.getBoolean("grinder.reportToConsole", true)) {
      final String consoleAddress =
        properties.getProperty("grinder.consoleAddress",
                               CommunicationDefaults.CONSOLE_ADDRESS);

      final int consolePort =
        properties.getInt("grinder.consolePort",
                          CommunicationDefaults.CONSOLE_PORT);

      try {
        consoleSender =
          new UnicastSender(getGrinderID(), consoleAddress, consolePort);
      }
      catch (CommunicationException e) {
        m_processLogger.output(
          "Unable to report to console (" + e.getMessage() +
          "); proceeding without the console. Set " +
          "grinder.reportToConsole=false to disable this warning.",
          Logger.LOG | Logger.TERMINAL);
      }
    }

    if (consoleSender != null) {
      m_consoleSender = consoleSender;

      m_receiveConsoleSignals =
        properties.getBoolean("grinder.receiveConsoleSignals", true);
    }
    else {
      // Null Sender implementation.
      m_consoleSender = new Sender() {
          public void send(Message message) { }
          public void flush() { }
          public void queue(Message message) { }
          public void shutdown() { }
        };

      m_receiveConsoleSignals = false;
    }

    m_pluginRegistry = new PluginRegistry(this);
    m_testRegistry = new TestRegistry();
    m_scriptContext = new ScriptContextImplementation(this);
    Grinder.grinder = m_scriptContext;
    m_shutdown = false;
  }

  public final void initialiseDataWriter() {

    final PrintWriter dataWriter = m_loggerImplementation.getDataWriter();

    dataWriter.print("Thread, Run, Test, Milliseconds since start");

    final ExpressionView[] detailExpressionViews =
      CommonStatisticsViews.getDetailStatisticsView().getExpressionViews();

    for (int i = 0; i < detailExpressionViews.length; ++i) {
      dataWriter.print(", " + detailExpressionViews[i].getDisplayName());
    }

    dataWriter.println();
  }

  public final Sender getConsoleSender() {
    return m_consoleSender;
  }

  public final LoggerImplementation getLoggerImplementation() {
    return m_loggerImplementation;
  }

  public final Logger getLogger() {
    return m_processLogger;
  }

  public final PluginRegistry getPluginRegistry() {
    return m_pluginRegistry;
  }

  public final TestRegistry getTestRegistry() {
    return m_testRegistry;
  }

  public final String getGrinderID() {
    return m_grinderID;
  }

  public final GrinderProperties getProperties() {
    return m_properties;
  }

  public final boolean getRecordTime() {
    return m_recordTime;
  }

  public final Grinder.ScriptContext getScriptContext() {
    return m_scriptContext;
  }

  public final boolean getReceiveConsoleSignals() {
    return m_receiveConsoleSignals;
  }

  /**
   * Unsynchronised for efficiency. {@link GrinderProcess} calls
   * {@link #setExecutionStartTime} just before launching threads,
   * after which it is never called again.
   *
   * @param startTime Start of execution, in milliseconds since Epoch.
   */
  public final void setExecutionStartTime(long startTime) {
    m_executionStartTime = startTime;
  }

  /**
   * Unsynchronised for efficiency. {@link GrinderProcess} calls
   * {@link #setExecutionStartTime} just before launching threads,
   * after which it is never called again.
   *
   * @return Start of execution, in milliseconds since Epoch.
   */
  public final long getExecutionStartTime() {
    return m_executionStartTime;
  }

  public final boolean getShutdown() {
    return m_shutdown;
  }

  public final void shutdown() {
    // Interrupt any sleepers.
    Sleeper.shutdownAllCurrentSleepers();

    // Worker threads poll this before each test execution.
    m_shutdown = true;
  }
}
