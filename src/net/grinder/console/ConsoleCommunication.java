// Copyright (C) 2000, 2001, 2002, 2003, 2004 Philip Aston
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

package net.grinder.console;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import net.grinder.communication.Acceptor;
import net.grinder.communication.CommunicationException;
import net.grinder.communication.ConnectionType;
import net.grinder.communication.FanOutServerSender;
import net.grinder.communication.Message;
import net.grinder.communication.Receiver;
import net.grinder.communication.Sender;
import net.grinder.communication.ServerReceiver;
import net.grinder.console.common.ConsoleException;
import net.grinder.console.common.ErrorHandler;
import net.grinder.console.common.DisplayMessageConsoleException;
import net.grinder.console.model.ConsoleProperties;


/**
 * Handles communication for the console.
 *
 * @author Philip Aston
 * @version $Revision$
 */
final class ConsoleCommunication {

  private final ConsoleProperties m_properties;
  private final ErrorHandler m_errorHandler;

  private Acceptor m_acceptor = null;
  private Receiver m_receiver = null;
  private Sender m_sender = null;
  private boolean m_deaf = true;

  public ConsoleCommunication(ConsoleProperties properties,
                              ErrorHandler errorHandler) {
    m_properties = properties;
    m_errorHandler = errorHandler;

    reset();

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
  }

  private void reset() {
    try {
      if (m_acceptor != null) {
         m_acceptor.shutdown();
      }
    }
    catch (CommunicationException e) {
      m_errorHandler.handleException(e);
      return;
    }

    if (m_sender != null) {
      m_sender.shutdown();
    }

    if (m_receiver != null) {
      m_receiver.shutdown();
    }

    synchronized (this) {
      while (!m_deaf) {
        try {
          wait();
        }
        catch (InterruptedException e) {
          m_errorHandler.handleException(e);
          return;
        }
      }
    }

    try {
      m_acceptor = new Acceptor(m_properties.getConsoleHost(),
                                m_properties.getConsolePort(),
                                1);
    }
    catch (CommunicationException e) {
      m_errorHandler.handleException(
        new DisplayMessageConsoleException(
          "localBindError.text", "Failed to bind to local address", e));

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

            m_errorHandler.handleException(exception);
          }
        }
      };

    acceptorProblemListener.setDaemon(true);
    acceptorProblemListener.start();

    m_receiver = new ServerReceiver(m_acceptor, ConnectionType.REPORT, 5);
    m_sender = new FanOutServerSender(m_acceptor, ConnectionType.CONTROL, 3);

    synchronized (this) {
      m_deaf = false;
      notifyAll();
    }
  }

  public void send(Message message) {

    if (m_sender == null) {
      m_errorHandler.handleResourceErrorMessage(
        "sendError.text", "Failed to send message");
    }
    else {
      try {
        m_sender.send(message);
      }
      catch (CommunicationException e) {
        m_errorHandler.handleException(
          new DisplayMessageConsoleException(
            "sendError.text", "Failed to send message", e));
      }
    }
  }

  /**
   * @return The message.
   */
  public Message waitForMessage() {
    while (true) {
      synchronized (this) {
        while (m_deaf) {
          try {
            wait();
          }
          catch (InterruptedException e) {
            // Ignore because its a pain to propagate.
          }
        }
      }

      try {
        final Message message = m_receiver.waitForMessage();

        if (message == null) {
          // Current receiver has been shutdown.
          synchronized (this) {
            m_deaf = true;
            notifyAll();
          }
        }

        return message;
      }
      catch (CommunicationException e) {
        m_errorHandler.handleException(
          new ConsoleException(e.getMessage(), e));
      }
    }
  }
}
