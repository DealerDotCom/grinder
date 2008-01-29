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
// COPYRIGHT HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
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
import net.grinder.engine.process.jython.JythonScriptEngine.PyDispatcher;

import org.python.core.PyFunction;
import org.python.core.PyMethod;
import org.python.core.PyObject;


/**
 * An instrumented <code>PyJavaInstance</code>, used to wrap PyFunctions and
 * PyMethods.
 *
 * @author Philip Aston
 * @version $Revision$
 */
class InstrumentedPyJavaInstanceForPyMethodsAndPyFunctions
  extends AbstractInstrumentedPyJavaInstance {

  private final PyFunction m_pyFunction;
  private final PyMethod m_pyMethod;

  public InstrumentedPyJavaInstanceForPyMethodsAndPyFunctions(
    Test test,
    PyDispatcher dispatcher,
    PyFunction pyFunction) {
    super(test, dispatcher, pyFunction);
    m_pyFunction = pyFunction;
    m_pyMethod = null;
  }

  public InstrumentedPyJavaInstanceForPyMethodsAndPyFunctions(
    Test test,
    PyDispatcher dispatcher,
    PyMethod pyMethod) {
    super(test, dispatcher, pyMethod);
    m_pyFunction = null;
    m_pyMethod = pyMethod;
  }

  public final PyObject invoke(final String name) {

    if (name == InstrumentedPyInstance.TARGET_FIELD_NAME) {
      // Under Jython 2.1, wrapped.__target__() comes through this path. Under
      // Jython 2.2, it is dispatched via __find_attr__ and this code path is
      // unnecessary.
      if (m_pyFunction != null) {
        return m_pyFunction.__call__();
      }
      else {
        return m_pyMethod.__call__();
      }
    }

    return getDispatcher().dispatch(
      new Dispatcher.Callable() {
        public Object call() {
          return InstrumentedPyJavaInstanceForPyMethodsAndPyFunctions
            .super.invoke(name);
        }
      }
    );
  }

  public final PyObject invoke(final String name, final PyObject arg1) {

    if (name == InstrumentedPyInstance.TARGET_FIELD_NAME) {
      if (m_pyFunction != null) {
        return m_pyFunction.__call__(arg1);
      }
      else {
        return m_pyMethod.__call__(arg1);
      }
    }

    return getDispatcher().dispatch(
      new Dispatcher.Callable() {
        public Object call() {
          return InstrumentedPyJavaInstanceForPyMethodsAndPyFunctions
            .super.invoke(name, arg1);
        }
      }
    );
  }

  public final PyObject invoke(
    final String name, final PyObject arg1, final PyObject arg2) {

    if (name == InstrumentedPyInstance.TARGET_FIELD_NAME) {
      if (m_pyFunction != null) {
        return m_pyFunction.__call__(arg1, arg2);
      }
      else {
        return m_pyMethod.__call__(arg1, arg2);
      }
    }

    return getDispatcher().dispatch(
      new Dispatcher.Callable() {
        public Object call() {
          return InstrumentedPyJavaInstanceForPyMethodsAndPyFunctions
            .super.invoke(name, arg1, arg2);
        }
      }
    );
  }

  public final PyObject invoke(final String name, final PyObject[] args) {

    if (name == InstrumentedPyInstance.TARGET_FIELD_NAME) {
      if (m_pyFunction != null) {
        return m_pyFunction.__call__(args);
      }
      else {
        return m_pyMethod.__call__(args);
      }
    }

    return getDispatcher().dispatch(
      new Dispatcher.Callable() {
          public Object call() {
            return InstrumentedPyJavaInstanceForPyMethodsAndPyFunctions
              .super.invoke(name, args);
          }
      }
    );
  }

  public final PyObject invoke(
    final String name, final PyObject[] args, final String[] keywords) {

    /*
    // Neither Jython 2.1 or 2.2 take this path.
    if (name == InstrumentedPyInstance.TARGET_FIELD_NAME) {
      if (m_pyFunction != null) {
        return m_pyFunction.__call__(args, keywords);
      }
      else {
        return m_pyMethod.__call__(args, keywords);
      }
    }
    */

    return getDispatcher().dispatch(
      new Dispatcher.Callable() {
        public Object call() {
          return InstrumentedPyJavaInstanceForPyMethodsAndPyFunctions
            .super.invoke(name, args, keywords);
        }
      }
    );
  }
}

