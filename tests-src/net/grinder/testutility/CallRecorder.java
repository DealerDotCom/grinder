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
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;

import junit.framework.Assert;


/**
 *  Utility class used to record and assert method invocations.
 *
 * @author    Philip Aston
 */
public class CallRecorder extends Assert {

  private final LinkedList m_callDataList = new LinkedList();
  private boolean m_ignoreObjectMethods = false;

  /**
   *  Reset the call data.
   */
  public final void resetCallHistory() {
    m_callDataList.clear();
  }

  public boolean hasBeenCalled() {
    return m_callDataList.size() > 0;
  }

  public String getCallHistory() {
    final StringBuffer result = new StringBuffer();

    final Iterator iterator = m_callDataList.iterator();

    while(iterator.hasNext()) {
      result.append(iterator.next());
      result.append("\n");
    }

    return result.toString();
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
      m_callDataList.add(new CallData(method, parameters, result));
    }
  }

  public final void recordFailure(
    Method method, Object[] parameters, Throwable throwable) {

    if (shouldRecord(method)) {
      m_callDataList.add(new CallData(method, parameters, throwable));
    }
  }

  /**
   *  Check that no methods have been called.
   */
  public final void assertNoMoreCalls() {
    assertEquals("Call history:\n" + getCallHistory(),
                 0, m_callDataList.size());
  }

  /**
   *  Check the given method was called.
   */
  public final CallData assertSuccess(String methodName, Object[] parameters) {
    final CallData result = assertCalledInternal(methodName, parameters);
    assertNull(result.getThrowable());
    return result;
  }

  public final CallData assertSuccess(String methodName,
                                      Class[] parameterTypes) {
    final CallData result = assertCalledInternal(methodName, parameterTypes);
    assertNull(result.getThrowable());
    return result;
  }


  public final CallData assertSuccess(String methodName) {
    final CallData result = assertCalledInternal(methodName, new Class[0]);
    assertNull(result.getThrowable());
    return result;
  }

  public final CallData assertSuccess(String methodName, Object object1) {
    return assertSuccess(methodName, new Object[] { object1 });
  }

  public final CallData assertSuccess(String methodName,
                                      Object object1,
                                      Object object2) {
    return assertSuccess(methodName, new Object[] { object1, object2 });
  }

  public final CallData assertSuccess(String methodName,
                                      Object object1,
                                      Object object2,
                                      Object object3) {
    return assertSuccess(methodName,
                         new Object[] { object1, object2, object3 });
  }

  public final CallData assertSuccess(String methodName, Class class1) {
    return assertSuccess(methodName, new Class[] { class1 });
  }

  public final CallData assertSuccess(String methodName,
                                      Class class1,
                                      Class class2) {
    return assertSuccess(methodName, new Class[] { class1, class2 });
  }

  public final CallData assertSuccess(String methodName,
                                      Class class1,
                                      Class class2,
                                      Class class3) {
    return assertSuccess(methodName, new Class[] { class1, class2, class3 });
  }

  /**
   *  Check the given method was called, and that it threw the given
   *  exception.
   */
  public final CallData assertFailed(String methodName, Object[] parameters,
                                     Throwable throwable) {

    final CallData callData = assertCalledInternal(methodName, parameters);
    assertEquals(throwable, callData.getThrowable());
    return callData;
  }

  public final CallData assertFailed(String methodName, Class[] parameterTypes,
                                     Throwable throwable) {

    final CallData callData = assertCalledInternal(methodName, parameterTypes);
    assertEquals(throwable, callData.getThrowable());
    return callData;
  }

  /**
   *  Check the given method was called, and that it threw an
   *  exception of the given type.
   */
  public final CallData assertFailed(String methodName, Object[] parameters,
                                     Class throwableType) {

    final CallData callData = assertCalledInternal(methodName, parameters);
    assertTrue(
      throwableType.isAssignableFrom(callData.getThrowable().getClass()));
    return callData;
  }

  public final CallData assertFailed(String methodName, Class[] parameterTypes,
                                     Class throwableType) {

    final CallData callData = assertCalledInternal(methodName, parameterTypes);
    assertNotNull(callData.getThrowable());
    assertTrue(
      throwableType.isAssignableFrom(callData.getThrowable().getClass()));
    return callData;
  }

  public final CallData getCallData() {
    // Check the earliest call first.
    return (CallData) m_callDataList.removeFirst();
  }

  private final CallData assertCalledInternal(String methodName,
                Object[] parameters) {

    final CallData callData = getCallData();

    if (parameters.length == 0) {
      parameters = null;
    }

    // Just check method names match. Don't worry about modifiers
    // etc., or even which class the method belongs to.
    assertEquals(methodName, callData.getMethodName());
    AssertUtilities. assertArraysEqual(
      "Expected " + parametersToString(parameters) +
      " but was " + parametersToString(callData.getParameters()),
      parameters, callData.getParameters());

    return callData;
  }

  private final CallData assertCalledInternal(String methodName,
                Class[] parameterTypes) {

    final CallData callData = getCallData();

    // Just check method names match. Don't worry about modifiers
    // etc., or even which class the method belongs to.
    assertEquals(methodName, callData.getMethodName());

    final Class[] actualParameterTypes = callData.getParameterTypes();

    if (parameterTypes != null || actualParameterTypes != null) {
      assertNotNull(parameterTypes);
      assertNotNull(actualParameterTypes);

      assertEquals("Called with the correct number of parameters",
                   parameterTypes.length,
                   actualParameterTypes.length);

      for (int i = 0; i < parameterTypes.length; ++i) {
        assertTrue("Parameter  " + i + " is instance of  " +
                   actualParameterTypes[i].getName() +
                   " which supports the interfaces " +
                   Arrays.asList(actualParameterTypes[i].getInterfaces()) +
                   " and is not assignable from " +
                   parameterTypes[i].getName(),
                   parameterTypes[i].isAssignableFrom(
                     actualParameterTypes[i]));
      }
    }


    return callData;
  }

  final static String parametersToString(Object[] parameters) {

    final StringBuffer result = new StringBuffer();

    result.append('(');

    if (parameters != null) {
      for (int i = 0; i < parameters.length; ++i) {
        if (i != 0) {
          result.append(", ");
        }

        if (parameters[i] != null && !parameters[i].getClass().isPrimitive()) {
          result.append("\"");
          result.append(parameters[i]);
          result.append("\"");
        }
        else {
          result.append(parameters[i]);
        }
      }
    }

    result.append(')');

    return result.toString();
  }
}
