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

import net.grinder.communication.Message;
import net.grinder.console.common.ConsoleException;
import net.grinder.console.common.ErrorHandler;


/**
 * Handles communication for the console.
 *
 * @author Philip Aston
 * @version $Revision$
 */
public interface ConsoleCommunication {

  /**
   * Set the error handler. Any errors the
   * <code>ConsoleCommunication</code> has queued up will be reported
   * immediately.
   *
   * @param errorHandler Where to report errors.
   */
  void setErrorHandler(ErrorHandler errorHandler);

  /**
   * Send a message to the worker processes.
   *
   * @param message The message.
   */
  void send(Message message);

  /**
   * Get the ProcessControl.
   *
   * @return The <code>ProcessControl</code>.
   */
  ProcessControl getProcessControl();

  /**
   * Interface for things that can handle messages.
   */
  public interface MessageHandler {

    /**
     * The handler implements this to receive a message.
     *
     * @param message The message.
     * @return <code>true</code> => The handler processed the message.
     * @throws ConsoleException If the handler attempted to process
     * the message, but failed.
     */
    boolean process(Message message) throws ConsoleException;
  }

  /**
   * Add a message hander.
   *
   * @param messageHandler The message handler.
   */
  void addMessageHandler(MessageHandler messageHandler);

  /**
   * Wait to receive a message, then process it.
   *
   * @return <code>true</code> => the message was processed by a handler.
   * @exception ConsoleException If an error occurred in message processing.
   */
  boolean processOneMessage() throws ConsoleException;
}
