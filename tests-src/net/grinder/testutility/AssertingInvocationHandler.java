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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 *  Dynamic proxy based test utility class that records details of
 *  invocations and allows a controlling unit test to later enquire
 *  about which invocations occurred and in what order. Because it is
 *  based on java.lang.reflect.Proxy it can only intercept invocations
 *  which are defined by interfaces.
 *
 * @author    Philip Aston
 */
public abstract class AssertingInvocationHandler
     extends CallRecorder implements InvocationHandler {

  private final Class m_delegateClass;
  private final Class[] m_delegateInterfaces;

  public AssertingInvocationHandler(Class delegateClass) {
    m_delegateClass = delegateClass;
    m_delegateInterfaces = getAllInterfaces(m_delegateClass);
  }

  public final Object invoke(Object proxy, Method method, Object[] parameters)
    throws Throwable {

    try {
      // We allow our subclass to override methods, but insist on a
      // prefix to ensure it really wants to.
      final Method subclassMethod = getSubclassMethod(method);

      final Object result;

      if (subclassMethod != null) {
        result = subclassMethod.invoke(this, parameters);
      }
      else {
        result = invokeInternal(method, parameters);
      }
      
      recordSuccess(method.getName(), parameters, result);
      return result;
    }
    catch (Throwable t) {
      recordFailure(method.getName(), parameters, t);
      throw t;
    }
  }

  private Method getSubclassMethod(Method method) {
    try {
      return getClass().getMethod("stub_" + method.getName(),
                                  method.getParameterTypes());
    }
    catch (NoSuchMethodException e) {
      return null;
    }
  }

  protected abstract Object invokeInternal(Method method, Object[] parameters)
    throws Throwable;

  public final Object getProxy() {
    return Proxy.newProxyInstance(
      m_delegateClass.getClassLoader(), m_delegateInterfaces, this);
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
