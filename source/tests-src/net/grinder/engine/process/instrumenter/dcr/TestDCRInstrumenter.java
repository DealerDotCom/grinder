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

package net.grinder.engine.process.instrumenter.dcr;

import net.grinder.common.Test;
import net.grinder.engine.process.instrumenter.AbstractInstrumenterTestCase;
import net.grinder.util.weave.Weaver;
import net.grinder.util.weave.WeavingException;
import net.grinder.util.weave.agent.ExposeInstrumentation;
import net.grinder.util.weave.j2se6.ASMTransformerFactory;
import net.grinder.util.weave.j2se6.DCRWeaver;

import org.python.core.Py;
import org.python.core.PyObject;
import org.python.util.PythonInterpreter;


/**
 * Unit tests for {@link DCRInstrumenter}.
 *
 * @author Philip Aston
 * @version $Revision:$
 */
public class TestDCRInstrumenter extends AbstractInstrumenterTestCase {

  private static final Weaver s_weaver;

  static {
    try {
      s_weaver =
        new DCRWeaver(new ASMTransformerFactory(InstrumentationLocator.class),
                      ExposeInstrumentation.getInstrumentation());
    }
    catch (WeavingException e) {
      throw new ExceptionInInitializerError(e);
    }
  }


  public TestDCRInstrumenter() throws Exception {
    super(new DCRInstrumenter(
                s_weaver,
                InstrumentationLocator.getInstrumentationRegistry()));
  }

  @Override protected void tearDown() throws Exception {
    super.tearDown();
    ((InstrumentationLocator)InstrumentationLocator.getInstrumentationRegistry())
    .clearInstrumentation();
  }

  @Override protected void assertTestReference(PyObject pyObject, Test test) {
    // No-op, DCRInstrumenter doesn't support __test__.
  }

  @Override
  protected void assertTargetReference(PyObject proxy,
                                       Object original,
                                       boolean unwrapTarget) {
    // DCRInstrumenter doesn't support __target__.
  }

  public void testCreateProxyWithNonWrappableParameters() throws Exception {

    // The types that can be wrapped depend on the Instrumenter.

    final PythonInterpreter interpreter = getInterpretter();

    // Can't wrap PyInteger.
    interpreter.exec("x=1");
    assertNotWrappable(interpreter.get("x"));

    // Can't wrap PyClass.
    interpreter.exec("class Foo: pass");
    assertNotWrappable(interpreter.get("Foo"));

    assertNotWrappable(null);

    assertNotWrappable(Object.class);
    assertNotWrappable(new Object());
    assertNotWrappable(new String());
    assertNotWrappable(java.util.Random.class);

    // Can't wrap classes in net.grinder.*.
    assertNotWrappable(this);
  }

  @Override
  protected PyObject proxyToPyObject(Object proxy) {
    return Py.java2py(proxy);
  }

  @Override
  protected boolean isProxyInstrumentation() {
    return false;
  }
}
