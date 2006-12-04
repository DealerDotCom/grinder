// Copyright (C) 2000 Paco Gomez
// Copyright (C) 2000 - 2006 Philip Aston
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
// REGENTS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
// INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
// (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
// SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
// HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
// STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
// ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
// OF THE POSSIBILITY OF SUCH DAMAGE.

package net.grinder.engine.process;

import java.io.File;
import java.io.PrintWriter;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.Timer;
import java.util.TimerTask;

import net.grinder.common.GrinderBuild;
import net.grinder.common.GrinderException;
import net.grinder.common.GrinderProperties;
import net.grinder.common.Logger;
import net.grinder.common.WorkerProcessReport;
import net.grinder.communication.ClientSender;
import net.grinder.communication.CommunicationDefaults;
import net.grinder.communication.CommunicationException;
import net.grinder.communication.ConnectionType;
import net.grinder.communication.Connector;
import net.grinder.communication.HandlerChainSender;
import net.grinder.communication.Message;
import net.grinder.communication.MessagePump;
import net.grinder.communication.QueuedSender;
import net.grinder.communication.QueuedSenderDecorator;
import net.grinder.communication.Receiver;
import net.grinder.console.messages.RegisterTestsMessage;
import net.grinder.engine.common.ConsoleListener;
import net.grinder.engine.common.EngineException;
import net.grinder.engine.messages.InitialiseGrinderMessage;
import net.grinder.engine.process.jython.JythonScriptEngine;
import net.grinder.statistics.ExpressionView;
import net.grinder.statistics.StatisticsServicesImplementation;
import net.grinder.statistics.StatisticsTable;
import net.grinder.statistics.TestStatisticsMap;
import net.grinder.util.JVM;
import net.grinder.util.thread.Monitor;


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

  private final ProcessContext m_context;
  private final LoggerImplementation m_loggerImplementation;
  private final InitialiseGrinderMessage m_initialisationMessage;
  private final ConsoleListener m_consoleListener;
  private final TestStatisticsMap m_accumulatedStatistics;
  private final Monitor m_eventSynchronisation = new Monitor();
  private final MessagePump m_messagePump;

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

    m_loggerImplementation = new LoggerImplementation(
      m_initialisationMessage.getWorkerIdentity().getName(),
      properties.getProperty("grinder.logDirectory", "."),
      properties.getBoolean("grinder.logProcessStreams", true),
      properties.getInt("grinder.numberOfOldLogs", 1));

    final Logger processLogger = m_loggerImplementation.getProcessLogger();
    processLogger.output("The Grinder version " +
                         GrinderBuild.getVersionString());
    processLogger.output(JVM.getInstance().toString());

    final QueuedSender consoleSender;

    if (m_initialisationMessage.getReportToConsole()) {
      final Connector connector =
        new Connector(
          properties.getProperty("grinder.consoleHost",
                                 CommunicationDefaults.CONSOLE_HOST),
          properties.getInt("grinder.consolePort",
                            CommunicationDefaults.CONSOLE_PORT),
          ConnectionType.WORKER);

      consoleSender =
        new QueuedSenderDecorator(ClientSender.connect(connector));
    }
    else {
      // Null Sender implementation.
      consoleSender = new QueuedSender() {
          public void send(Message message) { }
          public void flush() { }
          public void queue(Message message) { }
          public void shutdown() { }
        };
    }

    m_context =
      new ProcessContextImplementation(
        m_initialisationMessage.getWorkerIdentity(),
        properties,
        processLogger,
        m_loggerImplementation.getFilenameFactory(),
        consoleSender,
        StatisticsServicesImplementation.getInstance());

    // If we don't call getLocalHost() before spawning our
    // ConsoleListener thread, any attempt to call it afterwards will
    // silently crash the JVM. Reproduced with both J2SE 1.3.1-b02 and
    // J2SE 1.4.1_03-b02 on W2K. Do not ask me why, I've stopped
    // caring.
    try { java.net.InetAddress.getLocalHost(); }
    catch (UnknownHostException e) { /* Ignore */ }

    m_consoleListener =
      new ConsoleListener(m_eventSynchronisation, processLogger);
    m_accumulatedStatistics =
      new TestStatisticsMap(
        m_context.getStatisticsServices().getStatisticsSetFactory());

    final HandlerChainSender handlerChainSender = new HandlerChainSender();
    handlerChainSender.add(m_consoleListener.getMessageHandler());
    m_messagePump = new MessagePump(agentReceiver, handlerChainSender, 1);
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
    final Logger logger = m_context.getProcessLogger();

    final Timer timer = new Timer(true);
    timer.schedule(new TickLoggerTimerTask(), 0, 1000);

    final JythonScriptEngine scriptEngine =
      new JythonScriptEngine(m_context.getScriptContext());

    m_context.getTestRegistry().setScriptEngine(scriptEngine);

    final File scriptFile = m_initialisationMessage.getScriptFile();
    logger.output("executing \"" + scriptFile.getPath() + "\" using " +
                  scriptEngine.getDescription());

    scriptEngine.initialise(scriptFile,
                            m_initialisationMessage.getScriptDirectory());

    final GrinderProperties properties = m_context.getProperties();
    final short numberOfThreads =
      properties.getShort("grinder.threads", (short)1);
    final int reportToConsoleInterval =
      properties.getInt("grinder.reportToConsole.interval", 500);
    final int duration = properties.getInt("grinder.duration", 0);

    // Don't initialise the data writer until now as the script may
    // declare new statistics.
    final PrintWriter dataWriter = m_loggerImplementation.getDataWriter();

    dataWriter.print("Thread, Run, Test, Milliseconds since start");

    final ExpressionView[] detailExpressionViews =
      m_context.getStatisticsServices()
      .getDetailStatisticsView().getExpressionViews();

    for (int i = 0; i < detailExpressionViews.length; ++i) {
      dataWriter.print(", " + detailExpressionViews[i].getDisplayName());
    }

    dataWriter.println();

    final QueuedSender consoleSender = m_context.getConsoleSender();

    consoleSender.send(
      m_context.createStatusMessage(
        WorkerProcessReport.STATE_STARTED, (short)0, numberOfThreads));

    final GrinderThread[] runnable = new GrinderThread[numberOfThreads];

    for (int i = 0; i < numberOfThreads; i++) {
      runnable[i] =
        new GrinderThread(m_eventSynchronisation, m_context,
                          m_loggerImplementation, scriptEngine, i);
    }

    logger.output("starting threads", Logger.LOG | Logger.TERMINAL);

    m_context.setExecutionStartTime();

    // Start the threads.
    for (int i = 0; i < numberOfThreads; i++) {
      final Thread t = new Thread(runnable[i], "Grinder thread " + i);
      t.setDaemon(true);
      t.start();
    }

    final TimerTask reportTimerTask =
      new ReportToConsoleTimerTask(numberOfThreads);
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
        logger.output("will shutdown after " + duration + " ms",
                      Logger.LOG | Logger.TERMINAL);

        timer.schedule(shutdownTimerTask, duration);
      }

      // Wait for a termination event.
      synchronized (m_eventSynchronisation) {
        while (GrinderThread.getNumberOfThreads() > 0) {

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
        if (GrinderThread.getNumberOfThreads() > 0) {

          logger.output("waiting for threads to terminate",
                        Logger.LOG | Logger.TERMINAL);

          m_context.shutdown();

          final long time = System.currentTimeMillis();
          final long maximumShutdownTime = 10000;

          while (GrinderThread.getNumberOfThreads() > 0) {
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
      consoleSender.send(
        m_context.createStatusMessage(
          WorkerProcessReport.STATE_FINISHED, (short)0, (short)0));
    }

    consoleSender.shutdown();

    logger.output("Final statistics for this process:");

    final StatisticsTable statisticsTable =
      new StatisticsTable(
        m_context.getStatisticsServices().getSummaryStatisticsView(),
        m_accumulatedStatistics);

    statisticsTable.print(logger.getOutputLogWriter());

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
    return m_context.getProcessLogger();
  }

  private class ReportToConsoleTimerTask extends TimerTask {
    private final short m_totalThreads;

    public ReportToConsoleTimerTask(short totalThreads) {
      m_totalThreads = totalThreads;
    }

    public void run() {
      m_loggerImplementation.getDataWriter().flush();

      if (!m_communicationShutdown) {
        final QueuedSender consoleSender = m_context.getConsoleSender();

        try {
          final TestStatisticsMap sample =
            m_context.getTestRegistry().getTestStatisticsMap().reset();
          m_accumulatedStatistics.add(sample);

          // We look up the new tests after we've taken the sample to
          // avoid a race condition when new tests are being added.
          final Collection newTests =
            m_context.getTestRegistry().getNewTests();

          if (newTests != null) {
            consoleSender.queue(new RegisterTestsMessage(newTests));
          }

          if (sample.size() > 0) {
            consoleSender.queue(
              m_context.createReportStatisticsMessage(sample));
          }

          consoleSender.send(
            m_context.createStatusMessage(WorkerProcessReport.STATE_RUNNING,
                                          GrinderThread.getNumberOfThreads(),
                                          m_totalThreads));
        }
        catch (CommunicationException e) {
          final Logger logger = m_context.getProcessLogger();

          logger.output("Report to console failed: " + e.getMessage(),
                        Logger.LOG | Logger.TERMINAL);

          e.printStackTrace(logger.getErrorLogWriter());

          m_communicationShutdown = true;
        }
      }
    }
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
}
