// Copyright (C) 2003, 2004 Philip Aston
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

package net.grinder.util.thread;

import net.grinder.common.GrinderException;


/**
 * Work queue and worker threads.
 *
 * @author Philip Aston
 * @version $Revision$
 */
public final class Kernel {

  private final ThreadSafeQueue m_workQueue = new ThreadSafeQueue();
  private final ThreadPool m_threadPool;

  /**
   * Constructor.
   *
   * @param numberOfThreads Number of worker threads to use.
   */
  public Kernel(int numberOfThreads) {

    final ThreadPool.RunnableFactory runnableFactory =
      new ThreadPool.RunnableFactory() {
        public Runnable create() {
          return new Runnable() {
              public void run() { process(); }
            };
        };
      };

    m_threadPool = new ThreadPool("Kernel", numberOfThreads, runnableFactory);
    m_threadPool.start();
  }

  /**
   * Queue some work.
   *
   * @param work The work.
   * @throws ShutdownException If the Kernel has been stopped.
   */
  public void execute(Runnable work) throws ShutdownException {
    if (m_threadPool.isStopped()) {
      throw new ShutdownException("Kernel is stopped");
    }

    try {
      m_workQueue.queue(work);
    }
    catch (ThreadSafeQueue.ShutdownException e) {
      throw new ShutdownException("Kernel is stopped", e);
    }
  }

  /**
   * Shut down this kernel, waiting for work to complete.
   *
   * @throws InterruptedException If our thread is interrupted whilst
   * we are waiting for work to complete.
   */
  public void gracefulShutdown() throws InterruptedException {

    // Wait until the queue is empty.
    synchronized (m_workQueue.getMutex()) {
      while (m_workQueue.getSize() > 0) {
        m_workQueue.getMutex().wait();
      }
    }

    m_threadPool.stopAndWait();
  }

  /**
   * Shut down this kernel, discarding any outstanding work.
   */
  public void forceShutdown() {
    m_workQueue.shutdown();
    m_threadPool.stop();
  }

  private void process() {
    try {
      while (true) {
        final Runnable runnable =
          (Runnable) m_workQueue.dequeue(!m_threadPool.isStopped());

        if (runnable == null) {
          // We're shutting down and the queue is empty.
          break;
        }

        runnable.run();
      }
    }
    catch (ThreadSafeQueue.ShutdownException e) {
      // We've been shutdown, exit this thread.
    }
  }

  /**
   * Exception that indicates <code>Kernel</code> has been shutdown.
   */
  public static final class ShutdownException extends GrinderException {

    private ShutdownException(String s) {
      super(s);
    }

    private ShutdownException(String s, Exception e) {
      super(s, e);
    }
  }
}
