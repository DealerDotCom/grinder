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
    (ScriptContext)m_scriptContextStubFactory.getStub();

  public void testInitialise() throws Exception {
    final JythonScriptEngine scriptEngine =
      new JythonScriptEngine(m_scriptContext);

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
    //new File(directory, "__init__.py").createNewFile();

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
    new JythonScriptEngine(m_scriptContext).initialise(
      scriptFile, getDirectory());
  }

  public void testShutdown() throws Exception {
    final JythonScriptEngine scriptEngine =
      new JythonScriptEngine(m_scriptContext);

    final File scriptFile = new File(getDirectory(), "script");

    final PrintWriter w1 = new PrintWriter(new FileWriter(scriptFile));
    w1.println("class TestRunner:pass");
    w1.close();

    scriptEngine.initialise(scriptFile, null);
    scriptEngine.shutdown();

    s_lastCallbackObject = null;

    final PrintWriter w2 = new PrintWriter(new FileWriter(scriptFile));
    w2.println("from net.grinder.engine.process.jython import TestJythonScriptEngine");
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
    final RandomStubFactory loggerStubFactory =
      new RandomStubFactory(Logger.class);
    final Logger logger = (Logger)loggerStubFactory.getStub();

    m_scriptContextStubFactory.setResult("getLogger", logger);

    final JythonScriptEngine scriptEngine =
      new JythonScriptEngine(m_scriptContext);

    final File scriptFile = new File(getDirectory(), "script");

    final PrintWriter w1 = new PrintWriter(new FileWriter(scriptFile));
    w1.println("grinder.threadID");
    w1.println("grinder.runNumber");
    w1.println("class TestRunner:pass");
    w1.close();

    scriptEngine.initialise(scriptFile, null);

    // Only one warning.
    final CallData outputCall =
      loggerStubFactory.assertSuccess("output", String.class, Integer.class);
    AssertUtilities.assertContains(
      (String)outputCall.getParameters()[0], "deprecated");
    loggerStubFactory.assertNoMoreCalls();

    m_scriptContextStubFactory.assertSuccess("getLogger");
    m_scriptContextStubFactory.assertSuccess("getThreadID");
    m_scriptContextStubFactory.assertSuccess("getRunNumber");
    m_scriptContextStubFactory.assertNoMoreCalls();
  }

  public void testWorkerRunnable() throws Exception {
    final JythonScriptEngine scriptEngine =
      new JythonScriptEngine(m_scriptContext);

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

  public void testCreateProxy() throws Exception {
    System.setProperty("python.verbose", "warning");

    final JythonScriptEngine scriptEngine =
      new JythonScriptEngine(m_scriptContext);

    final PythonInterpreter interpreter =
      new PythonInterpreter(null, new PySystemState());

    final Test test = new StubTest(1, "test");

    final DispatcherStubFactory dispatcherStubFactory =
      new DispatcherStubFactory();
    final Dispatcher dispatcher = dispatcherStubFactory.getDispatcher();

    final PyObject one = new PyInteger(1);
    final PyObject two = new PyInteger(2);
    final PyObject three = new PyInteger(3);
    final PyObject six = new PyInteger(6);

    // PyFunctions.
    interpreter.exec("def return1(): return 1");
    final PyObject pyFunction = interpreter.get("return1");
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
    interpreter.exec(
      "class Foo:\n" +
      " def two(self): return 2\n" +
      " def identity(self, x): return x\n" +
      " def sum(self, x, y): return x + y\n" +
      " def sum3(self, x, y, z): return x + y + z\n" +
      "x=Foo()");
    final PyObject pyInstance = interpreter.get("x");
    final PyObject pyInstanceProxy =
      (PyObject)scriptEngine.createInstrumentedProxy(test,
                                                     dispatcher,
                                                     pyInstance);
    final PyObject result2 = pyInstanceProxy.invoke("two");
    assertEquals(new Integer(2), result2.__tojava__(Integer.class));
    dispatcherStubFactory.assertSuccess("dispatch", Invokeable.class);
    dispatcherStubFactory.assertNoMoreCalls();
    assertSame(test,
      pyInstanceProxy.__getattr__("__test__").__tojava__(Test.class));
    assertNull(pyInstanceProxy.__findattr__("__blah__"));

    final PyObject result2b = pyInstanceProxy.invoke("identity", one);
    assertSame(one, result2b);
    dispatcherStubFactory.assertSuccess("dispatch", Invokeable.class);
    dispatcherStubFactory.assertNoMoreCalls();

    final PyObject result2c = pyInstanceProxy.invoke("sum", one, two);
    assertEquals(three, result2c);
    dispatcherStubFactory.assertSuccess("dispatch", Invokeable.class);
    dispatcherStubFactory.assertNoMoreCalls();

    final PyObject result2d =
      pyInstanceProxy.invoke("sum3", new PyObject[] { one, two, three });
    assertEquals(six, result2d);
    dispatcherStubFactory.assertSuccess("dispatch", Invokeable.class);
    dispatcherStubFactory.assertNoMoreCalls();

    final PyObject result2e =
      pyInstanceProxy.invoke("sum",
                             new PyObject[] {one, two},
                             new String [] { "x", "y" });
    assertEquals(three, result2e);
    dispatcherStubFactory.assertSuccess("dispatch", Invokeable.class);
    dispatcherStubFactory.assertNoMoreCalls();

    // PyMethod.
    interpreter.exec("x=Foo.two");
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
    assertNull(pyMethodProxy.__findattr__("__blah__"));

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
    assertNull(pyJavaProxy.__findattr__("__blah__"));

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
    assertNull(pyJavaMethodProxy.__findattr__("__blah__"));

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

    final PyObject result8c = javaProxy.invoke("sum", one, two);
    assertEquals(three, result8c);
    dispatcherStubFactory.assertSuccess("dispatch", Invokeable.class);
    dispatcherStubFactory.assertNoMoreCalls();

    final PyObject result8d =
      javaProxy.invoke("sum3", new PyObject[] { one, two, three });
    assertEquals(six, result8d);
    dispatcherStubFactory.assertSuccess("dispatch", Invokeable.class);
    dispatcherStubFactory.assertNoMoreCalls();

    final PyObject result8e = javaProxy.invoke("sum",
                                               new PyObject[] {one, two},
                                               Py.NoKeywords);
    assertEquals(three, result8e);
    dispatcherStubFactory.assertSuccess("dispatch", Invokeable.class);
    dispatcherStubFactory.assertNoMoreCalls();
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

    final PythonInterpreter interpreter =
      new PythonInterpreter(null, new PySystemState());

    // Can't wrap PyInteger.
    interpreter.exec("x=1");
    assertNotWrappable(interpreter.get("x"));

    // Can't wrap PyClass.
    interpreter.exec("class Foo: pass");
    assertNotWrappable(interpreter.get("Foo"));
  }

  public void testPyDispatcherErrorHandling() throws Exception {
    final JythonScriptEngine scriptEngine =
      new JythonScriptEngine(m_scriptContext);

    final PythonInterpreter interpreter =
      new PythonInterpreter(null, new PySystemState());

    final Test test = new StubTest(1, "test");

    final DispatcherStubFactory dispatcherStubFactory =
      new DispatcherStubFactory();
    final Dispatcher dispatcher = dispatcherStubFactory.getDispatcher();

    interpreter.exec("def blah(): raise 'a problem'");
    final PyObject pyFunction = interpreter.get("blah");
    final PyObject pyFunctionProxy =
      (PyObject)scriptEngine.createInstrumentedProxy(test,
                                                     dispatcher,
                                                     pyFunction);
    try {
      pyFunctionProxy.invoke("__call__");
      fail("Expected PyException");
    }
    catch (PyException e) {
      AssertUtilities.assertContains(e.toString(), "a problem");
    }

    dispatcherStubFactory.assertFailed("dispatch",
                                       new Class[] { Invokeable.class },
                                       PyException.class);

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
    final JythonScriptEngine scriptEngine =
      new JythonScriptEngine(m_scriptContext);

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
      return (Dispatcher)getStub();
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
