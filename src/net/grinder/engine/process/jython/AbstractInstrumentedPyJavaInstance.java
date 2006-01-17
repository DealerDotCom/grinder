// Copyright (C) 2002, 2003, 2004, 2005 2006 Philip Aston
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
import net.grinder.engine.process.jython.JythonScriptEngine.PyDispatcher;

import org.python.core.Py;
import org.python.core.PyJavaInstance;
import org.python.core.PyObject;


/**
 * An instrumented <code>PyJavaInstance</code>.
 *
 * @author Philip Aston
 * @version $Revision$
 */
abstract class AbstractInstrumentedPyJavaInstance extends PyJavaInstance {
  private final PyDispatcher m_dispatcher;
  private final PyObject m_pyTest;
  private final Object m_target;

  public AbstractInstrumentedPyJavaInstance(Test test,
                                            PyDispatcher dispatcher,
                                            Object target) {
    super(target);

    m_dispatcher = dispatcher;
    m_pyTest = new PyJavaInstance(test);
    m_target = target;
  }

  protected final PyDispatcher getDispatcher() {
    return m_dispatcher;
  }

  public PyObject __findattr__(String name) {
    // Valid because name is interned.
    if (name == InstrumentedPyInstance.TEST_FIELD_NAME) {
      return m_pyTest;
    }

    if (name == InstrumentedPyInstance.TARGET_FIELD_NAME) {
      return Py.java2py(m_target);
    }

    return super.__findattr__(name);
  }
}

