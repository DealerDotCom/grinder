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
import net.grinder.engine.process.Instrumenter;
import net.grinder.engine.process.ScriptEngine.TestInstrumentation;
import net.grinder.script.NotWrappableTypeException;
import net.grinder.utils.weave.ASMTransformerFactory;
import net.grinder.utils.weave.AdviceImpl;
import net.grinder.utils.weave.DCRWeaver;
import net.grinder.utils.weave.Weaver;
import net.grinder.utils.weave.WeavingException;

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

  private final Weaver m_weaver =
    new DCRWeaver(new ASMTransformerFactory(AdviceImpl.class));

  /**
   * {@inheritDoc}
   */
  public Object createInstrumentedProxy(Test test,
                                        TestInstrumentation testInstrumentation,
                                        Object o)
    throws NotWrappableTypeException {

    if (o instanceof PyObject) {
      // Jython object.
      if (o instanceof PyInstance ||
          o instanceof PyFunction||
          o instanceof PyMethod) {
        instrument(o, "invoke");
      }
      else if (o instanceof PyReflectedFunction) {
        // __call__ method; would invoke() do?
        instrument(o, "__call__");
      }
    }
    else if (o instanceof PyProxy) {
      // Jython object that extends a Java class.
      final PyInstance pyInstance = ((PyProxy)o)._getPyInstance();
      instrument(pyInstance, "invoke");
    }
    else if (o == null) {
      throw new NotWrappableTypeException("Can't wrap null/None");
    }
    else {
      // Java object.
      // All methods?
      // Static methods will use class itself as the reference.
      // TODO
    }

    try {
      m_weaver.applyChanges();
    }
    catch (WeavingException e) {
      throw new NotWrappableTypeException(e.getMessage());
    }

    return o;
  }

  private void instrument(Object o, String methodName)
    throws NotWrappableTypeException {

    System.err.printf("%s %s %s", o.getClass(), o, methodName);

    for (Method m : o.getClass().getMethods()) {
      if (!m.getName().equals(methodName)) {
        continue;
      }

      m_weaver.weave(m);
    }
  }
}
