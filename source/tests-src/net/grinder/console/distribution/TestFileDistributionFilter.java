// Copyright (C) 2005 Philip Aston
// Copyright (C) 2005 Martin Wagner
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

package net.grinder.console.distribution;

import java.io.File;
import java.io.FileFilter;
import java.util.regex.Pattern;

import net.grinder.testutility.AbstractFileTestCase;


/**
 * Unit test for {@link FileDistributionFilter}.
 *
 * @author Philip Aston
 * @version $Revision$
 */
public class TestFileDistributionFilter extends AbstractFileTestCase {

  public void testFilter() throws Exception {
    final Pattern pattern = Pattern.compile("^a.*[^/]$|.*exclude.*|.*b/$");

    final FileFilter filter = new FileDistributionFilter(pattern, 100000L);

    final String[] acceptableFilenames = new String[] {
      "DoesntStartWithA.acceptable",
      "blah blah blah",
      "blah-file-store",
    };

    for (int i = 0; i < acceptableFilenames.length; ++i) {
      final File f = new File(getDirectory(), acceptableFilenames[i]);
      f.createNewFile();
      assertTrue(f.getPath() + " is acceptable", filter.accept(f));
    }

    final String[] unacceptableFileNames = new String[] {
      "exclude me",
      "a file beginning with a",
      "a directory ending with b",
    };

    for (int i = 0; i < unacceptableFileNames.length; ++i) {
      final File f = new File(getDirectory(), unacceptableFileNames[i]);
      f.createNewFile();

      assertTrue(f.getPath() + " is unacceptable", !filter.accept(f));
    }

    final File timeFile = new File(getDirectory(), "time file");
    timeFile.createNewFile();
    assertTrue(timeFile.getPath() + " is acceptable", filter.accept(timeFile));
    timeFile.setLastModified(123L);
    assertTrue(timeFile.getPath() + " is unacceptable",
               !filter.accept(timeFile));

    // Add an error margin, as Linux does not support setting the modification
    // date with millisecond precision.
    timeFile.setLastModified(101001L);
    assertTrue(timeFile.getPath() + " is acceptable", filter.accept(timeFile));

    final String[] acceptableDirectoryNames = new String[] {
      "a directory ending with b.not",
      "include me",
    };

    for (int i = 0; i < acceptableDirectoryNames.length; ++i) {
      final File f = new File(getDirectory(), acceptableDirectoryNames[i]);
      f.mkdir();
      assertTrue(f.getPath() + " is acceptable", filter.accept(f));
    }

    final String[] unacceptableDirectoryNames = new String[] {
      "a directory ending with b",
      "exclude me",
    };

    for (int i = 0; i < unacceptableDirectoryNames.length; ++i) {
      final File f = new File(getDirectory(), unacceptableDirectoryNames[i]);
      f.mkdir();
      assertTrue(f.getPath() + " is unacceptable", !filter.accept(f));
    }

    final File timeDirectory = new File(getDirectory(), "time directory");
    timeDirectory.mkdir();
    assertTrue(timeDirectory.getPath() + " is acceptable",
               filter.accept(timeDirectory));
    timeDirectory.setLastModified(123L);
    assertTrue(timeDirectory.getPath() + " is acceptable",
               filter.accept(timeDirectory));

    final File fileStoreDirectory = new File(getDirectory(), "foo-file-store");
    fileStoreDirectory.mkdir();
    assertTrue(fileStoreDirectory.getPath() + " is acceptable",
               filter.accept(fileStoreDirectory));

    final File readMeFile = new File(fileStoreDirectory, "README.txt");
    readMeFile.createNewFile();
    assertTrue(fileStoreDirectory.getPath() + " is unacceptable",
               !filter.accept(fileStoreDirectory));

    readMeFile.delete();
    assertTrue(fileStoreDirectory.getPath() + " is acceptable",
               filter.accept(fileStoreDirectory));
  }
}
