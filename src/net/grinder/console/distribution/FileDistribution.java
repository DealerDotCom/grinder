// Copyright (C) 2005 Philip Aston
// All rights reserved.
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
import java.util.EventListener;
import java.util.regex.Pattern;

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
public interface FileDistribution {

  /**
   * Accessor for our {@link AgentCacheState}.
   *
   * @return The agent cache state.
   */
  AgentCacheState getAgentCacheState();

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
  FileDistributionHandler getHandler(Directory directory,
    Pattern distributionFileFilterPattern);

  /**
   * Scan the given directory for files that have been recently modified. Update
   * the agent cache state appropriately.
   *
   * @param directory The directory to scan.
   */
  void scanDistributionFiles(Directory directory);

  /**
   * Add a listener that will be sent events about files that have changed when
   * {@link scanDistributionFiles} is called.
   *
   * @param listener
   *          The listener.
   */
  void addFilesChangedListener(FilesChangedListener listener);


  /**
   * ChangedFilesListener..
   *
   * @see FileDistribution#addFilesChangedListener(FilesChangedListener)
   */
  interface FilesChangedListener extends EventListener {

    /**
     * Called with a changed file.
     *
     * @param file The file that has changed.
     */
    void filesChanged(File[] file);
  }
}
