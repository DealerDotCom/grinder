// Copyright (C) 2000 Paco Gomez
// Copyright (C) 2000, 2001, 2002, 2003 Philip Aston
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

import org.python.core.PyFinalizableInstance;
import org.python.core.PyFunction;
import org.python.core.PyInstance;
import org.python.core.PyMethod;
import org.python.core.PyObject;

import net.grinder.common.Test;
import net.grinder.engine.EngineException;
import net.grinder.script.NotWrappableTypeException;
import net.grinder.statistics.TestStatistics;
import net.grinder.statistics.TestStatisticsFactory;


/**
 * Represents an individual test. Holds configuration information and
 * the tests statistics.
 *
 * Package scope.
 *
 * @author Philip Aston
 * @version $Revision$
 **/
final class TestData implements TestRegistry.RegisteredTest {

  private final Test m_test;

  /**
   * Cumulative statistics for our test that haven't yet been set to
   * the console.
   */
  private final TestStatistics m_statistics =
    TestStatisticsFactory.getInstance().create();

  TestData(Test testDefinition) {
    m_test = testDefinition;
  }

  Test getTest() {
    return m_test;
  }

  TestStatistics getStatistics() {
    return m_statistics;
  }

  Object dispatch(Invokeable invokeable) throws Exception {
    final ThreadContext threadContext = ThreadContext.getThreadInstance();

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
   * We could have defined overloaded createProxy methods that
   * take a PyInstance, PyFunction etc., and return decorator
   * PyObjects. There's no obvious way of doing this in a
   * polymorphic way, so we would be forced to have n factories,
   * n types of decorator, and probably run into identity
   * issues. Instead we lean on Jython and force it to give us
   * Java proxy which we then dynamically subclass with our own
   * type of PyJavaInstance.
   *
   * <p>Later....
   * <br>This works fine for wrapping the following:
   * <ul>
   *   <li>Java instances and classes</li>
   *   <li>PyClass</li>
   *   <li>PyFunction</li>
   *   <li>PyMethod</li>
   *   <li>Python primitives (integers, strings, floats, complexes, ...)</li>
   *   <li>Python tuples, lists, dictionaries</li>
   *  </ul>
   * </p>
   *
   * <p>Of course we're only really interested in the things we can
   * invoke in some way. We throw NotWrappableTypeException for the
   * things we don't want to handle.</p>
   *
   * <p>The specialised PyJavaInstance works suprisingly well for
   * everything bar PyInstances. It can't work for PyInstances,
   * because invoking on the PyJavaInstance calls the PyInstance
   * which in turn attempts to call back on the PyJavaInstance. Use
   * specialised PyInstance objects to handle this case.</p>
   */
  public Object createProxy(Object o) throws NotWrappableTypeException {

    if (o instanceof PyObject) {
      // Jython object.
      if (o instanceof PyFinalizableInstance) {
        return new TestPyFinalizableInstance(this, (PyFinalizableInstance)o);
      }
      else if (o instanceof PyInstance) {
        return new TestPyInstance(this, (PyInstance)o);
      }
      else if (o instanceof PyFunction) {
        return new TestPyJavaInstance(this, o);
      }
      else if (o instanceof PyMethod) {
        return new TestPyJavaInstance(this, o);
      }
    }
    else {
      // Java object.

      final Class c = o.getClass();

      // NB Jython uses Java types for some primitives and strings.
      if (!c.isArray() &&
          !(o instanceof Number) &&
          !(o instanceof String)) {
        return new TestPyJavaInstance(this, o);
      }
    }

    throw new NotWrappableTypeException(o.getClass().getName());
  }
}
