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

package net.grinder.engine.agent;

import java.io.OutputStream;

import net.grinder.communication.CommunicationException;
import net.grinder.communication.FanOutStreamSender;
import net.grinder.communication.Message;
import net.grinder.communication.StreamSender;
import net.grinder.engine.common.EngineException;


/**
 * Class that can start worker processes.
 *
 * @author Philip Aston
 * @version $Revision$
 */
final class WorkerProcessFactory implements ProcessFactory {

  private final WorkerProcessCommandLine m_commandLine;
  private final String m_hostIDPrefix;
  private final FanOutStreamSender m_fanOutStreamSender;
  private final Message m_initialisationMessage;

  public WorkerProcessFactory(WorkerProcessCommandLine commandLine,
                              String hostID,
                              FanOutStreamSender fanOutStreamSender,
                              Message initialisationMessage) {

    m_commandLine = commandLine;
    m_hostIDPrefix = hostID;
    m_fanOutStreamSender = fanOutStreamSender;
    m_initialisationMessage = initialisationMessage;
  }

  public ChildProcess create(int processIndex,
                             OutputStream outputStream,
                             OutputStream errorStream) throws EngineException {

    final String grinderID = m_hostIDPrefix + "-" + processIndex;

    final ChildProcess process =
      new ChildProcess(grinderID, m_commandLine.getCommandArray(grinderID),
                       outputStream, errorStream);

    final OutputStream processStdin = process.getStdinStream();

    try {
      new StreamSender(processStdin).send(m_initialisationMessage);
    }
    catch (CommunicationException e) {
      throw new EngineException("Failed to send initialisation message", e);
    }

    m_fanOutStreamSender.add(processStdin);

    return process;
  }
}
