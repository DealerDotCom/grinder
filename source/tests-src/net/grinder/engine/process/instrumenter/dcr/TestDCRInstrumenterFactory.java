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

import java.lang.instrument.Instrumentation;

import junit.framework.TestCase;

import net.grinder.testutility.RandomStubFactory;
import net.grinder.util.weave.agent.ExposeInstrumentation;


/**
 * Unit tests for {@link DCRInstrumenterFactory}.
 *
 * @author Philip Aston
 * @version $Revision:$
 */
public class TestDCRInstrumenterFactory extends TestCase {

  private Instrumentation m_originalInstrumentation;

  @Override
  protected void setUp() throws Exception {
    m_originalInstrumentation = ExposeInstrumentation.getInstrumentation();
  }

  @Override
  protected void tearDown() throws Exception {
    ExposeInstrumentation.premain("", m_originalInstrumentation);
  }

  private final RandomStubFactory<Instrumentation>
    m_instrumentationStubFactory =
      RandomStubFactory.create(Instrumentation.class);
  private final Instrumentation m_instrumentation =
    m_instrumentationStubFactory.getStub();

  public void testCreateWithNoInstrumentation() throws Exception {
    ExposeInstrumentation.premain("", null);

    assertEquals(0, DCRInstrumenterFactory.create().size());
  }

  public void testCreateWithNoRetransformation() throws Exception {
    ExposeInstrumentation.premain("", m_instrumentation);

    m_instrumentationStubFactory.setResult("isRetransformClassesSupported",
                                           false);

    assertEquals(0, DCRInstrumenterFactory.create().size());

    m_instrumentationStubFactory.setThrows("isRetransformClassesSupported",
                                           new NoSuchMethodError());

    assertEquals(0, DCRInstrumenterFactory.create().size());
  }

  public void testWithInstrumentation() throws Exception {
    assertEquals(2, DCRInstrumenterFactory.create().size());
  }
}
