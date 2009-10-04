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

package net.grinder.util.weave.j2se6;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import net.grinder.util.weave.Weaver;
import net.grinder.util.weave.WeavingException;
import net.grinder.util.weave.agent.ExposeInstrumentation;


/**
 * {@link Weaver} that uses Java 6 dynamic class retransformation.
 *
 * @author Philip Aston
 * @version $Revision:$
 */
public final class DCRWeaver implements Weaver {

  // Guarded by this.
  private final Map<Method, LocationImpl> m_wovenMethods =
    new HashMap<Method, LocationImpl>();

  // Guarded by this.
  private final Map<Method, LocationImpl> m_pendingMethods =
    new HashMap<Method, LocationImpl>();

  private final ClassFileTransformerFactory m_transformerFactory;

  /**
   * Constructor.
   *
   * @param transformerFactory Used to create the transformer.
   */
  public DCRWeaver(ClassFileTransformerFactory transformerFactory) {
    m_transformerFactory = transformerFactory;
  }

  /**
   * {@inheritDoc}
   */
  public Location weave(Method m) {
    synchronized (this) {
      final Location alreadyWoven = m_wovenMethods.get(m);

      if (alreadyWoven != null) {
        return alreadyWoven;
      }

      final Location alreadyPending = m_pendingMethods.get(m);

      if (alreadyPending != null) {
        return alreadyPending;
      }

      final LocationImpl newLocation = new LocationImpl();

      m_pendingMethods.put(m, newLocation);

      return newLocation;
    }
  }

  /**
   * {@inheritDoc}
   */
  public void applyChanges() throws WeavingException {
    synchronized (this) {
      if (m_pendingMethods.size() > 0) {
        final Map<Method, String> methodsAndLocations =
          new HashMap<Method, String>();
        final Set<Class<?>> classes = new HashSet<Class<?>>();

        for (Entry<Method, LocationImpl> entry : m_pendingMethods.entrySet()) {
          methodsAndLocations.put(entry.getKey(),
                               entry.getValue().getLocationString());
          classes.add(entry.getKey().getDeclaringClass());
        }

        final Instrumentation instrumentation =
          ExposeInstrumentation.getInstrumentation();

        if (instrumentation == null) {
          throw new WeavingException(
            "Instrumentation not available, " +
            "does the command line specify the Java agent?");
        }

        final ClassFileTransformer transformer =
          m_transformerFactory.create(methodsAndLocations);

        instrumentation.addTransformer(transformer, true);

        try {
          instrumentation.retransformClasses(classes.toArray(new Class<?>[0]));
        }
        catch (UnmodifiableClassException e) {
          throw new WeavingException("Failed to modify class", e);
        }

        instrumentation.removeTransformer(transformer);

        m_wovenMethods.putAll(m_pendingMethods);
        m_pendingMethods.clear();
      }
    }
  }

  private static final class LocationImpl implements Location {
    String getLocationString() {
      return "L" + hashCode();
    }
  }

  /**
   * Factory that generates {@link ClassFileTransformer}s which perform
   * the weaving.
   */
  public interface ClassFileTransformerFactory {

    /**
     * Factory method.
     *
     * @param methodsAndLocations
     *          Map from methods to instrument to their location strings.
     * @return The transformer.
     */
    ClassFileTransformer create(Map<Method, String> methodsAndLocations);
  }
}
