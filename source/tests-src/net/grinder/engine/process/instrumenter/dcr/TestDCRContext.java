// Copyright (C) 2009 - 2011 Philip Aston
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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;

import java.lang.instrument.Instrumentation;

import net.grinder.common.LoggerStubFactory;
import net.grinder.util.weave.agent.ExposeInstrumentation;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;


/**
 * Unit tests for {@link DCRContext}.
 *
 * @author Philip Aston
 */
public class TestDCRContext {

  private Instrumentation m_originalInstrumentation;

  @Mock private Instrumentation m_instrumentation;

  private final LoggerStubFactory m_loggerStubFactory =
    new LoggerStubFactory();

  @Before public void setUp() throws Exception {
    m_originalInstrumentation = ExposeInstrumentation.getInstrumentation();
    assertNotNull(m_originalInstrumentation);

    MockitoAnnotations.initMocks(this);
    when(m_instrumentation.isRetransformClassesSupported()).thenReturn(true);
  }

  @After public void tearDown() throws Exception {
    ExposeInstrumentation.premain("", m_originalInstrumentation);
  }

  @Test public void testCreateWithNoInstrumentation() throws Exception {
    ExposeInstrumentation.premain("", null);

    assertNull(DCRContext.create(
                 m_loggerStubFactory.getLogger()));

    m_loggerStubFactory.assertOutputMessageContains("does not support");
    m_loggerStubFactory.assertNoMoreCalls();
  }

  @Test public void testCreateWithNoRetransformation() throws Exception {
    ExposeInstrumentation.premain("", m_instrumentation);

    when(m_instrumentation.isRetransformClassesSupported()).thenReturn(false);

    assertNull(DCRContext.create(m_loggerStubFactory.getLogger()));

    m_loggerStubFactory.assertOutputMessageContains("does not support");
    m_loggerStubFactory.assertNoMoreCalls();
  }

  @Test public void testCreateWithBadRetransformation() throws Exception {
    ExposeInstrumentation.premain("", m_instrumentation);

    when(m_instrumentation.isRetransformClassesSupported())
      .thenThrow(new NoSuchMethodError());

    assertNull(DCRContext.create(m_loggerStubFactory.getLogger()));

    m_loggerStubFactory.assertOutputMessageContains("does not support");
    m_loggerStubFactory.assertNoMoreCalls();
  }

  @Test public void testWithBadAdvice() throws Exception {
    try {
      new DCRContext(m_instrumentation,
                     TestDCRContext.class,
                     RecorderLocator.getRecorderRegistry());
      fail("Expected AssertionError");
    }
    catch (AssertionError e) {
    }
  }
}
