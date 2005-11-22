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

package net.grinder.engine.agent;

import java.io.File;
import java.io.OutputStream;

import net.grinder.common.WorkerIdentity;
import net.grinder.communication.CommunicationException;
import net.grinder.communication.FanOutStreamSender;
import net.grinder.communication.StreamSender;
import net.grinder.engine.common.EngineException;
import net.grinder.engine.messages.InitialiseGrinderMessage;


/**
 * Class that starts workers as separate processes.
 *
 * @author Philip Aston
 * @version $Revision$
 */
final class ProcessWorkerFactory implements WorkerFactory {

  private final WorkerProcessCommandLine m_commandLine;
  private final FanOutStreamSender m_fanOutStreamSender;
  private final AgentIdentityImplementation m_agentIdentity;
  private final boolean m_reportToConsole;
  private final File m_scriptFile;
  private final File m_scriptDirectory;

  public ProcessWorkerFactory(WorkerProcessCommandLine commandLine,
                              FanOutStreamSender fanOutStreamSender,
                              AgentIdentityImplementation agentIdentity,
                              boolean reportToConsole,
                              File scriptFile,
                              File scriptDirectory) {

    m_commandLine = commandLine;
    m_agentIdentity = agentIdentity;
    m_fanOutStreamSender = fanOutStreamSender;
    m_reportToConsole = reportToConsole;
    m_scriptFile = scriptFile;
    m_scriptDirectory = scriptDirectory;
  }

  public Worker create(OutputStream outputStream,
                       OutputStream errorStream) throws EngineException {

    final WorkerIdentity workerIdentity =
      m_agentIdentity.createWorkerIdentity();

    final Worker process =
      new ProcessWorker(workerIdentity.getName(),
                        m_commandLine.getCommandArray(),
                        outputStream,
                        errorStream);

    final OutputStream processStdin = process.getCommunicationStream();

    try {
      final InitialiseGrinderMessage initialisationMessage =
        new InitialiseGrinderMessage(workerIdentity,
                                     m_reportToConsole,
                                     m_scriptFile,
                                     m_scriptDirectory);

      new StreamSender(processStdin).send(initialisationMessage);
    }
    catch (CommunicationException e) {
      throw new EngineException("Failed to send initialisation message", e);
    }

    m_fanOutStreamSender.add(processStdin);

    return process;
  }
}
