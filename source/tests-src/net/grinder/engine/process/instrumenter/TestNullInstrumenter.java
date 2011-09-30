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

package net.grinder.engine.process.instrumenter;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import net.grinder.engine.process.Instrumenter;
import net.grinder.engine.process.Recorder;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;


/**
 * Unit tests for {@link NullInstrumenter}.
 *
 * @author Philip Aston
 */
public class TestNullInstrumenter {

  @Mock private Recorder m_recorder;
  @Mock private net.grinder.common.Test m_test;

  private Object m_target = new Object();

  @Before public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
  }

  @Test public void testCreateInstrumentedProxy() throws Exception {

    final Instrumenter instrumenter = new NullInstrumenter();

    final Object result =
      instrumenter.createInstrumentedProxy(m_test, m_recorder, m_target);
    assertNull(result);
  }

  @Test public void testInstrument() throws Exception {

    final Instrumenter instrumenter = new NullInstrumenter();

    final boolean result = instrumenter.instrument(m_test, m_recorder, null);
    assertFalse(result);
  }

  @Test public void testGetDescription() throws Exception {
    assertNull(new NullInstrumenter().getDescription());
  }
}
