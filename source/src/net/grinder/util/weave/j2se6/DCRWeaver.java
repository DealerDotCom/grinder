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
import java.lang.reflect.Constructor;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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
  private final Set<Class<?>> m_pendingClasses = new HashSet<Class<?>>();

  private final PointCutRegistryImplementation m_pointCutRegistry =
    new PointCutRegistryImplementation();
  private final ClassFileTransformer m_transformer;

  private final Instrumentation m_instrumentation;

  /**
   * Constructor.
   *
   * @param transformerFactory Used to create the transformer.
   * @throws WeavingException If our Java agent is not installed.
   */
  public DCRWeaver(ClassFileTransformerFactory transformerFactory)
    throws WeavingException {

    m_instrumentation = ExposeInstrumentation.getInstrumentation();

    if (m_instrumentation == null) {
      throw new WeavingException(
        "Instrumentation not available, " +
        "does the command line specify the Java agent?");
    }

    m_transformer = transformerFactory.create(m_pointCutRegistry);

    m_instrumentation.addTransformer(m_transformer, true);
  }

  /**
   * {@inheritDoc}
   */
  public Location weave(Method method) {
    return m_pointCutRegistry.add(method);
  }

  /**
   * {@inheritDoc}
   */
  public void applyChanges() throws WeavingException {
    synchronized (this) {
      if (m_pendingClasses.size() > 0) {
        try {
          m_instrumentation.retransformClasses(
            m_pendingClasses.toArray(new Class<?>[0]));
        }
        catch (UnmodifiableClassException e) {
          throw new WeavingException("Failed to modify class", e);
        }

        m_pendingClasses.clear();
      }
    }
  }

  private static final class LocationImpl implements Location {
    String getLocationString() {
      return "L" + hashCode();
    }
  }

  /**
   * Something that remembers all the point cuts.
   */
  public interface PointCutRegistry {
    /**
     * Return the registered constructor point cuts for a class.
     *
     * @param internalClassName
     *          The name of the class, in internal form. For example, {@code
     *          java/util/List}. Passed through to
     *          {@link ClassFileTransformer#transform}.
     * @return A map of constructors to location strings. Each method in a class
     *         has at most one location string.
     */
    Map<Constructor<?>, String> getConstructorPointCutsForClass(
      String internalClassName);

    /**
     * Return the registered method point cuts for a class.
     *
     * @param internalClassName
     *          The name of the class, in internal form. For example, {@code
     *          java/util/List}. Passed through to
     *          {@link ClassFileTransformer#transform}.
     * @return A map of methods to location strings. Each method in a class
     *         has at most one location string.
     */
    Map<Method, String> getMethodPointCutsForClass(String internalClassName);
  }

  /**
   * Factory that generates {@link ClassFileTransformer}s which perform
   * the weaving.
   */
  public interface ClassFileTransformerFactory {

    /**
     * Factory method.
     *
     * @param pointCutRegistry The point cut registry.
     * @return The transformer.
     */
    ClassFileTransformer create(PointCutRegistry pointCutRegistry);
  }

  private final class PointCutRegistryImplementation
    implements PointCutRegistry {
    // Guarded by this.
    private final Map<Member, LocationImpl> m_wovenMembers =
      new HashMap<Member, LocationImpl>();

    // Pre-calculated mapping of internal class name -> constructor -> location
    // string for efficiency.
    // Guarded by this.
    private final Map<String, Map<Constructor<?>, String>>
      m_internalClassNameToConstructorToLocation =
        new HashMap<String, Map<Constructor<?>, String>>();

    // Pre-calculated mapping of internal class name -> method -> location
    // string for efficiency.
    // Guarded by this.
    private final Map<String, Map<Method, String>>
      m_internalClassNameToMethodToLocation =
        new HashMap<String, Map<Method, String>>();

    public Map<Constructor<?>, String>
      getConstructorPointCutsForClass(String className) {
      return m_internalClassNameToConstructorToLocation.get(className);
    }

    public Map<Method, String> getMethodPointCutsForClass(String className) {
      return m_internalClassNameToMethodToLocation.get(className);
    }

    public Location add(Method method) {
      return add(method, m_internalClassNameToMethodToLocation);
    }

    private <T extends Member> Location add(
      T member,
      Map<String, Map<T, String>> classNameToMemberToLocation) {

      synchronized(this) {
        final LocationImpl alreadyWoven = m_wovenMembers.get(member);

        if (alreadyWoven != null) {
          return alreadyWoven;
        }
      }

      final String className = member.getDeclaringClass().getName();
      final String internalClassName = className.replace('.', '/');
      final LocationImpl location = new LocationImpl();

      synchronized(this) {
        final Map<T, String> methodNameToLocation;

        final Map<T, String> existing =
          classNameToMemberToLocation.get(internalClassName);

        if (existing != null) {
          methodNameToLocation = existing;
        }
        else {
          methodNameToLocation = new HashMap<T, String>();
          classNameToMemberToLocation.put(internalClassName,
                                          methodNameToLocation);
        }

        m_wovenMembers.put(member, location);

        methodNameToLocation.put(member, location.getLocationString());
      }

      synchronized(DCRWeaver.this) {
        m_pendingClasses.add(member.getDeclaringClass());
      }

      return location;
    }
  }
}
