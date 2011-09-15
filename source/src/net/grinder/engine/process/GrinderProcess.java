// Copyright (C) 2000 Paco Gomez
// Copyright (C) 2000 - 2011 Philip Aston
// Copyright (C) 2003 Kalyanaraman Venkatasubramaniy
// Copyright (C) 2004 Slavik Gnatenko
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

import java.io.PrintWriter;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import net.grinder.common.GrinderBuild;
import net.grinder.common.GrinderException;
import net.grinder.common.GrinderProperties;
import net.grinder.common.Logger;
import net.grinder.common.SkeletonThreadLifeCycleListener;
import net.grinder.common.Test;
import net.grinder.common.processidentity.WorkerProcessReport;
import net.grinder.communication.ClientSender;
import net.grinder.communication.CommunicationException;
import net.grinder.communication.ConnectionType;
import net.grinder.communication.Message;
import net.grinder.communication.MessageDispatchSender;
import net.grinder.communication.MessagePump;
import net.grinder.communication.QueuedSender;
import net.grinder.communication.QueuedSenderDecorator;
import net.grinder.communication.Receiver;
import net.grinder.engine.common.ConnectorFactory;
import net.grinder.engine.common.EngineException;
import net.grinder.engine.communication.ConsoleListener;
import net.grinder.engine.messages.InitialiseGrinderMessage;
import net.grinder.engine.process.instrumenter.MasterInstrumenter;
import net.grinder.engine.process.jython.JythonScriptEngine;
import net.grinder.messages.console.RegisterTestsMessage;
import net.grinder.messages.console.ReportStatisticsMessage;
import net.grinder.messages.console.WorkerProcessReportMessage;
import net.grinder.script.Grinder;
import net.grinder.script.InternalScriptContext;
import net.grinder.script.InvalidContextException;
import net.grinder.script.Statistics;
import net.grinder.statistics.ExpressionView;
import net.grinder.statistics.StatisticsServices;
import net.grinder.statistics.StatisticsServicesImplementation;
import net.grinder.statistics.StatisticsTable;
import net.grinder.statistics.TestStatisticsMap;
import net.grinder.synchronisation.BarrierGroups;
import net.grinder.synchronisation.GlobalBarrierGroups;
import net.grinder.synchronisation.LocalBarrierGroups;
import net.grinder.util.JVM;
import net.grinder.util.ListenerSupport;
import net.grinder.util.Sleeper;
import net.grinder.util.SleeperImplementation;
import net.grinder.util.StandardTimeAuthority;
import net.grinder.util.TimeAuthority;
import net.grinder.util.ListenerSupport.Informer;
import net.grinder.util.thread.BooleanCondition;
import net.grinder.util.thread.Condition;


/**
 * The controller for a worker process.
 *
 * <p>Package scope.</p>
 *
 * @author Paco Gomez
 * @author Philip Aston
 * @version $Revision$
 * @see net.grinder.engine.process.GrinderThread
 */
final class GrinderProcess {

  private final boolean m_reportTimesToConsole;
  private final QueuedSender m_consoleSender;
  private final LoggerImplementation m_loggerImplementation;
  private final Sleeper m_sleeper;
  private final InitialiseGrinderMessage m_initialisationMessage;
  private final ConsoleListener m_consoleListener;
  private final StatisticsServices m_statisticsServices;
  private final TestStatisticsMap m_accumulatedStatistics;
  private final TestStatisticsHelperImplementation m_testStatisticsHelper;
  private final TestRegistryImplementation m_testRegistryImplementation;
  private final Condition m_eventSynchronisation = new Condition();
  private final MessagePump m_messagePump;

  private final ThreadStarter m_invalidThreadStarter =
    new InvalidThreadStarter();

  private final Times m_times = new Times();

  private final ThreadContexts m_threadContexts = new ThreadContexts();

  private final ListenerSupport<ProcessLifeCycleListener>
    m_processLifeCycleListeners =
      new ListenerSupport<ProcessLifeCycleListener>();

  // Guarded by m_eventSynchronisation.
  private ThreadStarter m_threadStarter = m_invalidThreadStarter;

  private boolean m_shutdownTriggered;
  private boolean m_communicationShutdown;

  /**
   * Creates a new <code>GrinderProcess</code> instance.
   *
   * @param agentReceiver
   *          Receiver used to listen to the agent.
   * @exception GrinderException
   *          If the process could not be created.
   */
  public GrinderProcess(Receiver agentReceiver) throws GrinderException {

    m_initialisationMessage =
      (InitialiseGrinderMessage)agentReceiver.waitForMessage();

    if (m_initialisationMessage == null) {
      throw new EngineException("No control stream from agent");
    }

    final GrinderProperties properties =
      m_initialisationMessage.getProperties();

    m_reportTimesToConsole =
      properties.getBoolean("grinder.reportTimesToConsole", true);

    m_loggerImplementation = new LoggerImplementation(
      m_initialisationMessage.getWorkerIdentity().getName(),
      properties.getProperty(GrinderProperties.LOG_DIRECTORY, "."),
      properties.getBoolean("grinder.logProcessStreams", true),
      properties.getInt("grinder.numberOfOldLogs", 1));

    final Logger processLogger = m_loggerImplementation.getProcessLogger();
    processLogger.output("The Grinder version " +
                         GrinderBuild.getVersionString());
    processLogger.output(JVM.getInstance().toString());
    processLogger.output("time zone is " +
                         new SimpleDateFormat("z (Z)").format(new Date()));

    final MessageDispatchSender messageDispatcher = new MessageDispatchSender();

    final BarrierGroups localBarrierGroups = new LocalBarrierGroups();
    final BarrierGroups globalBarrierGroups;

    if (m_initialisationMessage.getReportToConsole()) {
      m_consoleSender =
        new QueuedSenderDecorator(
          ClientSender.connect(
            new ConnectorFactory(ConnectionType.WORKER).create(properties)));

      globalBarrierGroups =
        new GlobalBarrierGroups(m_consoleSender,
                                messageDispatcher,
                                m_initialisationMessage.getWorkerIdentity());
    }
    else {
      m_consoleSender = new NullQueuedSender();
      globalBarrierGroups = new LocalBarrierGroups();
    }

    final ThreadStarter delegatingThreadStarter = new ThreadStarter() {
      public int startThread(Object testRunner)
        throws EngineException, InvalidContextException {

        final ThreadStarter threadStarter;

        synchronized (m_eventSynchronisation) {
          threadStarter = m_threadStarter;
        }

        return threadStarter.startThread(testRunner);
      }
    };

    m_statisticsServices = StatisticsServicesImplementation.getInstance();

    m_accumulatedStatistics =
      new TestStatisticsMap(m_statisticsServices.getStatisticsSetFactory());
    m_testStatisticsHelper =
      new TestStatisticsHelperImplementation(
        m_statisticsServices.getStatisticsIndexMap());

    m_testRegistryImplementation =
      new TestRegistryImplementation(
                       m_threadContexts,
                       m_statisticsServices.getStatisticsSetFactory(),
                       m_testStatisticsHelper,
                       m_times.getTimeAuthority());

    final Logger externalLogger =
      new ExternalLogger(processLogger, m_threadContexts);

    m_sleeper = new SleeperImplementation(
      m_times.getTimeAuthority(),
      externalLogger,
      properties.getDouble("grinder.sleepTimeFactor", 1.0d),
      properties.getDouble("grinder.sleepTimeVariation", 0.2d));

    final Statistics scriptStatistics =
      new ScriptStatisticsImplementation(m_threadContexts,
                                         m_statisticsServices,
                                         m_consoleSender);

    final ThreadStopper threadStopper = new ThreadStopper() {
      public boolean stopThread(int threadNumber) {
        return m_threadContexts.shutdown(threadNumber);
      }
    };

    final InternalScriptContext scriptContext =
      new ScriptContextImplementation(
        m_initialisationMessage.getWorkerIdentity(),
        m_initialisationMessage.getFirstWorkerIdentity(),
        m_threadContexts,
        properties,
        externalLogger,
        new ExternalFilenameFactory(m_loggerImplementation.getFilenameFactory(),
                                    m_threadContexts),
        m_sleeper,
        new SSLControlImplementation(m_threadContexts),
        scriptStatistics,
        m_testRegistryImplementation,
        delegatingThreadStarter,
        threadStopper,
        localBarrierGroups,
        globalBarrierGroups);

    Grinder.grinder = scriptContext;

    final PluginRegistryImplementation pluginRegistry =
      new PluginRegistryImplementation(externalLogger,
                                       scriptContext,
                                       m_threadContexts,
                                       m_statisticsServices,
                                       m_times.getTimeAuthority());

    m_processLifeCycleListeners.add(pluginRegistry);

    m_processLifeCycleListeners.add(m_threadContexts);

    // If we don't call getLocalHost() before spawning our
    // ConsoleListener thread, any attempt to call it afterwards will
    // silently crash the JVM. Reproduced with both J2SE 1.3.1-b02 and
    // J2SE 1.4.1_03-b02 on W2K. Do not ask me why, I've stopped
    // caring.
    try { java.net.InetAddress.getLocalHost(); }
    catch (UnknownHostException e) { /* Ignore */ }

    m_consoleListener =
      new ConsoleListener(m_eventSynchronisation, processLogger);

    m_consoleListener.registerMessageHandlers(messageDispatcher);
    m_messagePump = new MessagePump(agentReceiver, messageDispatcher, 1);
  }

  /**
   * The application's main loop. This is split from the constructor as
   * theoretically it might be called multiple times. The constructor sets up
   * the static configuration, this does a single execution.
   *
   * <p>
   * This method is interruptible, in the same sense as
   * {@link net.grinder.util.thread.InterruptibleRunnable#interruptibleRun()}.
   * We don't implement that method because we want to be able to throw
   * exceptions.
   * </p>
   *
   * @throws GrinderException
   *           If something went wrong.
   */
  public void run() throws GrinderException {
    final Logger logger = m_loggerImplementation.getProcessLogger();

    final Timer timer = new Timer(true);
    timer.schedule(new TickLoggerTimerTask(), 0, 1000);

    final ScriptEngine scriptEngine = new JythonScriptEngine();

    // Don't start the message pump until we've initialised Jython. Jython 2.5+
    // tests to see whether the stdin stream is a tty, and on some versions of
    // Windows, this synchronises on the stream object's monitor. This clashes
    // with the message pump which starts a thread to call
    // StreamRecevier.waitForMessage(), and so also synchronises on that
    // monitor. See bug 2936167.
    m_messagePump.start();

    final StringBuilder numbers = new StringBuilder("worker process ");
    numbers.append(m_initialisationMessage.getWorkerIdentity().getNumber());

    final int agentNumber =
      m_initialisationMessage
      .getWorkerIdentity().getAgentIdentity().getNumber();

    if (agentNumber >= 0) {
      numbers.append(" of agent number ");
      numbers.append(agentNumber);
    }

    logger.output(numbers.toString());

    final GrinderProperties properties =
      m_initialisationMessage.getProperties();
    final short numberOfThreads =
      properties.getShort("grinder.threads", (short)1);
    final int reportToConsoleInterval =
      properties.getInt("grinder.reportToConsole.interval", 500);
    final int duration = properties.getInt("grinder.duration", 0);

    final MasterInstrumenter instrumenter =
      new MasterInstrumenter(
        logger,
        // This property name is poor, since it really means "If DCR
        // instrumentation is available, use it for Jython". I'm not
        // renaming it, since I expect it only to last a few releases,
        // until DCR becomes the default.
        properties.getBoolean("grinder.dcrinstrumentation", false));

    m_testRegistryImplementation.setInstrumenter(instrumenter);

    logger.output("executing \"" + m_initialisationMessage.getScript() +
      "\" using " + scriptEngine.getDescription());

    scriptEngine.initialise(m_initialisationMessage.getScript());

    // Don't initialise the data writer until now as the script may
    // declare new statistics.
    final PrintWriter dataWriter = m_loggerImplementation.getDataWriter();

    dataWriter.print("Thread, Run, Test, Start time (ms since Epoch)");

    final ExpressionView[] detailExpressionViews =
      m_statisticsServices.getDetailStatisticsView().getExpressionViews();

    for (int i = 0; i < detailExpressionViews.length; ++i) {
      dataWriter.print(", " + detailExpressionViews[i].getDisplayName());
    }

    dataWriter.println();

    sendStatusMessage(WorkerProcessReport.STATE_STARTED,
                      (short)0,
                      numberOfThreads);

    final ThreadSynchronisation threadSynchronisation =
      new ThreadSynchronisation(m_eventSynchronisation);

    logger.output("starting threads", Logger.LOG | Logger.TERMINAL);

    synchronized (m_eventSynchronisation) {
      m_threadStarter =
        new ThreadStarterImplementation(threadSynchronisation, scriptEngine);

      for (int i = 0; i < numberOfThreads; i++) {
        m_threadStarter.startThread(null);
      }
    }

    threadSynchronisation.startThreads();

    m_times.setExecutionStartTime();
    logger.output("start time is " + m_times.getExecutionStartTime() +
                  " ms since Epoch");

    final TimerTask reportTimerTask =
      new ReportToConsoleTimerTask(threadSynchronisation);
    final TimerTask shutdownTimerTask = new ShutdownTimerTask();

    // Schedule a regular statistics report to the console. We don't
    // need to schedule this at a fixed rate. Each report contains the
    // work done since the last report.

    // First (empty) report to console to start it recording if its
    // not already.
    reportTimerTask.run();

    timer.schedule(reportTimerTask, reportToConsoleInterval,
                   reportToConsoleInterval);

    try {
      if (duration > 0) {
        logger.output("will shut down after " + duration + " ms",
                      Logger.LOG | Logger.TERMINAL);

        timer.schedule(shutdownTimerTask, duration);
      }

      // Wait for a termination event.
      synchronized (m_eventSynchronisation) {
        while (!threadSynchronisation.isFinished()) {

          if (m_consoleListener.checkForMessage(ConsoleListener.ANY ^
                                                ConsoleListener.START)) {
            break;
          }

          if (m_shutdownTriggered) {
            logger.output("specified duration exceeded, shutting down",
                          Logger.LOG | Logger.TERMINAL);
            break;
          }

          m_eventSynchronisation.waitNoInterrruptException();
        }
      }

      synchronized (m_eventSynchronisation) {
        if (!threadSynchronisation.isFinished()) {

          logger.output("waiting for threads to terminate",
                        Logger.LOG | Logger.TERMINAL);

          m_threadStarter = m_invalidThreadStarter;
          m_threadContexts.shutdownAll();

          // Interrupt any sleepers.
          SleeperImplementation.shutdownAllCurrentSleepers();

          final long time = System.currentTimeMillis();
          final long maximumShutdownTime = 10000;

          while (!threadSynchronisation.isFinished()) {
            if (System.currentTimeMillis() - time > maximumShutdownTime) {
              logger.output("ignoring unresponsive threads",
                            Logger.LOG | Logger.TERMINAL);
              break;
            }

            m_eventSynchronisation.waitNoInterrruptException(
              maximumShutdownTime);
          }
        }
      }
    }
    finally {
      reportTimerTask.cancel();
      shutdownTimerTask.cancel();
    }

    scriptEngine.shutdown();

    // Final report to the console.
    reportTimerTask.run();

    m_loggerImplementation.getDataWriter().close();

    if (!m_communicationShutdown) {
      sendStatusMessage(WorkerProcessReport.STATE_FINISHED,
                        (short)0,
                        (short)0);
    }

    m_consoleSender.shutdown();

    final long elapsedTime = m_times.getElapsedTime();
    logger.output("elapsed time is " + elapsedTime + " ms");

    logger.output("Final statistics for this process:");

    final StatisticsTable statisticsTable =
      new StatisticsTable(m_statisticsServices.getSummaryStatisticsView(),
                          m_statisticsServices.getStatisticsIndexMap(),
                          m_accumulatedStatistics);

    statisticsTable.print(logger.getOutputLogWriter(), elapsedTime);

    timer.cancel();

    logger.output("finished", Logger.LOG | Logger.TERMINAL);
  }

  public void shutdown(boolean inputStreamIsStdin) {
    if (!inputStreamIsStdin) {
      // Sadly it appears its impossible to interrupt a read() on a process
      // input stream (at least under W2K), so we can't shut down the message
      // pump cleanly. It runs in a daemon thread, so this isn't a big
      // deal.
      m_messagePump.shutdown();
    }

    m_loggerImplementation.close();
  }

  public Logger getLogger() {
    return m_loggerImplementation.getProcessLogger();
  }

  private class ReportToConsoleTimerTask extends TimerTask {
    private final ThreadSynchronisation m_threads;

    public ReportToConsoleTimerTask(ThreadSynchronisation threads) {
      m_threads = threads;
    }

    public void run() {
      m_loggerImplementation.getDataWriter().flush();

      if (!m_communicationShutdown) {
        try {
          final TestStatisticsMap sample =
            m_testRegistryImplementation.getTestStatisticsMap().reset();
          m_accumulatedStatistics.add(sample);

          // We look up the new tests after we've taken the sample to
          // avoid a race condition when new tests are being added.
          final Collection<Test> newTests =
            m_testRegistryImplementation.getNewTests();

          if (newTests != null) {
            m_consoleSender.queue(new RegisterTestsMessage(newTests));
          }

          if (sample.size() > 0) {
            if (!m_reportTimesToConsole) {
              m_testStatisticsHelper.removeTestTimeFromSample(sample);
            }

            m_consoleSender.queue(new ReportStatisticsMessage(sample));
          }

          sendStatusMessage(WorkerProcessReport.STATE_RUNNING,
                            m_threads.getNumberOfRunningThreads(),
                            m_threads.getTotalNumberOfThreads());
        }
        catch (CommunicationException e) {
          final Logger logger = m_loggerImplementation.getProcessLogger();

          logger.output("Report to console failed: " + e.getMessage(),
                        Logger.LOG | Logger.TERMINAL);

          e.printStackTrace(logger.getErrorLogWriter());

          m_communicationShutdown = true;
        }
      }
    }
  }

  private void sendStatusMessage(short state,
                                 short numberOfThreads,
                                 short totalNumberOfThreads)
    throws CommunicationException {

    m_consoleSender.send(new WorkerProcessReportMessage(
                           m_initialisationMessage.getWorkerIdentity(),
                           state,
                           numberOfThreads,
                           totalNumberOfThreads));
  }

  private class ShutdownTimerTask extends TimerTask {
    public void run() {
      synchronized (m_eventSynchronisation) {
        m_shutdownTriggered = true;
        m_eventSynchronisation.notifyAll();
      }
    }
  }

  private static class TickLoggerTimerTask extends TimerTask {
    public void run() {
      LoggerImplementation.tick();
    }
  }

  /**
   * Implement {@link WorkerThreadSynchronisation}. I looked hard at JSR 166's
   * <code>CountDownLatch</code> and <code>CyclicBarrier</code>, but neither
   * of them allow for the waiting thread to be interrupted by other events.
   *
   * <p>Package scope for unit tests.</p>
   */
  static class ThreadSynchronisation implements WorkerThreadSynchronisation {
    private final BooleanCondition m_started = new BooleanCondition();
    private final Condition m_threadEventCondition;

    private short m_numberCreated = 0;
    private short m_numberAwaitingStart = 0;
    private short m_numberFinished = 0;

    ThreadSynchronisation(Condition condition) {
      m_threadEventCondition = condition;
    }

    /**
     * The number of worker threads that have been created but not run to
     * completion.
     */
    public short getNumberOfRunningThreads() {
      synchronized (m_threadEventCondition) {
        return (short)(m_numberCreated - m_numberFinished);
      }
    }

    public boolean isReadyToStart() {
      synchronized (m_threadEventCondition) {
        return m_numberAwaitingStart >= getNumberOfRunningThreads();
      }
    }

    public boolean isFinished() {
      return getNumberOfRunningThreads() <= 0;
    }

    /**
     * The number of worker threads that have been created.
     */
    public short getTotalNumberOfThreads() {
      synchronized (m_threadEventCondition) {
        return m_numberCreated;
      }
    }

    public void threadCreated() {
      synchronized (m_threadEventCondition) {
        ++m_numberCreated;
      }
    }

    public void startThreads() {
      synchronized (m_threadEventCondition) {
        while (!isReadyToStart()) {
          m_threadEventCondition.waitNoInterrruptException();
        }

        m_numberAwaitingStart = 0;
      }

      m_started.set(true);
    }

    public void awaitStart() {
      synchronized (m_threadEventCondition) {
        ++m_numberAwaitingStart;

        if (isReadyToStart()) {
          m_threadEventCondition.notifyAll();
        }
      }

      m_started.await(true);
    }

    public void threadFinished() {
      synchronized (m_threadEventCondition) {
        ++m_numberFinished;

        if (isReadyToStart() || isFinished()) {
          m_threadEventCondition.notifyAll();
        }
      }
    }
  }

  private final class ThreadStarterImplementation implements ThreadStarter {
    private final ThreadSynchronisation m_threadSynchronisation;
    private final ScriptEngine m_scriptEngine;

    private final ProcessLifeCycleListener m_threadLifeCycleCallbacks =
      new ProcessLifeCycleListener() {
        public void threadCreated(final ThreadContext threadContext) {
          m_processLifeCycleListeners.apply(
            new Informer<ProcessLifeCycleListener>() {
              public void inform(ProcessLifeCycleListener listener) {
                listener.threadCreated(threadContext);
              }
            });
        }

        public void threadStarted(final ThreadContext threadContext) {
          m_processLifeCycleListeners.apply(
            new Informer<ProcessLifeCycleListener>() {
              public void inform(ProcessLifeCycleListener listener) {
                listener.threadStarted(threadContext);
              }
            });
        }
      };

    private int m_i = -1;

    private ThreadStarterImplementation(
      ThreadSynchronisation threadSynchronisation,
      ScriptEngine scriptEngine) {
      m_threadSynchronisation = threadSynchronisation;
      m_scriptEngine = scriptEngine;
    }

    public int startThread(Object testRunner) throws EngineException {
      final int threadNumber;
      synchronized (this) {
        threadNumber = ++m_i;
      }

      final GrinderThread runnable;

      if (testRunner != null) {
        runnable =
          new GrinderThread(m_threadSynchronisation,
                            m_threadLifeCycleCallbacks,
                            m_initialisationMessage.getProperties(),
                            m_sleeper,
                            m_loggerImplementation,
                            m_statisticsServices,
                            m_scriptEngine,
                            threadNumber,
                            m_scriptEngine.createWorkerRunnable(testRunner));
      }
      else {
        runnable =
          new GrinderThread(m_threadSynchronisation,
                            m_threadLifeCycleCallbacks,
                            m_initialisationMessage.getProperties(),
                            m_sleeper,
                            m_loggerImplementation,
                            m_statisticsServices,
                            m_scriptEngine,
                            threadNumber,
                            null);
      }

      final Thread t = new Thread(runnable, "Grinder thread " + threadNumber);
      t.setDaemon(true);
      t.start();

      return threadNumber;
    }
  }

  /**
   * Package scope for unit tests.
   */
  static final class InvalidThreadStarter implements ThreadStarter {
    public int startThread(Object testRunner) throws InvalidContextException {
      throw new InvalidContextException(
        "You should not start worker threads until the main thread has " +
        "initialised the script engine, or after all other threads have " +
        "shut down. Typically, you should only call startWorkerThread() from " +
        "another worker thread.");
    }
  }

  /**
   * Package scope for unit tests.
   */
  static final class Times {
    private volatile long m_executionStartTime;

    private final TimeAuthority  m_timeAuthority = new StandardTimeAuthority();

    /**
     * {@link GrinderProcess} calls {@link #setExecutionStartTime} just
     * before launching threads, after which it is never called again.
     */
    public void setExecutionStartTime() {
      m_executionStartTime = m_timeAuthority.getTimeInMilliseconds();
    }

    /**
     * {@link GrinderProcess} calls {@link #setExecutionStartTime} just before
     * launching threads, after which it is never called again.
     *
     * @return Start of execution, in milliseconds since the Epoch.
     */
    public long getExecutionStartTime() {
      return m_executionStartTime;
    }

    /**
     * Elapsed time since execution was started.
     *
     * @return The time in milliseconds.
     * @see #getExecutionStartTime()
     */
    public long getElapsedTime() {
      return m_timeAuthority.getTimeInMilliseconds() - getExecutionStartTime();
    }

    public TimeAuthority getTimeAuthority() {
      return m_timeAuthority;
    }
  }

  /**
   * Package scope for unit tests.
   */
  static final class ThreadContexts
    implements ProcessLifeCycleListener, ThreadContextLocator {

    private final ThreadLocal<ThreadContext> m_threadContextThreadLocal =
      new ThreadLocal<ThreadContext>();

    // Guarded by self.
    private final Map<Integer, ThreadContext> m_threadContextsMap =
      new HashMap<Integer, ThreadContext>();

    // Guarded by m_threadContextsMap.
    private boolean m_allShutdown;

    public ThreadContext get() {
      return m_threadContextThreadLocal.get();
    }

    public void threadCreated(ThreadContext threadContext) {
      final Integer threadNumber = threadContext.getThreadNumber();

      final boolean shutdown;

      synchronized (m_threadContextsMap) {
        shutdown = m_allShutdown;

        if (!shutdown) {
          threadContext.registerThreadLifeCycleListener(
            new SkeletonThreadLifeCycleListener() {
              public void endThread() {
                m_threadContextsMap.remove(threadNumber);
              }
            });

          // Very unlikely, harmless race here - we could store a reference to
          // a thread context that is in the process of shutting down.
          m_threadContextsMap.put(threadNumber, threadContext);
        }
      }

      if (shutdown) {
        // Stop new threads in their tracks.
        threadContext.shutdown();
      }
    }

    public void threadStarted(ThreadContext threadContext) {
      m_threadContextThreadLocal.set(threadContext);
    }

    public boolean shutdown(int threadNumber) {
      final ThreadContext threadContext;

      synchronized (m_threadContextsMap) {
        threadContext = m_threadContextsMap.get(threadNumber);
      }

      if (threadContext != null) {
        threadContext.shutdown();
        return true;
      }

      return false;
    }

    public void shutdownAll() {
      final ThreadContext[] threadContexts;

      synchronized (m_threadContextsMap) {
        m_allShutdown = true;

        threadContexts =
          m_threadContextsMap.values().toArray(
            new ThreadContext[m_threadContextsMap.size()]);
      }

      for (int i = 0; i < threadContexts.length; ++i) {
        threadContexts[i].shutdown();
      }
    }
  }

  /**
   * Package scope for unit tests.
   */
  static final class NullQueuedSender implements QueuedSender {
    public void send(Message message) { }

    public void flush() { }

    public void queue(Message message) { }

    public void shutdown() { }
  }
}
