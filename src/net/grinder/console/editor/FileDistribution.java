// Copyright (C) 2004, 2005 Philip Aston
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

import java.util.Set;

import org.apache.oro.text.regex.Pattern;

import net.grinder.console.communication.DistributionControl;
import net.grinder.util.Directory;


/**
 * File Distribution.
 *
 * @author Philip Aston
 * @version $Revision$
 */
public final class FileDistribution {

  private final DistributionControl m_distributionControl;
  private CacheStateImplementation m_cacheState =
    new CacheStateImplementation();

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
   * <p>The FileDistributionHandler updates our simple model of the
   * remote cache state. Callers should only use one
   * FileDistributionHandler at a time. Using multiple instances
   * concurrently will result in undefined behaviour.</p>
   *
   * @param directory The base distribution directory.
   * @param distributionFileFilterPattern Current filter pattern.
   * @return Handler for new file distribution.
   */
  public FileDistributionHandler getHandler(
    Directory directory,
    Pattern distributionFileFilterPattern) {

    final Set connectedAgents = m_distributionControl.getConnectedAgents();

    if (!m_cacheState.isValid(directory,
                              distributionFileFilterPattern,
                              connectedAgents)) {
      m_distributionControl.clearFileCaches();
      m_cacheState =
        new CacheStateImplementation(directory,
                                     distributionFileFilterPattern,
                                     connectedAgents);
    }

    return new FileDistributionHandlerImplementation(
      directory.getAsFile(),
      directory.listContents(
        new FileDistributionFilter(distributionFileFilterPattern,
                                   m_cacheState.getTimeLastUpdateCompleted())),
      m_distributionControl,
      m_cacheState);
  }

  /**
   * Simplistic model of remote caches.
   */
  interface CacheState {
    long getTimeLastUpdateCompleted();
    void updateComplete();
  }

  /**
   * Package scope for the unit tests.
   */
  static class CacheStateImplementation implements CacheState {
    private final Directory m_directory;
    private final Pattern m_fileFilterPattern;
    private final Set m_connectedAgents;

    private long m_timeLastUpdateCompleted = -1;

    public CacheStateImplementation() {
      m_directory = null;
      m_fileFilterPattern = null;
      m_connectedAgents = null;
    }

    public CacheStateImplementation(Directory directory,
                                    Pattern fileFilterPattern,
                                    Set connectedAgents) {
      m_directory = directory;
      m_fileFilterPattern = fileFilterPattern;
      m_connectedAgents = connectedAgents;
    }

    public boolean isValid(Directory directory,
                           Pattern fileFilterPattern,
                           Set connectedAgents) {
      if (m_directory == null ||
          m_fileFilterPattern == null ||
          m_connectedAgents == null) {
        return false;
      }

      return
        m_directory.equals(directory) &&
        m_fileFilterPattern.equals(fileFilterPattern) &&
        m_connectedAgents.containsAll(connectedAgents);
    }

    public long getTimeLastUpdateCompleted() {
      return m_timeLastUpdateCompleted;
    }

    public void updateComplete() {
      m_timeLastUpdateCompleted = System.currentTimeMillis();
    }
  }
}
