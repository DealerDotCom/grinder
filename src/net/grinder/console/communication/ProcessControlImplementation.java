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

import net.grinder.communication.Message;
import net.grinder.console.messages.ReportStatusMessage;
import net.grinder.engine.messages.DistributeFileMessage;
import net.grinder.engine.messages.ResetGrinderMessage;
import net.grinder.engine.messages.StartGrinderMessage;
import net.grinder.engine.messages.StopGrinderMessage;
import net.grinder.util.Directory;
import net.grinder.util.FileContents;


/**
 * Issue commands to the agent and worker processes.
 *
 * @author Philip Aston
 * @version $Revision$
 */
final class ProcessControlImplementation implements ProcessControl {

  private final ConsoleCommunication m_communication;
  private final ProcessStatusSet m_processStatusSet;
  private final DistributionStatus m_distributionStatus;

  ProcessControlImplementation(ConsoleCommunication consoleCommunication,
                               ProcessStatusSet processStatusSet,
                               DistributionStatus distributionStatus) {
    m_communication = consoleCommunication;
    m_processStatusSet = processStatusSet;
    m_distributionStatus = distributionStatus;

    consoleCommunication.addMessageHandler(
      new ConsoleCommunication.MessageHandler() {
        public boolean process(Message message) {
          if (message instanceof ReportStatusMessage) {
            m_processStatusSet.addStatusReport((ReportStatusMessage)message);
            return true;
          }

          return false;
        }
      });

    m_processStatusSet.startProcessing();
  }

  /**
   * Signal the worker processes to start.
   */
  public void startWorkerProcesses() {
    m_processStatusSet.processEvent();
    m_communication.send(new StartGrinderMessage(null));
  }

  /**
   * Signal the worker processes to reset.
   */
  public void resetWorkerProcesses() {
    m_processStatusSet.processEvent();
    m_communication.send(new ResetGrinderMessage());
  }

  /**
   * Signal the worker processes to stop.
   */
  public void stopWorkerProcesses() {
    m_processStatusSet.processEvent();
    m_communication.send(new StopGrinderMessage());
  }

  /**
   * Add a listener for process status data.
   *
   * @param listener The listener.
   */
  public void addProcessStatusListener(ProcessStatusListener listener) {
    m_processStatusSet.addListener(listener);
  }

  /**
   * Get a {@link FileDistributionHandler} which will handle the
   * distribution of files from the given directory.
   *
   * @param  directory The directory.
   * @return The distribution handler.
   */
  public FileDistributionHandler getFileDistributionHandler(
    Directory directory) {

    return new FileDistributionHandlerImplementation(directory.getAsFile(),
                                                     directory.listContents());
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

    public int getNumberOfFiles() {
      return m_files.length;
    }

    public boolean sendNextFile() throws FileContents.FileContentsException {

      if (m_fileIndex < getNumberOfFiles()) {
        try {
          m_communication.send(
            new DistributeFileMessage(
              new FileContents(m_directory, m_files[m_fileIndex])));
        }
        finally {
          ++m_fileIndex;
        }

        return true;
      }

      m_distributionStatus.setAll(System.currentTimeMillis());
      return false;
    }

    public String getNextFileName() {
      if (m_fileIndex < m_files.length) {
        return m_files[m_fileIndex].getPath();
      }

      return "";
    }
  }
}
