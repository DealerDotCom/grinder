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
import net.grinder.engine.process.jython.JythonScriptEngine.PyDispatcher;

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

  public InstrumentedPyJavaInstanceForPyMethodsAndPyFunctions(Test test,
                                    PyDispatcher dispatcher,
                                    Object target) {
    super(test, dispatcher, target);
  }

  public final PyObject invoke(final String name) {
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
    return getDispatcher().dispatch(
      new Dispatcher.Callable() {
        public Object call() {
          return InstrumentedPyJavaInstanceForPyMethodsAndPyFunctions
            .super.invoke(name, arg1);
        }
      }
      );
  }

  public final PyObject invoke(final String name, final PyObject arg1,
                               final PyObject arg2) {
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
    return getDispatcher().dispatch(
      new Dispatcher.Callable() {
        public Object call() {
          return InstrumentedPyJavaInstanceForPyMethodsAndPyFunctions
            .super.invoke(name, args);
        }
      }
      );
  }

  public final PyObject invoke(final String name, final PyObject[] args,
                               final String[] keywords) {
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

