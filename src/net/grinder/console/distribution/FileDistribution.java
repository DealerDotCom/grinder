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

import java.util.Set;

import org.apache.oro.text.regex.Pattern;

import net.grinder.console.communication.DistributionControl;
import net.grinder.util.Directory;


/**
 * File Distribution. Has a model of the agent cache state, and is a
 * factory for {@link FileDistributionHandler}s.
 *
 * @author Philip Aston
 * @version $Revision$
 */
public final class FileDistribution {

  private final DistributionControl m_distributionControl;
  private final AgentCacheStateImplementation m_cacheState =
    new AgentCacheStateImplementation();

  /**
   * Constructor.
   *
   * @param distributionControl A <code>DistributionControl</code>.
   */
  public FileDistribution(DistributionControl distributionControl) {
    m_distributionControl = distributionControl;
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

    if (!m_cacheState.validate(directory,
                               distributionFileFilterPattern,
                               connectedAgents)) {
      m_distributionControl.clearFileCaches();
    }

    return new FileDistributionHandlerImplementation(
      directory.getAsFile(),
      directory.listContents(
        new FileDistributionFilter(distributionFileFilterPattern,
                                   m_cacheState.getEarliestFileTime())),
      m_distributionControl,
      m_cacheState);
  }

  /**
   * Used to update the cache state.
   */
  interface UpdateAgentCacheState {
    void updateStarted();
    void updateComplete();
  }

  /**
   * Package scope for the unit tests.
   */
  static final class AgentCacheStateImplementation
    implements AgentCacheState, UpdateAgentCacheState {

    private static final int UP_TO_DATE = 0;
    private static final int UPDATING = 1;
    private static final int OUT_OF_DATE = 2;

    private Directory m_directory;
    private Pattern m_fileFilterPattern;
    private Set m_connectedAgents;

    private int m_state = OUT_OF_DATE;
    private long m_earliestFileTime = -1;
    private long m_updateStartTime = -1;

    public AgentCacheStateImplementation() {
      m_directory = null;
      m_fileFilterPattern = null;
      m_connectedAgents = null;
    }

    public boolean validate(Directory directory,
                            Pattern fileFilterPattern,
                            Set connectedAgents) {
      if (m_directory == null ||
          !m_directory.equals(directory) ||
          m_fileFilterPattern == null ||
          !m_fileFilterPattern.equals(fileFilterPattern) ||
          m_connectedAgents == null ||
          !m_connectedAgents.containsAll(connectedAgents)) {

        m_directory = directory;
        m_fileFilterPattern = fileFilterPattern;
        m_connectedAgents = connectedAgents;
        return false;
      }
      else {
        m_connectedAgents = connectedAgents;
        return true;
      }
    }

    public long getEarliestFileTime() {
      return m_earliestFileTime;
    }

    public boolean getOutOfDate() {
      return UP_TO_DATE != m_state;
    }

    public void setOutOfDate() {
      m_state = OUT_OF_DATE;
    }

    public void updateStarted() {
      m_state = UPDATING;
      m_updateStartTime = System.currentTimeMillis();
    }

    public void updateComplete() {

      if (m_state == UPDATING) {
        // Only mark clean if we haven't been marked out of date
        // during the update.
        m_state = UP_TO_DATE;
      }

      // Even if we're not up to date, we've at least transfered all
      // files older than this time.
      m_earliestFileTime = m_updateStartTime;
    }
  }
}
