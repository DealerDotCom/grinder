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

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.Random;

import net.grinder.common.Logger;
import net.grinder.common.StubTest;
import net.grinder.common.Test;
import net.grinder.common.UncheckedGrinderException;
import net.grinder.engine.common.EngineException;
import net.grinder.engine.process.ScriptEngine.Dispatcher;
import net.grinder.engine.process.ScriptEngine.WorkerRunnable;
import net.grinder.engine.process.ScriptEngine.Dispatcher.Invokeable;
import net.grinder.engine.process.jython.JythonScriptEngine;
import net.grinder.script.NotWrappableTypeException;
import net.grinder.script.Grinder.ScriptContext;
import net.grinder.testutility.AbstractFileTestCase;
import net.grinder.testutility.AssertUtilities;
import net.grinder.testutility.CallData;
import net.grinder.testutility.RandomStubFactory;
import net.grinder.util.thread.UncheckedInterruptedException;

import org.python.core.Py;
import org.python.core.PyException;
import org.python.core.PyInteger;
import org.python.core.PyObject;
import org.python.core.PySystemState;
import org.python.util.PythonInterpreter;

/**
 * Unit tests for {@link JythonScriptEngine}.
 *
 * @author Philip Aston
 * @version $Revision$
 */
public class TestJythonScriptEngine extends AbstractFileTestCase {

  private final RandomStubFactory m_scriptContextStubFactory =
    new RandomStubFactory(ScriptContext.class);

  private final ScriptContext m_scriptContext =
    (ScriptContext) m_scriptContextStubFactory.getStub();

  {
    PySystemState.initialize();
  }

  private final PythonInterpreter m_interpreter =
    new PythonInterpreter(null, new PySystemState());

  private final PyObject m_one = new PyInteger(1);
  private final PyObject m_two = new PyInteger(2);
  private final PyObject m_three = new PyInteger(3);
  private final PyObject m_six = new PyInteger(6);


  private final Test m_test = new StubTest(1, "test");
  private final DispatcherStubFactory m_dispatcherStubFactory =
    new DispatcherStubFactory();
  private final Dispatcher m_dispatcher =
    m_dispatcherStubFactory.getDispatcher();

  public void testInitialise() throws Exception {
    final JythonScriptEngine scriptEngine = new JythonScriptEngine(
      m_scriptContext);

    AssertUtilities.assertContains(scriptEngine.getDescription(), "Jython");

    final File scriptFile = new File(getDirectory(), "script");

    try {
      scriptEngine.initialise(scriptFile, null);
      fail("Expected JythonScriptExecutionException");
    }
    catch (JythonScriptExecutionException e) {
      AssertUtilities.assertContains(e.getShortMessage(), "IOError");
    }

    scriptFile.createNewFile();

    try {
      scriptEngine.initialise(scriptFile, null);
      fail("Expected EngineException");
    }
    catch (EngineException e) {
      AssertUtilities.assertContains(e.getMessage(), "no callable");
    }

    final Writer w1 = new FileWriter(scriptFile);
    w1.write("TestRunner = 1");
    w1.close();

    try {
      scriptEngine.initialise(scriptFile, null);
      fail("Expected EngineException");
    }
    catch (EngineException e) {
      AssertUtilities.assertContains(e.getMessage(), "no callable");
    }

    final PrintWriter w2 = new PrintWriter(new FileWriter(scriptFile));
    w2.println("class TestRunner:pass");
    w2.close();

    scriptEngine.initialise(scriptFile, null);
    scriptEngine.shutdown();

    final File directory = new File(getDirectory(), "foo");
    directory.mkdirs();
    // new File(directory, "__init__.py").createNewFile();

    final PrintWriter w3 = new PrintWriter(new FileWriter(scriptFile, true));
    w3.println("import foo");
    w3.close();

    try {
      scriptEngine.initialise(scriptFile, null);
      fail("Expected JythonScriptExecutionException");
    }
    catch (JythonScriptExecutionException e) {
      AssertUtilities.assertContains(e.getShortMessage(), "ImportError");
    }

    // Jython caches modules, so we need to use a fresh interpreter to
    // avoid a repeated import error.
    new JythonScriptEngine(m_scriptContext).initialise(scriptFile,
      getDirectory());
  }

  public void testShutdown() throws Exception {
    final JythonScriptEngine scriptEngine = new JythonScriptEngine(
      m_scriptContext);

    final File scriptFile = new File(getDirectory(), "script");

    final PrintWriter w1 = new PrintWriter(new FileWriter(scriptFile));
    w1.println("class TestRunner:pass");
    w1.close();

    scriptEngine.initialise(scriptFile, null);
    scriptEngine.shutdown();

    s_lastCallbackObject = null;

    final PrintWriter w2 = new PrintWriter(new FileWriter(scriptFile));
    w2
        .println("from net.grinder.engine.process.jython import TestJythonScriptEngine");
    w2.println("import sys");

    w2.println("def f():");
    w2.println(" TestJythonScriptEngine.callback(TestJythonScriptEngine)");
    w2.println("sys.exitfunc = f");

    w2.println("class TestRunner:pass");
    w2.close();

    scriptEngine.initialise(scriptFile, null);
    scriptEngine.shutdown();

    assertSame(TestJythonScriptEngine.class, s_lastCallbackObject);

    s_lastCallbackObject = null;

    final PrintWriter w3 = new PrintWriter(new FileWriter(scriptFile));
    w3.println("import sys");

    w3.println("def f(): raise 'a problem'");
    w3.println("sys.exitfunc = f");

    w3.println("class TestRunner:pass");
    w3.close();

    scriptEngine.initialise(scriptFile, null);

    try {
      scriptEngine.shutdown();
      fail("Expected JythonScriptExecutionException");
    }
    catch (JythonScriptExecutionException e) {
      AssertUtilities.assertContains(e.getShortMessage(), "a problem");
    }
  }

  private static Object s_lastCallbackObject;

  public static void callback(Object o) {
    s_lastCallbackObject = o;
  }

  public void testScriptContextAndImplicitGrinderWarning() throws Exception {
    final RandomStubFactory loggerStubFactory = new RandomStubFactory(
      Logger.class);
    final Logger logger = (Logger) loggerStubFactory.getStub();

    m_scriptContextStubFactory.setResult("getLogger", logger);

    final JythonScriptEngine scriptEngine = new JythonScriptEngine(
      m_scriptContext);

    final File scriptFile = new File(getDirectory(), "script");

    final PrintWriter w1 = new PrintWriter(new FileWriter(scriptFile));
    w1.println("grinder.threadID");
    w1.println("grinder.runNumber");
    w1.println("class TestRunner:pass");
    w1.close();

    scriptEngine.initialise(scriptFile, null);

    // Only one warning.
    final CallData outputCall = loggerStubFactory.assertSuccess("output",
      String.class, Integer.class);
    AssertUtilities.assertContains((String) outputCall.getParameters()[0],
      "deprecated");
    loggerStubFactory.assertNoMoreCalls();

    m_scriptContextStubFactory.assertSuccess("getLogger");
    m_scriptContextStubFactory.assertSuccess("getThreadID");
    m_scriptContextStubFactory.assertSuccess("getRunNumber");
    m_scriptContextStubFactory.assertNoMoreCalls();
  }

  public void testWorkerRunnable() throws Exception {
    final JythonScriptEngine scriptEngine = new JythonScriptEngine(
      m_scriptContext);

    final File scriptFile = new File(getDirectory(), "script");

    final PrintWriter w1 = new PrintWriter(new FileWriter(scriptFile));
    w1.println("class TestRunner:pass");
    w1.close();

    scriptEngine.initialise(scriptFile, null);

    try {
      scriptEngine.createWorkerRunnable();
      fail("Expected EngineException");
    }
    catch (EngineException e) {
      AssertUtilities.assertContains(e.getMessage(), "is not callable");
    }

    final PrintWriter w2 = new PrintWriter(new FileWriter(scriptFile));
    w2.println("class TestRunner:");
    w2.println(" def __init__(self): raise 'a problem'");
    w2.close();

    scriptEngine.initialise(scriptFile, null);

    try {
      scriptEngine.createWorkerRunnable();
      fail("Expected JythonScriptExecutionException");
    }
    catch (JythonScriptExecutionException e) {
      AssertUtilities.assertContains(e.getShortMessage(), "a problem");
    }

    final PrintWriter w3 = new PrintWriter(new FileWriter(scriptFile));
    w3.println("class TestRunner:");
    w3.println(" def __call__(self): pass");
    w3.close();

    scriptEngine.initialise(scriptFile, null);
    final WorkerRunnable runnable3a = scriptEngine.createWorkerRunnable();
    final WorkerRunnable runnable3b = scriptEngine.createWorkerRunnable();
    assertNotSame(runnable3a, runnable3b);
    runnable3a.run();
    runnable3b.run();

    runnable3a.shutdown();

    final PrintWriter w4 = new PrintWriter(new FileWriter(scriptFile));
    w4.println("class TestRunner:");
    w4.println(" def __call__(self): raise 'a problem'");
    w4.close();

    scriptEngine.initialise(scriptFile, null);
    final WorkerRunnable runnable4 = scriptEngine.createWorkerRunnable();

    try {
      runnable4.run();
      fail("Expected JythonScriptExecutionException");
    }
    catch (JythonScriptExecutionException e) {
      AssertUtilities.assertContains(e.getShortMessage(), "a problem");
    }

    final PrintWriter w5 = new PrintWriter(new FileWriter(scriptFile));
    w5.println("class TestRunner:");
    w5.println(" def __call__(self): pass");
    w5.println(" def __del__(self): raise 'a problem'");
    w5.close();

    scriptEngine.initialise(scriptFile, null);
    final WorkerRunnable runnable5 = scriptEngine.createWorkerRunnable();

    try {
      runnable5.shutdown();
      fail("Expected JythonScriptExecutionException");
    }
    catch (JythonScriptExecutionException e) {
      AssertUtilities.assertContains(e.getShortMessage(), "a problem");
    }

    // Try it again, __del__ should now be disabled.
    runnable5.shutdown();
  }

  public void testCreateProxyWithPyFunction() throws Exception {
    final JythonScriptEngine scriptEngine = new JythonScriptEngine(
      m_scriptContext);

    m_interpreter.exec("def return1(): return 1");
    final PyObject pyFunction = m_interpreter.get("return1");
    final PyObject pyFunctionProxy = (PyObject) scriptEngine
        .createInstrumentedProxy(m_test, m_dispatcher, pyFunction);
    final PyObject result = pyFunctionProxy.invoke("__call__");
    assertEquals(new Integer(1), result.__tojava__(Integer.class));
    m_dispatcherStubFactory.assertSuccess("dispatch", Invokeable.class);
    m_dispatcherStubFactory.assertNoMoreCalls();
    assertSame(m_test, pyFunctionProxy.__getattr__("__test__").__tojava__(
      Test.class));

    m_interpreter.exec("def multiply(x, y): return x * y");
    final PyObject pyFunction2 = m_interpreter.get("multiply");
    final PyObject pyFunctionProxy2 = (PyObject) scriptEngine
        .createInstrumentedProxy(m_test, m_dispatcher, pyFunction2);
    final PyObject result2 =
      pyFunctionProxy2.invoke("__call__", m_two, m_three);
    assertEquals(m_six, result2);
    m_dispatcherStubFactory.assertSuccess("dispatch", Invokeable.class);
    m_dispatcherStubFactory.assertNoMoreCalls();

    final PyObject result3 =
      pyFunctionProxy2.invoke("__call__", new PyObject[] { m_two, m_three});
    assertEquals(m_six, result3);
    m_dispatcherStubFactory.assertSuccess("dispatch", Invokeable.class);
    m_dispatcherStubFactory.assertNoMoreCalls();

    // From Jython.
    m_interpreter.set("proxy", pyFunctionProxy);

    m_interpreter.exec("result4 = proxy()");
    final PyObject result4 = m_interpreter.get("result4");
    assertEquals(new Integer(1), result4.__tojava__(Integer.class));
    m_dispatcherStubFactory.assertSuccess("dispatch", Invokeable.class);
    m_dispatcherStubFactory.assertNoMoreCalls();
  }

  public void testCreateProxyWithPyInstance() throws Exception {
    final JythonScriptEngine scriptEngine = new JythonScriptEngine(
      m_scriptContext);

    // PyInstance.
    m_interpreter.exec(
      "class Foo:\n" +
      " def two(self): return 2\n" +
      " def identity(self, x): return x\n" +
      " def sum(self, x, y): return x + y\n" +
      " def sum3(self, x, y, z): return x + y + z\n" +
      "x=Foo()");
    final PyObject pyInstance = m_interpreter.get("x");
    final PyObject pyInstanceProxy = (PyObject) scriptEngine
        .createInstrumentedProxy(m_test, m_dispatcher, pyInstance);
    final PyObject result1 = pyInstanceProxy.invoke("two");
    assertEquals(new Integer(2), result1.__tojava__(Integer.class));
    m_dispatcherStubFactory.assertSuccess("dispatch", Invokeable.class);
    m_dispatcherStubFactory.assertNoMoreCalls();
    assertSame(m_test, pyInstanceProxy.__getattr__("__test__").__tojava__(
      Test.class));
    assertNull(pyInstanceProxy.__findattr__("__blah__"));

    final PyObject result2 = pyInstanceProxy.invoke("identity", m_one);
    assertSame(m_one, result2);
    m_dispatcherStubFactory.assertSuccess("dispatch", Invokeable.class);
    m_dispatcherStubFactory.assertNoMoreCalls();

    final PyObject result3 = pyInstanceProxy.invoke("sum", m_one, m_two);
    assertEquals(m_three, result3);
    m_dispatcherStubFactory.assertSuccess("dispatch", Invokeable.class);
    m_dispatcherStubFactory.assertNoMoreCalls();

    final PyObject result4 = pyInstanceProxy.invoke("sum3", new PyObject[] {
        m_one, m_two, m_three });
    assertEquals(m_six, result4);
    m_dispatcherStubFactory.assertSuccess("dispatch", Invokeable.class);
    m_dispatcherStubFactory.assertNoMoreCalls();

    final PyObject result5 = pyInstanceProxy.invoke("sum", new PyObject[] {
        m_one, m_two }, new String[] { "x", "y" });
    assertEquals(m_three, result5);
    m_dispatcherStubFactory.assertSuccess("dispatch", Invokeable.class);
    m_dispatcherStubFactory.assertNoMoreCalls();

    // From Jython.
    m_interpreter.set("proxy", pyInstanceProxy);

    m_interpreter.exec("result6 = proxy.sum(2, 4)");
    final PyObject result6 = m_interpreter.get("result6");
    assertEquals(m_six, result6);
    m_dispatcherStubFactory.assertSuccess("dispatch", Invokeable.class);
    m_dispatcherStubFactory.assertNoMoreCalls();
  }

  public void testCreateProxyWithPyMethod() throws Exception {
    final JythonScriptEngine scriptEngine =
      new JythonScriptEngine(m_scriptContext);

    m_interpreter.exec(
      "class Foo:\n" +
      " def two(self): return 2\n" +
      " def identity(self, x): return x\n" +
      " def sum(self, x, y): return x + y\n" +
      " def sum3(self, x, y, z): return x + y + z\n" +
      "x=Foo()");
    final PyObject pyInstance = m_interpreter.get("x");
    m_interpreter.exec("y=Foo.two");
    final PyObject pyMethod = m_interpreter.get("y");
    final PyObject pyMethodProxy = (PyObject) scriptEngine
        .createInstrumentedProxy(m_test, m_dispatcher, pyMethod);
    final PyObject result = pyMethodProxy.invoke("__call__", pyInstance);
    assertEquals(new Integer(2), result.__tojava__(Integer.class));
    m_dispatcherStubFactory.assertSuccess("dispatch", Invokeable.class);
    m_dispatcherStubFactory.assertNoMoreCalls();
    assertSame(m_test, pyMethodProxy.__getattr__("__test__").__tojava__(
      Test.class));
    assertNull(pyMethodProxy.__findattr__("__blah__"));

    // From Jython.
    m_interpreter.set("proxy", pyMethodProxy);

    m_interpreter.exec("result2 = proxy(x)");
    final PyObject result2 = m_interpreter.get("result2");
    assertEquals(new Integer(2), result2.__tojava__(Integer.class));
    m_dispatcherStubFactory.assertSuccess("dispatch", Invokeable.class);
    m_dispatcherStubFactory.assertNoMoreCalls();
  }

  public void testCreateProxyWithPyJavaInstance() throws Exception {
    final JythonScriptEngine scriptEngine =
      new JythonScriptEngine(m_scriptContext);

    m_interpreter.exec("from java.util import Random\nx=Random()");
    final PyObject pyJava = m_interpreter.get("x");
    final PyObject pyJavaProxy = (PyObject) scriptEngine
        .createInstrumentedProxy(m_test, m_dispatcher, pyJava);
    final PyObject result = pyJavaProxy.invoke("getClass");
    assertEquals(Random.class, result.__tojava__(Class.class));
    m_dispatcherStubFactory.assertSuccess("dispatch", Invokeable.class);
    m_dispatcherStubFactory.assertNoMoreCalls();
    assertSame(m_test,
               pyJavaProxy.__getattr__("__test__").__tojava__(Test.class));
    assertNull(pyJavaProxy.__findattr__("__blah__"));

    // From Jython.
    m_interpreter.set("proxy", pyJavaProxy);

    m_interpreter.exec("result2 = proxy.getClass()");
    final PyObject result2 = m_interpreter.get("result2");
    assertEquals(Random.class, result2.__tojava__(Class.class));
    m_dispatcherStubFactory.assertSuccess("dispatch", Invokeable.class);
    m_dispatcherStubFactory.assertNoMoreCalls();
  }

  public void testCreateProxyWithPyReflectedFunction() throws Exception {

    final JythonScriptEngine scriptEngine =
      new JythonScriptEngine(m_scriptContext);

    m_interpreter.exec("from java.util import Random\nx=Random()");
    final PyObject pyJava = m_interpreter.get("x");
    m_interpreter.exec("y=Random.nextInt");
    final PyObject pyJavaMethod = m_interpreter.get("y");
    final PyObject pyJavaMethodProxy = (PyObject) scriptEngine
        .createInstrumentedProxy(m_test, m_dispatcher, pyJavaMethod);
    final PyObject result = pyJavaMethodProxy.__call__(pyJava);
    assertTrue(result.__tojava__(Object.class) instanceof Integer);
    m_dispatcherStubFactory.assertSuccess("dispatch", Invokeable.class);
    m_dispatcherStubFactory.assertNoMoreCalls();
    assertSame(m_test, pyJavaMethodProxy.__getattr__("__test__").__tojava__(
      Test.class));
    assertNull(pyJavaMethodProxy.__findattr__("__blah__"));

    // From Jython.
    m_interpreter.set("proxy", pyJavaMethodProxy);

    m_interpreter.exec("result2 = proxy(x)");
    final PyObject result2 = m_interpreter.get("result2");
    assertTrue(result2.__tojava__(Object.class) instanceof Integer);
    m_dispatcherStubFactory.assertSuccess("dispatch", Invokeable.class);
    m_dispatcherStubFactory.assertNoMoreCalls();
  }

  public void testCreateProxyWithPyProxy() throws Exception {

    final JythonScriptEngine scriptEngine =
      new JythonScriptEngine(m_scriptContext);

    // PyProxy's come paired with PyInstances - need to call
    // __tojava__ to get the PyProxy.
    m_interpreter.exec("from java.util import Random");
    m_interpreter.exec(
      "class PyRandom(Random):\n" +
      " def one(self): return 1\n" +
      "x=PyRandom()");
    final Object pyProxy = m_interpreter.get("x").__tojava__(Object.class);
    final PyObject pyProxyProxy = (PyObject) scriptEngine
        .createInstrumentedProxy(m_test, m_dispatcher, pyProxy);
    final PyObject result = pyProxyProxy.invoke("one");
    assertEquals(new Integer(1), result.__tojava__(Integer.class));
    m_dispatcherStubFactory.assertSuccess("dispatch", Invokeable.class);
    m_dispatcherStubFactory.assertNoMoreCalls();
    assertSame(m_test, pyProxyProxy.__getattr__("__test__")
        .__tojava__(Test.class));

    // From Jython.
    m_interpreter.set("proxy", pyProxyProxy);

    m_interpreter.exec("result2 = proxy.one()");
    final PyObject result2 = m_interpreter.get("result2");
    assertEquals(m_one, result2);
    m_dispatcherStubFactory.assertSuccess("dispatch", Invokeable.class);
    m_dispatcherStubFactory.assertNoMoreCalls();

    m_interpreter.exec("result3 = proxy.nextInt()");
    final PyObject result3 = m_interpreter.get("result3");
    assertNotNull(result3);
    m_dispatcherStubFactory.assertSuccess("dispatch", Invokeable.class);
    m_dispatcherStubFactory.assertNoMoreCalls();
  }

  public void testCreateProxyWithJavaInstance() throws Exception {

    final JythonScriptEngine scriptEngine = new JythonScriptEngine(
      m_scriptContext);

    final Object java = new MyClass();
    final PyObject javaProxy = (PyObject) scriptEngine.createInstrumentedProxy(
      m_test, m_dispatcher, java);
    final PyObject result = javaProxy.invoke("addOne", Py.java2py(new Integer(
      10)));
    assertEquals(new Integer(11), result.__tojava__(Integer.class));
    m_dispatcherStubFactory.assertSuccess("dispatch", Invokeable.class);
    m_dispatcherStubFactory.assertNoMoreCalls();
    assertSame(m_test, javaProxy.__getattr__("__test__").__tojava__(Test.class));

    final PyObject result1 = javaProxy.invoke("getClass");
    assertEquals(MyClass.class, result1.__tojava__(Class.class));
    m_dispatcherStubFactory.assertSuccess("dispatch", Invokeable.class);
    m_dispatcherStubFactory.assertNoMoreCalls();

    final PyObject result2 = javaProxy.invoke("sum", m_one, m_two);
    assertEquals(m_three, result2);
    m_dispatcherStubFactory.assertSuccess("dispatch", Invokeable.class);
    m_dispatcherStubFactory.assertNoMoreCalls();

    final PyObject result3 = javaProxy.invoke("sum3", new PyObject[] { m_one,
        m_two, m_three });
    assertEquals(m_six, result3);
    m_dispatcherStubFactory.assertSuccess("dispatch", Invokeable.class);
    m_dispatcherStubFactory.assertNoMoreCalls();

    final PyObject result4 = javaProxy.invoke("sum", new PyObject[] { m_one,
        m_two }, Py.NoKeywords);
    assertEquals(m_three, result4);
    m_dispatcherStubFactory.assertSuccess("dispatch", Invokeable.class);
    m_dispatcherStubFactory.assertNoMoreCalls();

    // From Jython.
    m_interpreter.set("proxy", javaProxy);

    m_interpreter.exec("result5 = proxy.sum3(0, -29, 30)");
    final PyObject result5 = m_interpreter.get("result5");
    assertEquals(m_one, result5);
    m_dispatcherStubFactory.assertSuccess("dispatch", Invokeable.class);
    m_dispatcherStubFactory.assertNoMoreCalls();

    m_interpreter.exec("result6 = proxy.sum(1, 1)");
    final PyObject result6 = m_interpreter.get("result6");
    assertEquals(m_two, result6);
    m_dispatcherStubFactory.assertSuccess("dispatch", Invokeable.class);
    m_dispatcherStubFactory.assertNoMoreCalls();

    m_interpreter.exec("result7 = proxy.getClass()");
    final PyObject result7 = m_interpreter.get("result7");
    assertEquals(MyClass.class, result7.__tojava__(Class.class));
    m_dispatcherStubFactory.assertSuccess("dispatch", Invokeable.class);
    m_dispatcherStubFactory.assertNoMoreCalls();
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
    assertNotWrappable(new Short((short) 56));
    assertNotWrappable(new Byte((byte) 56));

    final PythonInterpreter interpreter = new PythonInterpreter(null,
      new PySystemState());

    // Can't wrap PyInteger.
    interpreter.exec("x=1");
    assertNotWrappable(interpreter.get("x"));

    // Can't wrap PyClass.
    interpreter.exec("class Foo: pass");
    assertNotWrappable(interpreter.get("Foo"));
  }

  public void testPyDispatcherErrorHandling() throws Exception {
    final JythonScriptEngine scriptEngine = new JythonScriptEngine(
      m_scriptContext);

    final DispatcherStubFactory dispatcherStubFactory = new DispatcherStubFactory();
    final Dispatcher dispatcher = dispatcherStubFactory.getDispatcher();

    m_interpreter.exec("def blah(): raise 'a problem'");
    final PyObject pyFunction = m_interpreter.get("blah");
    final PyObject pyFunctionProxy = (PyObject) scriptEngine
        .createInstrumentedProxy(m_test, dispatcher, pyFunction);
    try {
      pyFunctionProxy.invoke("__call__");
      fail("Expected PyException");
    }
    catch (PyException e) {
      AssertUtilities.assertContains(e.toString(), "a problem");
    }

    dispatcherStubFactory.assertFailed("dispatch",
      new Class[] { Invokeable.class }, PyException.class);

    dispatcherStubFactory.assertNoMoreCalls();

    final UncheckedGrinderException e = new UncheckedInterruptedException(null);
    dispatcherStubFactory.setThrows("dispatch", e);

    try {
      pyFunctionProxy.invoke("__call__");
      fail("Expected UncheckedGrinderException");
    }
    catch (UncheckedGrinderException e2) {
      assertSame(e, e2);
    }
  }

  private void assertNotWrappable(Object o) throws Exception {
    final JythonScriptEngine scriptEngine = new JythonScriptEngine(
      m_scriptContext);

    try {
      scriptEngine.createInstrumentedProxy(null, null, o);
      fail("Expected NotWrappableTypeException");
    }
    catch (NotWrappableTypeException e) {
    }
  }

  public static final class DispatcherStubFactory extends RandomStubFactory {
    public DispatcherStubFactory() {
      super(Dispatcher.class);
    }

    Dispatcher getDispatcher() {
      return (Dispatcher) getStub();
    }

    public Object override_dispatch(Object proxy, Invokeable invokeable) {
      return invokeable.call();
    }
  }

  public static class MyClass {
    public int addOne(int i) {
      return i + 1;
    }

    public int sum(int x, int y) {
      return x + y;
    }

    public int sum3(int x, int y, int z) {
      return x + y + z;
    }
  }
}
