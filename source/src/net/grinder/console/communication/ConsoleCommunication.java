// Copyright (C) 2004, 2005, 2006 Philip Aston
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

import net.grinder.communication.MessageDispatchRegistry;
import net.grinder.console.common.ErrorHandler;


/**
 * Handles communication for the console.
 *
 * @author Philip Aston
 * @version $Revision$
 */
public interface ConsoleCommunication {

  /**
   * Returns the message dispatch registry which callers can use to register new
   * message handlers.
   *
   * @return The registry.
   */
  MessageDispatchRegistry getMessageDispatchRegistry();

  /**
   * Wait to receive a message, then process it.
   */
  void processOneMessage();

  /**
   * Get a ProcessControl implementation.
   *
   * @return The <code>ProcessControl</code>.
   */
  ProcessControl getProcessControl();

  /**
   * Get a DistributionControl implementation.
   *
   * @return The <code>DistributionControl</code>.
   */
  DistributionControl getDistributionControl();

  /**
   * Set the error handler. Any errors the
   * <code>ConsoleCommunication</code> has queued up will be reported
   * immediately.
   *
   * @param errorHandler Where to report errors.
   */
  void setErrorHandler(ErrorHandler errorHandler);

  /**
   * How many connections have been accepted? Used by the unit tests.
   *
   * @return The number of accepted connections.
   */
  int getNumberOfConnections();
}
