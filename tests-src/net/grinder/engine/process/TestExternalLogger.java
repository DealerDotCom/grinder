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
import net.grinder.testutility.StubInvocationHandler;


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

    processLoggerFactory.assertSuccess("output",
                                       new Object[] { "Hello" }, null);
    processLoggerFactory.assertNotCalled();

    processLoggerFactory.resetCallHistory();
    externalLogger.error("Hello again", Logger.TERMINAL);

    processLoggerFactory.assertSuccess(
      "error", new Object[] { "Hello again", new Integer(Logger.TERMINAL) },
      null);

    processLoggerFactory.assertNotCalled();

    final Object errorLogWriter = externalLogger.getErrorLogWriter();
    processLoggerFactory.assertSuccess("getErrorLogWriter", new Object[] {},
                                       errorLogWriter);

    final Object outputLogWriter = externalLogger.getOutputLogWriter();
    processLoggerFactory.assertSuccess("getOutputLogWriter", new Object[] {},
                                       outputLogWriter);
  }

  public void testSeveralLoggers() throws Exception {
    final LoggerStubFactory processLoggerFactory = new LoggerStubFactory();
    final Logger processLogger = processLoggerFactory.getLogger();

    final ThreadLoggerStubFactory threadLoggerFactory1 =
      new ThreadLoggerStubFactory();
    final ThreadLogger threadLogger1 = threadLoggerFactory1.getThreadLogger();

    final ThreadLoggerStubFactory threadLoggerFactory2 =
      new ThreadLoggerStubFactory();
    final ThreadLogger threadLogger2 = threadLoggerFactory2.getThreadLogger();

    final ThreadContextStubFactory threadContextFactory1 =
      new ThreadContextStubFactory(threadLogger1);
    final ThreadContext threadContext1 =
      threadContextFactory1.getThreadContext();

    final ThreadContextStubFactory threadContextFactory2 =
      new ThreadContextStubFactory(threadLogger2);
    final ThreadContext threadContext2 =
      threadContextFactory2.getThreadContext();
    
    final ThreadContextLocator threadContextLocator =
       new StubThreadContextLocator();

    final ExternalLogger externalLogger =
      new ExternalLogger(processLogger, threadContextLocator);

    threadContextLocator.set(threadContext1);

    externalLogger.output("Testing", Logger.LOG | Logger.TERMINAL);
    threadLoggerFactory1.assertSuccess(
      "output",
      new Object[] { "Testing", new Integer(Logger.LOG | Logger.TERMINAL) });

    final Object errorLogWriter = externalLogger.getErrorLogWriter();
    threadLoggerFactory1.assertSuccess("getErrorLogWriter", new Object[] {},
                                       errorLogWriter);

    processLoggerFactory.assertNotCalled();
    threadLoggerFactory1.assertNotCalled();
    threadLoggerFactory2.assertNotCalled();

    threadContextLocator.set(null);

    externalLogger.error("Another test");
    processLoggerFactory.assertSuccess("error",
                                       new Object[] { "Another test" });

    processLoggerFactory.assertNotCalled();
    threadLoggerFactory1.assertNotCalled();
    threadLoggerFactory2.assertNotCalled();
  }

  public void testMultithreaded() throws Exception {

    final LoggerStubFactory processLoggerFactory = new LoggerStubFactory();
    final Logger processLogger = processLoggerFactory.getLogger();

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

    processLoggerFactory.assertNotCalled();
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
      final ThreadLoggerStubFactory threadLoggerFactory =
        new ThreadLoggerStubFactory();
      final ThreadLogger threadLogger = threadLoggerFactory.getThreadLogger();

      final ThreadContextStubFactory threadContextFactory =
        new ThreadContextStubFactory(threadLogger);
      final ThreadContext threadContext =
        threadContextFactory.getThreadContext();

      m_threadContextLocator.set(threadContext);

      for (int i=0; i<100; ++i) {
        m_externalLogger.output("Testing", Logger.TERMINAL);

        threadLoggerFactory.assertSuccess(
          "output", new Object[] { "Testing", new Integer(Logger.TERMINAL) });

        final Object outputLogWriter = m_externalLogger.getOutputLogWriter();
        threadLoggerFactory.assertSuccess(
          "getOutputLogWriter", new Object[] {}, outputLogWriter);

        threadLoggerFactory.assertNotCalled();
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

  /** Must be public so that stub_ methods can be called externally. */
  public static class ThreadContextStubFactory extends StubInvocationHandler {

    private final ThreadLogger m_threadLogger;

    public ThreadContextStubFactory(ThreadLogger threadLogger) {
      super(ThreadContext.class);
      m_threadLogger = threadLogger;
    }

    public final ThreadContext getThreadContext() {
      return (ThreadContext)getProxy();
    }

    public ThreadLogger stub_getThreadLogger() {
      return m_threadLogger;
    }
  }
}
