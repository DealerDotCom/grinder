// Copyright (C) 2002, 2003, 2004, 2005, 2006 Philip Aston
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

import org.python.core.ClonePyInstance;
import org.python.core.PyClass;
import org.python.core.PyInstance;
import org.python.core.PyJavaInstance;
import org.python.core.PyMethod;
import org.python.core.PyObject;


/**
 * An instrumented <code>PyInstance</code>.
 *
 * @author Philip Aston
 * @version $Revision$
 */
final class InstrumentedPyInstance extends ClonePyInstance {
  /** The field name that allows the test to be obtained from a proxy. */
  static final String TEST_FIELD_NAME = "__test__";

  /** The field name that allows the target to be obtained from a proxy. */
  static final String TARGET_FIELD_NAME = "__target__";

  private final JythonScriptEngine.PyInstrumentedProxyFactory m_proxyFactory;
  private final PyDispatcher m_dispatcher;
  private final Test m_test;
  private final PyObject m_pyTest;

  public InstrumentedPyInstance(
    final JythonScriptEngine.PyInstrumentedProxyFactory proxyFactory,
    Test test,
    PyDispatcher dispatcher,
    PyClass targetClass,
    PyInstance target) {

    super(targetClass, target);

    m_proxyFactory = proxyFactory;
    m_dispatcher = dispatcher;
    m_test = test;
    m_pyTest = new PyJavaInstance(test);
  }

  public PyObject __findattr__(String name) {
    if (name == TEST_FIELD_NAME) { // Valid because name is interned.
      return m_pyTest;
    }

    if (name == TARGET_FIELD_NAME) {
      return getTarget();
    }

    final PyObject unadorned =
      InstrumentedPyInstance.super.__findattr__(name);

    if (unadorned instanceof PyMethod) {
      // We create new instrumentation every time.
      //
      // At one point, we cached the instrumented method. However, Jython
      // doesn't cache the underlying PyMethod (it creates a new PyMethod
      // whenever one is asked for), so its hard/too expensive to check the
      // cached value is correct.
      //
      // Another bad idea was to call __setattr__ with the instrumented method,
      // and use our dictionary as the cache. We share our dictionary with our
      // target, so invocations on the target
      // ended up being instrumented.
      return m_proxyFactory.instrumentPyMethod(
        m_test, m_dispatcher, (PyMethod)unadorned);
    }

    return unadorned;
  }

  public PyObject invoke(final String name) {
    return m_dispatcher.dispatch(
      new Dispatcher.Callable() {
        public Object call() {
          return InstrumentedPyInstance.super.invoke(name);
        }
      }
    );
  }

  public PyObject invoke(final String name, final PyObject arg1) {
    return m_dispatcher.dispatch(
      new Dispatcher.Callable() {
        public Object call() {
          return InstrumentedPyInstance.super.invoke(name, arg1);
        }
      }
    );
  }

  public PyObject invoke(final String name, final PyObject arg1,
                         final PyObject arg2) {
    return m_dispatcher.dispatch(
      new Dispatcher.Callable() {
        public Object call() {
          return InstrumentedPyInstance.super.invoke(name, arg1, arg2);
        }
      }
    );
  }
}

