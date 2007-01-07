// Copyright (C) 2007 Philip Aston
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
import java.util.Timer;

import net.grinder.communication.Message;
import net.grinder.communication.MessageDispatchRegistry;
import net.grinder.communication.MessageDispatchRegistry.AbstractHandler;
import net.grinder.console.messages.AgentProcessReportMessage;
import net.grinder.console.messages.WorkerProcessReportMessage;
import net.grinder.engine.messages.ResetGrinderMessage;
import net.grinder.engine.messages.StartGrinderMessage;
import net.grinder.engine.messages.StopGrinderMessage;


/**
 * Implementation of {@link ProcessControl}.
 *
 * @author Philip Aston
 * @version $Revision:$
 */
public class ProcessControlImplementation implements ProcessControl {

  private final ConsoleCommunication m_consoleCommunication;

  private final ProcessStatusImplementation m_processStatusSet;

  /**
   * Constructor.
   *
   * @param timer
   *          Timer that can be used to schedule housekeeping tasks.
   * @param consoleCommunication
   *          The console communication handler.
   */
  public ProcessControlImplementation(
    Timer timer,
    ConsoleCommunication consoleCommunication) {

    m_consoleCommunication = consoleCommunication;
    m_processStatusSet = new ProcessStatusImplementation(timer);

    final MessageDispatchRegistry messageDispatchRegistry =
      consoleCommunication.getMessageDispatchRegistry();

    messageDispatchRegistry.set(
      AgentProcessReportMessage.class,
      new AbstractHandler() {
        public void send(Message message) {
          m_processStatusSet.addAgentStatusReport(
            (AgentProcessReportMessage)message);
        }
      }
    );

    messageDispatchRegistry.set(
      WorkerProcessReportMessage.class,
      new AbstractHandler() {
        public void send(Message message) {
          m_processStatusSet.addWorkerStatusReport(
            (WorkerProcessReportMessage)message);
        }
      }
    );
  }

  /**
   * Signal the worker processes to start.
   *
   * @param script The script file to run.
   */
  public void startWorkerProcesses(File script) {
    m_consoleCommunication.sendToAgents(new StartGrinderMessage(script));
  }

  /**
   * Signal the worker processes to reset.
   */
  public void resetWorkerProcesses() {
    m_consoleCommunication.sendToAgents(new ResetGrinderMessage());
  }

  /**
   * Signal the worker processes to stop.
   */
  public void stopWorkerProcesses() {
    m_consoleCommunication.sendToAgents(new StopGrinderMessage());
  }

  /**
   * Add a listener for process status data.
   *
   * @param listener The listener.
   */
  public void addProcessStatusListener(ProcessStatus.Listener listener) {
    m_processStatusSet.addListener(listener);
  }

  /**
   * How many agents are live?
   *
   * @return The number of agents.
   */
  public int getNumberOfLiveAgents() {
    return m_processStatusSet.getNumberOfLiveAgents();
  }
}
