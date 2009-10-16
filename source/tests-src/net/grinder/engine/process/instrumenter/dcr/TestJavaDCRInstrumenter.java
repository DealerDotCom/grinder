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

import junit.framework.TestCase;
import net.grinder.script.NotWrappableTypeException;
import net.grinder.util.weave.Weaver;
import net.grinder.util.weave.WeavingException;
import net.grinder.util.weave.agent.ExposeInstrumentation;
import net.grinder.util.weave.j2se6.ASMTransformerFactory;
import net.grinder.util.weave.j2se6.DCRWeaver;


/**
 * Unit tests for {@link JavaDCRInstrumenter}.
 *
 * @author Philip Aston
 * @version $Revision:$
 */
public class TestJavaDCRInstrumenter extends TestCase {

  private static final Weaver s_weaver;
  private final JavaDCRInstrumenter m_instrumenter;

  static {
    try {
      s_weaver =
        new DCRWeaver(new ASMTransformerFactory(RecorderLocator.class),
                      ExposeInstrumentation.getInstrumentation());
    }
    catch (WeavingException e) {
      throw new ExceptionInInitializerError(e);
    }
  }

  public TestJavaDCRInstrumenter() throws Exception {
    m_instrumenter =
      new JavaDCRInstrumenter(s_weaver, RecorderLocator.getRecorderRegistry());
  }

  @Override protected void tearDown() throws Exception {
    RecorderLocator.clearRecorders();
  }

  private void assertNotWrappable(Object o) throws Exception {
    try {
      m_instrumenter.createInstrumentedProxy(null, null, o);
      fail("Expected NotWrappableTypeException");
    }
    catch (NotWrappableTypeException e) {
    }
  }

  public void testCreateProxyWithNonWrappableParameters() throws Exception {

    assertNotWrappable(Object.class);
    assertNotWrappable(new Object());
    assertNotWrappable(new String());
    assertNotWrappable(java.util.Random.class);

    // Can't wrap classes in net.grinder.*.
    assertNotWrappable(this);
  }
}
