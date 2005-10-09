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

import java.util.regex.Pattern;

import net.grinder.console.communication.DistributionControl;
import net.grinder.util.Directory;


/**
 * File Distribution. Has a model of the agent cache state, and is a
 * factory for {@link FileDistributionHandler}s.
 *
 * <p>The agent cache state is reset if the parameters passed to
 * {@link #getHandler} change. Client code can actively invalidate the
 * agent cache state by calling .{@link
 * AgentCacheStateImplementation#setOutOfDate} on the result of {@link
 * #getAgentCacheState}. They may want to do this, for example, if a
 * parameter they will pass to {@link #getHandler} changes and they are
 * using events from the {@link AgentCacheState} to update a UI.</p>
 *
 * @author Philip Aston
 * @version $Revision$
 */
public final class FileDistribution {

  private final DistributionControl m_distributionControl;
  private final AgentCacheStateImplementation m_cacheState =
    new AgentCacheStateImplementation();

  private Directory m_lastDirectory;
  private Pattern m_lastFileFilterPattern;

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
   * Used to update the cache state.
   */
  interface UpdateAgentCacheState {
    void updateStarted();
    void updateComplete();
  }
}
