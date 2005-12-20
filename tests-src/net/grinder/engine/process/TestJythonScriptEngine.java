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

package net.grinder.engine.process;

import java.util.Random;

import net.grinder.common.StubTest;
import net.grinder.common.Test;
import net.grinder.engine.process.ScriptEngine.Dispatcher;
import net.grinder.engine.process.ScriptEngine.Dispatcher.Invokeable;
import net.grinder.engine.process.jython.JythonScriptEngine;
import net.grinder.script.NotWrappableTypeException;
import net.grinder.script.Grinder.ScriptContext;
import net.grinder.testutility.RandomStubFactory;

import org.python.core.Py;
import org.python.core.PyObject;
import org.python.core.PySystemState;
import org.python.util.PythonInterpreter;

import junit.framework.TestCase;


/**
 * Unit tests for {@link JythonScriptEngine}.
 *
 * @author Philip Aston
 * @version $Revision$
 */
public class TestJythonScriptEngine extends TestCase {

  public void testCreateProxy() throws Exception {
    System.setProperty("python.verbose", "warning");

    PySystemState.initialize();

    final PySystemState systemState = new PySystemState();
    final PythonInterpreter interpreter =
      new PythonInterpreter(null, systemState);

    final RandomStubFactory scriptContextStubFactory =
      new RandomStubFactory(ScriptContext.class);
    final ScriptContext scriptContext =
      (ScriptContext)scriptContextStubFactory.getStub();

    final JythonScriptEngine scriptEngine =
      new JythonScriptEngine(scriptContext);

    final Test test = new StubTest(1, "test");

    final DispatcherStubFactory dispatcherStubFactory =
      new DispatcherStubFactory();
    final Dispatcher dispatcher = dispatcherStubFactory.getDispatcher();

    // PyFunction.
    interpreter.exec("def blah(): return 1");
    final PyObject pyFunction = interpreter.get("blah");
    final PyObject pyFunctionProxy =
      (PyObject)scriptEngine.createInstrumentedProxy(test,
                                                     dispatcher,
                                                     pyFunction);
    final PyObject result1 = pyFunctionProxy.invoke("__call__");
    assertEquals(new Integer(1), result1.__tojava__(Integer.class));
    dispatcherStubFactory.assertSuccess("dispatch", Invokeable.class);
    dispatcherStubFactory.assertNoMoreCalls();
    assertSame(test,
      pyFunctionProxy.__getattr__("__test__").__tojava__(Test.class));

    // PyInstance.
    interpreter.exec("class Foo:\n def bah(self): return 2\nx=Foo()");
    final PyObject pyInstance = interpreter.get("x");
    final PyObject pyInstanceProxy =
      (PyObject)scriptEngine.createInstrumentedProxy(test,
                                                     dispatcher,
                                                     pyInstance);
    final PyObject result2 = pyInstanceProxy.invoke("bah");
    assertEquals(new Integer(2), result2.__tojava__(Integer.class));
    dispatcherStubFactory.assertSuccess("dispatch", Invokeable.class);
    dispatcherStubFactory.assertNoMoreCalls();
    assertSame(test,
      pyInstanceProxy.__getattr__("__test__").__tojava__(Test.class));

    // PyMethod.
    interpreter.exec("x=Foo.bah");
    final PyObject pyMethod = interpreter.get("x");
    final PyObject pyMethodProxy =
      (PyObject)scriptEngine.createInstrumentedProxy(test,
                                                     dispatcher,
                                                     pyMethod);
    final PyObject result3 = pyMethodProxy.invoke("__call__", pyInstanceProxy);
    assertEquals(new Integer(2), result3.__tojava__(Integer.class));
    dispatcherStubFactory.assertSuccess("dispatch", Invokeable.class);
    dispatcherStubFactory.assertNoMoreCalls();
    assertSame(test,
      pyMethodProxy.__getattr__("__test__").__tojava__(Test.class));

    // PyJavaInstance.
    interpreter.exec("from java.util import Random\nx=Random()");
    final PyObject pyJava = interpreter.get("x");
    final PyObject pyJavaProxy =
      (PyObject)scriptEngine.createInstrumentedProxy(test, dispatcher, pyJava);
    final PyObject result4 = pyJavaProxy.invoke("getClass");
    assertEquals(Random.class, result4.__tojava__(Class.class));
    dispatcherStubFactory.assertSuccess("dispatch", Invokeable.class);
    dispatcherStubFactory.assertNoMoreCalls();
    assertSame(test,
      pyJavaProxy.__getattr__("__test__").__tojava__(Test.class));

    // PyReflectedFunction
    interpreter.exec("y=Random.nextInt");
    final PyObject pyJavaMethod = interpreter.get("y");
    final PyObject pyJavaMethodProxy =
      (PyObject)scriptEngine.createInstrumentedProxy(test,
                                                     dispatcher,
                                                     pyJavaMethod);
    final PyObject result5 = pyJavaMethodProxy.__call__(pyJava);
    assertTrue(result5.__tojava__(Object.class) instanceof Integer);
    dispatcherStubFactory.assertSuccess("dispatch", Invokeable.class);
    dispatcherStubFactory.assertNoMoreCalls();
    assertSame(test,
      pyJavaMethodProxy.__getattr__("__test__").__tojava__(Test.class));

    // PyProxy. PyProxy's come paired with PyInstances - need to call
    // __tojava__ to get the PyProxy.
    interpreter.exec("class PyRandom(Random):\n def one(self): return 1\n" +
                     "x=PyRandom()");
    final Object pyProxy = interpreter.get("x").__tojava__(Object.class);
    final PyObject pyProxyProxy =
      (PyObject)scriptEngine.createInstrumentedProxy(test, dispatcher, pyProxy);
    final PyObject result7 = pyProxyProxy.invoke("one");
    assertEquals(new Integer(1), result7.__tojava__(Integer.class));
    dispatcherStubFactory.assertSuccess("dispatch", Invokeable.class);
    dispatcherStubFactory.assertNoMoreCalls();
    assertSame(test,
      pyProxyProxy.__getattr__("__test__").__tojava__(Test.class));

    // Java object.
    final Object java = new MyClass();
    final PyObject javaProxy =
      (PyObject)scriptEngine.createInstrumentedProxy(test, dispatcher, java);
    final PyObject result8 = javaProxy.invoke("addOne",
                                              Py.java2py(new Integer(10)));
    assertEquals(new Integer(11), result8.__tojava__(Integer.class));
    dispatcherStubFactory.assertSuccess("dispatch", Invokeable.class);
    dispatcherStubFactory.assertNoMoreCalls();
    assertSame(test,
      javaProxy.__getattr__("__test__").__tojava__(Test.class));
  }

  public static class MyClass {
    public int addOne(int i) {
      return i + 1;
    }
  }

  public void testCreateProxyWithNonWrappableParameters() throws Exception {

    // Can't wrap arrays.
    assertNotWrappable(new int[] { 1, 2, 3 });
    assertNotWrappable(new Object[] { "foo", new Object() });

    // Can't wrap strings.
    assertNotWrappable("foo bah");
    assertNotWrappable(new String());

    // Can't wrap numbers.
    assertNotWrappable(new Long(56));
    assertNotWrappable(new Integer(56));
    assertNotWrappable(new Short((short)56));
    assertNotWrappable(new Byte((byte)56));

    PySystemState.initialize();

    final PySystemState systemState = new PySystemState();
    final PythonInterpreter interpreter =
      new PythonInterpreter(null, systemState);

    // Can't wrap PyInteger.
    interpreter.exec("x=1");
    assertNotWrappable(interpreter.get("x"));

    // Can't wrap PyClass.
    interpreter.exec("class Foo: pass");
    assertNotWrappable(interpreter.get("Foo"));
  }

  public static final class DispatcherStubFactory extends RandomStubFactory {
    public DispatcherStubFactory() {
      super(Dispatcher.class);
    }

    Dispatcher getDispatcher() {
      return (Dispatcher)getStub();
    }

    public Object override_dispatch(Object proxy, Invokeable invokeable) {
      return invokeable.call();
    }
  }

  private void assertNotWrappable(Object o) throws Exception {
    final RandomStubFactory scriptContextStubFactory =
      new RandomStubFactory(ScriptContext.class);
    final ScriptContext scriptContext =
      (ScriptContext)scriptContextStubFactory.getStub();

    final JythonScriptEngine scriptEngine =
      new JythonScriptEngine(scriptContext);

    try {
      scriptEngine.createInstrumentedProxy(null, null, o);
      fail("Expected NotWrappableTypeException");
    }
    catch (NotWrappableTypeException e) {
    }
  }
}
