// Copyright (C) 2005 Philip Aston
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

import org.apache.oro.text.regex.Pattern;
import org.apache.oro.text.regex.PatternMatcher;
import org.apache.oro.text.regex.Perl5Matcher;


/**
 * File Distribution Filter.
 *
 * @author Philip Aston
 * @version $Revision$
 */
final class FileDistributionFilter implements FileFilter {
  private final PatternMatcher m_matcher = new Perl5Matcher();
  private final Pattern m_distributionFileFilterPattern;
  private final long m_earliestTime;

  public FileDistributionFilter(Pattern distributionFileFilterPattern,
                                long earliestTime) {
    m_distributionFileFilterPattern = distributionFileFilterPattern;
    m_earliestTime = earliestTime;
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

      return file.lastModified() > m_earliestTime;
    }
  }
}
