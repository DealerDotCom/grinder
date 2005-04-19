// Copyright (C) 2000 Paco Gomez
// Copyright (C) 2000, 2001, 2002, 2003, 2004, 2005 Philip Aston
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

import net.grinder.common.FilenameFactory;
import net.grinder.common.GrinderException;
import net.grinder.common.GrinderProperties;
import net.grinder.common.Logger;
import net.grinder.communication.QueuedSender;
import net.grinder.console.messages.WorkerProcessStatusMessage;
import net.grinder.script.Grinder;
import net.grinder.script.SSLControl;
import net.grinder.util.Sleeper;


/**
 * Process wide state.
 *
 * @author Philip Aston
 * @version $Revision$
 */
final class ProcessContext {
  private final String m_agentID;
  private final String m_workerID;
  private final String m_uniqueProcessID;
  private final GrinderProperties m_properties;
  private final boolean m_recordTime;
  private final Logger m_processLogger;
  private final QueuedSender m_consoleSender;
  private final ThreadContextLocator m_threadContextLocator;
  private final PluginRegistryImplementation m_pluginRegistry;
  private final TestRegistry m_testRegistry;
  private final Grinder.ScriptContext m_scriptContext;
  private final Sleeper m_sleeper;

  private long m_executionStartTime;
  private boolean m_shutdown;

  ProcessContext(String agentID, String workerID, GrinderProperties properties,
                 Logger logger, FilenameFactory filenameFactory,
                 QueuedSender consoleSender)
    throws GrinderException {

    m_agentID = agentID;
    m_workerID = workerID;
    m_uniqueProcessID = workerID + ":" + System.currentTimeMillis();
    m_properties = properties;
    m_recordTime = properties.getBoolean("grinder.recordTime", true);
    m_processLogger = logger;
    m_consoleSender = consoleSender;
    m_threadContextLocator = new ThreadContextLocatorImplementation();

    final Logger externalLogger =
      new ExternalLogger(m_processLogger, m_threadContextLocator);

    final FilenameFactory externalFilenameFactory =
      new ExternalFilenameFactory(filenameFactory, m_threadContextLocator);

    m_sleeper = new Sleeper(
      properties.getDouble("grinder.sleepTimeFactor", 1.0d),
      properties.getDouble("grinder.sleepTimeVariation", 0.2d),
      externalLogger);

    final SSLControl sslControl =
      new SSLControlImplementation(m_threadContextLocator);

    m_scriptContext = new ScriptContextImplementation(
      m_workerID,
      m_threadContextLocator,
      properties,
      m_consoleSender,
      externalLogger,
      externalFilenameFactory,
      m_sleeper,
      sslControl);

    m_pluginRegistry =
      new PluginRegistryImplementation(externalLogger, m_scriptContext,
                                       m_threadContextLocator);

    m_testRegistry = new TestRegistry(m_threadContextLocator);
    TestRegistry.setInstance(m_testRegistry);

    Grinder.grinder = m_scriptContext;
    m_shutdown = false;
  }

  public QueuedSender getConsoleSender() {
    return m_consoleSender;
  }

  public WorkerProcessStatusMessage createStatusMessage(
    short state, short numberOfThreads, short totalNumberOfThreads) {

    return new WorkerProcessStatusMessage(m_uniqueProcessID, m_workerID, state,
                                   numberOfThreads, totalNumberOfThreads);
  }

  public Logger getProcessLogger() {
    return m_processLogger;
  }

  public PluginRegistryImplementation getPluginRegistry() {
    return m_pluginRegistry;
  }

  public TestRegistry getTestRegistry() {
    return m_testRegistry;
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

  public ThreadContextLocator getThreadContextLocator() {
    return m_threadContextLocator;
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

  private static final class ThreadContextLocatorImplementation
    implements ThreadContextLocator  {

    private final ThreadLocal m_threadContextThreadLocal = new ThreadLocal();

    public ThreadContext get() {
      return (ThreadContext)m_threadContextThreadLocal.get();
    }

    public void set(ThreadContext threadContext) {
      m_threadContextThreadLocal.set(threadContext);
    }
  }
}
