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

import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

import net.grinder.common.UncheckedGrinderException;
import net.grinder.engine.common.EngineException;
import net.grinder.engine.process.ScriptEngine.TestInstrumentation;
import net.grinder.util.Pair;
import extra166y.CustomConcurrentHashMap;


/**
 * Static methods that weaved code uses to dispatch enter and exit calls to the
 * appropriate {@link ScriptEngine.TestInstrumentation}.
 *
 * @author Philip Aston
 * @version $Revision:$
 */
public final class InstrumentationLocator {

  /** Singleton reference used to represent static method invocations. */
  private static final Object STATIC = new Object();

  // Location strings are interned, so we use an identity hash map.
  // The mapping between the references and the instrumentation is stored as
  // an ordered list of (reference, instrumentation) pairs. Again,
  // identity equality is used to find the appropriate instrumentations for
  // a reference. The number of instrumented items for each location will be
  // low (typically 1),
  private static final ConcurrentMap<String, InstrumentationList>
    s_instrumentation =
      new CustomConcurrentHashMap<String, InstrumentationList>(STRONG,
                                                               IDENTITY,
                                                               STRONG,
                                                               IDENTITY,
                                                               101);

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

    final List<Pair<Object, TestInstrumentation>> instrumentationList =
      s_instrumentation.get(locationID);

    if (instrumentationList != null) {
      final Object t = target == null ? STATIC : target;

      try {
        for (Pair<Object, TestInstrumentation> pair : instrumentationList) {
          if (pair.getFirst() == t) {
              pair.getSecond().startTest();
          }
        }
      }
      catch (EngineException e) {
        throw new InstrumentationFailureException(e);
      }
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

    final List<Pair<Object, TestInstrumentation>> instrumentationList =
      s_instrumentation.get(locationID);

    if (instrumentationList != null) {
      final Object t = target == null ? STATIC : target;

      // Iterate over instrumentation in reverse.
      final ListIterator<Pair<Object, TestInstrumentation>> i =
        instrumentationList.listIterator(instrumentationList.size());

      try {
        while (i.hasPrevious()) {
          final Pair<Object, TestInstrumentation> pair = i.previous();

          if (pair.getFirst() == t) {
              pair.getSecond().endTest(success);
          }
        }
      }
      catch (EngineException e) {
        throw new InstrumentationFailureException(e);
      }
    }
  }

  static void register(Object target,
                       String locationID,
                       TestInstrumentation instrumentation) {
    // Most locations will have a single instrumentation. When they don't,
    // we create and discard a list needlessly. This is fine - we are optimising
    // the enter/exit methods by using concurrent structures, the
    // instrumentation registration process can be relatively slow.
    final InstrumentationList
      newInstrumentations = new InstrumentationListImplementation();

    final InstrumentationList oldInstrumentations =
      s_instrumentation.putIfAbsent(locationID.intern(),
                                    newInstrumentations);

    final InstrumentationList instrumentations =
      oldInstrumentations != null ? oldInstrumentations : newInstrumentations;

    instrumentations.add(
      new Pair<Object, TestInstrumentation>(
        target != null ? target : STATIC, instrumentation));
  }

  /**
   * Accessor for the unit tests.
   *
   * @return Our internal structure.
   */
  static ConcurrentMap<String, InstrumentationList> getInstrumentation() {
    return s_instrumentation;
  }

  private static final class InstrumentationFailureException
    extends UncheckedGrinderException {

    private InstrumentationFailureException(EngineException cause) {
      super("Instrumentation Failure", cause);
    }
  }

  /** Poor man's typedef. */
  private interface InstrumentationList
    extends List<Pair<Object, TestInstrumentation>> {};

    /** Poor man's typedef. */
  private static class InstrumentationListImplementation
    extends CopyOnWriteArrayList<Pair<Object, TestInstrumentation>>
    implements InstrumentationList {};
}
