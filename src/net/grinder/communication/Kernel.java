// Copyright (C) 2003 Philip Aston
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

package net.grinder.communication;


/**
 * Work queue and worker threads.
 *
 * @author Philip Aston
 * @version $Revision$
 */
final class Kernel {

  private final ThreadSafeQueue m_workQueue = new ThreadSafeQueue();
  private final ThreadGroup m_threadGroup = new ThreadGroup("Kernel");

  /**
   * Constructor.
   *
   * @param numberOfThreads Number of worker threads to use.
   */
  public Kernel(int numberOfThreads) {

    m_threadGroup.setDaemon(true);

    for (int i = 0; i < numberOfThreads; ++i) {
      new WorkerThread(m_threadGroup, i).start();
    }
  }

  /**
   * Queue some work.
   *
   * @param work The work.
   */
  public void execute(Runnable work) throws ThreadSafeQueue.ShutdownException {
    m_workQueue.queue(work);
  }

  /**
   * Shut down this kernel.
   */
  public void shutdown() {
    m_workQueue.shutdown();
    m_threadGroup.interrupt();
  }

  private final class WorkerThread extends Thread {

    public WorkerThread(ThreadGroup threadGroup, int workerThreadIndex) {
      super(threadGroup, "Worker thread " + workerThreadIndex);
      setDaemon(true);
    }

    public void run() {

      try {
        while (true) {
          final Runnable runnable = (Runnable) m_workQueue.dequeue(true);
          runnable.run();
        }
      }
      catch (ThreadSafeQueue.ShutdownException e) {
        // We've been shutdown, exit this thread.
      }
    }
  }
}
