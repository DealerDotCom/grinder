// Copyright (C) 2004, 2005 Philip Aston
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
 * Manages launching a set of workers.
 *
 * @author Philip Aston
 * @version $Revision$
 */
final class WorkerLauncher {

  private final Kernel m_kernel = new Kernel(1);
  private final WorkerFactory m_workerFactory;
  private final Object m_notifyOnFinish;
  private final Logger m_logger;

  /**
   * Fixed size array with a slot for all potential workers. Synchronise on
   * m_workers before accessing entries. If an entry is null and its index is
   * less than m_nextWorkerIndex, the worker has finished or the WorkerLauncher
   * has been shutdown.
   */
  private final Worker[] m_workers;

  /**
   * The next worker to start. Only increases.
   */
  private int m_nextWorkerIndex = 0;

  public WorkerLauncher(int numberOfWorkers,
                         WorkerFactory workerFactory,
                         Object notifyOnFinish,
                         Logger logger) {

    m_workerFactory = workerFactory;
    m_notifyOnFinish = notifyOnFinish;
    m_logger = logger;

    m_workers = new Worker[numberOfWorkers];

    Runtime.getRuntime().addShutdownHook(
      new Thread("The Grim Reaper") {
        public void run() { destroyAllWorkers(); }
      });
  }

  public void startAllWorkers() throws EngineException {
    startSomeWorkers(m_workers.length - m_nextWorkerIndex);
  }

  public boolean startSomeWorkers(int numberOfWorkers)
    throws EngineException {

    final int numberToStart =
      Math.min(numberOfWorkers, m_workers.length - m_nextWorkerIndex);

    for (int i = 0; i < numberToStart; ++i) {
      final int workerIndex = m_nextWorkerIndex;

      synchronized (m_workers) {
        m_workers[workerIndex] = m_workerFactory.create(System.out, System.err);
      }

      m_logger.output("worker " +
                      m_workers[workerIndex].getIdentity().getName() +
                      " started");

      try {
        m_kernel.execute(new WaitForWorkerTask(workerIndex));
      }
      catch (Kernel.ShutdownException e) {
        m_logger.error("Kernel unexpectedly shutdown");
        e.printStackTrace(m_logger.getErrorLogWriter());
        return false;
      }

      ++m_nextWorkerIndex;
    }

    return m_workers.length > m_nextWorkerIndex;
  }

  private final class WaitForWorkerTask implements Runnable {

    private final int m_workerIndex;

    public WaitForWorkerTask(int workerIndex) {
      m_workerIndex = workerIndex;
    }

    public void run() {
      try {
        final Worker worker;

        synchronized (m_workers) {
          worker = m_workers[m_workerIndex];
        }

        if (worker != null) {
          worker.waitFor();

          synchronized (m_workers) {
            m_workers[m_workerIndex] = null;
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
    if (m_nextWorkerIndex < m_workers.length) {
      return false;
    }

    synchronized (m_workers) {
      for (int i = 0; i < m_workers.length; i++) {
        if (m_workers[i] != null) {
          return false;
        }
      }
    }

    try {
      m_kernel.gracefulShutdown();
    }
    catch (InterruptedException e) {
      // Oh well.
    }

    return true;
  }

  public void dontStartAnyMore() {
    m_nextWorkerIndex = m_workers.length;
  }

  public void destroyAllWorkers() {
    dontStartAnyMore();

    synchronized (m_workers) {
      for (int i = 0; i < m_workers.length; i++) {
        if (m_workers[i] != null) {
          m_workers[i].destroy();
        }
      }
    }
  }
}
