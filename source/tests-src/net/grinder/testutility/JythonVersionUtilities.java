// Copyright (C) 2010 - 2011 Philip Aston
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

import java.net.URL;
import java.util.Arrays;
import java.util.List;

import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Various test utilities for setting the context for different Jython
 * installations.
 *
 * @author Philip Aston
 * @version $Revision:$
 */
public class JythonVersionUtilities {

  private static final List<String> GRINDER_AND_PYTHON_CLASSES =
    Arrays.asList("net.grinder.*",
                  "org.python.*",
                  "test.*",
                  "+net.grinder.util.weave.agent.*");

  // The default classloader uses Jython 2.2.1, so there's no need for a
  // special suite method for that version.

  public static TestSuite jython21Suite(Class<?extends TestCase> suite)
    throws Exception {

    final TestSuite result = new TestSuite(suite.getName() + " [Jython 2.1]");
    result.addTest(jythonSuite(suite, "jython2_1.dir"));
    return result;
  }

  public static TestSuite jython25Suite(Class<? extends TestCase> suite)
    throws Exception {

    final TestSuite result = new TestSuite(suite.getName() + " [Jython 2.5]");
    result.addTest(jythonSuite(suite, "jython2_5_2.dir"));
    result.addTest(jythonSuite(suite, "jython2_5_0.dir"));
    result.addTest(jythonSuite(suite, "jython2_5_1.dir"));

    return result;
  }

  @SuppressWarnings("unchecked")
  private static TestSuite jythonSuite(Class<? extends TestCase> suite,
                                       String pythonHomeProperty)
    throws Exception {

    final String oldPythonHome = System.getProperty("python.home");

    final String pythonHome = System.getProperty(pythonHomeProperty);

    if (pythonHome == null) {
      System.err.println("***** " +
                         pythonHomeProperty +
                         " not set, skipping tests for Jython version.");
      return new TestSuite();
    }

    System.setProperty("python.home", pythonHome);

    try {
      final URL jythonJarURL = new URL("file://" + pythonHome + "/jython.jar");

      final ClassLoader classLoader =
        BlockingClassLoader.createClassLoader(GRINDER_AND_PYTHON_CLASSES,
                                              Arrays.asList(jythonJarURL));

      final Class<? extends TestCase> suiteReloaded =
        (Class<? extends TestCase>) classLoader.loadClass(suite.getName());

      return new TestSuite(suiteReloaded, pythonHome);
    }
    finally {
      if (oldPythonHome != null) {
        System.setProperty("python.home", oldPythonHome);
      }
      else {
        System.clearProperty("python.home");
      }
    }
  }
}
