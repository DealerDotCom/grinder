// Copyright (C) 2000 Paco Gomez
// Copyright (C) 2000 - 2006 Philip Aston
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

import net.grinder.common.GrinderProperties;
import net.grinder.engine.common.EngineException;
import net.grinder.engine.process.ScriptEngine.ScriptExecutionException;
import net.grinder.util.Sleeper;


/**
 * The class executed by each thread.
 *
 * @author Paco Gomez
 * @author Philip Aston
 * @version $Revision$
 */
class GrinderThread implements java.lang.Runnable {

  /**
   * m_numberOfThreads is incremented in constructor
   * rather than in run to avoid pathological race conditions. Hence
   * it really means "the number of GrinderThread's that have been
   * created but not run to completion"
   */
  private static short s_numberOfThreads = 0;

  private final Object m_notifyOnCompletion;
  private final ProcessContext m_processContext;
  private final ScriptEngine m_scriptEngine;
  private final ThreadContext m_context;

  private final long m_initialSleepTime;
  private final int m_numberOfRuns;

  /**
   * The constructor.
   */
  public GrinderThread(Object notifyOnCompletion,
                       ProcessContext processContext,
                       LoggerImplementation loggerImplementation,
                       ScriptEngine scriptEngine,
                       int threadID)
    throws EngineException {

    m_notifyOnCompletion = notifyOnCompletion;
    m_processContext = processContext;
    m_scriptEngine = scriptEngine;

    m_context =
      new ThreadContextImplementation(
        processContext,
        loggerImplementation.createThreadLogger(threadID),
        loggerImplementation.getFilenameFactory().
        createSubContextFilenameFactory(Integer.toString(threadID)),
        loggerImplementation.getDataWriter());

    final GrinderProperties properties = processContext.getProperties();

    m_initialSleepTime = properties.getLong("grinder.initialSleepTime", 0);

    m_numberOfRuns = properties.getInt("grinder.runs", 1);

    incrementThreadCount();    // See s_numberOfThreads javadoc.
  }

  /**
   * The thread's main loop.
   */
  public void run() {

    m_processContext.getThreadContextLocator().set(m_context);

    final ThreadLogger logger = m_context.getThreadLogger();
    final PrintWriter errorWriter = logger.getErrorLogWriter();

    logger.setCurrentRunNumber(-1);

    try {
      final ScriptEngine.WorkerRunnable scriptThreadRunnable =
        m_scriptEngine.createWorkerRunnable();

      m_processContext.getSleeper().sleepFlat(m_initialSleepTime);

      if (m_numberOfRuns == 0) {
        logger.output("about to run forever");
      }
      else {
        logger.output("about to do " + m_numberOfRuns + " run" +
                      (m_numberOfRuns == 1 ? "" : "s"));
      }

      int currentRun;

      for (currentRun = 0;
           m_numberOfRuns == 0 || currentRun < m_numberOfRuns;
           currentRun++) {

        logger.setCurrentRunNumber(currentRun);

        m_context.beginRunEvent();

        try {
          scriptThreadRunnable.run();
        }
        catch (ScriptExecutionException e) {
          final Throwable cause = e.getCause();

          if (cause instanceof ShutdownException ||
              cause instanceof Sleeper.ShutdownException) {
            logger.output("shutdown");
            break;
          }

          // Sadly PrintWriter only exposes its lock object to subclasses.
          synchronized (errorWriter) {
            logger.error("Aborted run due to " + e.getShortMessage());
            e.printStackTrace(errorWriter);
          }
        }

        m_context.endRunEvent();
      }

      logger.setCurrentRunNumber(-1);

      logger.output("finished " + currentRun +
                    (currentRun == 1 ? " run" : " runs"));

      try {
        scriptThreadRunnable.shutdown();
      }
      catch (ScriptExecutionException e) {
        // Sadly PrintWriter only exposes its lock object to subclasses.
        synchronized (errorWriter) {
          logger.error(
            "Aborted test runner shutdown due to " + e.getShortMessage());
          e.printStackTrace(errorWriter);
        }
      }
    }
    catch (ScriptExecutionException e) {
      synchronized (errorWriter) {
        logger.error("Aborting thread due to " + e.getShortMessage());
        e.printStackTrace(errorWriter);
      }
    }
    catch (Exception e) {
      synchronized (errorWriter) {
        logger.error("Aborting thread due to " + e);
        e.printStackTrace(errorWriter);
      }
    }
    finally {
      logger.setCurrentRunNumber(-1);
      decrementThreadCount();

      synchronized (m_notifyOnCompletion) {
        m_notifyOnCompletion.notifyAll();
      }
    }
  }

  private static synchronized void incrementThreadCount() {
    s_numberOfThreads++;
  }

  private static synchronized void decrementThreadCount() {
    s_numberOfThreads--;
  }

  public static final short getNumberOfThreads() {
    return s_numberOfThreads;
  }
}
