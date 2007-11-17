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
import java.io.FileFilter;
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

  private static final String PRIVATE_DIRECTORY_NAME = ".grinder";

  private final ListenerSupport m_filesChangedListeners = new ListenerSupport();

  private final DistributionControl m_distributionControl;
  private final UpdateableAgentCacheState m_cacheState;

  // Guarded by this.
  private long m_lastScanTime;

  // Guarded by this.
  private Directory m_directory;

  // Guarded by this.
  private DistributionFileFilter m_distributionFilter;

  /**
   * Constructor.
   *
   * @param distributionControl A <code>DistributionControl</code>.
   * @param directory The base distribution directory.
   * @param distributionFileFilterPattern -
   *            The filter. Files with names that match this pattern will be
   *            filtered out.
   */
  public FileDistributionImplementation(
    DistributionControl distributionControl,
    Directory directory,
    Pattern distributionFileFilterPattern) {
    this(distributionControl,
         new AgentCacheStateImplementation(),
         directory,
         distributionFileFilterPattern);
  }

  /**
   * <p>Package scope for unit tests.</p>
   */
  FileDistributionImplementation(DistributionControl distributionControl,
                                 UpdateableAgentCacheState agentCacheState,
                                 Directory directory,
                                 Pattern distributionFileFilterPattern) {

    m_distributionControl = distributionControl;
    m_cacheState = agentCacheState;
    setDirectory(directory);
    setFileFilterPattern(distributionFileFilterPattern);

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
   * Update the distribution directory.
   *
   * @param directory The base distribution directory.
   */
  public void setDirectory(Directory directory) {
    synchronized (this) {
      m_directory = directory;
    }

    m_cacheState.setOutOfDate();
  }

  /**
   * Update the pattern used to filter out files that shouldn't be distributed.
   *
   * @param distributionFileFilterPattern -
   *            The filter. Files with names that match this pattern will be
   *            filtered out.
   */
  public void setFileFilterPattern(Pattern distributionFileFilterPattern) {
    synchronized (this) {
      m_distributionFilter =
        new DistributionFileFilter(distributionFileFilterPattern);
    }

    m_cacheState.setOutOfDate();
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
   * @return Handler for new file distribution.
   */
  public FileDistributionHandler getHandler() {

    // Scan to ensure we've seen what we're about to distribute and don't
    // invalidate the cache immediately after distribution.
    scanDistributionFiles();

    final long earliestFileTime = m_cacheState.getEarliestFileTime();

    if (earliestFileTime < 0) {
      m_distributionControl.clearFileCaches();
    }

    synchronized (this) {
      m_distributionFilter.setEarliestTime(earliestFileTime);

      return new FileDistributionHandlerImplementation(
        m_directory.getFile(),
        m_directory.listContents(m_distributionFilter),
        m_distributionControl,
        m_cacheState);
    }
  }

  /**
   * Scan the given directory for files that have been recently modified. Update
   * the agent cache state appropriately. Notify our listeners if changed files
   * are discovered.
   *
   * <p>
   * This method is too coupled to the agent cache. Perhaps this and the file
   * watcher support should be factored out into a separate class.
   * </p>
   *
   * <p>
   * Currently, the file listeners only get notification for things that match
   * the distribution filter.
   * </p>
   */
  public void scanDistributionFiles() {

    final long scanTime;
    final Directory directory;
    final DistributionFileFilter filter;

    synchronized (this) {
      scanTime = m_lastScanTime;
      directory = m_directory;
      filter = m_distributionFilter;
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
      final File privateDirectory =
        new File(directory.getFile(), PRIVATE_DIRECTORY_NAME);
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

    filter.setEarliestTime(scanTime);

    final File[] laterFiles = directory.listContents(filter, true, true);

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

  /**
   * Package scope for unit tests.
   */
  static final class DistributionFileFilter implements FileFilter {
    private final Pattern m_distributionFileFilterPattern;
    private long m_earliestTime = -1;

    public DistributionFileFilter(Pattern distributionFileFilterPattern) {
      m_distributionFileFilterPattern = distributionFileFilterPattern;
    }

    public void setEarliestTime(long earliestTime) {
      m_earliestTime = earliestTime;
    }

    public boolean accept(File file) {
      final String name = file.getName();

      if (file.isDirectory()) {
        if (name.equals(PRIVATE_DIRECTORY_NAME)) {
          return false;
        }

        if (name.endsWith("-file-store")) {
          final File readmeFile = new File(file, "README.txt");

          if (readmeFile.isFile()) {
            return false;
          }
        }

        if (m_distributionFileFilterPattern.matcher(name + "/").matches()) {
          return false;
        }

        return true;
      }
      else {
        if (m_distributionFileFilterPattern.matcher(name).matches()) {
          return false;
        }

        return file.lastModified() >= m_earliestTime;
      }
    }
  }


  /**
   * Return a FileFilter that can be used to test  whether the given file is
   * one that will be distributed.
   *
   * @return The filter.
   */
  public FileFilter getDistributionFileFilter() {
    // We don't simple return m_distributionFilter, since it can change.

    return new FileFilter() {
      public boolean accept(File file) {
        synchronized (FileDistributionImplementation.this) {
          m_distributionFilter.setEarliestTime(-1);
          return m_distributionFilter.accept(file);
        }
      }
    };
  }
}
