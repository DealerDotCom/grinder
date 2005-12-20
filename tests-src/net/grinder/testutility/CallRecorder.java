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

import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.NoSuchElementException;

import net.grinder.util.thread.Monitor;

import junit.framework.Assert;


/**
 *  Utility class used to record and assert method invocations.
 *
 * @author    Philip Aston
 */
public class CallRecorder extends Assert implements CallAssertions {

  private final Monitor m_callDataListMonitor = new Monitor();
  private final LinkedList m_callDataList = new LinkedList();
  private boolean m_ignoreObjectMethods = false;

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

  public final CallData getCallData() {
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

  private boolean shouldRecord(Method method) {
    return
      !m_ignoreObjectMethods ||
      method.getDeclaringClass() != Object.class;
  }

  public final void recordSuccess(Method method, Object[] parameters,
          Object result) {

    if (shouldRecord(method)) {
      synchronized (m_callDataListMonitor) {
        m_callDataList.add(new CallData(method, parameters, result));
        m_callDataListMonitor.notifyAll();
      }
    }
  }

  public final void recordFailure(
    Method method, Object[] parameters, Throwable throwable) {

    if (shouldRecord(method)) {
      synchronized (m_callDataListMonitor) {
        m_callDataList.add(new CallData(method, parameters, throwable));
        m_callDataListMonitor.notifyAll();
      }
    }
  }

  public final CallData assertSuccess(String methodName, Object[] parameters) {
    return getCallData().assertSuccess(methodName, parameters);
  }

  public final CallData assertSuccess(String methodName,
                                      Class[] parameterTypes) {
    return getCallData().assertSuccess(methodName, parameterTypes);
  }

  public final CallData assertSuccess(String methodName) {
    return getCallData().assertSuccess(methodName);
  }

  public final CallData assertSuccess(String methodName, Object object1) {
    return getCallData().assertSuccess(methodName, object1);
  }

  public final CallData assertSuccess(String methodName,
                                      Object object1,
                                      Object object2) {
    return getCallData().assertSuccess(methodName, object1, object2);
  }

  public final CallData assertSuccess(String methodName,
                                      Object object1,
                                      Object object2,
                                      Object object3) {
    return getCallData().assertSuccess(methodName, object1, object2, object3);
  }

  public final CallData assertSuccess(String methodName, Class class1) {
    return getCallData().assertSuccess(methodName, class1);
  }

  public final CallData assertSuccess(String methodName,
                                      Class class1,
                                      Class class2) {
    return getCallData().assertSuccess(methodName, class1, class2);
  }

  public final CallData assertSuccess(String methodName,
                                      Class class1,
                                      Class class2,
                                      Class class3) {
    return getCallData().assertSuccess(methodName, class1, class2, class3);
  }

  public final CallData assertFailed(String methodName,
                                     Object[] parameters,
                                     Throwable throwable) {
    return getCallData().assertFailed(methodName, parameters, throwable);
  }

  public final CallData assertFailed(String methodName,
                                     Class[] parameterTypes,
                                     Throwable throwable) {
    return getCallData().assertFailed(methodName, parameterTypes, throwable);
  }

  /**
   *  Check the given method was called, and that it threw an
   *  exception of the given type.
   */
  public final CallData assertFailed(String methodName,
                                     Object[] parameters,
                                     Class throwableType) {
    return getCallData().assertFailed(methodName, parameters, throwableType);
  }

  public final CallData assertFailed(String methodName,
                                     Class[] parameterTypes,
                                     Class throwableType) {
    return getCallData().assertFailed(methodName,
                                      parameterTypes,
                                      throwableType);
  }
}
