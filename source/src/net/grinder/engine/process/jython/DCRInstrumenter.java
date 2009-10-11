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

package net.grinder.engine.process.jython;

import java.lang.reflect.Method;

import net.grinder.common.Test;
import net.grinder.engine.process.InstrumentationLocator;
import net.grinder.engine.process.Instrumenter;
import net.grinder.engine.process.ScriptEngine.Instrumentation;
import net.grinder.script.NotWrappableTypeException;
import net.grinder.util.weave.Weaver;
import net.grinder.util.weave.WeavingException;
import net.grinder.util.weave.j2se6.ASMTransformerFactory;
import net.grinder.util.weave.j2se6.DCRWeaver;

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

  private final Weaver m_weaver;

  /**
   * Constructor for DCRInstrumenter.
   */
  public DCRInstrumenter() {
    try {
      m_weaver =
        new DCRWeaver(new ASMTransformerFactory(InstrumentationLocator.class));
    }
    catch (WeavingException e) {
      throw new AssertionError(e);
    }
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
      if (target instanceof PyInstance ||
          target instanceof PyFunction||
          target instanceof PyMethod) {
        instrument(target, "invoke", instrumentation);
      }
      else if (target instanceof PyReflectedFunction) {
        // __call__ method; would invoke() do?
        instrument(target, "__call__", instrumentation);
      }
    }
    else if (target instanceof PyProxy) {
      // Jython object that extends a Java class.
      final PyInstance pyInstance = ((PyProxy)target)._getPyInstance();
      instrument(pyInstance, "invoke", instrumentation);
    }
    else if (target == null) {
      throw new NotWrappableTypeException("Can't wrap null/None");
    }
    else {
      // Java object.
      // All methods?
      // Static methods will use class itself as the reference.
    }

    try {
      m_weaver.applyChanges();
    }
    catch (WeavingException e) {
      throw new NotWrappableTypeException(e.getMessage());
    }

    return target;
  }

  private void instrument(Object target,
                          String methodName,
                          Instrumentation instrumentation)
    throws NotWrappableTypeException {

    // System.err.printf("%s %s %s", o.getClass(), o, methodName);

    for (Method m : target.getClass().getMethods()) {
      if (!m.getName().equals(methodName)) {
        continue;
      }

      final String location = m_weaver.weave(m);

      InstrumentationLocator.register(target,
                                      location,
                                      instrumentation);
    }
  }
}
