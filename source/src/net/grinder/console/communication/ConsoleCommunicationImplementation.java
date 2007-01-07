// Copyright (C) 2000 - 2007 Philip Aston
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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Timer;
import java.util.TimerTask;

import net.grinder.communication.Acceptor;
import net.grinder.communication.CommunicationException;
import net.grinder.communication.ConnectionType;
import net.grinder.communication.FanOutServerSender;
import net.grinder.communication.Message;
import net.grinder.communication.MessageDispatchRegistry;
import net.grinder.communication.MessageDispatchSender;
import net.grinder.communication.ServerReceiver;
import net.grinder.console.common.DisplayMessageConsoleException;
import net.grinder.console.common.ErrorHandler;
import net.grinder.console.common.ErrorQueue;
import net.grinder.console.common.Resources;
import net.grinder.console.model.ConsoleProperties;
import net.grinder.util.thread.WakeableCondition;


/**
 * Handles communication for the console.
 *
 * @author Philip Aston
 * @version $Revision$
 */
public final class ConsoleCommunicationImplementation
  implements ConsoleCommunication {

  private static final long CHECK_PEER_STATUS_PERIOD = 1000;

  private final int m_idlePollDelay;
  private final Resources m_resources;
  private final ConsoleProperties m_properties;

  private final ErrorQueue m_errorQueue = new ErrorQueue();

  private final MessageDispatchSender m_messageDispatcher =
    new MessageDispatchSender();

  private final WakeableCondition m_processing = new WakeableCondition();

  private Acceptor m_acceptor = null;
  private ServerReceiver m_receiver = null;
  private FanOutServerSender m_sender = null;

  /**
   * Constructor.
   *
   * @param resources
   *          Resources.
   * @param properties
   *          Console properties.
   * @param timer
   *          Timer that can be used to schedule housekeeping tasks.
   * @param idlePollDelay
   *          Time in milliseconds that our ServerReceiver threads should sleep
   *          for if there's no incoming messages.
   * @throws DisplayMessageConsoleException
   *           If properties are invalid.
   */
  public ConsoleCommunicationImplementation(Resources resources,
                                            ConsoleProperties properties,
                                            Timer timer,
                                            int idlePollDelay)
    throws DisplayMessageConsoleException {

    m_resources = resources;
    m_properties = properties;
    m_idlePollDelay = idlePollDelay;

    properties.addPropertyChangeListener(
      new PropertyChangeListener() {
        public void propertyChange(PropertyChangeEvent event) {
          final String property = event.getPropertyName();

          if (property.equals(ConsoleProperties.CONSOLE_HOST_PROPERTY) ||
              property.equals(ConsoleProperties.CONSOLE_PORT_PROPERTY)) {
            reset();
          }
        }
      });

    reset();

    timer.schedule(new TimerTask() {
        public void run() {
          if (m_sender != null) {
            m_sender.isPeerShutdown();
          }
        }
      },
      CHECK_PEER_STATUS_PERIOD,
      CHECK_PEER_STATUS_PERIOD);
  }

  /**
   * Set the error handler. Any errors the
   * <code>ConsoleCommunication</code> has queued up will be reported
   * immediately.
   *
   * @param errorHandler Where to report errors.
   */
  public void setErrorHandler(ErrorHandler errorHandler) {
    m_errorQueue.setErrorHandler(errorHandler);
  }

  private void reset() {
    try {
      if (m_acceptor != null) {
        m_acceptor.shutdown();
      }
    }
    catch (CommunicationException e) {
      m_errorQueue.handleException(e);
      return;
    }

    if (m_sender != null) {
      m_sender.shutdown();
    }

    if (m_receiver != null) {
      m_receiver.shutdown();

      // Wait until we're deaf. This requires that some other thread executes
      // processOneMessage(). We can't suck on m_receiver ourself as there may
      // be valid pending messages queued up.

      m_processing.await(false);
    }

    try {
      m_acceptor = new Acceptor(m_properties.getConsoleHost(),
                                m_properties.getConsolePort(),
                                1);
    }
    catch (CommunicationException e) {
      m_errorQueue.handleException(
        new DisplayMessageConsoleException(
          m_resources, "localBindError.text", e));

      // Wake up any threads waiting in processOneMessage().
      m_processing.wakeUpAllWaiters();

      return;
    }

    final Thread acceptorProblemListener =
      new Thread("Acceptor problem listener") {
        public void run() {
          while (true) {
            final Exception exception = m_acceptor.getPendingException(true);

            if (exception == null) {
              // Acceptor is shutting down.
              break;
            }

            m_errorQueue.handleException(exception);
          }
        }
      };

    acceptorProblemListener.setDaemon(true);
    acceptorProblemListener.start();

    m_receiver = new ServerReceiver();

    try {
      m_receiver.receiveFrom(m_acceptor,
                             new ConnectionType[] {
                              ConnectionType.AGENT,
                              ConnectionType.CONSOLE_CLIENT,
                              ConnectionType.WORKER,
                             },
                             5,
                             m_idlePollDelay);
    }
    catch (CommunicationException e) {
      throw new AssertionError(e);
    }

    try {
      m_sender = new FanOutServerSender(m_acceptor, ConnectionType.AGENT, 3);
    }
    catch (Acceptor.ShutdownException e) {
      // I am tempted to make this an assertion.
      // Currently, this condition can only happen if the accept() call throws
      // an exception. I guess this might reasonably happen if a network i/f
      // goes away immediately after we create the Acceptor. It's not easy for
      // us to reset ourselves at this point (I certainly don't want to
      // recurse), so we notify the user. Users could get going again by
      // reseting new console address info, but most likely they'll just restart
      // the console.
      m_processing.wakeUpAllWaiters();
      m_errorQueue.handleException(e);
      return;
    }

    m_processing.set(true);
  }

  /**
   * Returns the message dispatch registry which callers can use to register new
   * message handlers.
   *
   * @return The registry.
   */
  public MessageDispatchRegistry getMessageDispatchRegistry() {
    return m_messageDispatcher;
  }

  /**
   * Wait to receive a message, then process it.
   */
  public void processOneMessage() {
    while (true) {
      if (!m_processing.await(true)) {
        // await() interrupted before we were listening.
        return;
      }

      try {
        final Message message = m_receiver.waitForMessage();

        if (message == null) {
          // Current receiver has been shut down.
          m_processing.set(false);

          // We return, to give our caller a chance to handle any shut down.
        }
        else {
          m_messageDispatcher.send(message);
        }

        break;
      }
      catch (CommunicationException e) {
        // The receive or send failed. We only set m_processing to false when
        // our receiver has been shut down.
        m_errorQueue.handleException(e);
      }
    }
  }

  /**
   * How many connections have been accepted and are still active? Used by the
   * unit tests.
   *
   * @return The number of accepted connections.
   */
  public int getNumberOfConnections() {
    return m_acceptor == null ? 0 : m_acceptor.getNumberOfConnections();
  }

  /**
   * Send the given message to the agent processes (which may pass it on to
   * their workers).
   *
   * <p>Any errors that occur will be handled with the error handler, see
   * {@link #setErrorHandler}.</p>
   *
   * @param message The message to send.
   */
  public void sendToAgents(Message message) {
    if (m_sender == null) {
      m_errorQueue.handleResourceErrorMessage(
        "sendError.text", "Failed to send message");
    }
    else {
      try {
        m_sender.send(message);
     }
     catch (CommunicationException e) {
       m_errorQueue.handleException(
         new DisplayMessageConsoleException(
           m_resources, "sendError.text", e));
     }
   }
  }
}
