// Copyright (C) 2009 Philip Aston
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

package net.grinder.engine.process.instrumenter;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import net.grinder.common.Test;
import net.grinder.engine.process.InstrumentationRegistry;
import net.grinder.engine.process.Instrumenter;
import net.grinder.engine.process.ScriptEngine.Instrumentation;
import net.grinder.script.NotWrappableTypeException;
import net.grinder.util.weave.Weaver;
import net.grinder.util.weave.WeavingException;

import org.python.core.PyFunction;
import org.python.core.PyInstance;
import org.python.core.PyMethod;
import org.python.core.PyObject;
import org.python.core.PyProxy;
import org.python.core.PyReflectedFunction;


/**
 * DCRInstrumenter.
 *
 * @author Philip Aston
 * @version $Revision:$
 */
public final class DCRInstrumenter implements Instrumenter {

  private static final String[] NON_INSTRUMENTABLE_PACKAGES = {
    "net.grinder",
    "extra166y",
    "org.objectweb.asm",
  };

  private static final ClassLoader BOOTSTRAP_CLASSLOADER =
    Object.class.getClassLoader();

  private final Weaver m_weaver;
  private final InstrumentationRegistry m_instrumentationRegistry;

  /**
   * Constructor for DCRInstrumenter.
   *
   * @param weaver The weaver.
   * @param instrumentationRegistry The instrumentation registry.
   */
  public DCRInstrumenter(Weaver weaver,
                         InstrumentationRegistry instrumentationRegistry) {
    m_weaver = weaver;
    m_instrumentationRegistry = instrumentationRegistry;
  }

  /**
   * {@inheritDoc}
   */
  public Object createInstrumentedProxy(Test test,
                                        Instrumentation instrumentation,
                                        Object target)
    throws NotWrappableTypeException {

    if (target instanceof PyObject) {
      // Jython object.
      if (target instanceof PyInstance) {
        instrumentPublicMethodsByName(target, "invoke", instrumentation, true);
      }
      else if (target instanceof PyFunction ||
               target instanceof PyMethod) {
        instrumentPublicMethodsByName(
          target, "__call__", instrumentation, false);
      }
      else if (target instanceof PyReflectedFunction) {
        final Method callMethod;

        try {
          callMethod = PyReflectedFunction.class.getMethod("__call__",
                                                           PyObject.class);
        }
        catch (Exception e) {
          throw new NotWrappableTypeException(
            "Correct version of Jython?: " + e.getLocalizedMessage());
        }

        instrument(target, callMethod, instrumentation);
      }
      else {
        // Fail, rather than guess a generic approach.
        throw new NotWrappableTypeException("Unknown PyObject");
      }
    }
    else if (target instanceof PyProxy) {
      // Jython object that extends a Java class.
      // We can't just use the Java wrapping, since then we'd miss the
      // Jython methods.
      final PyObject pyInstance = ((PyProxy)target)._getPyInstance();
      instrumentPublicMethodsByName(
        pyInstance, "invoke", instrumentation, true);
    }
    else if (target == null) {
      throw new NotWrappableTypeException("Can't wrap null/None");
    }
    else if (target instanceof Class<?>) {
      instrumentClass(target, (Class<?>)target, instrumentation);
    }
    else {
      // Java object.
      instrumentClass(target, target.getClass(), instrumentation);
    }

    try {
      m_weaver.applyChanges();
    }
    catch (WeavingException e) {
      throw new NotWrappableTypeException(e.getMessage());
    }

    return target;
  }

  private void instrumentClass(Object target,
                               Class<?> targetClass,
                               Instrumentation instrumentation)
    throws NotWrappableTypeException {

    if (!isInstrumentable(targetClass)) {
      throw new NotWrappableTypeException("Cannot instrument " +
                                          targetClass);
    }

    for (Constructor<?> constructor : targetClass.getDeclaredConstructors()) {
       instrument(targetClass, constructor, instrumentation);
    }

    Class<?> c = targetClass;

    do {
      for (Method method : c.getDeclaredMethods()) {
        instrument(
          Modifier.isStatic(method.getModifiers()) ? targetClass : target,
          method,
          instrumentation);
      }

      c = c.getSuperclass();
    }
    while (isInstrumentable(c));
  }

  private static boolean isInstrumentable(Class<?> targetClass) {

    // We disallow instrumentation of these classes to avoid the need for
    // complex protection against recursion in the engine itself.
    if (targetClass.getClassLoader() == BOOTSTRAP_CLASSLOADER) {
      return false;
    }

    // Package can be null.
    final Package thePackage = targetClass.getPackage();

    if (thePackage != null) {
      final String packageName = thePackage.getName();

      for (String prefix : NON_INSTRUMENTABLE_PACKAGES) {
        if (packageName.startsWith(prefix)) {
          return false;
        }
      }
    }

    return true;
  }

  private void instrumentPublicMethodsByName(Object target,
                                             String methodName,
                                             Instrumentation instrumentation,
                                             boolean includeSuperClassMethods)
    throws NotWrappableTypeException {

    // getMethods() includes superclass methods.
    for (Method method : target.getClass().getMethods()) {
      if (!includeSuperClassMethods &&
          target.getClass() != method.getDeclaringClass()) {
        continue;
      }

      if (!method.getName().equals(methodName)) {
        continue;
      }

      instrument(target, method, instrumentation);
    }
  }

  private void instrument(Object target,
                          Constructor<?> constructor,
                          Instrumentation instrumentation) {
    final String location = m_weaver.weave(constructor);

//    System.out.printf("register(%s, %s, %s, %s)%n",
//                      target.hashCode(), location,
//                      target,
//                      constructor);

    m_instrumentationRegistry.register(target,
                                       location,
                                       instrumentation);
  }

  private void instrument(Object target,
                          Method method,
                          Instrumentation instrumentation) {
    final String location = m_weaver.weave(method);

//    System.out.printf("register(%s, %s, %s, %s)%n",
//                      target.hashCode(), location,
//                      target,
//                      method);

    m_instrumentationRegistry.register(target,
                                       location,
                                       instrumentation);
  }
}
