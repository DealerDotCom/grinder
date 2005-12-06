// Copyright (C) 2005 Philip Aston
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

/**
 * Lock object that has two states. A caller can wait for the state to change
 * to a particular value, but can also be woken by another thread.
 *
 * @author Philip Aston
 * @version $Revision$
 */
public final class InterruptibleCondition {
  private final Object m_monitor;
  private boolean m_state = false;
  private int m_waiters = 0;
  private boolean m_interrupt;

  /**
   * Constructor.
   */
  public InterruptibleCondition() {
    m_monitor = this;
  }

  /**
   * Wait for our state to match the passed value.
   *
   * @param state
   *          State to wait for.
   * @return The new state value. Can differ from the parameter if we have been
   *         woken by another thread calling {@link #interruptAllWaiters()}.
   * @throws InterruptedException
   *           If the thread is interrupted whilst waiting.
   */
  public boolean await(boolean state) throws InterruptedException {
    synchronized (m_monitor) {
      ++m_waiters;

      try {
        while (m_state != state && !m_interrupt) {
          m_monitor.wait();
        }
      }
      finally {
        --m_waiters;
        m_monitor.notifyAll();
      }

      return m_state;
    }
  }

  /**
   * Set the state to the passed value.
   *
   * @param state The new state.
   */
  public void set(boolean state) {
    synchronized (m_monitor) {
      m_state = state;
      m_monitor.notifyAll();
    }
  }

  /**
   * Interrupt other threads that are waiting in {@link #await(boolean)}.
   *
   * @throws InterruptedException
   *           If we are interrupted waiting for other threads to exit
   *           {@link #await(boolean)}.
   */
  public void interruptAllWaiters() throws InterruptedException {
    synchronized (m_monitor) {
      if (m_waiters == 0) {
        return;
      }

      m_interrupt = true;
      m_monitor.notifyAll();

      try {
        while (m_waiters > 0) {
          m_monitor.wait();
        }
      }
      finally {
        m_interrupt = false;
        m_monitor.notifyAll();
      }
    }
  }
}
