// Copyright (C) 2005 Philip Aston
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

import org.python.core.PyJavaInstance;
import org.python.core.PyObject;
import org.python.core.PyReflectedFunction;


/**
 * An instrumented <code>PyReflectedFunction</code>.
 *
 * @author Philip Aston
 * @version $Revision$
 */
class InstrumentedPyReflectedFunction extends PyReflectedFunction {
  private final PyDispatcher m_dispatcher;
  private final PyObject m_pyTest;

  public InstrumentedPyReflectedFunction(Test test,
                                         PyDispatcher dispatcher,
                                         PyReflectedFunction target) {
    super(target.__name__);

    // We follow the same logic as PyReflectedFunction.copy(), except we
    // shallow copy argslist as ReflectedArgs is package scope.
    __doc__ = target.__doc__;
    nargs = target.nargs;
    argslist = target.argslist;

    m_dispatcher = dispatcher;
    m_pyTest = new PyJavaInstance(test);
  }

  public PyObject __findattr__(String name) {
    if (name == "__test__") { // Valid because name is interned.
      return m_pyTest;
    }

    return super.__findattr__(name);
  }

  public PyObject __call__(final PyObject self, final PyObject[] args,
                           final String[] keywords) {

    return m_dispatcher.dispatch(
      new Dispatcher.Invokeable() {
        public Object call() {
          return InstrumentedPyReflectedFunction.super.__call__(
            self, args, keywords);
        }
      });
  }
}

