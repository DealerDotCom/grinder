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

package net.grinder.engine.process;

import static extra166y.CustomConcurrentHashMap.IDENTITY;
import static extra166y.CustomConcurrentHashMap.STRONG;

import java.util.Collections;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

import net.grinder.common.UncheckedGrinderException;
import net.grinder.engine.common.EngineException;
import net.grinder.engine.process.ScriptEngine.TestInstrumentation;
import extra166y.CustomConcurrentHashMap;


/**
 * Static methods that weaved code uses to dispatch enter and exit calls to the
 * appropriate {@link ScriptEngine.TestInstrumentation}.
 *
 * @author Philip Aston
 * @version $Revision:$
 */
public final class InstrumentationLocator {

  /**
   * Target reference -> location -> instrumentation list. Location strings are
   * interned, so we use an identity hash map for both maps. We use concurrent
   * structures throughout to avoid synchronisation. The target reference is the
   * first key to minimise the cost of traversing woven code for
   * non-instrumented references, which is important if {@code Object}, {@code
   * PyObject}, etc. are instrumented.
   */
  private static final ConcurrentMap<Object,
                                     ConcurrentMap<String,
                                                   List<TestInstrumentation>>>
    s_instrumentation =
      new CustomConcurrentHashMap<Object,
                                  ConcurrentMap<String,
                                                List<TestInstrumentation>>>(
            STRONG, IDENTITY, STRONG, IDENTITY, 101);

  private static final ConcurrentMap<String, List<TestInstrumentation>>
    s_staticInstrumentation =
      new CustomConcurrentHashMap<String, List<TestInstrumentation>>(
            STRONG, IDENTITY, STRONG, IDENTITY, 101);

  private static List<TestInstrumentation> getInstrumentationList(
    Object target,
    String locationID) {

    final ConcurrentMap<String, List<TestInstrumentation>> locationMap =
      target == null ? s_staticInstrumentation : s_instrumentation.get(target);

    if (locationMap != null) {
      final List<TestInstrumentation> list = locationMap.get(locationID);

      if (list != null) {
        return list;
      }
    }

    return Collections.<TestInstrumentation>emptyList();
  }

  /**
   * Called when a weaved method is entered.
   *
   * @param target
   *          The reference used to call the method.
   * @param locationID
   *          Unique identity generated when the method was instrumented.
   *          Will be interned.
   * @throws EngineException
   */
  public static void enter(Object target, String locationID) {

    try {
      for (TestInstrumentation instrumentation :
           getInstrumentationList(target, locationID)) {
        instrumentation.startTest();
      }
    }
    catch (EngineException e) {
      throw new InstrumentationFailureException(e);
    }
  }

  /**
   * Called when a weaved method is exited.
   *
   * @param target
   *          The reference used to call the method.
   * @param locationID
   *          Unique identity generated when the method was instrumented.
   *          Will be interned.
   * @param success
   *          {@code true} if the exit was a normal return, {code false} if an
   *          exception was thrown.
   */
  public static void exit(Object target, String locationID, boolean success) {

    final List<TestInstrumentation> instrumentationList =
      getInstrumentationList(target, locationID);

    // Iterate over instrumentation in reverse.
    final ListIterator<TestInstrumentation> i =
      instrumentationList.listIterator(instrumentationList.size());

    try {
      while (i.hasPrevious()) {
        final TestInstrumentation  instrumentation = i.previous();

        instrumentation.endTest(success);
      }
    }
    catch (EngineException e) {
      throw new InstrumentationFailureException(e);
    }
  }

  /**
   * Interface to allow instrumentation to be registered.
   *
   * TODO - make this non-static and extract interface to break package
   * circularity.
   *
   * @param target
   *          The target reference, or {@code null} for static methods.
   * @param location
   *          String that uniquely identifies the instrumentation location.
   * @param instrumentation
   *          The instrumentation to apply.
   */
  public static void register(Object target,
                              String location,
                              TestInstrumentation instrumentation) {

    // We will create and quickly discard many maps and lists here to avoid
    // needing to lock the ConcurrentMaps. It is important that the
    // enter/exit methods are lock free, the instrumentation registration
    // process can be relatively slow.

    final ConcurrentMap<String, List<TestInstrumentation>> locationMap;

    if (target == null) {
      locationMap = s_staticInstrumentation;
    }
    else {
      final ConcurrentMap<String, List<TestInstrumentation>> newMap =
        new CustomConcurrentHashMap<String, List<TestInstrumentation>>(
              STRONG, IDENTITY, STRONG, IDENTITY, 0);

      final ConcurrentMap<String, List<TestInstrumentation>> oldMap =
        s_instrumentation.putIfAbsent(target, newMap);

      locationMap = oldMap != null ? oldMap : newMap;
    }

    final List<TestInstrumentation> newList =
      new CopyOnWriteArrayList<TestInstrumentation>();

    final List<TestInstrumentation> oldList =
      locationMap.putIfAbsent(location, newList);

    (oldList != null ? oldList : newList).add(instrumentation);
  }

  /**
   * Accessor for the unit tests.
   *
   * @return Our internal structure.
   */
  static void clearInstrumentation() {
    s_instrumentation.clear();
  }

  private static final class InstrumentationFailureException
    extends UncheckedGrinderException {

    private InstrumentationFailureException(EngineException cause) {
      super("Instrumentation Failure", cause);
    }
  }
}
