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
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;

import net.grinder.engine.process.Instrumenter;
import net.grinder.util.weave.WeavingException;
import net.grinder.util.weave.agent.ExposeInstrumentation;
import net.grinder.util.weave.j2se6.ASMTransformerFactory;
import net.grinder.util.weave.j2se6.DCRWeaver;


/**
 * DCRInstrumenter.
 *
 * @author Philip Aston
 * @version $Revision:$
 */
public final class DCRInstrumenterFactory {

  /**
   * Return a list of available DCRInstrumenters.
   * @return The instrumenters.
   */
  public static List<Instrumenter> create() {
    // TODO Split out Jython instrumentation from Java instrumentation?
    // Separate Jython 2.5 instrumentation?

    final Instrumentation instrumentation =
      ExposeInstrumentation.getInstrumentation();

    if (instrumentation == null) {
      return Collections.emptyList();
    }

    try {
    final Method m =
      Instrumentation.class.getMethod("isRetransformClassesSupported");

      if (!(Boolean)m.invoke(null)) {
        return Collections.emptyList();
      }
    }
    catch (Exception e1) {
      return Collections.emptyList();
    }

    final ASMTransformerFactory transformerFactory;

    try {
      transformerFactory =
        new ASMTransformerFactory(RecorderLocator.class);
    }
    catch (WeavingException e) {
      throw new AssertionError(e);
    }

     final Instrumenter instrumenter =
       new DCRInstrumenter(new DCRWeaver(transformerFactory, instrumentation),
                           RecorderLocator.getRecorderRegistry());

    return Collections.singletonList(instrumenter);
  }
}
