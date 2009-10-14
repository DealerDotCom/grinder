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
import static extra166y.CustomConcurrentHashMap.WEAK;

import java.util.Collections;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

import net.grinder.common.UncheckedGrinderException;
import net.grinder.engine.common.EngineException;
import net.grinder.engine.process.ScriptEngine.Instrumentation;
import extra166y.CustomConcurrentHashMap;


/**
 * Static methods that weaved code uses to dispatch enter and exit calls to the
 * appropriate {@link ScriptEngine.Instrumentation}.
 *
 * @author Philip Aston
 * @version $Revision:$
 */
public final class InstrumentationLocator implements InstrumentationRegistry {

  private static final InstrumentationLocator s_instance =
    new InstrumentationLocator();

  /**
   * Target reference -> location -> instrumentation list. Location strings are
   * interned, so we use an identity hash map for both maps. We use concurrent
   * structures throughout to avoid synchronisation. The target reference is the
   * first key to minimise the cost of traversing woven code for
   * non-instrumented references, which is important if {@code Object}, {@code
   * PyObject}, etc. are instrumented.
   */
  private final ConcurrentMap<Object,
                              ConcurrentMap<String, List<Instrumentation>>>
  m_instrumentation =
      new CustomConcurrentHashMap<Object,
                                  ConcurrentMap<String, List<Instrumentation>>>(
            WEAK, IDENTITY, STRONG, IDENTITY, 101);

  private List<Instrumentation> getInstrumentationList(
    Object target,
    String locationID) {

    final ConcurrentMap<String, List<Instrumentation>> locationMap =
      m_instrumentation.get(target);

    if (locationMap != null) {
      final List<Instrumentation> list = locationMap.get(locationID);

      if (list != null) {
        return list;
      }
    }

    return Collections.<Instrumentation>emptyList();
  }

  /**
   * Called when a weaved method is entered.
   *
   * @param target
   *          The reference used to call the method. The class is used for
   *          static methods or constructors.
   * @param location
   *          Unique identity generated when the method was instrumented.
   *          Will be interned.
   * @throws EngineException
   */
  public static void enter(Object target, String location) {
//   System.out.printf("enter(%s, %s, %s)%n",
//    target.hashCode(), target.getClass(), location);
    try {
      for (Instrumentation instrumentation :
           s_instance.getInstrumentationList(target, location)) {
//        System.out.printf("enter(%s, %s)%n",
//          target == null ? null : target.hashCode(), location);
        instrumentation.start();
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
   *          The reference used to call the method. The class is used for
   *          static methods or constructors.
   * @param location
   *          Unique identity generated when the method was instrumented.
   *          Will be interned.
   * @param success
   *          {@code true} if the exit was a normal return, {code false} if an
   *          exception was thrown.
   */
  public static void exit(Object target, String location, boolean success) {
    final List<Instrumentation> instrumentationList =
      s_instance.getInstrumentationList(target, location);

    // Iterate over instrumentation in reverse.
    final ListIterator<Instrumentation> i =
      instrumentationList.listIterator(instrumentationList.size());

    try {
      while (i.hasPrevious()) {
        // System.out.printf("exit(%s, %s)%n",
        // target == null ? null : target.hashCode(), location);
        i.previous().end(success);
      }
    }
    catch (EngineException e) {
      throw new InstrumentationFailureException(e);
    }
  }

  /**
   * Expose our instrumentation registry to the package.
   *
   * @return The instrumentation registry.
   */
  static InstrumentationRegistry getInstrumentationRegistry() {
    return s_instance;
  }

  /**
   * Registration method.
   *
   * @param target
   *          The target reference, or {@code null} for static methods.
   * @param location
   *          String that uniquely identifies the instrumentation location.
   * @param instrumentation
   *          The instrumentation to apply.
   */
  public void register(Object target,
                       String location,
                       Instrumentation instrumentation) {

//     System.out.printf("register(%s, %s, %s, %s)%n",
//                      target.hashCode(), location,
//                          target,
//                          target.getClass());

    // We will create and quickly discard many maps and lists here to avoid
    // needing to lock the ConcurrentMaps. It is important that the
    // enter/exit methods are lock free, the instrumentation registration
    // process can be relatively slow.

    final ConcurrentMap<String, List<Instrumentation>> newMap =
      new CustomConcurrentHashMap<String, List<Instrumentation>>(
            STRONG, IDENTITY, STRONG, IDENTITY, 0);

    final ConcurrentMap<String, List<Instrumentation>> oldMap =
      m_instrumentation.putIfAbsent(target, newMap);

    final ConcurrentMap<String, List<Instrumentation>> locationMap =
      oldMap != null ? oldMap : newMap;

    final List<Instrumentation> newList =
      new CopyOnWriteArrayList<Instrumentation>();

    final List<Instrumentation> oldList =
      locationMap.putIfAbsent(location.intern(), newList);

    (oldList != null ? oldList : newList).add(instrumentation);
  }

  /**
   * Accessor for the unit tests.
   *
   * @return Our internal structure.
   */
  void clearInstrumentation() {
    m_instrumentation.clear();
  }

  private static final class InstrumentationFailureException
    extends UncheckedGrinderException {

    private InstrumentationFailureException(EngineException cause) {
      super("Instrumentation Failure", cause);
    }
  }
}
