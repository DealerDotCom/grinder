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

import java.lang.reflect.Array;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 *  Factory that generates various types of test object.
 *
 * @author    Philip Aston
 */
public final class RandomObjectFactory {

  private final Random m_random = new Random();

  public Object generateParameter(Class type) {

    if (Boolean.TYPE == type || Boolean.class == type) {
      return new Boolean(m_random.nextBoolean());
    }

    if (Character.TYPE == type || Character.class == type) {
      return new Character((char) m_random.nextInt());
    }

    if (Byte.TYPE == type || Byte.class == type) {
      return new Byte((byte) m_random.nextInt(0x100));
    }

    if (Short.TYPE == type || Short.class == type) {
      return new Short((short) m_random.nextInt(0x10000));
    }

    if (Integer.TYPE == type || Integer.class == type) {
      return new Integer(m_random.nextInt());
    }

    if (Long.TYPE == type || Long.class == type) {
      return new Long(m_random.nextLong());
    }

    if (Float.TYPE == type || Float.class == type) {
      return new Float(m_random.nextFloat());
    }

    if (Double.TYPE == type || Double.class == type) {
      return new Double(m_random.nextDouble());
    }

    if (Void.TYPE == type) {
      return null;
    }

    if (String.class == type) {
      final byte[] bytes = new byte[Math.abs(m_random.nextInt()) % 50];
      m_random.nextBytes(bytes);

      return new String(bytes);
    }

    if (Map.class == type) {
      final int size = Math.abs(m_random.nextInt()) % 10;
      final Map result = new HashMap(size);

      for (int i = 0; i < size; ++i) {
        result.put(
          generateParameter(String.class), generateParameter(String.class));
      }

      return result;
    }

    if (type.isArray()) {
      final Class componentType = type.getComponentType();

      if (Byte.TYPE == componentType) {
        // Handle byte[] as special case for optimisation and larger arrays.
        return new byte[Math.abs(m_random.nextInt()) % 1000];
      }

      final int size = Math.abs(m_random.nextInt()) % 10;
      final Object result = Array.newInstance(componentType, size);

      for (int i = 0; i < size; ++i) {
        final Object o = generateParameter(componentType);
        Array.set(result, i, o);
      }

      return result;
    }

    // Resort to something that supports same interfaces as type.
    return new DummyInvocationHandler(type).getProxy();
  }

  public Object[] generateParameters(Class[] parameterTypes) {

    if (parameterTypes.length == 0) {
      return null;
    }
    else {
      final Object[] parameters = new Object[parameterTypes.length];

      for (int j = 0; j < parameters.length; ++j) {
        parameters[j] = generateParameter(parameterTypes[j]);
      }

      return parameters;
    }
  }

  /**
   *  Factory for dummy objects that support a particular interface.
   */
  public static class DummyInvocationHandler implements InvocationHandler {

    private final Class m_delegateInterface;
    private final Class[] m_allDelegateInterfaces;

    public DummyInvocationHandler(Class delegateInterface) {
      m_delegateInterface = delegateInterface;
      m_allDelegateInterfaces =
        StubInvocationHandler.getAllInterfaces(delegateInterface);
    }

    public Object invoke(Object proxy, Method method, Object[] parameters)
         throws Throwable {

      try {
        // Allow this class or any subclass to override methods. Also
        // passes equals(), hashCode(), toString() through to this
        // instance.
        final Method ourMethod =
            getClass().getMethod(method.getName(), method.getParameterTypes());
        return ourMethod.invoke(this, parameters);
      }
      catch (NoSuchMethodException e) {
      }

      return null;
    }

    public Object getProxy() {
      return Proxy.newProxyInstance(getClass().getClassLoader(),
                                    m_allDelegateInterfaces,
                                    this);
    }

    /**
     *  Fix up equals() so we can correctly be compared to one of our
     *  Proxy's.
     */
    public boolean equals(Object o) {
      if (o instanceof Proxy) {
        return super.equals(Proxy.getInvocationHandler(o));
      }

      return super.equals(o);
    }

    public String toString() {
      return "a random " + m_delegateInterface.getName();
    }
  }
}
