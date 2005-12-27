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

package net.grinder.testutility;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.NoSuchElementException;

import net.grinder.util.thread.Monitor;

import junit.framework.Assert;
import junit.framework.AssertionFailedError;


/**
 *  Utility class used to record and assert method invocations.
 *
 * @author    Philip Aston
 */
public class CallRecorder extends Assert implements CallAssertions {

  private final Monitor m_callDataListMonitor = new Monitor();
  private final LinkedList m_callDataList = new LinkedList();
  private boolean m_ignoreObjectMethods = false;
  private boolean m_ignoreCallOrder = false;

  /**
   *  Reset the call data.
   */
  public final void resetCallHistory() {
    synchronized (m_callDataListMonitor) {
      m_callDataList.clear();
      m_callDataListMonitor.notifyAll();
    }
  }

  /**
   * Wait until we're called. Fail if we take more than <code>timeout</code>
   * milliseconds.
   *
   * @param timeout
   *          Maximum time in milliseconds to wait for.
   */
  public void waitUntilCalled(int timeout) {
    final long expires = System.currentTimeMillis() + timeout;

    synchronized (m_callDataListMonitor) {
      while (m_callDataList.size() == 0) {
        m_callDataListMonitor.waitNoInterrruptException(timeout);

        if (System.currentTimeMillis() > expires) {
          fail("Timed out waiting to be called after " + timeout + " ms");
        }
      }
    }
  }

  public String getCallHistory() {
    final StringBuffer result = new StringBuffer();

    synchronized (m_callDataListMonitor) {
      final Iterator iterator = m_callDataList.iterator();

      while(iterator.hasNext()) {
        result.append(iterator.next());
        result.append("\n");
      }
    }

    return result.toString();
  }

  /**
   *  Check that no methods have been called.
   */
  public final void assertNoMoreCalls() {
    synchronized (m_callDataListMonitor) {
      assertEquals("Call history:\n" + getCallHistory(),
                   0, m_callDataList.size());
    }
  }

  private final CallData getCallData() {
    // Check the earliest call first.
    synchronized (m_callDataListMonitor) {
      try {
        return (CallData) m_callDataList.removeFirst();
      }
      catch (NoSuchElementException e) {
        fail("No more calls");
        return null; // Not reached.
      }
      finally {
        m_callDataListMonitor.notifyAll();
      }
    }
  }

  public void setIgnoreObjectMethods(boolean b) {
    m_ignoreObjectMethods = b;
  }

  public void setIgnoreCallOrder(boolean b) {
    m_ignoreCallOrder = b;
  }

  public final void record(CallData callData) {
    if (!m_ignoreObjectMethods ||
    callData.getMethod().getDeclaringClass() != Object.class) {
      synchronized (m_callDataListMonitor) {
        m_callDataList.add(callData);
        m_callDataListMonitor.notifyAll();
      }
    }
  }

  public final CallData assertSuccess(final String methodName,
                                      final Object[] parameters) {
    return new AssertMatchingCallTemplate() {
      public void test(CallData callData) {
        callData.assertSuccess(methodName, parameters);
      }
    }
    .run();
  }

  public final CallData assertSuccess(final String methodName,
                                      final Class[] parameterTypes) {
    return new AssertMatchingCallTemplate() {
      public void test(CallData callData) {
        callData.assertSuccess(methodName, parameterTypes);
      }
    }
    .run();
  }

  public final CallData assertSuccess(final String methodName) {
    return new AssertMatchingCallTemplate() {
      public void test(CallData callData) {
        callData.assertSuccess(methodName);
      }
    }
    .run();
  }

  public final CallData assertSuccess(final String methodName,
                                      final Object object1) {
    return new AssertMatchingCallTemplate() {
      public void test(CallData callData) {
        callData.assertSuccess(methodName, object1);
      }
    }
    .run();
  }

  public final CallData assertSuccess(final String methodName,
                                      final Object object1,
                                      final Object object2) {
    return new AssertMatchingCallTemplate() {
      public void test(CallData callData) {
        callData.assertSuccess(methodName, object1, object2);
      }
    }
    .run();
  }

  public final CallData assertSuccess(final String methodName,
                                      final Object object1,
                                      final Object object2,
                                      final Object object3) {
    return new AssertMatchingCallTemplate() {
      public void test(CallData callData) {
        callData.assertSuccess(methodName, object1, object2, object3);
      }
    }
    .run();
  }

  public final CallData assertSuccess(final String methodName,
                                      final Class class1) {
    return new AssertMatchingCallTemplate() {
      public void test(CallData callData) {
        callData.assertSuccess(methodName, class1);
      }
    }
    .run();
  }

  public final CallData assertSuccess(final String methodName,
                                      final Class class1,
                                      final Class class2) {
    return new AssertMatchingCallTemplate() {
      public void test(CallData callData) {
        callData.assertSuccess(methodName, class1, class2);
      }
    }
    .run();
  }

  public final CallData assertSuccess(final String methodName,
                                      final Class class1,
                                      final Class class2,
                                      final Class class3) {
    return new AssertMatchingCallTemplate() {
      public void test(CallData callData) {
        callData.assertSuccess(methodName, class1, class2, class3);
      }
    }
    .run();
  }

  public final CallData assertException(final String methodName,
                                        final Object[] parameters,
                                        final Throwable throwable) {
    return new AssertMatchingCallTemplate() {
      public void test(CallData callData) {
        callData.assertException(methodName, parameters, throwable);
      }
    }
    .run();
  }

  public final CallData assertException(final String methodName,
                                        final Class[] parameterTypes,
                                        final Throwable throwable) {
    return new AssertMatchingCallTemplate() {
      public void test(CallData callData) {
        callData.assertException(methodName, parameterTypes, throwable);
      }
    }
    .run();
  }

  public final CallData assertException(final String methodName,
                                        final Object[] parameters,
                                        final Class throwableType) {
    return new AssertMatchingCallTemplate() {
      public void test(CallData callData) {
        callData.assertException(methodName, parameters, throwableType);
      }
    }
    .run();
  }

  public final CallData assertException(final String methodName,
                                        final Class[] parameterTypes,
                                        final Class throwableType) {
    return new AssertMatchingCallTemplate() {
      public void test(CallData callData) {
        callData.assertException(methodName,
                                 parameterTypes,
                                 throwableType);
      }
    }
    .run();
  }

  private abstract class AssertMatchingCallTemplate {
    public final CallData run() {
      if (m_ignoreCallOrder) {
        synchronized (m_callDataListMonitor) {
          final Iterator iterator = m_callDataList.iterator();

          while (iterator.hasNext()) {
            try {
              final CallData callData = (CallData)iterator.next();

              test(callData);
              iterator.remove();

              return callData;
            }
            catch (AssertionFailedError e) {
            }
          }
        }

        fail("No matching call");
        return null; // Not reached.
      }
      else {
        final CallData callData = getCallData();
        test(callData);
        return callData;
      }
    }

    public abstract void test(CallData callData);
  }
}
