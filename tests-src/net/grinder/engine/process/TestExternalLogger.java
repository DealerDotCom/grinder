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

package net.grinder.engine.process;

import junit.framework.TestCase;

import net.grinder.common.LoggerStubFactory;
import net.grinder.common.Logger;


/**
 * Unit test case for <code>ExternalLogger</code>.
 *
 * @author Philip Aston
 * @version $Revision$
 */
public class TestExternalLogger extends TestCase {

  public TestExternalLogger(String name) {
    super(name);
  }

  public void testProcessLogging() throws Exception {
    final LoggerStubFactory processLoggerFactory = new LoggerStubFactory();
    final Logger processLogger = processLoggerFactory.getLogger();

    final ThreadContextLocator threadContextLocator =
      new StubThreadContextLocator();

    final ExternalLogger externalLogger =
      new ExternalLogger(processLogger, threadContextLocator);

    externalLogger.output("Hello");

    processLoggerFactory.assertSuccess(
      "output", new Object[] { "Hello", new Integer(Logger.LOG) }, null);
    processLoggerFactory.assertNotCalled();

    processLoggerFactory.resetCallHistory();
    externalLogger.error("Hello again", Logger.TERMINAL);

    processLoggerFactory.assertSuccess(
      "error", new Object[] { "Hello again", new Integer(Logger.TERMINAL) },
      null);

    processLoggerFactory.assertNotCalled();
  }

  /*
  private static class StubThreadLoggerInvocationHandler
    extends StubInvocationHandler(ThreadLogger.class) {

    public ThreadLogger getThreadLogger() {
      return (ThreadLogger)getProxy();
    }
  }

  public void testSeveralLoggers() throws Exception {
    final LogCounter processLogger = new LogCounter();

    final StubInvocationHandler threadLogger1 //ETC.
    final LogCounter threadLogger1 = new LogCounter();
    final LogCounter threadLogger2 = new LogCounter();

    final ThreadContext threadContext1 = new StubThreadContext() {
        public ThreadLogger getThreadLogger() { return threadLogger1; }
      };

    final ThreadContext threadContext2 = new StubThreadContext() {
        public ThreadLogger getThreadLogger() { return threadLogger2; }
      };
    
    final ThreadContextLocator threadContextLocator =
      new StubThreadContextLocator();

    final ExternalLogger externalLogger =
      new ExternalLogger(processLogger, threadContextLocator);

    threadContextLocator.set(threadLogger1);

    externalLogger.output("Testing", Logger.LOG | Logger.TERMINAL);
    assertNull(processLogger.getLastMessage());
    assertNull(threadLogger2.getLastMessage());
    assertEquals(1, threadLogger1.getNumberOfOutputs());
    assertEquals(0, threadLogger1.getNumberOfErrors());
    assertEquals("Testing", threadLogger1.getLastMessage());
    assertEquals(Logger.LOG | Logger.TERMINAL, threadLogger1.getLastWhere());

    threadContextLocator.set(null);

    processLogger.reset();
    threadLogger1.reset();
    threadLogger2.reset();

    externalLogger.error("Another test");
    assertNull(threadLogger1.getLastMessage());
    assertNull(threadLogger2.getLastMessage());
    assertEquals(0, processLogger.getNumberOfOutputs());
    assertEquals(1, processLogger.getNumberOfErrors());
    assertEquals("Another test", processLogger.getLastMessage());
    assertEquals(Logger.LOG, processLogger.getLastWhere());
  }

  public void testMultithreaded() throws Exception {

    final ThreadContextLocator threadContextLocator =
      new ThreadContextLocatorImplementation();

    final ExternalLogger externalLogger =
      new ExternalLogger(processLogger, threadContextLocator);

    TestThread threads[] = new TestThread[10];

    for (int i=0; i<threads.length; ++i) {
      threads[i] = new TestThread(externalLogger, threadContextLocator);
      threads[i].start();
    }

    for (int i=0; i<threads.length; ++i) {
      threads[i].join();
      assertTrue(threads[i].getOK());
    }
  }

  private static class TestThread extends Thread {
    private final ExternalLogger m_externalLogger;
    private final ThreadContextLocator m_threadContextLocator;
    private boolean m_ok = false;

    public TestThread(ExternalLogger externalLogger,
                      ThreadContextLocator threadContextLocator) {

      m_externalLogger = externalLogger;
      m_threadContextLocator = threadContextLocator;
    }

    public void run() {
      final Logger threadLogger = new LogCounter();

      final ThreadContext threadContext = new StubThreadContext() {
          public ThreadLogger getThreadLogger() { return threadLogger; }
        };

      m_threadContextLocator.set(threadContext);

      for (int i=0; i<100; ++i) {
      }

      m_ok = true;
    }

    public boolean getOK() {
      return m_ok;
    }
  }


  private static final class ThreadContextLocatorImplementation
    implements ThreadContextLocator  {

    private final ThreadLocal m_threadContextThreadLocal = new ThreadLocal();

    public ThreadContext get() {
      return (ThreadContext)m_threadContextThreadLocal.get();
    }

    public void set(ThreadContext threadContext) {
      m_threadContextThreadLocal.set(threadContext);
    }
  }
  */
}
