// Copyright (C) 2004 Philip Aston
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

package net.grinder.engine.agent;

import java.io.OutputStream;

import net.grinder.common.Logger;
import net.grinder.communication.CommunicationException;
import net.grinder.communication.FanOutStreamSender;
import net.grinder.communication.Message;
import net.grinder.communication.StreamSender;
import net.grinder.engine.common.EngineException;
import net.grinder.util.thread.Kernel;


/**
 * Manages launching of processes.
 *
 * @author Philip Aston
 * @version $Revision$
 */
final class ProcessLauncher {

  private final Kernel m_kernel = new Kernel(1);
  private final Logger m_logger;
  private final WorkerProcessCommandLine m_commandLine;
  private final FanOutStreamSender m_fanOutStreamSender;
  private final Message m_initialisationMessage;
  private final Object m_notifyOnFinish;

  /**
   * Fixed size array with a slot for all potential processes.
   * Synchronise on m_processes before accessing entries. If an
   * entry is null and its index is less than m_nextProcessIndex,
   * the process has finished or the ProcessLauncher has been
   * shutdown.
   */
  private final ChildProcess[] m_processes;

  /**
   * The next process to start. Only increases.
   */
  private int m_nextProcessIndex = 0;

  public ProcessLauncher(int numberOfProcesses,
                         WorkerProcessCommandLine workerProcessCommandLine,
                         FanOutStreamSender fanOutStreamSender,
                         Message initialisationMessage,
                         Object notifyOnFinish,
                         Logger logger) {

    m_logger = logger;
    m_fanOutStreamSender = fanOutStreamSender;
    m_initialisationMessage = initialisationMessage;
    m_notifyOnFinish = notifyOnFinish;
    m_commandLine = workerProcessCommandLine;

    m_processes = new ChildProcess[numberOfProcesses];
  }

  public void startAllProcesses() {
    startSomeProcesses(m_processes.length - m_nextProcessIndex);
  }

  public boolean startSomeProcesses(int numberOfProcesses) {

    final int numberToStart =
      Math.min(numberOfProcesses, m_processes.length - m_nextProcessIndex);

    for (int i = 0; i < numberToStart; ++i) {
      final int processIndex = m_nextProcessIndex;

      final String grinderID = m_commandLine.getGrinderID(processIndex);

      try {
        final ChildProcess process =
          new ChildProcess(m_commandLine.getCommandArray(grinderID),
                           System.out, System.err);

        final OutputStream processStdin = process.getStdinStream();

        new StreamSender(processStdin).send(m_initialisationMessage);
        m_fanOutStreamSender.add(processStdin);

        synchronized (m_processes) {
          m_processes[processIndex] = process;
        }
      }
      catch (EngineException e) {
        m_logger.error("Failed to create process");
        e.printStackTrace(m_logger.getErrorLogWriter());
        return false;
      }
      catch (CommunicationException e) {
        m_logger.error("Failed to communicate with process");
        e.printStackTrace(m_logger.getErrorLogWriter());
        return false;
      }

      try {
        m_kernel.execute(new ReapTask(processIndex));
      }
      catch (Kernel.ShutdownException e) {
        m_logger.error("Kernel unexpectedly shutdown");
        e.printStackTrace(m_logger.getErrorLogWriter());
        return false;
      }

      ++m_nextProcessIndex;

      final StringBuffer buffer = new StringBuffer();
      m_logger.output("process " + grinderID + " started");
    }

    return m_processes.length > m_nextProcessIndex;
  }

  private final class ReapTask implements Runnable {

    private final int m_processIndex;

    public ReapTask(int processIndex) {
      m_processIndex = processIndex;
    }

    public void run() {
      try {
        final ChildProcess process;

        synchronized (m_processes) {
          process = m_processes[m_processIndex];
        }

        if (process != null) {
          process.waitFor();

          synchronized (m_processes) {
            m_processes[m_processIndex] = null;
          }
        }
      }
      catch (InterruptedException e) {
        // Really an assertion failure. Can't use m_logger here
        // because its not thread safe.
        e.printStackTrace();
      }
      catch (EngineException e) {
        // Really an assertion failure. Can't use m_logger here
        // because its not thread safe.
        e.printStackTrace();
      }

      if (allFinished()) {
        synchronized (m_notifyOnFinish) {
          m_notifyOnFinish.notifyAll();
        }
      }
    }
  }

  public boolean allFinished() {
    if (m_nextProcessIndex < m_processes.length) {
      return false;
    }

    synchronized (m_processes) {
      for (int i = 0; i < m_processes.length; i++) {
        if (m_processes[i] != null) {
          return false;
        }
      }
    }

    return true;
  }

  public void dontStartAnyMore() {
    m_nextProcessIndex = m_processes.length;
  }

  public void destroy() {
    dontStartAnyMore();

    synchronized (m_processes) {
      for (int i = 0; i < m_processes.length; i++) {
        if (m_processes[i] != null) {
          m_processes[i].destroy();
        }
      }
    }
  }
}


