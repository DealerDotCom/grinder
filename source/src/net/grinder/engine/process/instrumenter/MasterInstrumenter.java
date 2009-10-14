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

package net.grinder.engine.process.instrumenter;

import java.util.ArrayList;
import java.util.List;

import net.grinder.common.Logger;
import net.grinder.common.Test;
import net.grinder.engine.common.EngineException;
import net.grinder.engine.process.Instrumenter;
import net.grinder.engine.process.ScriptEngine.Recorder;
import net.grinder.engine.process.instrumenter.dcr.DCRInstrumenter;
import net.grinder.engine.process.instrumenter.traditionaljython.TraditionalJythonInstrumenter;
import net.grinder.script.NotWrappableTypeException;


/**
 * MasterInstrumenter.
 *
 * @author Philip Aston
 * @version $Revision:$
 */
public final class MasterInstrumenter implements Instrumenter {

  private final List<Instrumenter> m_instrumenterList =
    new ArrayList<Instrumenter>();

  /**
   * Constructor for MasterInstrumenter.
   *
   * @param logger A logger.
   */
  public MasterInstrumenter(Logger logger) {
    m_instrumenterList.add(new RejectNullInstrumenter());

    logger.output("Loading instrumentation agents");

    // Override with property?
    try {
      m_instrumenterList.add(new TraditionalJythonInstrumenter());

      logger.output(" - traditional Jython instrumenter");
    }
    catch (EngineException e) {
      // Ignore.
    }

    // Split out Jython instrumentation from Java instrumentation?
    // Separate Jython 2.5 instrumentation?
    final Instrumenter dcrInstrumenter = DCRInstrumenter.createIfEnabled();

    if (dcrInstrumenter != null) {
      m_instrumenterList.add(dcrInstrumenter);

      logger.output(
        " - byte code transforminginstrumenter for Jython and Java");
    }
  }

  /**
   * {@inheritDoc}
   */
  public Object createInstrumentedProxy(Test test,
                                        Recorder recorder,
                                        Object target)
    throws NotWrappableTypeException {

    for (Instrumenter instrumenter : m_instrumenterList) {
      final Object result =
        instrumenter.createInstrumentedProxy(test, recorder, target);

      if (result != null) {
        return result;
      }
    }

    throw new NotWrappableTypeException("Failed to wrap " + target);
  }

  private static final class RejectNullInstrumenter implements Instrumenter {

    public Object createInstrumentedProxy(Test test,
                                          Recorder recorder,
                                          Object target)
      throws NotWrappableTypeException {

      if (target == null) {
        throw new NotWrappableTypeException("Can't wrap null/None");
      }

      return null;
    }
  }
}
