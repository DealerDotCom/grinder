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

package net.grinder.util.weave.j2se6;

import static net.grinder.testutility.AssertUtilities.assertArraysEqual;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;
import net.grinder.testutility.CallData;
import net.grinder.testutility.CallRecorder;
import net.grinder.testutility.RandomStubFactory;
import net.grinder.util.weave.WeavingException;
import net.grinder.util.weave.agent.ExposeInstrumentation;
import net.grinder.util.weave.j2se6.DCRWeaver.ClassFileTransformerFactory;
import net.grinder.util.weave.j2se6.DCRWeaver.PointCutRegistry;


/**
 * Unit tests for {@link ASMTransformerFactory}.
 *
 * TODO constructors
 * TODO static methods
 *
 * @author Philip Aston
 * @version $Revision:$
 */
public class TestASMTransformerFactory extends TestCase {

  private final PointCutRegistryStubFactory m_pointCutRegistryStubFactory =
    new PointCutRegistryStubFactory();
  private final PointCutRegistry m_pointCutRegistry =
    m_pointCutRegistryStubFactory.getStub();

  private static final CallRecorder s_callRecorder = new CallRecorder();

  public void testFactory() throws Exception {
    final ClassFileTransformerFactory transformerFactory =
      new ASMTransformerFactory(MyAdvice.class);

    assertNotNull(transformerFactory.create(m_pointCutRegistry));
  }

  public void testFactoryWithBadAdvice() throws Exception {
    try {
      new ASMTransformerFactory(BadAdvice1.class);
      fail("Expected WeavingException");
    }
    catch (WeavingException e) {
      assertTrue(e.getCause() instanceof NoSuchMethodException);
    }

    try {
      new ASMTransformerFactory(BadAdvice2.class);
      fail("Expected WeavingException");
    }
    catch (WeavingException e) {
      assertTrue(e.getCause() instanceof NoSuchMethodException);
    }

    try {
      new ASMTransformerFactory(BadAdvice3.class);
      fail("Expected WeavingException");
    }
    catch (WeavingException e) {
    }

    try {
      new ASMTransformerFactory(BadAdvice4.class);
      fail("Expected WeavingException");
    }
    catch (WeavingException e) {
    }
  }

  private static final Instrumentation getInstrumentation() {
    final Instrumentation instrumentation =
      ExposeInstrumentation.getInstrumentation();

    assertNotNull(
      "Instrumentation is not available, " +
      "please add -javaagent:grinder-agent.jar to the command line",
      instrumentation);

    return instrumentation;
  }

  public void testWithAgent() throws Exception {
    final Instrumentation instrumentation = getInstrumentation();
    assertTrue(instrumentation.isRetransformClassesSupported());

    final ClassFileTransformerFactory transformerFactory =
      new ASMTransformerFactory(MyAdvice.class);

    m_pointCutRegistryStubFactory.addMethod(A.class, "m1", "loc1");
    m_pointCutRegistryStubFactory.addMethod(A.class, "m2", "loc2");

    final ClassFileTransformer transformer =
      transformerFactory.create(m_pointCutRegistry);

    final A anotherA = new A();
    anotherA.m1();
    s_callRecorder.assertNoMoreCalls();

    instrumentation.addTransformer(transformer, true);
    instrumentation.retransformClasses(new Class[] { A.class, A2.class });

    final A a = new A();
    assertEquals(1, a.m1());

    s_callRecorder.assertSuccess("enter", a, "loc1");
    s_callRecorder.assertSuccess("exit", a, "loc1", true);
    s_callRecorder.assertNoMoreCalls();

    anotherA.m1();
    s_callRecorder.assertSuccess("enter", anotherA, "loc1");
    s_callRecorder.assertSuccess("exit", anotherA, "loc1", true);
    s_callRecorder.assertNoMoreCalls();

    try {
      a.m2();
      fail("Expected RuntimeException");
    }
    catch (RuntimeException e) {
    }

    s_callRecorder.assertSuccess("enter", a, "loc2");
    s_callRecorder.assertSuccess("exit", a, "loc2", false);
    s_callRecorder.assertNoMoreCalls();

    m_pointCutRegistryStubFactory.addMethod(A.class, "m1", "loc4");

    instrumentation.retransformClasses(new Class[] { A.class, A2.class });

    a.m1();
    // We only support one advice per method.
    s_callRecorder.assertSuccess("enter", a, "loc4");
    s_callRecorder.assertSuccess("exit", a, "loc4", true);
    s_callRecorder.assertNoMoreCalls();

    instrumentation.removeTransformer(transformer);
  }

  public void testSerializationNotBroken() throws Exception {
    final Instrumentation instrumentation = getInstrumentation();

    final ClassFileTransformerFactory transformerFactory =
      new ASMTransformerFactory(MyAdvice.class);

    m_pointCutRegistryStubFactory.addMethod(SerializableA.class, "m1", "loc1");

    final ClassFileTransformer transformer =
      transformerFactory.create(m_pointCutRegistry);

    final SerializableA a = new SerializableA();

    assertEquals(1, a.m1());
    s_callRecorder.assertNoMoreCalls();

    final byte[] originalBytes = serialize(a);

    instrumentation.addTransformer(transformer, true);
    instrumentation.retransformClasses(new Class[] { SerializableA.class, });

    assertEquals(1, a.m1());

    s_callRecorder.assertSuccess("enter", a, "loc1");
    s_callRecorder.assertSuccess("exit", a, "loc1", true);
    s_callRecorder.assertNoMoreCalls();

    final byte[] bytes = serialize(a);

    assertArraysEqual(originalBytes, bytes);

    instrumentation.removeTransformer(transformer);
  }

  public void testConstructors() throws Exception {
    final Instrumentation instrumentation = getInstrumentation();

    final ClassFileTransformerFactory transformerFactory =
      new ASMTransformerFactory(MyAdvice.class);

    m_pointCutRegistryStubFactory.addMethod(A2.class, "<init>", "loc1");

    final ClassFileTransformer transformer =
      transformerFactory.create(m_pointCutRegistry);

    new A2(1);
    s_callRecorder.assertNoMoreCalls();

    instrumentation.addTransformer(transformer, true);
    instrumentation.retransformClasses(new Class[] { A2.class, });

    final A2 a = new A2(1);

    s_callRecorder.assertSuccess("enter", a, "loc1");
    s_callRecorder.assertSuccess("exit", a, "loc1", true);
    s_callRecorder.assertNoMoreCalls();

    instrumentation.removeTransformer(transformer);
  }

  private static final byte[] serialize(final Object a) throws IOException {
    final ByteArrayOutputStream byteOutputStream =
      new ByteArrayOutputStream();

    final ObjectOutputStream objectOutputStream =
      new ObjectOutputStream(byteOutputStream);

    objectOutputStream.writeObject(a);
    objectOutputStream.close();

    return byteOutputStream.toByteArray();
  }

  public static final class A {
    public int m1() {
      return 1;
    }

    private void m2() {
      throw new RuntimeException("Test");
    }

    public static int m3() {
      return 2;
    }
  }

  public static final class A2 {
    public A2(int x) {
    }

    public int m1() {
      return 1;
    }

    public void m2() {
    }
  }

  public static final class SerializableA implements Serializable {
    public int m1() {
      return 1;
    }
  }

  public static final class MyAdvice {
    private static final Method ENTER_METHOD;
    private static final Method EXIT_METHOD;

    static {
      try {
        ENTER_METHOD = MyAdvice.class.getMethod(
          "enter", Object.class, String.class);
        EXIT_METHOD = MyAdvice.class.getMethod(
          "exit", Object.class, String.class, Boolean.TYPE);
      }
      catch (Exception e) {
        throw new ExceptionInInitializerError(e);
      }
    }

    public static void enter(Object reference, String location) {
      s_callRecorder.record(new CallData(ENTER_METHOD,
                                         null,
                                         reference,
                                         location));
    }

    public static void exit(Object reference,
                            String location,
                            boolean success) {

      s_callRecorder.record(new CallData(EXIT_METHOD,
                                         null,
                                         reference,
                                         location,
                                         success));
    }
  }

  public static final class BadAdvice1 {
  }

  public static final class BadAdvice2 {
    public static void enter(Object reference, String location) { }

    public static void exit(Object reference, String location) { }
  }

  public static final class BadAdvice3 {
    public void enter(Object reference, String location) { }

    public static void exit(Object reference,
                            String location,
                            boolean success) { }
  }

  public static final class BadAdvice4 {
    public static void enter(Object reference, String location) { }

    public void exit(Object reference, String location, boolean success) { }
  }

  public static final class PointCutRegistryStubFactory
    extends RandomStubFactory<PointCutRegistry> {

    private Map<String, Map<String, String>> m_data =
      new HashMap<String, Map<String, String>>();

    protected PointCutRegistryStubFactory() {
      super(PointCutRegistry.class);
    }

    public Map<String, String>
      override_getPointCutsForClass(Object stub, String className) {
      return m_data.get(className);
    }

    public void addMethod(Class<?> theClass,
                          String methodName,
                          String location) {

      final String internalClassName = theClass.getName().replace('.', '/');

      final Map<String, String> forClass;

      final Map<String, String> existing = m_data.get(internalClassName);

      if (existing != null) {
        forClass = existing;
      }
      else {
        forClass = new HashMap<String, String>();
        m_data.put(internalClassName, forClass);
      }

      forClass.put(methodName, location);
    }
  }
}
