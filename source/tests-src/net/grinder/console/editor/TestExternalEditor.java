// Copyright (C) 2007 Philip Aston
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
// REGENTS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
// INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
// (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
// SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
// HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
// STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
// ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
// OF THE POSSIBILITY OF SUCH DAMAGE.

package net.grinder.console.editor;

import java.beans.PropertyChangeListener;
import java.io.File;

import net.grinder.console.distribution.AgentCacheState;
import net.grinder.testutility.AbstractFileTestCase;
import net.grinder.testutility.AssertUtilities;

import junit.framework.TestCase;


/**
 * Unit tests for {@link ExternalEditor}.
 *
 * @author Philip Aston
 * @version $Revision:$
 */
public class TestExternalEditor extends AbstractFileTestCase {

  private static final String s_testClasspath =
    System.getProperty("java.class.path");

  public void testFileToCommandLine() throws Exception {
    final File file = new File("lah");

    final ExternalEditor externalEditor1 =
      new ExternalEditor(null, "foo", "bah dah");
    final String[] result1 = externalEditor1.fileToCommandLine(file);

    AssertUtilities.assertArraysEqual(
      new String[] { "foo", "bah", "dah", file.getAbsolutePath(), },
      result1);


    final ExternalEditor externalEditor2 =
      new ExternalEditor(null, "foo", "-f '%f'");
    final String[] result2 = externalEditor2.fileToCommandLine(file);

    AssertUtilities.assertArraysEqual(
      new String[] { "foo", "-f", "'" + file.getAbsolutePath() + "'", },
      result2);


    final ExternalEditor externalEditor3 =
      new ExternalEditor(null, "foo", null);
    final String[] result3 = externalEditor3.fileToCommandLine(file);

    AssertUtilities.assertArraysEqual(
      new String[] { "foo", file.getAbsolutePath(), },
      result3);
  }

  public void testOpen() throws Exception {
    final long[] lastInvalidAfter = new long[1];

    final AgentCacheState cacheState =
      new AgentCacheState() {

        public void addListener(PropertyChangeListener listener) {}
        public boolean getOutOfDate() { return false; }

        public void setOutOfDate() {}

        public void setOutOfDate(long invalidAfter) {
          lastInvalidAfter[0] = invalidAfter;
        }};


    final ExternalEditor externalEditor1 =
      new ExternalEditor(cacheState,
                         "/usr/bin/java",
                         "-classpath " + s_testClasspath + " " +
                         TouchClass.class.getName() + " " +
                         TouchClass.TOUCH + " %f");

    final File file = new File(getDirectory(), "hello world");
    file.createNewFile();
    file.setLastModified(0);
    assertEquals(0, file.lastModified());

    externalEditor1.open(file);

    for (int i = 0;
         i < 20 && ExternalEditor.getThreadGroup().activeCount() > 0;
         ++i) {
      Thread.sleep(i * 10);
    }

    final long firstModification = file.lastModified();
    assertTrue(firstModification!= 0);
    assertEquals(firstModification, lastInvalidAfter[0]);


    // Try again, this time not editing.
    final ExternalEditor externalEditor2 =
      new ExternalEditor(cacheState,
                         "/usr/bin/java",
                         "-classpath " + s_testClasspath + " " +
                         TouchClass.class.getName() + " " +
                         TouchClass.NOOP + " %f");

    file.setLastModified(0);
    assertEquals(0, file.lastModified());

    externalEditor2.open(file);

    for (int i = 0;
         i < 20 && ExternalEditor.getThreadGroup().activeCount() > 0;
         ++i) {
      Thread.sleep(i * 10);
    }

    assertEquals(0, file.lastModified());
    assertEquals(firstModification, lastInvalidAfter[0]);


    // Once more, this time interrupting the process.

    final ExternalEditor externalEditor3 =
      new ExternalEditor(cacheState,
                         "/usr/bin/java",
                         "-classpath " + s_testClasspath + " " +
                         TouchClass.class.getName() + " " +
                         TouchClass.SLEEP + " %f");

    file.setLastModified(0);
    assertEquals(0, file.lastModified());

    externalEditor3.open(file);

    ExternalEditor.getThreadGroup().interrupt();

    assertEquals(0, file.lastModified());
    assertEquals(firstModification, lastInvalidAfter[0]);
  }
}
