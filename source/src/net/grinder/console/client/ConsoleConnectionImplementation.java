// Copyright (C) 2006, 2007 Philip Aston
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

package net.grinder.console.client;


import net.grinder.communication.BlockingSender;
import net.grinder.communication.CommunicationException;
import net.grinder.communication.Message;
import net.grinder.console.communication.server.messages.GetNumberOfAgentsMessage;
import net.grinder.console.communication.server.messages.ResetRecordingMessage;
import net.grinder.console.communication.server.messages.ResetWorkerProcessesMessage;
import net.grinder.console.communication.server.messages.ResultMessage;
import net.grinder.console.communication.server.messages.StartRecordingMessage;
import net.grinder.console.communication.server.messages.StartWorkerProcessesMessage;
import net.grinder.console.communication.server.messages.StopRecordingMessage;


/**
 * Implementation of {@link ConsoleConnection} that uses a
 * {@link BlockingSender} to communicate with the console.
 *
 * @author Philip Aston
 * @version $Revision:$
 */
final class ConsoleConnectionImplementation implements ConsoleConnection {

  private final BlockingSender m_consoleSender;

  ConsoleConnectionImplementation(BlockingSender consoleSender) {
    m_consoleSender = consoleSender;
  }

  /**
   * Close the connection.
   */
  public void close() {
    m_consoleSender.shutdown();
  }

  /**
   * Start the console recording.
   *
   * @throws ConsoleConnectionException If a communication error occurred.
   */
  public void startRecording() throws ConsoleConnectionException {
    try {
      m_consoleSender.blockingSend(new StartRecordingMessage());
    }
    catch (CommunicationException e) {
      throw new ConsoleConnectionException("Failed to start recording", e);
    }
  }

  /**
   * Stop the console recording.
   *
   * @throws ConsoleConnectionException If a communication error occurred.
   */
  public void stopRecording() throws ConsoleConnectionException {
    try {
      m_consoleSender.blockingSend(new StopRecordingMessage());
    }
    catch (CommunicationException e) {
      throw new ConsoleConnectionException("Failed to stop recording", e);
    }
  }

  /**
   * Reset the console recording.
   *
   * @throws ConsoleConnectionException If a communication error occurred.
   */
  public void resetRecording() throws ConsoleConnectionException {
    try {
      m_consoleSender.blockingSend(new ResetRecordingMessage());
    }
    catch (CommunicationException e) {
      throw new ConsoleConnectionException("Failed to reset recording", e);
    }
  }

  /**
   * How many agents are connected?
   *
   * @return The number of agents.
   * @throws ConsoleConnectionException If a communication error occurred.
   */
  public int getNumberOfAgents() throws ConsoleConnectionException {
    final Message response;

    try {
      response = m_consoleSender.blockingSend(
        new GetNumberOfAgentsMessage());
    }
    catch (CommunicationException e) {
      throw new ConsoleConnectionException("getNumberOfLiveAgents()", e);
    }

    if (response instanceof ResultMessage) {
      final Object result = ((ResultMessage)response).getResult();

      if (result instanceof Integer) {
        return ((Integer)result).intValue();
      }
    }

    throw new ConsoleConnectionException("Unexpected response: " + response);
  }

  /**
   * Start all the worker processes.
   *
   * @param script
   *          File name of script. The File should be valid for the console
   *          process, not necessarily for the caller.
   * @throws ConsoleConnectionException
   *           If a communication error occurred.
   */
  public void startWorkerProcesses(String script)
  throws ConsoleConnectionException {
    try {
      m_consoleSender.blockingSend(new StartWorkerProcessesMessage(script));
    }
    catch (CommunicationException e) {
      throw new ConsoleConnectionException(
        "Failed to start worker processes", e);
    }
  }

  /**
   * Reset all the worker processes.
   *
   * @throws ConsoleConnectionException
   *           If a communication error occurred.
   */
  public void resetWorkerProcesses() throws ConsoleConnectionException {
    try {
      m_consoleSender.blockingSend(new ResetWorkerProcessesMessage());
    }
    catch (CommunicationException e) {
      throw new ConsoleConnectionException(
        "Failed to reset worker processes", e);
    }
  }
}
