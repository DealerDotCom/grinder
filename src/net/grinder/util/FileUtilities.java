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

import java.io.IOException;
import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;


/**
 * Utility methods for working with files.
 *
 * @author Philip Aston
 * @version $Revision$
 */
public final class FileUtilities  {

  private FileUtilities() {
  }

  /**
   * Delete the contents of a directory. The directory itself is not
   * removed.
   *
   * @param rootDirectory The directory.
   * @exception IOException If an error occurs.
   */
  public static void deleteContents(File rootDirectory) throws IOException {

    final List deleteList = new ArrayList();
    final Set visited = new HashSet();
    final Set toVisit = new HashSet();
    toVisit.add(rootDirectory);

    while (toVisit.size() > 0) {
      final File[] directories =
        (File[]) toVisit.toArray(new File[toVisit.size()]);

      for (int i = 0; i < directories.length; ++i) {
        final File directory = directories[i];

        if (!directory.isDirectory()) {
          throw new FileUtilitiesException(directory.getPath() +
                                           " is not a directory");
        }

        toVisit.remove(directory);
        visited.add(directory);

        final File[] children = directory.listFiles();

        for (int j = 0; j < children.length; ++j) {
          final File child = children[j];

          deleteList.add(child);

          if (child.isDirectory() && !visited.contains(child)) {
            toVisit.add(child);
          }
        }
      }
    }

    final ListIterator deleteIterator =
      deleteList.listIterator(deleteList.size());

    while (deleteIterator.hasPrevious()) {
      final File victim = (File)deleteIterator.previous();
      victim.delete();
    }
  }

  /**
   * An exception type used to report FileUtilities related problems.
   */
  public static final class FileUtilitiesException extends IOException {
    FileUtilitiesException(String message) {
      super(message);
    }
  }
}
