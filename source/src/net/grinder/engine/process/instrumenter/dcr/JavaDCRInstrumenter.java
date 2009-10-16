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

import net.grinder.engine.process.ScriptEngine.Recorder;
import net.grinder.script.NotWrappableTypeException;
import net.grinder.util.weave.Weaver;


/**
 * DCRInstrumenter.
 *
 * @author Philip Aston
 * @version $Revision:$
 */
final class JavaDCRInstrumenter extends DCRInstrumenter {

  /**
   * Constructor for DCRInstrumenter.
   *
   * @param weaver The weaver.
   * @param recorderRegistry The recorder registry.
   */
  public JavaDCRInstrumenter(Weaver weaver, RecorderRegistry recorderRegistry) {
    super(weaver, recorderRegistry);
  }

  /**
   * {@inheritDoc}
   */
  public String getDescription() {
    return "byte code transforming instrumenter for Java";
  }

  @Override
  protected Object instrument(Object target, Recorder recorder)
    throws NotWrappableTypeException {

    if (target instanceof Class<?>) {
      instrumentClass(target, (Class<?>)target, recorder);
    }
    else {
      instrumentClass(target, target.getClass(), recorder);
    }

    return target;
  }
}
