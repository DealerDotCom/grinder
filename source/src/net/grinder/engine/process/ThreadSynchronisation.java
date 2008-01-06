// Copyright (C) 2008 Philip Aston
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

import net.grinder.util.thread.Condition;
import net.grinder.util.thread.BooleanCondition;


/**
 * Implement {@link WorkerThreadSynchronisation}. I looked hard at JSR 166's
 * <code>CountDownLatch</code> and <code>CyclicBarrier</code>, but neither
 * of them allow for the waiting thread to be interrupted by other events.
 *
 * @author Philip Aston
 * @version $Revision:$
 */
class ThreadSynchronisation implements WorkerThreadSynchronisation {
  private final BooleanCondition m_started = new BooleanCondition();
  private final Condition m_allThreadsFinishedCondition;

  private short m_numberOfThreads;

  ThreadSynchronisation(Condition condition, short numberOfThreads) {
    m_allThreadsFinishedCondition = condition;
    m_numberOfThreads = numberOfThreads;
  }

  public short getNumberOfThreads() {
    synchronized (m_allThreadsFinishedCondition) {
      return m_numberOfThreads;
    }
  }

  public void startThreads() {
    m_started.set(true);
  }

  public void awaitStart() {
    m_started.await(true);
  }

  public void threadFinished() {
    synchronized (m_allThreadsFinishedCondition) {
      --m_numberOfThreads;

      if (m_numberOfThreads <= 0) {
        m_allThreadsFinishedCondition.notifyAll();
      }
    }
  }
}
