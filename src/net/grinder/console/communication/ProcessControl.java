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

package net.grinder.console.communication;

import java.io.File;

import net.grinder.util.Directory;
import net.grinder.util.FileContents;


/**
 * Interface for issuing commands to the agent and worker processes.
 *
 * @author Philip Aston
 * @version $Revision$
 */
public interface ProcessControl {

  /**
   * Signal the worker processes to start.
   *
   * @param script The script file to run.
   */
  void startWorkerProcesses(File script);

  /**
   * Signal the worker processes to reset.
   */
  void resetWorkerProcesses();

  /**
   * Signal the worker processes to stop.
   */
  void stopWorkerProcesses();

  /**
   * Get a {@link FileDistributionHandler} which will handle the
   * distribution of files from the given directory.
   *
   * @param  directory The directory.
   * @return The distribution handler.
   */
  FileDistributionHandler getFileDistributionHandler(Directory directory);

  /**
   * Add a listener for process status data.
   *
   * @param listener The listener.
   */
  void addProcessStatusListener(ProcessStatusListener listener);

  /**
   * Something that can handle the distribution of files.
   */
  public interface FileDistributionHandler {

    /**
     * Estimate of the number of files to process.
     *
     * @return How many.
     */
    int getNumberOfFiles();

    /**
     * Send the next file.
     *
     * @return <code>true</code> => there are more files to process.
     * @throws FileContents.FileContentsException If an error occurs
     * sending the file.
     */
    boolean sendNextFile() throws FileContents.FileContentsException;

    /**
     * Get the name of the next file to distribute.
     *
     * @return The name of the next file.
     */
    String getNextFileName();
  }
}
