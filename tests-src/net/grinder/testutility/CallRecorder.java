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

package net.grinder.testutility;

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

  /**
   *  Reset the call data.
   */
  public final void resetCallHistory() {
    m_callDataList.clear();
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

  public final void recordSuccess(String methodName, Object[] parameters,
				  Object result) {

    m_callDataList.add(new CallData(methodName, parameters, result));
  }

  public final void recordFailure(
    String methodName, Object[] parameters, Throwable throwable) {

    m_callDataList.add(new CallData(methodName, parameters, throwable));
  }

  /**
   *  Check that no methods have been called.
   */
  public final void assertNotCalled() {
    assertEquals("Call history:\n" + getCallHistory(),
                 0, m_callDataList.size());
  }

  /**
   *  Check the given method was called and that it returned the given
   *  result.
   */
  public final void assertSuccess(String methodName, Object[] parameters,
				  Object result) {

    final CallData callData = assertCalledInternal(methodName, parameters);
    assertEquals(result, callData.getResult());
  }

  public final void assertSuccess(String methodName, Class[] parameterTypes,
				  Object result) {

    final CallData callData = assertCalledInternal(methodName, parameterTypes);
    assertEquals(result, callData.getResult());
  }

  /**
   *  Check the given method was called.
   */
  public final void assertSuccess(String methodName, Object[] parameters) {
    assertCalledInternal(methodName, parameters);
  }

  public final void assertSuccess(String methodName, Class[] parameterTypes) {
    assertCalledInternal(methodName, parameterTypes);
  }

  /**
   *  Check the given method was called, and that it threw the given
   *  exception.
   */
  public final void assertFailed(String methodName, Object[] parameters,
				 Throwable throwable) {

    final CallData callData = assertCalledInternal(methodName, parameters);
    assertEquals(throwable, callData.getThrowable());
  }

  public final void assertFailed(String methodName, Class[] parameterTypes,
				 Throwable throwable) {

    final CallData callData = assertCalledInternal(methodName, parameterTypes);
    assertEquals(throwable, callData.getThrowable());
  }

  /**
   *  Check the given method was called, and that it threw an
   *  exception of the given type.
   */
  public final void assertFailed(String methodName, Object[] parameters,
				 Class throwableType) {

    final CallData callData = assertCalledInternal(methodName, parameters);
    assertTrue(
      throwableType.isAssignableFrom(callData.getThrowable().getClass()));
  }

  public final void assertFailed(String methodName, Class[] parameterTypes,
				 Class throwableType) {

    final CallData callData = assertCalledInternal(methodName, parameterTypes);
    assertTrue(
      throwableType.isAssignableFrom(callData.getThrowable().getClass()));
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
    assertArraysEqual("Expected " + parametersToString(parameters) +
                      " but was " +
                      parametersToString(callData.getParameters()),
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

      assertEquals(parameterTypes.length, actualParameterTypes.length);

      for (int i = 0; i < parameterTypes.length; ++i) {
        assertTrue("Parameter  " + i + " is instance of  " +
                   parameterTypes[i].getClass().getName(),
                   parameterTypes[i].isAssignableFrom(
                     actualParameterTypes[i]));
      }
    }


    return callData;
  }

  public static final class CallData {
    private final String m_methodName;
    private final Object[] m_parameters;
    private final Object m_result;
    private final Throwable m_throwable;

    CallData(String methodName, Object[] parameters, Object result) {
      m_methodName = methodName;
      m_parameters = parameters;
      m_result = result;
      m_throwable = null;
    }

    CallData(String methodName, Object[] parameters, Throwable throwable) {
      m_methodName = methodName;
      m_parameters = parameters;
      m_result = null;
      m_throwable = throwable;
    }

    public String getMethodName() {
      return m_methodName;
    }

    public Object[] getParameters() {
      return m_parameters;
    }

    public Class[] getParameterTypes() {
      final Class[] types = new Class[m_parameters.length];

      for (int i=0; i<types.length; ++i) {
        types[i] = m_parameters[i].getClass();
      }

      return types;
    }

    public Object getResult() {
      assertNull(m_throwable);
      return m_result;
    }

    public Throwable getThrowable() {
      return m_throwable;
    }

    public String toString() {
      final StringBuffer result = new StringBuffer();

      result.append(getMethodName());
      result.append(parametersToString(getParameters()));

      final Throwable throwable = getThrowable();

      if (throwable != null) {
        result.append(" threw " + throwable);
      }
      else {
        result.append(" returned " + getResult());
      }

      return result.toString();
    }
  }

  private final static String parametersToString(Object[] parameters) {
    
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

  /**
   * Assert that two arrays are equal.
   */
  public static void assertArraysEqual(Object[] array1, Object[] array2) {
    assertArraysEqual("", array1, array2);
  }
    

  public static void assertArraysEqual(String message, Object[] array1,
                                       Object[] array2) {

    if (array1 != null || array2 != null) {
      assertNotNull(message, array1);
      assertNotNull(message, array2);

      assertEquals(message, array1.length, array2.length);

      for (int i = 0; i < array1.length; ++i) {
        assertEquals(message, array1[i], array2[i]);
      }
    }
  }
}
