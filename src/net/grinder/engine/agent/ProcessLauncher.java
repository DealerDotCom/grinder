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

import net.grinder.common.Logger;
import net.grinder.engine.common.EngineException;
import net.grinder.util.thread.Kernel;
/**
 * Manages launching a set of processes.
 *
 * @author Philip Aston
 * @version $Revision$
 */
final class ProcessLauncher {

  private final Kernel m_kernel = new Kernel(1);
  private final ProcessFactory m_processFactory;
  private final Object m_notifyOnFinish;
  private final Logger m_logger;

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
                         ProcessFactory processFactory,
                         Object notifyOnFinish,
                         Logger logger) {

    m_processFactory = processFactory;
    m_notifyOnFinish = notifyOnFinish;
    m_logger = logger;

    m_processes = new ChildProcess[numberOfProcesses];

    Runtime.getRuntime().addShutdownHook(
      new Thread("The Grim Reaper") {
        public void run() { destroyAllProcesses(); }
      });
  }

  public void startAllProcesses() throws EngineException {
    startSomeProcesses(m_processes.length - m_nextProcessIndex);
  }

  public boolean startSomeProcesses(int numberOfProcesses)
    throws EngineException {

    final int numberToStart =
      Math.min(numberOfProcesses, m_processes.length - m_nextProcessIndex);

    for (int i = 0; i < numberToStart; ++i) {
      final int processIndex = m_nextProcessIndex;

      synchronized (m_processes) {
        m_processes[processIndex] = m_processFactory.create(processIndex,
                                                            System.out,
                                                            System.err);
      }

      m_logger.output("process " + m_processes[processIndex].getProcessName() +
                      " started");

      try {
        m_kernel.execute(new WaitForProcessTask(processIndex));
      }
      catch (Kernel.ShutdownException e) {
        m_logger.error("Kernel unexpectedly shutdown");
        e.printStackTrace(m_logger.getErrorLogWriter());
        return false;
      }

      ++m_nextProcessIndex;
    }

    return m_processes.length > m_nextProcessIndex;
  }

  private final class WaitForProcessTask implements Runnable {

    private final int m_processIndex;

    public WaitForProcessTask(int processIndex) {
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

  public void destroyAllProcesses() {
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


