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

package net.grinder.console.distribution;

import java.io.File;
import java.io.FileFilter;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import net.grinder.console.communication.DistributionControl;
import net.grinder.util.Directory;
import net.grinder.util.ListenerSupport;
import net.grinder.util.ListenerSupport.Informer;


/**
 * {@link FileDistribution} implementation.
 *
 * @author Philip Aston
 * @version $Revision$
 */
public final class FileDistributionImplementation implements FileDistribution {

  private final ListenerSupport m_filesChangedListeners = new ListenerSupport();

  private final DistributionControl m_distributionControl;
  private final UpdateableAgentCacheState m_cacheState;
  private long m_lastScanTime;

  private Directory m_lastDirectory;
  private Pattern m_lastFileFilterPattern;

  /**
   * Constructor.
   *
   * @param distributionControl A <code>DistributionControl</code>.
   */
  public FileDistributionImplementation(
    DistributionControl distributionControl) {
    this(distributionControl, new AgentCacheStateImplementation());
  }

  FileDistributionImplementation(DistributionControl distributionControl,
                   UpdateableAgentCacheState agentCacheState) {
    m_distributionControl = distributionControl;
    m_cacheState = agentCacheState;
  }


  /**
   * Accessor for our {@link AgentCacheState}.
   *
   * @return The agent cache state.
   */
  public AgentCacheState getAgentCacheState() {
    return m_cacheState;
  }

  /**
   * Get a {@link FileDistributionHandler} for a new file
   * distribution.
   *
   * <p>The FileDistributionHandler updates our simple model of the
   * remote cache state. Callers should only use one
   * FileDistributionHandler at a time for a given FileDistribution.
   * Using multiple instances concurrently will result in undefined
   * behaviour.</p>
   *
   * @param directory The base distribution directory.
   * @param distributionFileFilterPattern Current filter pattern.
   * @return Handler for new file distribution.
   */
  public FileDistributionHandler getHandler(
    Directory directory,
    Pattern distributionFileFilterPattern) {

    if (m_lastDirectory == null ||
        !m_lastDirectory.equals(directory) ||
        m_lastFileFilterPattern == null ||
        !m_lastFileFilterPattern.pattern().equals(
            distributionFileFilterPattern.pattern())) {

      m_cacheState.setOutOfDate();

      m_lastDirectory = directory;
      m_lastFileFilterPattern = distributionFileFilterPattern;
    }

    final long earliestFileTime = m_cacheState.getEarliestFileTime();

    if (earliestFileTime < 0) {
      m_distributionControl.clearFileCaches();
    }

    return new FileDistributionHandlerImplementation(
      directory.getFile(),
      directory.listContents(
        new FileDistributionFilter(distributionFileFilterPattern,
                                   earliestFileTime)),
      m_distributionControl,
      m_cacheState);
  }

  /**
   * Scan the given directory for files that have been recently modified. Update
   * the agent cache state appropriately.
   *
   * @param directory The directory to scan.
   */
  public void scanDistributionFiles(Directory directory) {
    final long now = System.currentTimeMillis();

    // We back up a little from m_lastScanTime to protect against
    // race conditions with processes that are modifying files.
    final long scanTime =
      Math.max(m_cacheState.getEarliestFileTime(), m_lastScanTime - 100);

    // Don't filter directories by time here, it would prevent listContents
    // from finding changes to files in directories.
    final FileFilter timeFilter = new FileFilter() {
        public boolean accept(File file) {
          return file.isDirectory() || file.lastModified() > scanTime;
        }
      };

    final File[] laterFiles = directory.listContents(timeFilter, true, true);

    m_lastScanTime = now;

    if (laterFiles.length > 0) {
      final Set changedFiles = new HashSet(laterFiles.length / 2);

      for (int i = 0; i < laterFiles.length; ++i) {
        final File laterFile = laterFiles[i];

        // We didn't filter directories by time when building up laterFiles,
        // do so now.
        if (laterFile.isDirectory() &&
            laterFile.lastModified() <= scanTime) {
          continue;
        }

        m_cacheState.setOutOfDate(laterFile.lastModified());
        changedFiles.add(laterFile);
      }

      final File[] changedFilesArray =
        (File[])changedFiles.toArray(new File[changedFiles.size()]);

      m_filesChangedListeners.apply(
        new Informer() {
          public void inform(Object listener) {
            ((FileChangedListener)listener).filesChanged(changedFilesArray);
          }
        });
    }
  }

  /**
   * Add a listener that will be sent events about files that have changed when
   * {@link #scanDistributionFiles} is called.
   *
   * @param listener
   *          The listener.
   */
  public void addFileChangedListener(FileChangedListener listener) {
    m_filesChangedListeners.add(listener);
  }
}
