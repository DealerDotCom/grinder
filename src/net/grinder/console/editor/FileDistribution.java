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

package net.grinder.console.editor;

import java.io.File;
import java.io.FileFilter;
import java.util.HashSet;
import java.util.Set;

import org.apache.oro.text.regex.Pattern;
import org.apache.oro.text.regex.PatternMatcher;
import org.apache.oro.text.regex.Perl5Matcher;

import net.grinder.console.communication.DistributionControl;
import net.grinder.util.Directory;
import net.grinder.util.FileContents;


/**
 * File Distribution.
 *
 * @author Philip Aston
 * @version $Revision$
 */
public final class FileDistribution {

  private final DistributionControl m_distributionControl;

  private long m_earliestLastModifiedTime = -1;
  private Set m_lastConnectedAgents = new HashSet();
  private Directory m_lastDirectory;

  /**
   * Constructor.
   *
   * @param distributionControl A <code>DistributionControl</code>.
   */
  public FileDistribution(DistributionControl distributionControl) {
    m_distributionControl = distributionControl;
  }

  /**
   * Get a {@link FileDistributionHandler} for a new file
   * distribution.
   *
   * @param directory The base distribution directory.
   * @param distributionFileFilterPattern Current filter pattern.
   * @return Handler for new file distribution.
   */
  public FileDistributionHandler getHandler(
    Directory directory,
    Pattern distributionFileFilterPattern) {

    final Set connectedAgents = m_distributionControl.getConnectedAgents();

    if (m_lastDirectory == null ||
        !m_lastDirectory.equals(directory) ||
        !m_lastConnectedAgents.containsAll(connectedAgents)) {
      m_distributionControl.clearFileCaches();
      m_earliestLastModifiedTime = -1;
    }

    m_lastDirectory = directory;
    m_lastConnectedAgents = connectedAgents;

    return new FileDistributionHandlerImplementation(
      directory.getAsFile(),
      directory.listContents(new Filter(distributionFileFilterPattern)));
  }

  /**
   * Package scope for the unit tests.
   */
  final class Filter implements FileFilter {
    private final PatternMatcher m_matcher = new Perl5Matcher();
    private final Pattern m_distributionFileFilterPattern;

    public Filter(Pattern distributionFileFilterPattern) {
      m_distributionFileFilterPattern = distributionFileFilterPattern;
    }

    public boolean accept(File file) {
      final String name = file.getName();

      if (file.isDirectory()) {
        if (m_matcher.contains(name + "/", m_distributionFileFilterPattern)) {
          return false;
        }

        if (name.endsWith("-file-store")) {
          final File readmeFile = new File(file, "README.txt");

          if (readmeFile.isFile()) {
            return false;
          }
        }

        return true;
      }
      else {
        if (m_matcher.contains(name, m_distributionFileFilterPattern)) {
          return false;
        }

        return file.lastModified() > m_earliestLastModifiedTime;
      }
    }
  }

  private final class FileDistributionHandlerImplementation
    implements FileDistributionHandler {

    private final File m_directory;
    private final File[] m_files;
    private int m_fileIndex = 0;

    FileDistributionHandlerImplementation(File directory, File[] files) {
      m_directory = directory;
      m_files = files;
    }

    public Result sendNextFile() throws FileContents.FileContentsException {

      if (m_fileIndex < m_files.length) {
        try {
          final int index = m_fileIndex;
          final File file = m_files[index];

          m_distributionControl.sendFile(new FileContents(m_directory, file));

          return new Result() {
              public int getProgressInCents() {
                return (index * 100) / m_files.length;
              }

              public String getFileName() {
                return file.getPath();
              }
            };
        }
        finally {
          ++m_fileIndex;
        }
      }

      m_earliestLastModifiedTime = System.currentTimeMillis();

      return null;
    }
  }
}
