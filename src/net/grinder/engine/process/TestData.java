// Copyright (C) 2000 Paco Gomez
// Copyright (C) 2000, 2001, 2002, 2003, 2004, 2005 Philip Aston
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
// REGENTS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
// INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
// (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
// SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
// HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
// STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
// ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
// OF THE POSSIBILITY OF SUCH DAMAGE.

package net.grinder.engine.process;

import org.python.core.PyFunction;
import org.python.core.PyInstance;
import org.python.core.PyMethod;
import org.python.core.PyObject;
import org.python.core.PyProxy;
import org.python.core.PyReflectedFunction;

import net.grinder.common.Test;
import net.grinder.engine.common.EngineException;
import net.grinder.script.NotWrappableTypeException;
import net.grinder.statistics.StatisticsSet;
import net.grinder.statistics.StatisticsSetFactory;


/**
 * Represents an individual test. Holds configuration information and
 * the tests statistics.
 *
 * Package scope.
 *
 * @author Philip Aston
 * @version $Revision$
 */
final class TestData implements TestRegistry.RegisteredTest {

  private final ThreadContextLocator m_threadContextLocator;
  private final Test m_test;

  /**
   * Cumulative statistics for our test that haven't yet been set to
   * the console.
   */
  private final StatisticsSet m_statistics;

  TestData(ThreadContextLocator threadContextLocator, Test testDefinition,
           StatisticsSetFactory statisticsSetFactory) {
    m_threadContextLocator = threadContextLocator;
    m_test = testDefinition;
    m_statistics = statisticsSetFactory.create();
  }

  Test getTest() {
    return m_test;
  }

  StatisticsSet getStatistics() {
    return m_statistics;
  }

  Object dispatch(Invokeable invokeable) throws Exception {
    final ThreadContext threadContext = m_threadContextLocator.get();

    if (threadContext == null) {
      throw new EngineException("Only Worker Threads can invoke tests");
    }

    return threadContext.invokeTest(this, invokeable);
  }

  /**
   * Interface for things that can be called.
   */
  interface Invokeable {
    Object call();
  }

  /**
   * We could have defined overloaded createProxy methods that take a
   * PyInstance, PyFunction etc., and return decorator PyObjects. There's no
   * obvious way of doing this in a polymorphic way, so we would be forced to
   * have n factories, n types of decorator, and probably run into identity
   * issues. Instead we lean on Jython and force it to give us Java proxy which
   * we then dynamically subclass with our own type of PyJavaInstance.
   *
   * <p>
   * Later.... <br>
   * This works fine for wrapping the following:
   * <ul>
   * <li>Java instances and classes</li>
   * <li>PyClass</li>
   * <li>PyFunction</li>
   * <li>PyMethod</li>
   * <li>PyReflectedFunction</li>
   * <li>Python primitives (integers, strings, floats, complexes, ...)</li>
   * <li>Python tuples, lists, dictionaries</li>
   * </ul>
   * </p>
   *
   * <p>
   * Of course we're only really interested in the things we can invoke in some
   * way. We throw NotWrappableTypeException for the things we don't want to
   * handle.
   * </p>
   *
   * <p>
   * The specialised PyJavaInstance works surprisingly well for everything bar
   * PyInstances. It can't work for PyInstances, because invoking on the
   * PyJavaInstance calls the PyInstance which in turn attempts to call back on
   * the PyJavaInstance. Use specialised PyInstance clone objects to handle this
   * case. We also need to handle PyReflectedFunctions as an exception.
   * </p>
   *
   * <p>
   * There's a subtle difference in the equality semantics of TestPyInstances
   * and TestPyJavaInstances. TestPyInstances compare do not equal to the
   * wrapped objects, where as due to <code>PyJavaInstance._is()</code>
   * semantics, TestPyJavaInstances <em>do</em> compare equal to the wrapped
   * objects. We can only influence one side of the comparison (we can't easily
   * alter the <code>_is</code> implementation of wrapped objects) so we can't
   * do anything nice about this.
   * </p>
   */
  public Object createProxy(Object o) throws NotWrappableTypeException {

    if (o instanceof PyObject) {
      // Jython object.
      if (o instanceof PyInstance) {
        return new InstrumentedPyInstance(this, (PyInstance)o);
      }
      else if (o instanceof PyFunction) {
        return new InstrumentedPyJavaInstance(this, o);
      }
      else if (o instanceof PyMethod) {
        return new InstrumentedPyJavaInstance(this, o);
      }
      else if (o instanceof PyReflectedFunction) {
        return new InstrumentedPyReflectedFunction(this,
                                                   (PyReflectedFunction)o);
      }
    }
    else if (o instanceof PyProxy) {
      // Jython object that extends a Java class.
      return new InstrumentedPyInstance(this, ((PyProxy)o)._getPyInstance());
    }
    else {
      // Java object.

      final Class c = o.getClass();

      // NB Jython uses Java types for some primitives and strings.
      if (!c.isArray() &&
          !(o instanceof Number) &&
          !(o instanceof String)) {
        return new InstrumentedPyJavaInstance(this, o);
      }
    }

    throw new NotWrappableTypeException(o.getClass().getName());
  }
}
