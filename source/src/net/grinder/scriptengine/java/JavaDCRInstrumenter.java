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

package net.grinder.scriptengine.java;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import net.grinder.script.NonInstrumentableTypeException;
import net.grinder.scriptengine.DCRContext;
import net.grinder.scriptengine.AbstractDCRInstrumenter;
import net.grinder.scriptengine.Recorder;
import net.grinder.util.weave.Weaver.TargetSource;


/**
 * DCR instrumenter for Java.
 *
 * @author Philip Aston
 */
final class JavaDCRInstrumenter extends AbstractDCRInstrumenter {

  /**
   * Constructor.
   *
   * @param context The DCR context.
   */
  public JavaDCRInstrumenter(DCRContext context) {
    super(context);
  }

  /**
   * {@inheritDoc}
   */
  public String getDescription() {
    return "byte code transforming instrumenter for Java";
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected boolean instrument(Object target, Recorder recorder)
    throws NonInstrumentableTypeException {

    if (target instanceof Class<?>) {
      instrumentClass((Class<?>)target, recorder);
    }
    else if (target != null) {
      instrumentInstance(target, recorder);
    }

    return true;
  }

  private void instrumentClass(Class<?> targetClass, Recorder recorder)
    throws NonInstrumentableTypeException {

    if (targetClass.isArray()) {
      throw new NonInstrumentableTypeException("Can't instrument arrays");
    }

    for (Constructor<?> constructor : targetClass.getDeclaredConstructors()) {
       getContext().add(targetClass, constructor, recorder);
    }

    // Instrument the static methods declared by the target class. Ignore
    // any parent class.
    for (Method method : targetClass.getDeclaredMethods()) {
      if (Modifier.isStatic(method.getModifiers())) {
        getContext().add(targetClass, method, TargetSource.CLASS, recorder);
      }
    }

//    Class<?> c = targetClass;
//
//    do {
//      for (Method method : c.getDeclaredMethods()) {
//        if (Modifier.isStatic(method.getModifiers())) {
//          instrument(c, method, recorder);
//        }
//      }
//
//      c = c.getSuperclass();
//    }
//    while (isInstrumentable(c));
  }

  private void instrumentInstance(Object target, Recorder recorder)
    throws NonInstrumentableTypeException {

    Class<?> c = target.getClass();

    if (c.isArray()) {
      throw new NonInstrumentableTypeException("Can't instrument arrays");
    }

    do {
      for (Method method : c.getDeclaredMethods()) {
        if (!Modifier.isStatic(method.getModifiers())) {
          getContext().add(target,
                           method,
                           TargetSource.FIRST_PARAMETER,
                           recorder);
        }
      }

      c = c.getSuperclass();
    }
    while (getContext().isInstrumentable(c));
  }
}
