// Copyright (C) 2000, 2001, 2002, 2003, 2004, 2005 Philip Aston
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
import java.io.File;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.Timer;

import net.grinder.communication.Acceptor;
import net.grinder.communication.CommunicationException;
import net.grinder.communication.ConnectionIdentity;
import net.grinder.communication.ConnectionType;
import net.grinder.communication.FanOutServerSender;
import net.grinder.communication.Message;
import net.grinder.communication.Receiver;
import net.grinder.communication.Sender;
import net.grinder.communication.ServerReceiver;
import net.grinder.console.common.ConsoleException;
import net.grinder.console.common.DisplayMessageConsoleException;
import net.grinder.console.common.ErrorHandler;
import net.grinder.console.common.ErrorQueue;
import net.grinder.console.common.Resources;
import net.grinder.console.messages.ReportStatusMessage;
import net.grinder.console.model.ConsoleProperties;
import net.grinder.engine.messages.ClearCacheMessage;
import net.grinder.engine.messages.DistributeFileMessage;
import net.grinder.engine.messages.ResetGrinderMessage;
import net.grinder.engine.messages.StartGrinderMessage;
import net.grinder.engine.messages.StopGrinderMessage;
import net.grinder.util.FileContents;


/**
 * Handles communication for the console.
 *
 * @author Philip Aston
 * @version $Revision$
 */
public final class ConsoleCommunicationImplementation
  implements ConsoleCommunication {

  private final Resources m_resources;
  private final ConsoleProperties m_properties;
  private final ProcessStatusSet m_processStatusSet;

  private final ErrorQueue m_errorQueue = new ErrorQueue();

  private final ProcessControl m_processControl =
    new ProcessControlImplementation();
  private final DistributionControl m_distributionControl =
    new DistributionControlImplementation();

  /**
   * Synchronise on m_connectedAgents before accessing.
   */
  private final Set m_connectedAgents = new HashSet();

  /**
   * Synchronise on m_messageHandlers before accessing.
   */
  private final List m_messageHandlers = new LinkedList();

  /**
   * Synchronise on m_agentConnectionListeners before accessing.
   */
  private final List m_agentConnectionListeners = new LinkedList();

  private Acceptor m_acceptor = null;
  private Receiver m_receiver = null;
  private Sender m_sender = null;

  /**
   * Synchronise on this before accessing.
   */
  private boolean m_deaf = true;

  /**
   * Constructor.
   *
   * @param resources Resources.
   * @param properties Console properties.
   * @param timer Timer that can be used to schedule housekeeping tasks.
   * @throws DisplayMessageConsoleException If properties are invalid.
   */
  public ConsoleCommunicationImplementation(Resources resources,
                                            ConsoleProperties properties,
                                            Timer timer)
    throws DisplayMessageConsoleException {

    m_resources = resources;
    m_properties = properties;

    addMessageHandler(
      new MessageHandler() {
        public boolean process(Message message) {
          if (message instanceof ReportStatusMessage) {
            m_processStatusSet.addStatusReport((ReportStatusMessage)message);
            return true;
          }

          return false;
        }
      });

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

    m_processStatusSet = new ProcessStatusSetImplementation(timer);
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
    }

    synchronized (this) {
      while (!m_deaf) {
        try {
          wait();
        }
        catch (InterruptedException e) {
          m_errorQueue.handleException(e);
          return;
        }
      }
    }

    try {
      m_acceptor = new Acceptor(m_properties.getConsoleHost(),
                                m_properties.getConsolePort(),
                                1);

      m_acceptor.addListener(
        ConnectionType.CONTROL,
        new Acceptor.Listener() {
          public void connectionAccepted(ConnectionType connectionType,
                                         ConnectionIdentity connection) {
            synchronized (m_connectedAgents) {
              m_connectedAgents.add(connection);
              fireAgentConnected();
            }
          }

          public void connectionClosed(ConnectionType connectionType,
                                       ConnectionIdentity connection) {
            // If the acceptor is shutdown, this will fire for each
            // connection - m_connectedAgents will be updated
            // correctly on reset.
            synchronized (m_connectedAgents) {
              m_connectedAgents.remove(connection);
              fireAgentDisconnected();
            }
          }
        });
    }
    catch (CommunicationException e) {
      m_errorQueue.handleException(
        new DisplayMessageConsoleException(
          m_resources, "localBindError.text", e));

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

    m_receiver = new ServerReceiver(m_acceptor, ConnectionType.REPORT, 5);
    m_sender = new FanOutServerSender(m_acceptor, ConnectionType.CONTROL, 3);

    synchronized (this) {
      m_deaf = false;
      notifyAll();
    }
  }

  /**
   * Get a ProcessControl implementation.
   *
   * @return The <code>ProcessControl</code>.
   */
  public ProcessControl getProcessControl() {
    return m_processControl;
  }

  /**
   * Get a DistributionControl implementation.
   *
   * @return The <code>DistributionControl</code>.
   */
  public DistributionControl getDistributionControl() {
    return m_distributionControl;
  }

  /**
   * Add a message hander.
   *
   * @param messageHandler The message handler.
   */
  public void addMessageHandler(
    ConsoleCommunication.MessageHandler messageHandler) {
    synchronized (m_messageHandlers) {
      m_messageHandlers.add(messageHandler);
    }
  }

  /**
   * Wait to receive a message, then process it.
   *
   * @return <code>true</code> => the message was processed by a handler.
   * @exception ConsoleException If an error occurred in message processing.
   */
  public boolean processOneMessage() throws ConsoleException  {
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

          return false;
        }

        synchronized (m_messageHandlers) {
          final Iterator iterator = m_messageHandlers.iterator();

          while (iterator.hasNext()) {
            final MessageHandler messageHandler =
              (MessageHandler)iterator.next();

            if (messageHandler.process(message)) {
              return true;
            }
          }
          return false;
        }
      }
      catch (CommunicationException e) {
        m_errorQueue.handleException(new ConsoleException(e.getMessage(), e));
      }
    }
  }

  private class ProcessControlImplementation implements ProcessControl {

   /**
     * Signal the worker processes to start.
     */
    public void startWorkerProcesses(File scriptFile) {
      m_processStatusSet.processEvent();
      send(new StartGrinderMessage(scriptFile));
    }

    /**
     * Signal the worker processes to reset.
     */
    public void resetWorkerProcesses() {
      m_processStatusSet.processEvent();
      send(new ResetGrinderMessage());
    }

    /**
     * Signal the worker processes to stop.
     */
    public void stopWorkerProcesses() {
      m_processStatusSet.processEvent();
      send(new StopGrinderMessage());
    }

    /**
     * Add a listener for process status data.
     *
     * @param listener The listener.
     */
    public void addProcessStatusListener(ProcessStatusListener listener) {
      m_processStatusSet.addListener(listener);
    }
  }

  private class DistributionControlImplementation
    implements DistributionControl {

    /**
     * Signal the agent processes to clear their file caches.
     */
    public void clearFileCaches() {
      send(new ClearCacheMessage());
    }

    /**
     * Send a file to the file caches.
     *
     * @param fileContents The file contents.
     */
    public void sendFile(FileContents fileContents) {
      send(new DistributeFileMessage(fileContents));
    }

    /**
     * Get a Set&lt;ConnectionIdentity&gt; of connected agent
     * processes.
     *
     * @return Copy of the set of connection identities.
     */
    public Set getConnectedAgents() {
      synchronized (m_connectedAgents) {
        return new HashSet(m_connectedAgents);
      }
    }
  }

  private void send(Message message) {
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

  /**
   * Register an {@link AgentConnectionListener}.
   *
   * @param listener The listener.
   */
  public void addAgentConnectionListener(AgentConnectionListener listener) {
    synchronized (m_agentConnectionListeners) {
      m_agentConnectionListeners.add(listener);
    }
  }

  private void fireAgentConnected() {
    synchronized (m_agentConnectionListeners) {
      final Iterator iterator = m_agentConnectionListeners.iterator();

      while (iterator.hasNext()) {
        final AgentConnectionListener listener =
          (AgentConnectionListener)iterator.next();

        listener.agentConnected();
      }
    }
  }

  private void fireAgentDisconnected() {
    synchronized (m_agentConnectionListeners) {
      final Iterator iterator = m_agentConnectionListeners.iterator();

      while (iterator.hasNext()) {
        final AgentConnectionListener listener =
          (AgentConnectionListener)iterator.next();

        listener.agentDisconnected();
      }
    }
  }

}
