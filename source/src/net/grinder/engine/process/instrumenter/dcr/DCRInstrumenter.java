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

package net.grinder.engine.process.instrumenter.dcr;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import net.grinder.common.Test;
import net.grinder.engine.process.Instrumenter;
import net.grinder.engine.process.ScriptEngine.Recorder;
import net.grinder.script.NotWrappableTypeException;
import net.grinder.util.weave.Weaver;
import net.grinder.util.weave.WeavingException;


/**
 * DCRInstrumenter.
 *
 * @author Philip Aston
 * @version $Revision:$
 */
abstract class DCRInstrumenter implements Instrumenter {

  private static final String[] NON_INSTRUMENTABLE_PACKAGES = {
    "net.grinder",
    "extra166y",
    "org.objectweb.asm",
  };

  private static final ClassLoader BOOTSTRAP_CLASSLOADER =
    Object.class.getClassLoader();

  private final Weaver m_weaver;
  private final RecorderRegistry m_recorderRegistry;

  /**
   * Constructor for DCRInstrumenter.
   *
   * @param weaver The weaver.
   * @param recorderRegistry The recorder registry.
   */
  public DCRInstrumenter(Weaver weaver,
                         RecorderRegistry recorderRegistry) {
    m_weaver = weaver;
    m_recorderRegistry = recorderRegistry;
  }

  /**
   * {@inheritDoc}
   */
  public Object createInstrumentedProxy(Test test,
                                        Recorder recorder,
                                        Object target)
    throws NotWrappableTypeException {

    final Object result = instrument(target, recorder);

    if (result != null) {
      try {
        m_weaver.applyChanges();
      }
      catch (WeavingException e) {
        throw new NotWrappableTypeException(e.getMessage());
      }
    }

    return result;
  }

  protected abstract Object instrument(Object target,  Recorder recorder)
    throws NotWrappableTypeException;

  public void instrumentClass(Object target,
                              Class<?> targetClass,
                              Recorder recorder)
    throws NotWrappableTypeException {

    if (!isInstrumentable(targetClass)) {
      throw new NotWrappableTypeException("Cannot instrument " +
                                          targetClass);
    }

    for (Constructor<?> constructor : targetClass.getDeclaredConstructors()) {
       instrument(targetClass, constructor, recorder);
    }

    Class<?> c = targetClass;

    do {
      for (Method method : c.getDeclaredMethods()) {
        instrument(
          Modifier.isStatic(method.getModifiers()) ? targetClass : target,
          method,
          recorder);
      }

      c = c.getSuperclass();
    }
    while (isInstrumentable(c));
  }

  public void instrumentPublicMethodsByName(Object target,
                                            String methodName,
                                            Recorder recorder,
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

      instrument(target, method, recorder);
    }
  }

  public void instrument(Object target,
                         Constructor<?> constructor,
                         Recorder recorder) {
    final String location = m_weaver.weave(constructor);

//    System.out.printf("register(%s, %s, %s, %s)%n",
//                      target.hashCode(), location,
//                      target,
//                      constructor);

    m_recorderRegistry.register(target, location, recorder);
  }

  public void instrument(Object target, Method method, Recorder recorder) {
    final String location = m_weaver.weave(method);

//    System.out.printf("register(%s, %s, %s, %s)%n",
//                      target.hashCode(), location,
//                      target,
//                      method);

    m_recorderRegistry.register(target, location, recorder);
  }

  private static boolean isInstrumentable(Class<?> targetClass) {

    // We disallow instrumentation of these classes to avoid the need for
    // complex protection against recursion in the engine itself.
    // Also, classes from the bootstrap classloader can't statically
    // refer to RecorderLocator.
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
}
