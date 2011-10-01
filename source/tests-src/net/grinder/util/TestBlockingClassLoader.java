// Copyright (C) 2005 - 2009 Philip Aston
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

import static java.util.Arrays.asList;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.URLClassLoader;

import org.junit.Test;


/**
 * Unit tests for {@link BlockingClassLoader}.
 *
 * @author Philip Aston
 */
public class TestBlockingClassLoader {

  @Test public void testWithBootLoaderClass() throws Exception {
    final BlockingClassLoader cl =
      new BlockingClassLoader((URLClassLoader)getClass().getClassLoader(),
                              asList(String.class.getName()),
                              true);

    assertSame(String.class, Class.forName("java.lang.String", false, cl));
  }

  @Test public void testWithBootLoaderClass2() throws Exception {
    final BlockingClassLoader cl =
      new BlockingClassLoader((URLClassLoader)getClass().getClassLoader(),
                               asList(String.class.getName()),
                               false);

    try {
      Class.forName("java.lang.String", false, cl);
      fail("Expected ClassNotFoundException");
    }
    catch (ClassNotFoundException e) {
    }
  }

  @Test public void testWithLoadableClass() throws Exception {
    final URLClassLoader cl =
      BlockingClassLoader.isolatingLoader(
        (URLClassLoader)getClass().getClassLoader(),
        asList("net.grinder.*"));

    final String name = "net.grinder.util.AnIsolatedClass";

    final Class<?> c = Class.forName(name, true, cl);

    assertSame(c, Class.forName(name, false, cl));
  }

  @Test public void testGetResource() throws Exception {
    final String resourceName = "TestResources.properties";

    assertNotNull(getClass().getClassLoader().getResource(resourceName));

    final BlockingClassLoader cl =
      new BlockingClassLoader((URLClassLoader)getClass().getClassLoader(),
                               asList("TestResources.properties"),
                               false);

    assertNull(cl.getResource(resourceName));
    assertNull(cl.getResource("Something else"));
  }

  @Test public void testGetResources() throws Exception {
    final String resourceName = "TestResources.properties";

    assertTrue(
      getClass().getClassLoader().getResources(resourceName).hasMoreElements());

    final BlockingClassLoader cl =
      new BlockingClassLoader((URLClassLoader)getClass().getClassLoader(),
                               asList("TestResources.properties"),
                               false);

    assertFalse(cl.getResources(resourceName).hasMoreElements());

    assertFalse(cl.getResources("Something else").hasMoreElements());
  }
}
