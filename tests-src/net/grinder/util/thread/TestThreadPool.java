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

package net.grinder.util.thread;

import junit.framework.TestCase;

/**
 *  Unit tests for <code>ThreadPool</code>.
 *
 * @author Philip Aston
 * @version $Revision$
 */
public class TestThreadPool extends TestCase {

  public TestThreadPool(String name) throws Exception {
    super(name);
  }

  private static final class NullRunnable implements InterruptibleRunnable {
    public void run() {
    }
  }

  private int m_count;

  private final class CountingRunnable implements InterruptibleRunnable {

    public void run() {
      for (int i=0; i<20; ++i) {
        synchronized(TestThreadPool.this) {
          ++m_count;
        }

        try {
          Thread.sleep(1);
        }
        catch (InterruptedException e) {
          // TODO
        }
      }
    }
  }


  private abstract class TestRunnableFactory
    implements ThreadPool.RunnableFactory {

    private int m_callCount = 0;

    public InterruptibleRunnable create() {
      ++m_callCount;
      try {
        return doCreate();
      }
      catch (Exception e) {
        e.printStackTrace();
        return null;
      }
    }

    public abstract InterruptibleRunnable doCreate();

    public int getCallCount() {
      return m_callCount;
    }
  }

  public void testWithNullRunnable() throws Exception {

    final TestRunnableFactory runnableFactory =
      new TestRunnableFactory() {
        public InterruptibleRunnable doCreate() { return new NullRunnable(); }
      };

    final ThreadPool threadPool = new ThreadPool("Test", 5, runnableFactory);

    assertEquals(5, runnableFactory.getCallCount());
    assertNotNull(threadPool.getThreadGroup());
    assertTrue(!threadPool.isStopped());
    assertEquals(5, threadPool.getThreadGroup().activeCount());

    threadPool.start();

    try {
      threadPool.start();
      fail("Expected IllegalStateException");
    }
    catch (IllegalStateException e) {
    }

    assertTrue(!threadPool.isStopped());

    while (threadPool.getThreadGroup().activeCount() > 0) {
      Thread.sleep(10);
    }

    threadPool.stopAndWait();
    assertTrue(threadPool.isStopped());
    assertEquals(0, threadPool.getThreadGroup().activeCount());

    try {
      threadPool.start();
      fail("Expected IllegalStateException");
    }
    catch (IllegalStateException e) {
    }
  }

  public void testWithCountingRunnable() throws Exception {

    final TestRunnableFactory runnableFactory =
      new TestRunnableFactory() {
        public InterruptibleRunnable doCreate() {
          return new CountingRunnable();
        }
      };

    final ThreadPool threadPool = new ThreadPool("Test", 10, runnableFactory);

    assertEquals(10, runnableFactory.getCallCount());
    assertEquals(10, threadPool.getThreadGroup().activeCount());

    threadPool.start();

    threadPool.stop();

    assertTrue(threadPool.isStopped());

    while (threadPool.getThreadGroup().activeCount() > 0) {
      Thread.sleep(10);
    }

    threadPool.stopAndWait();

    assertTrue(threadPool.isStopped());
    assertEquals(200, m_count);
  }
}

