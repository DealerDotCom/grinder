// Copyright (C) 2000, 2001, 2002, 2003, 2004 Philip Aston
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

import junit.framework.TestCase;

import java.util.Random;

import org.python.core.Py;
import org.python.core.PyException;
import org.python.core.PyInstance;
import org.python.core.PyJavaInstance;
import org.python.core.PyMethod;
import org.python.core.PyObject;
import org.python.core.PyString;
import org.python.core.PySystemState;
import org.python.util.PythonInterpreter;

import net.grinder.common.Test;
import net.grinder.common.StubTest;
import net.grinder.engine.common.EngineException;
import net.grinder.script.NotWrappableTypeException;
import net.grinder.testutility.RandomStubFactory;


/**
 * Unit test case for <code>TestTestData</code>.
 *
 * @author Philip Aston
 * @version $Revision$
 */
public class TestTestData extends TestCase {
  public TestTestData(String name) {
    super(name);
  }

  public void testTestData() throws Exception {
    final ThreadContextLocator threadContextLocator =
      new StubThreadContextLocator();
	
    final Test test1 = new StubTest(99, "Some stuff");

    final TestData testData1 = new TestData(threadContextLocator, test1);
    assertEquals(test1, testData1.getTest());
    assertNotNull(testData1.getStatistics());

    final Test test2 = new StubTest(-33, "");

    final TestData testData2 = new TestData(threadContextLocator, test2);
    assertEquals(test2, testData2.getTest());
    assertNotNull(testData2.getStatistics());
  }

  public void testDispatch() throws Exception {

    final ThreadContextLocator threadContextLocator =
      new StubThreadContextLocator();
	
    final Test test = new StubTest(2, "A description");
    final TestData testData = new TestData(threadContextLocator, test);

    final TestData.Invokeable invokeable =
      (TestData.Invokeable)
      (new RandomStubFactory(TestData.Invokeable.class)).getStub();

    try {
      testData.dispatch(invokeable);
      fail("Expected EngineException");
    }
    catch (EngineException e) {
    }

    final RandomStubFactory threadContextStubFactory =
      new RandomStubFactory(ThreadContext.class);

    threadContextLocator.set(
      (ThreadContext)threadContextStubFactory.getStub());

    final Object o = testData.dispatch(invokeable);

    threadContextStubFactory.assertSuccess(
      "invokeTest", new Object[] { testData, invokeable}, o );

    threadContextStubFactory.assertNoMoreCalls();
  }

  /**
   * Creates dynamic ThreadContext stubs which implement invokeTest by
   * delegating directly to the invokeable. Must be public so
   * override_ methods can be invoked.
   */
  public static class ThreadContextStubFactory extends RandomStubFactory {
    private final TestData m_expectedTestData;

    public ThreadContextStubFactory(TestData expectedTestData) {
      super(ThreadContext.class);
      m_expectedTestData = expectedTestData;
    }

    public Object override_invokeTest(Object proxy,
                                      TestData testData,
                                      TestData.Invokeable invokeable) {
      assertSame(m_expectedTestData, testData);
      return invokeable.call();
    }

    public ThreadContext getThreadContext() {
      return (ThreadContext)getStub();
    }
  }

  public void testCreateProxy() throws Exception {
    PySystemState.initialize();

    final PySystemState systemState = new PySystemState();
    final PythonInterpreter interpreter =
      new PythonInterpreter(null, systemState);

    final ThreadContextLocator threadContextLocator =
      new StubThreadContextLocator();

    final Test test = new StubTest(0, "");
    final TestData testData = new TestData(threadContextLocator, test);

    final ThreadContextStubFactory threadContextStubFactory =
      new ThreadContextStubFactory(testData);

    threadContextLocator.set(threadContextStubFactory.getThreadContext());

    // PyFunction.
    interpreter.exec("def blah(): return 1");
    final PyObject pyFunction = interpreter.get("blah");
    final PyObject pyFunctionProxy =
      (PyObject)testData.createProxy(pyFunction);
    final PyObject result1 = pyFunctionProxy.invoke("__call__");
    assertEquals(new Integer(1), result1.__tojava__(Integer.class));

    // PyInstance.
    interpreter.exec("class Foo:\n def bah(self): return 2\nx=Foo()");
    final PyObject pyInstance = interpreter.get("x");
    final PyObject pyInstanceProxy =
      (PyObject)testData.createProxy(pyInstance);
    final PyObject result2 = pyInstanceProxy.invoke("bah");
    assertEquals(new Integer(2), result2.__tojava__(Integer.class));

    // PyMethod.
    interpreter.exec("x=Foo.bah");
    final PyObject pyMethod = interpreter.get("x");
    final PyObject pyMethodProxy = (PyObject)testData.createProxy(pyMethod);
    final PyObject result3 = pyMethod.invoke("__call__", pyInstanceProxy);
    assertEquals(new Integer(2), result3.__tojava__(Integer.class));

    // PyJavaInstance.
    interpreter.exec("from java.util import Random\nx=Random()");
    final PyObject pyJava = interpreter.get("x");
    final PyObject pyJavaProxy = (PyObject)testData.createProxy(pyJava);
    final PyObject result4 = pyJavaProxy.invoke("getClass");
    assertEquals(Random.class, result4.__tojava__(Class.class));

    // PyProxy. PyProxy's come paired with PyInstances - need to call
    // __tojava__ to get the PyProxy.
    interpreter.exec("class PyRandom(Random):\n def one(self): return 1\n" +
                     "x=PyRandom()");
    final Object pyProxy = interpreter.get("x").__tojava__(Object.class);
    final PyObject pyProxyProxy = (PyObject)testData.createProxy(pyProxy);
    final PyObject result5 = pyProxyProxy.invoke("one");
    assertEquals(new Integer(1), result5.__tojava__(Integer.class));

    // Java object.
    final Object java = new MyClass();
    final PyObject javaProxy = (PyObject)testData.createProxy(java);
    final PyObject result6 = javaProxy.invoke("addOne",
                                              Py.java2py(new Integer(10)));
    assertEquals(new Integer(11), result6.__tojava__(Integer.class));
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

  private void assertNotWrappable(Object o) throws Exception {

    final Test test = new StubTest(0, "");

    final TestData testData =
      new TestData(new StubThreadContextLocator(), test);

    try {
      testData.createProxy(o);
      fail("Expected NotWrappableTypeException");
    }
    catch (NotWrappableTypeException e) {
    }
  }
}
