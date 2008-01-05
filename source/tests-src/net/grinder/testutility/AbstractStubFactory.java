// Copyright (C) 2004 - 2007 Philip Aston
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

package net.grinder.testutility;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 *  Dynamic proxy based test utility class that records details of
 *  invocations and allows a controlling unit test to later enquire
 *  about which invocations occurred and in what order. Because it is
 *  based on java.lang.reflect.Proxy it can only intercept invocations
 *  which are defined by interfaces.
 *
 * @author    Philip Aston
 */
public abstract class AbstractStubFactory extends CallRecorder {

  private final Object m_stub;
  private final Map m_resultMap = new HashMap();
  private final Map m_throwsMap = new HashMap();

  public AbstractStubFactory(Class stubbedInterface,
                             InvocationHandler invocationHandler) {

    final InvocationHandler decoratedInvocationHandler =
      new RecordingInvocationHandler(
        new StubResultInvocationHandler(
          new OverrideInvocationHandlerDecorator(invocationHandler, this)));

    m_stub = Proxy.newProxyInstance(stubbedInterface.getClassLoader(),
                                    getAllInterfaces(stubbedInterface),
                                    decoratedInvocationHandler);
  }

  private final class RecordingInvocationHandler implements InvocationHandler {

    private final InvocationHandler m_delegate;

    public RecordingInvocationHandler(InvocationHandler delegate) {
      m_delegate = delegate;
    }

    public Object invoke(Object proxy, Method method, Object[] parameters)
      throws Throwable {

      try {
        final Object result = m_delegate.invoke(proxy, method, parameters);
        record(new CallData(method, parameters, result));
        return result;
      }
      catch (InvocationTargetException t) {
        final Throwable targetException = t.getTargetException();
        record(new CallData(method, parameters, targetException));
        throw targetException;
      }
      catch (Throwable t) {
        record(new CallData(method, parameters, t));
        throw t;
      }
    }
  }

  private final class StubResultInvocationHandler
    implements InvocationHandler {

    private final InvocationHandler m_delegate;

    public StubResultInvocationHandler(InvocationHandler delegate) {
      m_delegate = delegate;
    }

    public Object invoke(Object proxy, Method method, Object[] parameters)
      throws Throwable {

      final String methodName = method.getName();

      if (m_throwsMap.containsKey(methodName)) {
        final Throwable t = (Throwable)m_throwsMap.get(methodName);
        t.fillInStackTrace();
        throw t;
      }

      if (m_resultMap.containsKey(methodName)) {
        return m_resultMap.get(methodName);
      }

      return m_delegate.invoke(proxy, method, parameters);
    }
  }

  public final Object getStub() {
    return m_stub;
  }

  public final void setResult(String methodName, Object result) {
    m_resultMap.put(methodName, result);
  }

  public final void setThrows(String methodName, Throwable result) {
    m_throwsMap.put(methodName, result);
  }

  public static Class[] getAllInterfaces(Class c) {
    final List interfaces = new ArrayList();

    if (c.isInterface()) {
      interfaces.add(c);
    }

    do {
      final Class[] moreInterfaces = c.getInterfaces();

      if (moreInterfaces != null) {
        interfaces.addAll(Arrays.asList(moreInterfaces));
      }

      c = c.getSuperclass();
    }
    while (c != null);

    return (Class[]) interfaces.toArray(new Class[0]);
  }
}
