// Copyright (C) 2005, 2006, 2007 Philip Aston
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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
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

  // Guarded by this.
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

    m_cacheState.addListener(new PropertyChangeListener() {
      public void propertyChange(PropertyChangeEvent event) {
        if (m_cacheState.getEarliestFileTime() < 0) {
          synchronized (FileDistributionImplementation.this) {
            m_lastScanTime = -1;
          }
        }
      }
    });
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

    // Scan to ensure we've seen what we're about to distribute and don't
    // invalidate the cache immediately after distribution.
    scanDistributionFiles(directory, distributionFileFilterPattern);

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
   * the agent cache state appropriately. Notify our listeners if changed files
   * are discovered.
   *
   * <p>
   * This method is too coupled to the agent cache. Perhaps this and the file
   * watcher support should be factored out into a separate class. Currently,
   * the file listeners only get notification for things that match the
   * distribution filter. This causes some very minor bugs, e.g. the editor file
   * tree won't refresh for changes to the contents of CVS directories. On the
   * other hand, if the agent cache was receiving notifications, it would have
   * to apply the distribution filter itself - to do this right would require
   * filtering the contents of directories that don't pass the distribution
   * filter, which is a pain to do efficiently.
   * </p>
   *
   * @param directory
   *          The directory to scan.
   * @param distributionFileFilterPattern
   *          Current filter pattern.
   */
  public void scanDistributionFiles(
    Directory directory,
    Pattern distributionFileFilterPattern) {

    final long scanTime;

    synchronized (this) {
      scanTime = m_lastScanTime;
    }

    // We only work with times obtained from the file system. This avoids
    // problems due to differences between the system clock and whatever the
    // (potentially remote) file system uses to generate timestamps. It also
    // avoids problems due to accuracy of file timestamps.
    try {
      // We create our temporary file below the given directory so we can be
      // fairly sure it's on the same file system. However, we don't want the
      // root directory timestamp to be constantly changing, so we create
      // files in a more long-lived working directory.
      final File privateDirectory = new File(directory.getFile(), ".grinder");
      privateDirectory.mkdir();

      final File temporaryFile =
        File.createTempFile(".scantime", "", privateDirectory);
      temporaryFile.deleteOnExit();
      synchronized (this) {
        m_lastScanTime = temporaryFile.lastModified();
      }
      temporaryFile.delete();
    }
    catch (IOException e) {
      synchronized (this) {
        m_lastScanTime = System.currentTimeMillis() - 1000;
      }
    }

    final File[] laterFiles =
      directory.listContents(
        new FileDistributionFilter(distributionFileFilterPattern, scanTime),
        true, true);

    if (laterFiles.length > 0) {
      final Set changedFiles = new HashSet(laterFiles.length / 2);

      for (int i = 0; i < laterFiles.length; ++i) {
        final File laterFile = laterFiles[i];

        // We didn't filter directories by time when building up laterFiles,
        // do so now.
        if (laterFile.isDirectory() &&
            laterFile.lastModified() < scanTime) {
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
