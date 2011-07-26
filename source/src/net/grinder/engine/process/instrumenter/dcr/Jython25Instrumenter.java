// Copyright (C) 2009-2011 Philip Aston
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

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import net.grinder.engine.process.ScriptEngine.Recorder;
import net.grinder.script.NonInstrumentableTypeException;
import net.grinder.util.weave.Weaver;
import net.grinder.util.weave.WeavingException;
import net.grinder.util.weave.Weaver.TargetSource;

import org.python.core.PyClass;
import org.python.core.PyFunction;
import org.python.core.PyInstance;
import org.python.core.PyMethod;
import org.python.core.PyObject;
import org.python.core.PyObjectDerived;
import org.python.core.PyProxy;
import org.python.core.PyReflectedConstructor;
import org.python.core.PyReflectedFunction;
import org.python.core.PyType;
import org.python.core.ThreadState;


/**
 * DCRInstrumenter for Jython 2.5+.
 *
 * @author Philip Aston
 * @version $Revision:$
 */
final class Jython25Instrumenter extends DCRInstrumenter {

  private final Instrumenter m_pyInstanceInstrumenter;
  private final Instrumenter m_pyFunctionInstrumenter;
  private final Instrumenter m_pyMethodInstrumenter;
  private final Instrumenter m_pyReflectedConstructorInstrumenter;
  private final Instrumenter m_pyReflectedFunctionInstrumenter;
  private final Instrumenter m_pyDerivedObjectInstrumenter;
  private final Instrumenter m_pyProxyInstrumenter;
  private final Instrumenter m_pyTypeInstrumenter;
  private final Instrumenter m_pyClassInstrumenter;

  /**
   * Constructor for DCRInstrumenter.
   *
   * @param weaver The weaver.
   * @param recorderRegistry The recorder registry.
   * @throws WeavingException If it looks like Jython 2.5 isn't available.
   */
  public Jython25Instrumenter(Weaver weaver,
                              RecorderRegistry recorderRegistry)
    throws WeavingException  {

    super(weaver, recorderRegistry);

    try {
      final List<Method> methodsForPyFunction = new ArrayList<Method>();

      for (Method method : PyFunction.class.getDeclaredMethods()) {
        // Roughly identify the fundamental __call__ methods, i.e. those
        // that call the actual func_code.
        if ((method.getName().equals("__call__") ||
             // Add function__call__ for refactoring in Jython 2.5.2.
             method.getName().equals("function___call__")) &&
            method.getParameterTypes().length >= 1 &&
            method.getParameterTypes()[0] == ThreadState.class) {
          methodsForPyFunction.add(method);
        }
      }

      assertAtLeastOneMethod(methodsForPyFunction);

      m_pyFunctionInstrumenter = new Instrumenter() {
          public void transform(Recorder recorder, Object target)
            throws NonInstrumentableTypeException {

            for (Method method : methodsForPyFunction) {
              instrument(target,
                         method,
                         TargetSource.FIRST_PARAMETER,
                         recorder);
            }
          }
        };

      final List<Method> methodsForPyInstance = new ArrayList<Method>();

      for (Method method : PyFunction.class.getDeclaredMethods()) {
        // Here we're finding the fundamental __call__ methods that also
        // take an instance argument.
        if (method.getName().equals("__call__") &&
            method.getParameterTypes().length >= 2 &&
            method.getParameterTypes()[0] == ThreadState.class &&
            method.getParameterTypes()[1] == PyObject.class) {
          methodsForPyInstance.add(method);
        }
      }

      assertAtLeastOneMethod(methodsForPyInstance);

      m_pyInstanceInstrumenter = new Instrumenter() {
          public void transform(Recorder recorder, Object target)
            throws NonInstrumentableTypeException {

            for (Method method : methodsForPyInstance) {
              instrument(target,
                         method,
                         TargetSource.THIRD_PARAMETER,
                         recorder);
            }
          }
        };

      final List<Method> methodsForPyMethod = new ArrayList<Method>();

      for (Method method : PyMethod.class.getDeclaredMethods()) {
        // Roughly identify the fundamental __call__ methods, i.e. those
        // that call the actual func_code.
        if ((method.getName().equals("__call__") ||
             // Add instancemethod___call__ for refactoring in Jython 2.5.2.
             method.getName().equals("instancemethod___call__")) &&
            method.getParameterTypes().length >= 1 &&
            method.getParameterTypes()[0] == ThreadState.class) {
          methodsForPyMethod.add(method);
        }
      }

      assertAtLeastOneMethod(methodsForPyMethod);

      m_pyMethodInstrumenter = new Instrumenter() {
          public void transform(Recorder recorder, Object target)
            throws NonInstrumentableTypeException {

            for (Method method : methodsForPyMethod) {
              instrument(target,
                         method,
                         TargetSource.FIRST_PARAMETER,
                         recorder);
            }
          }
        };

      final Method pyReflectedConstructorCall =
        PyReflectedConstructor.class.getDeclaredMethod("__call__",
                                                       PyObject.class,
                                                       PyObject[].class,
                                                       String[].class);

      m_pyReflectedConstructorInstrumenter = new Instrumenter() {
          public void transform(Recorder recorder, Object target)
            throws NonInstrumentableTypeException {
            instrument(target,
                       pyReflectedConstructorCall,
                       TargetSource.FIRST_PARAMETER,
                       recorder);
          }
        };

      final Method pyReflectedFunctionCall =
        PyReflectedFunction.class.getDeclaredMethod("__call__",
                                                    PyObject.class,
                                                    PyObject[].class,
                                                    String[].class);

      m_pyReflectedFunctionInstrumenter = new Instrumenter() {
          public void transform(Recorder recorder, Object target)
            throws NonInstrumentableTypeException {
            instrument(target,
                       pyReflectedFunctionCall,
                       TargetSource.FIRST_PARAMETER,
                       recorder);
          }
        };

      m_pyDerivedObjectInstrumenter = new Instrumenter() {
        public void transform(Recorder recorder, Object target)
            throws NonInstrumentableTypeException {
            instrument(target,
                       pyReflectedFunctionCall,
                       TargetSource.SECOND_PARAMETER,
                       recorder);
          }
        };

      // PyProxy is used for Jython objects that extend a Java class.
      // We can't just use the Java wrapping, since then we'd miss the
      // Jython methods.

      // Need to look up this method dynamically, the return type differs
      // between 2.2 and 2.5.
      final Method pyProxyPyInstanceMethod =
        PyProxy.class.getDeclaredMethod("_getPyInstance");

      m_pyProxyInstrumenter = new Instrumenter() {
          public void transform(Recorder recorder, Object target)
            throws NonInstrumentableTypeException {
            PyObject pyInstance;

            try {
              pyInstance = (PyObject) pyProxyPyInstanceMethod.invoke(target);
            }
            catch (Exception e) {
              throw new NonInstrumentableTypeException(
                "Could not call _getPyInstance", e);
            }

            m_pyInstanceInstrumenter.transform(recorder, pyInstance);
            m_pyDerivedObjectInstrumenter.transform(recorder, pyInstance);
          }
        };

      // Instrumenting a class doesn't instrument static methods.
      // This is inconsistent with respect to the Java DCR instrumenter,
      // but better fits Python, with its first class functions.

      // Unlike other classes, the "exposed method" type___call__ for PyType
      // does not delegate to the general __call__ method; its the other way
      // round. It can be called directly, so we instrument it.
      final Method pyTypeCall =
        PyType.class.getDeclaredMethod("type___call__",
                                       PyObject[].class,
                                       String[].class);

      m_pyTypeInstrumenter = new Instrumenter() {
          public void transform(Recorder recorder, Object target)
            throws NonInstrumentableTypeException {
            instrument(target,
                       pyTypeCall,
                       TargetSource.FIRST_PARAMETER,
                       recorder);
          }
        };

      final Method pyClassCall =
        PyClass.class.getDeclaredMethod("__call__",
                                        PyObject[].class,
                                        String[].class);

      m_pyClassInstrumenter = new Instrumenter() {
          public void transform(Recorder recorder, Object target)
            throws NonInstrumentableTypeException {
            instrument(target,
                       pyClassCall,
                       TargetSource.FIRST_PARAMETER,
                       recorder);
          }
        };
    }
    catch (NoSuchMethodException e) {
      throw new WeavingException("Jython 2.5 not found", e);
    }
  }

  private static void assertAtLeastOneMethod(List<Method> methods)
    throws WeavingException {
    if (methods.size() == 0) {
      throw new WeavingException("Jython 2.5 not found");
    }
  }

  /**
   * {@inheritDoc}
   */
  public String getDescription() {
    return "byte code transforming instrumenter for Jython 2.5";
  }

  @Override
  protected boolean instrument(Object target, Recorder recorder)
    throws NonInstrumentableTypeException {

    if (target instanceof PyObject) {
      // Jython object.
      if (target instanceof PyInstance) {
        m_pyInstanceInstrumenter.transform(recorder, target);
      }
      else if (target instanceof PyFunction) {
        m_pyFunctionInstrumenter.transform(recorder, target);
      }
      else if (target instanceof PyMethod) {
        m_pyMethodInstrumenter.transform(recorder, target);
      }
      else if (target instanceof PyObjectDerived) {
        m_pyDerivedObjectInstrumenter.transform(recorder, target);
      }
      else if (target instanceof PyReflectedConstructor) {
        m_pyReflectedConstructorInstrumenter.transform(recorder, target);
      }
      else if (target instanceof PyReflectedFunction) {
        m_pyReflectedFunctionInstrumenter.transform(recorder, target);
      }
      else if (target instanceof PyType) {
        m_pyTypeInstrumenter.transform(recorder, target);
      }
      else if (target instanceof PyClass) {
        m_pyClassInstrumenter.transform(recorder, target);
      }
      else {
        // Fail, rather than guess a generic approach.
        throw new NonInstrumentableTypeException("Unknown PyObject:" +
                                                 target.getClass());
      }
    }
    else if (target instanceof PyProxy) {
      m_pyProxyInstrumenter.transform(recorder, target);
    }
    else {
      // Let the Java instrumenter have a go.
      return false;
    }

    return true;
  }

  private interface Instrumenter {
    void transform(Recorder recorder, Object target)
      throws NonInstrumentableTypeException;
  }
}
