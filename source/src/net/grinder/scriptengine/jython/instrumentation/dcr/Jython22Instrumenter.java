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

package net.grinder.scriptengine.jython.instrumentation.dcr;

import java.lang.reflect.Method;

import net.grinder.script.NonInstrumentableTypeException;
import net.grinder.scriptengine.DCRContext;
import net.grinder.scriptengine.Recorder;
import net.grinder.util.weave.Weaver.TargetSource;

import org.python.core.PyClass;
import org.python.core.PyFunction;
import org.python.core.PyInstance;
import org.python.core.PyMethod;
import org.python.core.PyObject;
import org.python.core.PyProxy;


/**
 * DCR instrumenter for Jython 2.1, 2.2.
 *
 * @author Philip Aston
 */
public final class Jython22Instrumenter extends AbstractJythonDCRInstrumenter {

  /**
   * Constructor.
   *
   * @param context The DCR context.
   */
  public Jython22Instrumenter(DCRContext context) {
    super(context);
  }

  /**
   * {@inheritDoc}
   */
  public String getDescription() {
    return "byte code transforming instrumenter for Jython 2.1/2.2";
  }

  private void instrumentPublicMethodsByName(Object target,
                                             String methodName,
                                             TargetSource targetSource,
                                             Recorder recorder,
                                             boolean includeSuperClassMethods)
    throws NonInstrumentableTypeException {

    // getMethods() includes superclass methods.
    for (Method method : target.getClass().getMethods()) {
      if (!includeSuperClassMethods &&
          target.getClass() != method.getDeclaringClass()) {
        continue;
      }

      if (!method.getName().equals(methodName)) {
        continue;
      }

      getContext().add(target, method, targetSource, recorder);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override protected void transform(Recorder recorder, PyInstance target)
    throws NonInstrumentableTypeException {

    instrumentPublicMethodsByName(target,
                                  "invoke",
                                  TargetSource.FIRST_PARAMETER,
                                  recorder,
                                  true);
  }

  /**
   * {@inheritDoc}
   */
  @Override protected void transform(Recorder recorder, PyFunction target)
    throws NonInstrumentableTypeException {

    instrumentPublicMethodsByName(target,
                                  "__call__",
                                  TargetSource.FIRST_PARAMETER,
                                  recorder,
                                  false);
  }

  /**
   * {@inheritDoc}
   */
  @Override protected void transform(Recorder recorder, PyMethod target)
    throws NonInstrumentableTypeException {

    instrumentPublicMethodsByName(target,
                                  "__call__",
                                  TargetSource.FIRST_PARAMETER,
                                  recorder,
                                  false);
  }

  /**
   * {@inheritDoc}
   */
  @Override protected void transform(Recorder recorder, PyClass target)
    throws NonInstrumentableTypeException {

    instrumentPublicMethodsByName(target,
                                  "__call__",
                                  TargetSource.FIRST_PARAMETER,
                                  recorder,
                                  false);
  }

  /**
   * {@inheritDoc}
   */
  @Override protected void transform(Recorder recorder, PyProxy target)
    throws NonInstrumentableTypeException {

    final PyObject pyInstance = target._getPyInstance();

    instrumentPublicMethodsByName(pyInstance,
                                  "invoke",
                                  TargetSource.FIRST_PARAMETER,
                                  recorder,
                                  true);
  }
}
