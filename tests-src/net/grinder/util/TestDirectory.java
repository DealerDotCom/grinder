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


/**
 * Unit test case for {@link Directory}.
 *
 * @author Philip Aston
 * @version $Revision$
 */
public class TestDirectory extends AbstractFileTestCase {

  public void testConstruction() throws Exception {

    try {
      new Directory(new File(getDirectory(), "x"));
      fail("Expected DirectoryException");
    }
    catch (Directory.DirectoryException e) {
    }
  }

  public void testListContents() throws Exception {

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

    final Set expected = new HashSet();

    for (int i=0; i<files.length; ++i) {
      final File file = new File(getDirectory(), files[i]);
      file.getParentFile().mkdirs();
      file.createNewFile();

      // Result uses relative paths.
      expected.add(new File(files[i]));
    }
    
    final File[] allFiles = directory.listContents();

    for (int i=0; i<allFiles.length; ++i) {
      expected.remove(allFiles[i]);
    }

    assertEquals(0, expected.size());
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

    for (int i=0; i<files.length; ++i) {
      final File file = new File(getDirectory(), files[i]);
      file.getParentFile().mkdirs();
      file.createNewFile();
    }
    
    assertTrue(getDirectory().list().length > 0);

    directory.deleteContents();

    assertEquals(0, getDirectory().list().length);
  }

  public void testToFileContentsArray() throws Exception {

    final Directory directory = new Directory(getDirectory());
    assertEquals(0, directory.toFileContentsArray().length);

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

    final Map expected = new HashMap();

    for (int i=0; i<files.length; ++i) {
      final File file = new File(getDirectory(), files[i]);
      file.getParentFile().mkdirs();
      final FileOutputStream out = new FileOutputStream(file);
      final String contents = "Contents of " + files[i];
      out.write(contents.getBytes());
      out.close();

      // Result uses relative paths.
      expected.put(new File(files[i]), contents);
    }

    final FileContents[] fileContentsList = directory.toFileContentsArray();

    for (int i=0; i<fileContentsList.length; ++i) {
      final FileContents fileContents = fileContentsList[i];

      final String expectedContents =
        (String)expected.get(fileContents.getFilename());
      assertNotNull(expectedContents);
      assertEquals(expectedContents, new String(fileContents.getContents()));
    }
  }
}
