// Copyright (C) 2002, 2003, 2004, 2005 Philip Aston
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

package net.grinder.engine.process.jython;

import net.grinder.common.Test;
import net.grinder.engine.process.ScriptEngine.Dispatcher;
import net.grinder.engine.process.jython.JythonScriptEngine;
import net.grinder.engine.process.jython.JythonScriptEngine.PyDispatcher;

import org.python.core.PyMethod;
import org.python.core.PyObject;


/**
 * An instrumented <code>PyJavaInstance</code> that is used to wrap Java
 * instances.
 *
 * <p>
 * Plain InstrumentedPyJavaInstanceForPyMethodsAndPyFunctions's work for Jython
 * 2.1. However, in Jython 2.2 the invoke methods use {@link #__findattr__} to
 * determine which method to call, which bypassed our instrumentation. This
 * special version ensures that the methods we hand back are also instrumented
 * correctly.
 * </p>
 *
 * @author Philip Aston
 * @version $Revision$
 */
final class InstrumentedPyJavaInstanceForJavaInstances
  extends AbstractInstrumentedPyJavaInstance {

  private final JythonScriptEngine.PyInstrumentedProxyFactory m_proxyFactory;
  private final Test m_test;

  public InstrumentedPyJavaInstanceForJavaInstances(
    final JythonScriptEngine.PyInstrumentedProxyFactory proxyFactory,
    Test test,
    final PyDispatcher dispatcher,
    Object target) {

    super(test, dispatcher, target);

    m_proxyFactory = proxyFactory;
    m_test = test;
  }

  public PyObject __findattr__(String name) {
    final PyObject unadorned =
      InstrumentedPyJavaInstanceForJavaInstances.super.__findattr__(name);

    if (unadorned instanceof PyMethod) {
      // See notes in InstrumentedPyInstance about why we don't cache this.
      return m_proxyFactory.instrumentPyMethod(
        m_test, getDispatcher(), (PyMethod)unadorned);
    }

    return unadorned;
  }

  public PyObject invoke(final String name) {
    return getDispatcher().dispatch(
      new Dispatcher.Callable() {
        public Object call() {
          return InstrumentedPyJavaInstanceForJavaInstances.super.invoke(name);
        }
      }
      );
  }

  public PyObject invoke(final String name, final PyObject arg1) {
    return getDispatcher().dispatch(
      new Dispatcher.Callable() {
        public Object call() {
          return InstrumentedPyJavaInstanceForJavaInstances.super.invoke(
            name, arg1);
        }
      }
    );
  }

  public PyObject invoke(final String name, final PyObject arg1,
                         final PyObject arg2) {
    return getDispatcher().dispatch(
      new Dispatcher.Callable() {
        public Object call() {
          return InstrumentedPyJavaInstanceForJavaInstances.super.invoke(
            name, arg1, arg2);
        }
      }
    );
  }
}

