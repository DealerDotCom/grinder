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

import net.grinder.engine.messages.DistributeFilesMessage;
import net.grinder.engine.messages.ResetGrinderMessage;
import net.grinder.engine.messages.StartGrinderMessage;
import net.grinder.engine.messages.StopGrinderMessage;
import net.grinder.util.FileContents;


/**
 * Interface for issuing commands to the agent and worker processes.
 *
 * @author Philip Aston
 * @version $Revision$
 */
final class ProcessControlImplementation implements ProcessControl {

  private final ConsoleCommunication m_communication;

  ProcessControlImplementation(ConsoleCommunication consoleCommunication) {
    m_communication = consoleCommunication;
  }

  /**
   * Signal the worker processes to start.
   */
  public void startWorkerProcesses() {
    m_communication.getProcessStatusSet().processEvent();
    m_communication.send(new StartGrinderMessage(null));
  }

  /**
   * Signal the worker processes to reset.
   */
  public void resetWorkerProcesses() {
    m_communication.getProcessStatusSet().processEvent();
    m_communication.send(new ResetGrinderMessage());
  }

  /**
   * Signal the worker processes to stop.
   */
  public void stopWorkerProcesses() {
    m_communication.getProcessStatusSet().processEvent();
    m_communication.send(new StopGrinderMessage());
  }

  /**
   * Distribute a list of {@link FileContents}.
   *
   * @param files The {@link FileContents} list.
   */
  public void distributeFiles(FileContents[] files) {
    m_communication.send(new DistributeFilesMessage(files));
  }
}
