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

  private final Object m_proxy;

  public AssertingInvocationHandler(Class simulatedInterface) {

    final InvocationHandler invocationHandler =
      new RecordingInvocationHandler(
        new OverrideInvocationHandlerDecorator(this, this));

    m_proxy = Proxy.newProxyInstance(simulatedInterface.getClassLoader(),
                                     getAllInterfaces(simulatedInterface),
                                     invocationHandler);
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
        recordSuccess(method.getName(), parameters, result);
        return result;
      }
      catch (Throwable t) {
        recordFailure(method.getName(), parameters, t);
        throw t;
      }
    }
  }

  public final Object getProxy() {
    return m_proxy;
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
