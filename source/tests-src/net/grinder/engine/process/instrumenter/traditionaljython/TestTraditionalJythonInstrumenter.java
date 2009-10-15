// Copyright (C) 2005 - 2009 Philip Aston
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

package net.grinder.engine.process.instrumenter.traditionaljython;

import net.grinder.common.Test;
import net.grinder.engine.process.instrumenter.AbstractInstrumenterTestCase;

import org.python.core.PyObject;
import org.python.util.PythonInterpreter;



/**
 * Unit tests for {@link JythonInstrumenter}.
 *
 * @author Philip Aston
 * @version $Revision: 4057 $
 */
public class TestTraditionalJythonInstrumenter
  extends AbstractInstrumenterTestCase {

  public TestTraditionalJythonInstrumenter() throws Exception {
    super(new TraditionalJythonInstrumenter());
  }

  @Override protected void assertTestReference(PyObject pyObject, Test test) {
    assertSame(test, pyObject.__getattr__("__test__").__tojava__(Test.class));
  }

  @Override
  protected void assertTargetReference(PyObject proxy,
                                       Object original,
                                       boolean unwrapTarget) {
    final PyObject targetReference = proxy.__getattr__("__target__");

    final Object target =
      unwrapTarget ? targetReference.__tojava__(Object.class) : targetReference;

    assertSame(original, target);
    assertNotSame(proxy, target);
  }

  public void testCreateProxyWithNonWrappableParameters() throws Exception {

    // The types that can be wrapped depend on the Instrumenter.

    // Can't wrap arrays.
    assertNotWrappable(new int[] { 1, 2, 3 });
    assertNotWrappable(new Object[] { "foo", new Object() });

    // Can't wrap strings.
    assertNotWrappable("foo bah");

    // Can't wrap numbers.
    assertNotWrappable(new Long(56));
    assertNotWrappable(new Integer(56));
    assertNotWrappable(new Short((short) 56));
    assertNotWrappable(new Byte((byte) 56));

    final PythonInterpreter interpreter = getInterpretter();

    // Can't wrap PyInteger.
    interpreter.exec("x=1");
    assertNotWrappable(interpreter.get("x"));

    // Can't wrap PyClass.
    interpreter.exec("class Foo: pass");
    assertNotWrappable(interpreter.get("Foo"));

    // Can't wrap None.
    assertNotWrappable(null);
  }

  @Override
  protected PyObject proxyToPyObject(Object proxy) {
    return (PyObject)proxy;
  }

  @Override
  protected boolean isProxyInstrumentation() {
    return true;
  }
}
