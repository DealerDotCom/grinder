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

import net.grinder.common.FilenameFactory;
import net.grinder.testutility.StubInvocationHandler;


/**
 * Unit test case for <code>ExternalFilenameFactory</code>.
 *
 * @author Philip Aston
 * @version $Revision$
 */
public class TestExternalFilenameFactory extends TestCase {

  public TestExternalFilenameFactory(String name) {
    super(name);
  }

  public void testProcessFilenameFactory() throws Exception {
    final StubInvocationHandler filenameFactoryStubFactory =
      new StubInvocationHandler(FilenameFactory.class);
    final FilenameFactory processFilenameFactory =
      (FilenameFactory)filenameFactoryStubFactory.getProxy();

    final ThreadContextLocator threadContextLocator =
      new StubThreadContextLocator();

    final ExternalFilenameFactory externalFilenameFactory =
      new ExternalFilenameFactory(processFilenameFactory,
                                  threadContextLocator);

    final String result1 = externalFilenameFactory.createFilename("Prefix");

    filenameFactoryStubFactory.assertSuccess(
      "createFilename", new Object[] { "Prefix" }, result1);
    filenameFactoryStubFactory.assertNotCalled();

    final String result2 =
      externalFilenameFactory.createFilename("Prefix", "Suffix");

    filenameFactoryStubFactory.assertSuccess(
      "createFilename", new Object[] { "Prefix", "Suffix" }, result2);
    filenameFactoryStubFactory.assertNotCalled();
  }

  public void testSeveralFilenameFactories() throws Exception {
    final StubInvocationHandler processFilenameFactoryStubFactory =
      new StubInvocationHandler(FilenameFactory.class);
    final FilenameFactory processFilenameFactory =
      (FilenameFactory)processFilenameFactoryStubFactory.getProxy();

    final StubInvocationHandler threadFilenameFactoryStubFactory1 =
      new StubInvocationHandler(FilenameFactory.class);
    final FilenameFactory threadFilenameFactory1 =
      (FilenameFactory)threadFilenameFactoryStubFactory1.getProxy();

    final StubInvocationHandler threadFilenameFactoryStubFactory2 =
      new StubInvocationHandler(FilenameFactory.class);
    final FilenameFactory threadFilenameFactory2 =
      (FilenameFactory)threadFilenameFactoryStubFactory2.getProxy();

    final ThreadContextStubFactory threadContextFactory1 =
      new ThreadContextStubFactory(threadFilenameFactory1);
    final ThreadContext threadContext1 =
      threadContextFactory1.getThreadContext();

    final ThreadContextStubFactory threadContextFactory2 =
      new ThreadContextStubFactory(threadFilenameFactory2);
    final ThreadContext threadContext2 =
      threadContextFactory2.getThreadContext();
    
    final ThreadContextLocator threadContextLocator =
       new StubThreadContextLocator();

    final ExternalFilenameFactory externalFilenameFactory =
      new ExternalFilenameFactory(processFilenameFactory,
                                  threadContextLocator);

    threadContextLocator.set(threadContext1);

    final String result1 = externalFilenameFactory.createFilename("p");
    threadFilenameFactoryStubFactory1.assertSuccess(
      "createFilename", new Object[] { "p" }, result1);
    processFilenameFactoryStubFactory.assertNotCalled();
    threadFilenameFactoryStubFactory1.assertNotCalled();
    threadFilenameFactoryStubFactory2.assertNotCalled();

    final String result2 = externalFilenameFactory.createFilename("p", "s");
    threadFilenameFactoryStubFactory1.assertSuccess(
      "createFilename", new Object[] { "p", "s" }, result2);
    processFilenameFactoryStubFactory.assertNotCalled();
    threadFilenameFactoryStubFactory1.assertNotCalled();
    threadFilenameFactoryStubFactory2.assertNotCalled();

    threadContextLocator.set(null);

    final String result3 =
      externalFilenameFactory.createFilename("foo", "bah");
    processFilenameFactoryStubFactory.assertSuccess(
      "createFilename", new Object[] { "foo", "bah" }, result3);
    processFilenameFactoryStubFactory.assertNotCalled();
    threadFilenameFactoryStubFactory1.assertNotCalled();
    threadFilenameFactoryStubFactory2.assertNotCalled();
  }

  public void testMultithreaded() throws Exception {
    final StubInvocationHandler processFilenameFactoryStubFactory =
      new StubInvocationHandler(FilenameFactory.class);
    final FilenameFactory processFilenameFactory =
      (FilenameFactory)processFilenameFactoryStubFactory.getProxy();

    final ThreadContextLocator threadContextLocator =
      new StubThreadContextLocator();

    final ExternalFilenameFactory externalFilenameFactory =
      new ExternalFilenameFactory(processFilenameFactory,
                                  threadContextLocator);

    final TestThread threads[] = new TestThread[10];

    for (int i=0; i<threads.length; ++i) {
      threads[i] =
        new TestThread(externalFilenameFactory, threadContextLocator);
      threads[i].start();
    }

    for (int i=0; i<threads.length; ++i) {
      threads[i].join();
      assertTrue(threads[i].getOK());
    }

    processFilenameFactoryStubFactory.assertNotCalled();
  } 

  private static class TestThread extends Thread {
    private final ExternalFilenameFactory m_externalFilenameFactory;
    private final ThreadContextLocator m_threadContextLocator;
    private boolean m_ok = false;

    public TestThread(ExternalFilenameFactory externalFilenameFactory,
                      ThreadContextLocator threadContextLocator) {

      m_externalFilenameFactory = externalFilenameFactory;
      m_threadContextLocator = threadContextLocator;
    }

    public void run() {
      final StubInvocationHandler threadFilenameFactoryStubFactory =
        new StubInvocationHandler(FilenameFactory.class);
      final FilenameFactory threadFilenameFactory =
        (FilenameFactory)threadFilenameFactoryStubFactory.getProxy();

      final ThreadContextStubFactory threadContextFactory =
        new ThreadContextStubFactory(threadFilenameFactory);
      final ThreadContext threadContext =
        threadContextFactory.getThreadContext();

      m_threadContextLocator.set(threadContext);

      for (int i=0; i<100; ++i) {
        final String result1 =
          m_externalFilenameFactory.createFilename("blab blah", "blugh");

        threadFilenameFactoryStubFactory.assertSuccess(
          "createFilename", new Object[] { "blab blah", "blugh" });

        final String result2 = m_externalFilenameFactory.createFilename("xxx");

        threadFilenameFactoryStubFactory.assertSuccess(
          "createFilename", new Object[] { "xxx" }, result2);
        threadFilenameFactoryStubFactory.assertNotCalled();
      }

      m_ok = true;
    }

    public boolean getOK() {
      return m_ok;
    }
  }

  /**
   * Must be public so that override_ methods can be called
   * externally.
   */
  public static class ThreadContextStubFactory extends StubInvocationHandler {

    private final FilenameFactory m_filenameFactory;

    public ThreadContextStubFactory(FilenameFactory filenameFactory) {
      super(ThreadContext.class);
      m_filenameFactory = filenameFactory;
    }

    public final ThreadContext getThreadContext() {
      return (ThreadContext)getProxy();
    }

    public FilenameFactory override_getFilenameFactory(Object proxy) {
      return m_filenameFactory;
    }
  }
}
