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

import static java.util.Arrays.asList;
import static net.grinder.testutility.FileUtilities.createFile;
import static net.grinder.util.ClassLoaderUtilities.allResourceLines;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;

import net.grinder.testutility.AbstractFileTestCase;

import org.junit.Test;


/**
 * Unit tests for {@link ClassLoaderUtilities}.
 *
 * @author Philip Aston
 */
public class TestClassLoaderUtilities extends AbstractFileTestCase {

  @Test public void testAllResourceLinesNoResources() throws Exception {
    final List<String> resourceLines =
      allResourceLines(getClass().getClassLoader(), "test/foo");

    assertEquals(0, resourceLines.size());
  }

  @Test public void testAllResourceLinesResources() throws Exception {
    final File d1 = new File(getDirectory(), "one");
    createFile(new File(d1, "test/foo"), asList("a", "b"));

    final File d2 = new File(getDirectory(), "two");
    createFile(new File(d2, "test/foo"), asList("a", "c"));

    final ClassLoader cl1 =
      new URLClassLoader(new URL[] { d1.toURI().toURL() });
    final ClassLoader cl2 =
      new URLClassLoader(new URL[] { d1.toURI().toURL(),
                                     d2.toURI().toURL()});

    assertEquals(asList("a", "b"), allResourceLines(cl1, "test/foo"));

    assertEquals(asList("a", "b", "a", "c"), allResourceLines(cl2, "test/foo"));
  }
}
