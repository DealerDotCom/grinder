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

package net.grinder.testutility;

import static java.util.Collections.emptySet;
import static java.util.Collections.enumeration;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;

import net.grinder.util.IsolatingClassLoader;


/**
 * Classloader that prevents its parent from loading certain classes.
 *
 * <p>
 * Unlike {@link IsolatingClassLoader}, it doesn't load classes itself, so we
 * can load alternative implementations of the blocked classes in a child
 * classloader.
 * </p>
 *
 * @author Philip Aston
 * @version $Revision:$
 */
public class BlockingClassLoader extends URLClassLoader {

  /**
   * Utility method that creates a classloader that will hide a set of classes,
   * and allow alternative implementations to be provided.
   *
   * @param blockedClasses
   *          Classes and packages to hide. See
   *          {@link #BlockingClassLoader(URLClassLoader, List)} for wild card
   *          rules.
   * @param classPathEntries
   *          URLs from which alternative implementations can be loaded. They
   *          are prefixed to the current classloader's standard classpath.
   * @return The classloader.
   */
  public static URLClassLoader createClassLoader(List<String> blockedClasses,
                                                 List<URL> classPathEntries) {

    final URLClassLoader ourClassLoader =
      (URLClassLoader)BlockingClassLoader.class.getClassLoader();

    final BlockingClassLoader blockingClassLoader =
      new BlockingClassLoader(ourClassLoader, blockedClasses);

    final List<URL> ourClassPath = Arrays.asList(ourClassLoader.getURLs());
    final List<URL> classPath = new ArrayList<URL>(classPathEntries.size() +
                                                   ourClassPath.size());

    classPath.addAll(classPathEntries);
    classPath.addAll(ourClassPath);

    return new URLClassLoader(classPath.toArray(new URL[0]),
                              blockingClassLoader);
  }

  private final List<String> m_blockedClassNames = new ArrayList<String>();
  private final String[] m_blockedPrefixes;

  private final List<String> m_allowedClassNames = new ArrayList<String>();
  private final String[] m_allowedPrefixes;

  /**
   * Constructor.
   *
   * @param parent
   *          Parent classloader. We use its class path to load our classes.
   * @param blocked
   *          Array of fully qualified class names, or fully qualified prefixes
   *          ending in "*", that identify the packages or classes to block. A
   *          leading "+" can be added to a class name or package prefix to
   *          indicate that it is allowed, overriding blocking rules.
   *
   *          <p>
   *          Resource names may also be specified, fully qualified with '/'
   *          separators as necessary. Any wild card package names also filter
   *          resources; the '.' separators are translated internally to '/'s.
   *          </p>
   */
  public BlockingClassLoader(URLClassLoader parent, List<String> blockedList) {
    super(parent.getURLs(), parent);

    final List<String> allowedPrefixes = new ArrayList<String>();
    final List<String> blockedPrefixes = new ArrayList<String>();

    for (String blocked : blockedList) {
      final int index = blocked.indexOf('*');

      final boolean allowed = blocked.length() > 1 &&
                              blocked.charAt(0) == '+';

      if (index >= 0) {
        if (allowed) {
          allowedPrefixes.add(blocked.substring(1, index));
        }
        else {
          blockedPrefixes.add(blocked.substring(0, index));
        }
      }
      else {
        if (allowed) {
          m_allowedClassNames.add(blocked.substring(1));
        }
        else {
          m_blockedClassNames.add(blocked);
        }
      }
    }

    m_allowedPrefixes =
      allowedPrefixes.toArray(new String[allowedPrefixes.size()]);
    m_blockedPrefixes =
      blockedPrefixes.toArray(new String[blockedPrefixes.size()]);
  }

  private boolean isBlocked(String name, boolean isResource) {

    final String packageName = isResource ? name.replace('/', '.') : name;

    boolean allowed = m_allowedClassNames.contains(name);

    for (int i = 0; !allowed && i < m_allowedPrefixes.length; i++) {
      if (packageName.startsWith(m_allowedPrefixes[i])) {
        allowed = true;
      }
    }

    if (allowed) {
//      System.err.println("Allowing " + name);
      return false;
    }
    else {
      boolean blocked = m_blockedClassNames.contains(name);

      for (int i = 0; !blocked && i < m_blockedPrefixes.length; i++) {
        if (packageName.startsWith(m_blockedPrefixes[i])) {
          blocked = true;
        }
      }

      if (blocked) {
//        System.err.println("Blocking " + name);
        return true;
      }
    }

    return false;
  }

  /**
   * Override only to check parent ClassLoader if not blocked.
   *
   * {@inheritDoc}
   */
  @Override protected Class<?> loadClass(String name, boolean resolve)
    throws ClassNotFoundException  {

    synchronized (this) {
      if (isBlocked(name, false)) {
        throw new ClassNotFoundException();
      }

      return super.loadClass(name, resolve);
    }
  }

  /**
   * Override only to check parent ClassLoader if not blocked.
   *
   * {@inheritDoc}
   */
  @Override public URL getResource(String name) {

    if (isBlocked(name, true)) {
      return null;
    }

    return super.getResource(name);
  }

  /**
   * Override only to check parent ClassLoader if not blocked.
   *
   * {@inheritDoc}
   */
  @Override
  public Enumeration<URL> getResources(String name) throws IOException {

    if (isBlocked(name, true)) {
      final Set<URL> empty = emptySet();
      return enumeration(empty);
    }

    return super.getResources(name);
  }
}
