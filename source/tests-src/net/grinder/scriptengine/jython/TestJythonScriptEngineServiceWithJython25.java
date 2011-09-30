// Copyright (C) 2011 Philip Aston
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

package net.grinder.scriptengine.jython;

import static net.grinder.scriptengine.jython.instrumentation.AbstractJythonInstrumenterTestCase.assertVersion;
import net.grinder.common.GrinderProperties;
import net.grinder.engine.process.dcr.DCRContextImplementation;
import net.grinder.scriptengine.DCRContext;
import net.grinder.scriptengine.Instrumenter;
import net.grinder.scriptengine.jython.JythonScriptEngineService;
import net.grinder.testutility.Jython25Runner;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.python.core.PyInstance;


/**
 * Unit tests for {@link JythonScriptEngineService}.
 *
 * @author Philip Aston
 */
@RunWith(Jython25Runner.class)
public class TestJythonScriptEngineServiceWithJython25
  extends AbstractJythonScriptEngineTests {

  @Test public void testVersion() throws Exception {
    assertVersion("2.5");
  }

  @Test public void testCreateInstrumentedProxy() throws Exception {
    final GrinderProperties properties = new GrinderProperties();
    final DCRContext context = DCRContextImplementation.create(null);

    final Instrumenter instrumenter =
      new JythonScriptEngineService().createInstrumenter(properties, context);

    assertEquals("byte code transforming instrumenter for Jython 2.5",
                 instrumenter.getDescription());

    final Object original = new PyInstance();

    final Object proxy =
      instrumenter.createInstrumentedProxy(m_test, m_recorder, original);

    assertSame(original, proxy);
  }

  @Test public void testInstrument() throws Exception {
    final GrinderProperties properties = new GrinderProperties();
    final DCRContextImplementation context = DCRContextImplementation.create(null);

    final Instrumenter instrumenter =
      new JythonScriptEngineService().createInstrumenter(properties, context);

    instrumenter.instrument(m_test, m_recorder, new PyInstance());
  }
}
