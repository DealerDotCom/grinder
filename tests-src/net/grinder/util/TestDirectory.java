// Copyright (C) 2004 Philip Aston
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

package net.grinder.util;

import java.io.File;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import net.grinder.testutility.AbstractFileTestCase;
import net.grinder.testutility.AssertUtilities;
import net.grinder.testutility.FileUtilities;


/**
 * Unit test case for {@link Directory}.
 *
 * @author Philip Aston
 * @version $Revision$
 */
public class TestDirectory extends AbstractFileTestCase {

  public void testConstruction() throws Exception {

    final File file = new File(getDirectory(), "x");
    file.createNewFile();

    try {
      new Directory(file);
      fail("Expected DirectoryException");
    }
    catch (Directory.DirectoryException e) {
    }

    final Directory directory = new Directory(getDirectory());
    assertEquals(0, directory.getWarnings().length);

    assertEquals(getDirectory(), directory.getAsFile());
  }

  public void testEquality() throws Exception {

    final Directory d1 = new Directory(getDirectory());
    final Directory d2 = new Directory(getDirectory());

    final File f = new File(getDirectory(), "comeonpilgrimyouknowhelovesyou");
    f.mkdir();

    final Directory d3 = new Directory(f);

    assertEquals(d1, d1);
    assertEquals(d1, d2);
    AssertUtilities.assertNotEquals(d2, d3);

    assertEquals(d1.hashCode(), d1.hashCode());
    assertEquals(d1.hashCode(), d2.hashCode());

    AssertUtilities.assertNotEquals(d1, null);
    AssertUtilities.assertNotEquals(d1, f);
  }

  public void testListContents() throws Exception {

    final Directory directory = new Directory(getDirectory());

    final String[] files = {
      "first/three",
      "will-not-be-picked-up",
      "because/they/are/too/old",
      "directory/foo/bah/blah",
      "directory/blah",
      "a/b/c/d/e",
      "a/b/f/g/h",
      "a/b/f/g/i",
      "x",
      "y/z",
      "another",
    };

    final Set expected = new HashSet();

    for (int i=0; i<files.length; ++i) {
      final File file = new File(getDirectory(), files[i]);
      file.getParentFile().mkdirs();
      file.createNewFile();

      if (i < 3) {
        file.setLastModified(10000L * (i + 1));
      }
      else {
        // Result uses relative paths.
        expected.add(new File(files[i]));
      }
    }

    final File[] badDirectories = {
      new File(getDirectory(), "directory/foo/bah/blah.cantread"),
      new File(getDirectory(), "readonly"),
    };

    for (int i = 0; i < badDirectories.length; ++i) {
      badDirectories[i].getParentFile().mkdirs();
      badDirectories[i].mkdir();
      FileUtilities.setCanRead(badDirectories[i], false);
    }

    final File[] filesAfterTimeT = directory.listContents(50000L);

    for (int i=0; i<filesAfterTimeT.length; ++i) {
      assertTrue("Contains " + filesAfterTimeT[i],
                 expected.contains(filesAfterTimeT[i]));
    }

    final String[] warnings = directory.getWarnings();
    assertEquals(badDirectories.length, warnings.length);

    final StringBuffer warningsBuffer = new StringBuffer();

    for (int i = 0; i < warnings.length; ++i) {
      warningsBuffer.append(warnings[i]);
      warningsBuffer.append("\n");
    }

    final String warningsString = warningsBuffer.toString();

    for (int i = 0; i < badDirectories.length; ++i) {
      assertTrue(warningsBuffer + " contains " + badDirectories[i].getPath(),
                 warningsString.indexOf(badDirectories[i].getPath()) > -1);

      FileUtilities.setCanRead(badDirectories[i], true);
    }

    // Check that listContents() returns the lot.
    final File[] allFiles = directory.listContents();
    assertEquals(files.length, allFiles.length);
  }

  public void testDeleteContents() throws Exception {

    final Directory directory = new Directory(getDirectory());

    final String[] files = {
      "directory/foo/bah/blah",
      "directory/blah",
      "a/b/c/d/e",
      "a/b/f/g/h",
      "a/b/f/g/i",
      "x",
      "y/z",
      "another",
    };

    for (int i = 0; i < files.length; ++i) {
      final File file = new File(getDirectory(), files[i]);
      file.getParentFile().mkdirs();
      file.createNewFile();
    }
    
    assertTrue(getDirectory().list().length > 0);

    directory.deleteContents();

    assertEquals(0, getDirectory().list().length);

    // Can't test that deleteContents() throws an exception if
    // contents couldn't be deleted as File.delete() ignores file
    // permisions on W2K.
  }

  public void testCreate() throws Exception {
    final String[] directories = {
      "toplevel",
      "down/a/few",
    };

    for (int i=0; i<directories.length; ++i) {
      final Directory directory =
        new Directory(new File(getDirectory(), directories[i]));
      assertFalse(directory.getAsFile().exists());
      directory.create();
      assertTrue(directory.getAsFile().exists());
    }

    final File file = new File(getDirectory(), "readonly");
    file.createNewFile();
    FileUtilities.setCanRead(file, false);

    try {
      new Directory(new File(getDirectory(), "readonly/foo")).create();
      fail("Expected DirectoryException");
    }
    catch (Directory.DirectoryException e) {
    }
  }

  public void testDelete() throws Exception {
    final Directory directory1 =
      new Directory(new File(getDirectory(), "a/directory"));
    directory1.create();
    assertTrue(directory1.getAsFile().exists());
    directory1.delete();
    assertFalse(directory1.getAsFile().exists());

    final Directory directory2 =
      new Directory(new File(getDirectory(), "another"));
    directory2.create();
    final File file2 = new File(getDirectory(), "another/file");
    file2.createNewFile();

    try {
      directory2.delete();
      fail("Expected DirectoryException");
    }
    catch (Directory.DirectoryException e) {
    }
  }

  public void testGetRelativePath() throws Exception {
    final String[] files = {
      "path1",
      "some/other/path",
    };

    final Directory directory = new Directory(getDirectory());

    for (int i=0; i<files.length; ++i) {
      final File file = new File(getDirectory(), files[i]);
      file.getParentFile().mkdirs();
      file.createNewFile();

      final File result = directory.getRelativePath(file);
      assertFalse(result.isAbsolute());
      assertEquals(file, new File(getDirectory(), result.getPath()));
    }

    assertNull(directory.getRelativePath(null));
    assertNull(directory.getRelativePath(new File(getDirectory(), "foo")));    
  }
}
