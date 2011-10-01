// Copyright (C) 2011 Philip Aston
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
import java.util.List;


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

    while (resources.hasMoreElements()) {
      final URL nextElement = resources.nextElement();

      final InputStream in = nextElement.openStream();

      try {
        final BufferedReader reader =
          new BufferedReader(new InputStreamReader(in, "utf-8"));

        while (true) {
          final String line = reader.readLine();

          if (line == null) {
            break;
          }

          result.add(line);
        }

        reader.close();
      }
      finally {
        in.close();
      }
    }

    return result;
  }

}
