// Copyright (C) 2000, 2001, 2002, 2003 Philip Aston
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
import java.net.InetAddress;
import java.net.UnknownHostException;

import net.grinder.communication.CommunicationException;
import net.grinder.communication.Message;
import net.grinder.communication.MulticastSender;
import net.grinder.communication.Receiver;
import net.grinder.communication.ResetGrinderMessage;
import net.grinder.communication.Sender;
import net.grinder.communication.StartGrinderMessage;
import net.grinder.communication.StopGrinderMessage;
import net.grinder.communication.UnicastReceiver;
import net.grinder.console.common.ConsoleException;
import net.grinder.console.common.ConsoleExceptionHandler;
import net.grinder.console.common.DisplayMessageConsoleException;
import net.grinder.console.model.ConsoleProperties;


/**
 * @author Philip Aston
 * @version $Revision$
 */
final class ConsoleCommunication {

  private final ConsoleProperties m_properties;
  private final ConsoleExceptionHandler m_exceptionHandler;

  private Receiver m_receiver = null;
  private Sender m_sender = null;
  private boolean m_deaf = true;

  ConsoleCommunication(ConsoleProperties properties,
		       ConsoleExceptionHandler exceptionHandler) {
    m_properties = properties;
    m_exceptionHandler = exceptionHandler;

    resetReceiver();
    resetSender();

    properties.addPropertyChangeListener(
      new PropertyChangeListener() {
	public void propertyChange(PropertyChangeEvent event) {
	  final String property = event.getPropertyName();

	  if (property.equals(
		ConsoleProperties.CONSOLE_ADDRESS_PROPERTY) ||
	      property.equals(
		ConsoleProperties.CONSOLE_PORT_PROPERTY)) {
	    resetReceiver();
	  }
	  else if (property.equals(
		     ConsoleProperties.GRINDER_ADDRESS_PROPERTY) ||
		   property.equals(
		     ConsoleProperties.GRINDER_PORT_PROPERTY)) {
	    resetSender();
	  }
	}
      });
  }

  private final void resetReceiver() {
    try {
      if (m_receiver != null) {
	m_receiver.shutdown();
      }

      synchronized(this) {
	while (!m_deaf) {
	  try {
	    wait();
	  }
	  catch (InterruptedException e) {
	    // Ignore because its a pain to propagate.
	  }
	}
      }

      m_receiver =
	new UnicastReceiver(m_properties.getConsoleAddress(),
			    m_properties.getConsolePort());

      synchronized(this) {
	m_deaf = false;
	notifyAll();
      }
    }
    catch(CommunicationException e) {
      m_exceptionHandler.consoleExceptionOccurred(
	new DisplayMessageConsoleException(
	  "localBindError.text",
	  "Failed to bind to local address"));
    }
  }

  private final void resetSender() {
    String host;

    try {
      host = InetAddress.getLocalHost().getHostName();
    }
    catch (UnknownHostException e) {
      host = "UNNAMED HOST";
    }

    try {
      m_sender =
	new MulticastSender("Console (" + host + ")",
			    m_properties.getGrinderAddress(),
			    m_properties.getGrinderPort());
    }
    catch(CommunicationException e) {
      m_exceptionHandler.consoleExceptionOccurred(
	new DisplayMessageConsoleException(
	  "multicastConnectError.text",
	  "Failed to connect to multicast address"));
    }
  }

  final void sendStartMessage() throws CommunicationException {
    m_sender.send(new StartGrinderMessage());
  }

  final void sendResetMessage() throws CommunicationException {
    m_sender.send(new ResetGrinderMessage());
  }

  final void sendStopMessage() throws CommunicationException {
    m_sender.send(new StopGrinderMessage());
  }

  /**
   * @return The message.
   **/
  final Message waitForMessage() {
    while (true) {
      synchronized(this) {
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
	e.printStackTrace();
	m_exceptionHandler.consoleExceptionOccurred(
	  new ConsoleException(e.getMessage(), e));
      }
    }
  }
}
