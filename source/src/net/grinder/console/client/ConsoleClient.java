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



/**
 * Console API.
 *
 * <p>
 * <b>Warning: </b> This API is under development and not stable. It will
 * change.</p>
 *
 * @author Philip Aston
 * @version $Revision:$
 */
public interface ConsoleClient {

  /**
   * Start the console recording.
   *
   * @throws ConsoleClientException If a communication error occurred.
   */
  void startRecording() throws ConsoleClientException;

  /**
   * Stop the console recording.
   *
   * @throws ConsoleClientException If a communication error occurred.
   */
  void stopRecording() throws ConsoleClientException;

  /**
   * Reset the console recording.
   *
   * @throws ConsoleClientException If a communication error occurred.
   */
  void resetRecording() throws ConsoleClientException;

  /**
   * How many agents are connected?
   *
   * @return The number of agents.
   * @throws ConsoleClientException If a communication error occurred.
   */
  int getNumberOfAgents() throws ConsoleClientException;

  /**
   * Start all the worker processes.
   *
   * @param script
   *          File name of script. The File should be valid for the console
   *          process, not necessarily for the caller.
   * @throws ConsoleClientException
   *           If a communication error occurred.
   */
  void startWorkerProcesses(String script) throws ConsoleClientException;

  /**
   * Reset all the worker processes.
   *
   * @throws ConsoleClientException
   *           If a communication error occurred.
   */
  void resetWorkerProcesses() throws ConsoleClientException;
}
