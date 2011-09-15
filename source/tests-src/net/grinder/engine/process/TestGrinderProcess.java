// Copyright (C) 2008 - 2011 Philip Aston
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import net.grinder.common.ThreadLifeCycleListener;
import net.grinder.communication.QueuedSender;
import net.grinder.engine.process.GrinderProcess.ThreadContexts;
import net.grinder.engine.process.GrinderProcess.ThreadSynchronisation;
import net.grinder.engine.process.GrinderProcess.Times;
import net.grinder.script.InvalidContextException;
import net.grinder.util.thread.Condition;

import org.junit.AfterClass;
import org.junit.Test;
import org.mockito.ArgumentCaptor;


/**
 * Unit tests for {@link GrinderProcess}
 *
 * @author Philip Aston
 * @version $Revision$
 */
public class TestGrinderProcess {
  private static final ExecutorService s_executor =
    Executors.newCachedThreadPool();

  @AfterClass public static void shutdown() {
    s_executor.shutdown();
  }

  @Test public void testThreadSynchronisationZeroThreads() throws Exception {
    final Condition c = new Condition();

    final ThreadSynchronisation ts =
      new GrinderProcess.ThreadSynchronisation(c);

    assertEquals(0, ts.getTotalNumberOfThreads());
    assertEquals(0, ts.getNumberOfRunningThreads());
    assertTrue(ts.isReadyToStart());
    assertTrue(ts.isFinished());

    ts.startThreads();
    ts.awaitStart();
  }

  @Test public void testThreadSynchronisationNThreads() throws Exception {
    final Condition c = new Condition();
    final Thread[] threads = new Thread[100];

    final ThreadSynchronisation ts =
      new GrinderProcess.ThreadSynchronisation(c);

    for (int i = 0; i < threads.length; ++i) {
      threads[i] = new Thread(new MyRunnable(ts, i % 3 == 0));
    }

    assertEquals(100, ts.getTotalNumberOfThreads());
    assertEquals(100, ts.getNumberOfRunningThreads());
    assertFalse(ts.isReadyToStart());
    assertFalse(ts.isFinished());

    for (int i = 0; i < threads.length; ++i) {
      threads[i].start();
    }

    ts.startThreads();

    synchronized (c) {
      while (!ts.isFinished()) {
        c.waitNoInterrruptException();
      }
    }

    assertTrue(ts.isFinished());
    assertEquals(0, ts.getNumberOfRunningThreads());
    assertEquals(100, ts.getTotalNumberOfThreads());
  }

  private static class MyRunnable implements Runnable {
    private final ThreadSynchronisation m_ts;
    private final boolean m_failBeforeStart;

    public MyRunnable(ThreadSynchronisation ts, boolean failBeforeStart) {
      m_ts = ts;
      m_failBeforeStart = failBeforeStart;
      ts.threadCreated();
    }

    public void run() {
      shortSleep();

      if (m_failBeforeStart) {
        m_ts.threadFinished();
      }
      else {
        m_ts.awaitStart();

        shortSleep();

        m_ts.threadFinished();
      }
    }

    private void shortSleep() {
      try {
        Thread.sleep(10);
      }
      catch (InterruptedException e) {
      }
    }
  }

  @Test public void testTimes() {
    final Times times = new Times();
    assertNotNull(times.getTimeAuthority());

    assertEquals(0, times.getExecutionStartTime());
    final long t1 = System.currentTimeMillis();
    times.setExecutionStartTime();
    final long t2 = System.currentTimeMillis();

    // Fudge required since nanoTime() is more precise that currentTimeMillis().
    final long FUDGE = 10;

    assertTrue(t1 - FUDGE <= times.getExecutionStartTime());
    assertTrue(times.getExecutionStartTime() <= t2 + FUDGE);
    final long elapsedTime = times.getElapsedTime();
    assertTrue(elapsedTime <= (System.currentTimeMillis() - t1));
    assertTrue(elapsedTime >= 0);
  }

  @Test public void testThreadContextsThreadContextLocator() throws Exception {
    final ThreadContexts threadContexts = new ThreadContexts();
    assertNull(threadContexts.get());

    final ThreadContext threadContext1 = mock(ThreadContext.class);

    threadContexts.threadStarted(threadContext1);
    assertSame(threadContext1, threadContexts.get());

    final Future<ThreadContext> future =
      s_executor.submit(new Callable<ThreadContext>() {

      public ThreadContext call() throws Exception {
        return threadContexts.get();
      }});

    final ThreadContext otherContext = future.get();

    assertNotSame(otherContext, threadContexts.get());
    assertSame(threadContext1, threadContexts.get());
  }

  @Test public void testThreadContextsThreadCreated() throws Exception {
    final ThreadContexts threadContexts = new ThreadContexts();

    final ThreadContext threadContext = mock(ThreadContext.class);
    when(threadContext.getThreadNumber()).thenReturn(99);

    threadContexts.threadCreated(threadContext);

    final ArgumentCaptor<ThreadLifeCycleListener> listenerCaptor=
      ArgumentCaptor.forClass(ThreadLifeCycleListener.class);

    verify(threadContext)
      .registerThreadLifeCycleListener(listenerCaptor.capture());

    listenerCaptor.getValue().endThread();

    // Thread context discarded because thread has ended.
    assertFalse(threadContexts.shutdown(99));
  }

  @Test public void testThreadContextsShutdown() throws Exception {
    final ThreadContexts threadContexts = new ThreadContexts();
    assertFalse(threadContexts.shutdown(2));

    final ThreadContext threadContext = mock(ThreadContext.class);
    when(threadContext.getThreadNumber()).thenReturn(2);

    threadContexts.threadCreated(threadContext);

    assertTrue(threadContexts.shutdown(2));
    assertTrue(threadContexts.shutdown(2));

    verify(threadContext, times(2)).shutdown();
  }

  @Test public void testThreadContextsShutdownAll() throws Exception {
    final ThreadContexts threadContexts = new ThreadContexts();

    final ThreadContext threadContext1 = mock(ThreadContext.class);
    when(threadContext1.getThreadNumber()).thenReturn(1);
    threadContexts.threadCreated(threadContext1);

    final ThreadContext threadContext2 = mock(ThreadContext.class);
    when(threadContext2.getThreadNumber()).thenReturn(2);
    threadContexts.threadCreated(threadContext2);

    threadContexts.shutdownAll();

    verify(threadContext1).shutdown();
    verify(threadContext2).shutdown();

    final ThreadContext threadContext3 = mock(ThreadContext.class);
    when(threadContext3.getThreadNumber()).thenReturn(2);
    threadContexts.threadCreated(threadContext3);

    verify(threadContext3).shutdown();
  }

  @Test public void testInvalidThreadStarter() throws Exception {
    final ThreadStarter starter = new GrinderProcess.InvalidThreadStarter();

    try {
      starter.startThread(null);
      fail("Expected InvalidContextException");
    }
    catch (InvalidContextException e) {
    }
  }

  @Test public void testCoverNullQueuedSender() throws Exception {
    final QueuedSender sender = new GrinderProcess.NullQueuedSender();

    sender.send(null);
    sender.queue(null);
    sender.flush();
    sender.shutdown();

    sender.send(null);
    sender.queue(null);
    sender.flush();
    sender.shutdown();
  }
}
