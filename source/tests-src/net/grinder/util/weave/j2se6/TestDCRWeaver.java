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

import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.lang.reflect.Method;
import java.util.Map;

import net.grinder.testutility.CallData;
import net.grinder.testutility.RandomStubFactory;
import static net.grinder.testutility.AssertUtilities.assertArraysEqual;
import net.grinder.util.weave.Weaver;
import net.grinder.util.weave.WeavingException;
import net.grinder.util.weave.agent.ExposeInstrumentation;
import net.grinder.util.weave.j2se6.DCRWeaver.ClassFileTransformerFactory;
import junit.framework.TestCase;


/**
 * Unit test for {@link DCRWeaver}.
 * TestDCRWeaver.
 *
 * @author Philip Aston
 * @version $Revision:$
 */
public class TestDCRWeaver extends TestCase {

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

  public void testMethodRegistration() throws Exception {
    final Weaver weaver = new DCRWeaver(m_classFileTransformerFactory);

    final Method method = getClass().getDeclaredMethod("myMethod");

    weaver.weave(method);
    weaver.weave(method);
    weaver.weave(method);

    m_classFileTransformerFactoryStubFactory.assertNoMoreCalls();
  }

  public void testWeavingWithNoInstrumentation() throws Exception {
    final Weaver weaver = new DCRWeaver(m_classFileTransformerFactory);

    final Method method = getClass().getDeclaredMethod("myMethod");

    weaver.weave(method);
    weaver.weave(method);
    weaver.weave(method);

    m_classFileTransformerFactoryStubFactory.assertNoMoreCalls();

    try {
      weaver.applyChanges();
      fail("Expected WeavingException");
    }
    catch (WeavingException e) {
    }

    m_classFileTransformerFactoryStubFactory.assertNoMoreCalls();
  }

  public void testWeavingWithInstrumentation() throws Exception {
    final Weaver weaver = new DCRWeaver(m_classFileTransformerFactory);

    final Method method = getClass().getDeclaredMethod("myMethod");

    weaver.weave(method);

    m_classFileTransformerFactoryStubFactory.assertNoMoreCalls();

    ExposeInstrumentation.premain("", m_instrumentation);

    weaver.applyChanges();

    final CallData createCall =
      m_classFileTransformerFactoryStubFactory.assertSuccess("create",
                                                             Map.class);
    m_classFileTransformerFactoryStubFactory.assertNoMoreCalls();

    final Object transformer = createCall.getResult();

    m_instrumentationStubFactory.assertSuccess("addTransformer",
                                               transformer,
                                               true);

    final CallData retransformClassesCall =
      m_instrumentationStubFactory.assertSuccess("retransformClasses",
                                                 new Class[0].getClass());
    assertArraysEqual((Class[])retransformClassesCall.getParameters()[0],
                      new Class[] { getClass(),});

    m_instrumentationStubFactory.assertSuccess("removeTransformer",
                                               transformer);

    m_instrumentationStubFactory.assertNoMoreCalls();

    weaver.weave(method);
    weaver.applyChanges();

    m_classFileTransformerFactoryStubFactory.assertNoMoreCalls();
    m_instrumentationStubFactory.assertNoMoreCalls();
  }

  public void testWeavingWithBadInstrumentation() throws Exception {
    final Weaver weaver = new DCRWeaver(m_classFileTransformerFactory);

    final Method method = getClass().getDeclaredMethod("myMethod");

    weaver.weave(method);
    weaver.weave(method);

    m_classFileTransformerFactoryStubFactory.assertNoMoreCalls();

    ExposeInstrumentation.premain("", m_instrumentation);

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
