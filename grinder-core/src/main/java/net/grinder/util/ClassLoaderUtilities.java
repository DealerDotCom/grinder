// Copyright (C) 2011 - 2012 Philip Aston
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

package net.grinder.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.grinder.engine.common.EngineException;


/**
 * Class loader related utilities.
 *
 * @author Philip Aston
 */
public class ClassLoaderUtilities {

  /**
   * Find all the resources with the given path, load them, and return their
   * contents as a list of Strings.
   *
   * <p>Property file style comments can be added using "#".</p>
   *
   * <p>Lines are processed as follows:
   * <ul>
   * <li>Comments are removed from each line.</li>
   * <li>Leading and trailing white space is removed from each line.</li>
   * <li>Blank lines are discarded.</li>
   * </ul>
   * </p>
   *
   * @param classLoader
   *          Starting class loader to search. The parent class loaders will be
   *          searched first - see {@link ClassLoader#getResources}.
   * @param resourceName
   *          The name of the resources. Multiple resources may have the same
   *          name if they are loaded from different class loaders.
   * @return The contents of the resources, line by line.
   * @throws IOException
   *           If there was a problem parsing the resources.
   */
  public static List<String> allResourceLines(ClassLoader classLoader,
                                              String resourceName)
    throws IOException {

    final List<String> result = new ArrayList<String>();

    final Enumeration<URL> resources = classLoader.getResources(resourceName);

    // See http://findbugs.sourceforge.net/bugDescriptions.html
    // #DMI_COLLECTION_OF_URLS.
    final Set<String> seenURLs = new HashSet<String>();

    while (resources.hasMoreElements()) {
      final URL url = resources.nextElement();

      final String urlString = url.toString();

      if (seenURLs.contains(urlString)) {
        continue;
      }

      seenURLs.add(urlString);

      final InputStream in = url.openStream();

      try {
        final BufferedReader reader =
          new BufferedReader(new InputStreamReader(in, "utf-8"));

        while (true) {
          String line = reader.readLine();

          if (line == null) {
            break;
          }

          final int comment = line.indexOf('#');

          if (comment >= 0) {
            line = line.substring(0, comment);
          }

          line = line.trim();

          if (line.length() > 0) {
            result.add(line);
          }
        }

        reader.close();
      }
      finally {
        in.close();
      }
    }

    return result;
  }

  /**
   * Simple mechanism for dynamically specified implementations.
   *
   * <p>
   * The classpath is searched for all resources called {@code resourceName}.
   * Matching resources are then parsed with {@link #allResourceLines} to obtain
   * a list of implementation class names. These classes are dynamically loaded,
   * and returned.
   * </p>
   *
   * @param resourceName
   *          The name of the resources to find.
   * @param cls
   *          Implementation classes must be assignable to this class.
   * @return The implementation classes.
   * @throws EngineException
   *           If a class could not be loaded.
   * @param <T>
   *          Constrains type of {@code cls}.
   */
  @SuppressWarnings("unchecked")
  public static <T> List<Class<? extends T>>
    dynamicallyLoadImplementations(String resourceName,
                                   Class<T> cls) throws EngineException {

    final List<String> implementationNames;

    try {
      implementationNames =
        allResourceLines(cls.getClassLoader(), resourceName);
    }
    catch (IOException e) {
      throw new EngineException("Failed to load implementation", e);
    }

    final List<Class<? extends T>> result = new ArrayList<Class<? extends T>>();

    for (String implementationName : implementationNames) {
      try {
        final Class<?> implementationClass = Class.forName(implementationName);

        if (cls.isAssignableFrom(implementationClass)) {
          result.add((Class<? extends T>) implementationClass);
        }
        else {
          throw new EngineException(implementationName +
                                    " does not implement " +
                                    cls.getName());
        }
      }
      catch (ClassNotFoundException e) {
        throw new EngineException("Could not load '" + implementationName + "'",
                                  e);
      }
    }

    return result;
  }
}
