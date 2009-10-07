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

import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.lang.reflect.Method;
import java.util.Map;

import junit.framework.TestCase;
import net.grinder.testutility.CallData;
import net.grinder.testutility.RandomStubFactory;
import net.grinder.util.weave.Weaver;
import net.grinder.util.weave.WeavingException;
import net.grinder.util.weave.Weaver.Location;
import net.grinder.util.weave.agent.ExposeInstrumentation;
import net.grinder.util.weave.j2se6.DCRWeaver.ClassFileTransformerFactory;
import net.grinder.util.weave.j2se6.DCRWeaver.PointCutRegistry;


/**
 * Unit test for {@link DCRWeaver}.
 * TestDCRWeaver.
 *
 * @author Philip Aston
 * @version $Revision:$
 */
public class TestDCRWeaver extends TestCase {
  private Instrumentation m_originalInstrumentation;

  final RandomStubFactory<ClassFileTransformerFactory>
    m_classFileTransformerFactoryStubFactory =
      RandomStubFactory.create(ClassFileTransformerFactory.class);
  final ClassFileTransformerFactory m_classFileTransformerFactory =
    m_classFileTransformerFactoryStubFactory.getStub();

  final RandomStubFactory<Instrumentation> m_instrumentationStubFactory =
    RandomStubFactory.create(Instrumentation.class);
  final Instrumentation m_instrumentation =
    m_instrumentationStubFactory.getStub();

  @SuppressWarnings("unused")
  private void myMethod() {
  }

  @SuppressWarnings("unused")
  private void myOtherMethod() {
  }

  @Override
  public void setUp() {
    m_originalInstrumentation = ExposeInstrumentation.getInstrumentation();
  }

  @Override
  public void tearDown() {
    ExposeInstrumentation.premain("", m_originalInstrumentation);
  }

  public void testMethodRegistration() throws Exception {
    ExposeInstrumentation.premain("", m_instrumentation);

    final Weaver weaver = new DCRWeaver(m_classFileTransformerFactory);

    final CallData createCall =
      m_classFileTransformerFactoryStubFactory.assertSuccess(
        "create", PointCutRegistry.class);
    m_classFileTransformerFactoryStubFactory.assertNoMoreCalls();

    final Object transformer = createCall.getResult();

    m_instrumentationStubFactory.assertSuccess(
      "addTransformer", transformer, true);
    m_instrumentationStubFactory.assertNoMoreCalls();

    final Method method = getClass().getDeclaredMethod("myMethod");

    final Location l1 = weaver.weave(method);
    final Location l2 = weaver.weave(method);
    assertEquals(l1, l2);

    weaver.weave(method);

    m_classFileTransformerFactoryStubFactory.assertNoMoreCalls();
    m_instrumentationStubFactory.assertNoMoreCalls();

    final PointCutRegistry pointCutRegistry =
      (PointCutRegistry) createCall.getParameters()[0];

    final String internalClassName = getClass().getName().replace('.', '/');

    final Map<String, String> pointCuts =
      pointCutRegistry.getPointCutsForClass(internalClassName);

    assertEquals(1, pointCuts.size());

    final String location1 = pointCuts.get("myMethod");
    assertNotNull(location1);

    final Method method2 = getClass().getDeclaredMethod("myOtherMethod");

    weaver.weave(method);
    weaver.weave(method2);

    m_classFileTransformerFactoryStubFactory.assertNoMoreCalls();
    m_instrumentationStubFactory.assertNoMoreCalls();

    final Map<String, String> pointCuts2 =
      pointCutRegistry.getPointCutsForClass(internalClassName);

    assertEquals(2, pointCuts2.size());

    assertEquals(location1, pointCuts2.get("myMethod"));
    assertNotNull(pointCuts2.get("myOtherMethod"));
  }

  public void testWeavingWithNoInstrumentation() throws Exception {
    ExposeInstrumentation.premain("", null);

    try {
      new DCRWeaver(m_classFileTransformerFactory);
      fail("Expected WeavingException");
    }
    catch (WeavingException e) {
    }

    m_classFileTransformerFactoryStubFactory.assertNoMoreCalls();
  }

  public void testWeavingWithInstrumentation() throws Exception {
    ExposeInstrumentation.premain("", m_instrumentation);

    final Weaver weaver = new DCRWeaver(m_classFileTransformerFactory);

    final CallData createCall =
      m_classFileTransformerFactoryStubFactory.assertSuccess(
        "create", PointCutRegistry.class);
    m_classFileTransformerFactoryStubFactory.assertNoMoreCalls();

    final Object transformer = createCall.getResult();

    m_instrumentationStubFactory.assertSuccess(
      "addTransformer", transformer, true);
    m_instrumentationStubFactory.assertNoMoreCalls();

    final Method method = getClass().getDeclaredMethod("myMethod");
    weaver.weave(method);

    m_classFileTransformerFactoryStubFactory.assertNoMoreCalls();

    weaver.applyChanges();

    m_classFileTransformerFactoryStubFactory.assertNoMoreCalls();

    final CallData retransformClassesCall =
      m_instrumentationStubFactory.assertSuccess("retransformClasses",
                                                 new Class[0].getClass());
    assertArraysEqual((Class[])retransformClassesCall.getParameters()[0],
                      new Class[] { getClass(),});

    m_instrumentationStubFactory.assertNoMoreCalls();

    weaver.weave(method);
    weaver.applyChanges();

    m_classFileTransformerFactoryStubFactory.assertNoMoreCalls();
    m_instrumentationStubFactory.assertNoMoreCalls();
  }

  public void testWeavingWithBadInstrumentation() throws Exception {
    ExposeInstrumentation.premain("", m_instrumentation);

    final Weaver weaver = new DCRWeaver(m_classFileTransformerFactory);

    final Method method = getClass().getDeclaredMethod("myMethod");

    weaver.weave(method);
    weaver.weave(method);

    final Exception uce = new UnmodifiableClassException();
    m_instrumentationStubFactory.setThrows("retransformClasses", uce);

    try {
      weaver.applyChanges();
      fail("Expected WeavingException");
    }
    catch (WeavingException e) {
      assertSame(e.getCause(), uce);
    }
  }
}
