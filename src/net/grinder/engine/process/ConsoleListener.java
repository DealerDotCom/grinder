// Copyright (C) 2001, 2002, 2003 Philip Aston
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

package net.grinder.engine.process;

import net.grinder.common.GrinderProperties;
import net.grinder.common.Logger;
import net.grinder.communication.CommunicationDefaults;
import net.grinder.communication.CommunicationException;
import net.grinder.communication.Message;
//import net.grinder.communication.MulticastReceiver;
import net.grinder.communication.Receiver;
import net.grinder.communication.ResetGrinderMessage;
import net.grinder.communication.StartGrinderMessage;
import net.grinder.communication.StopGrinderMessage;


/**
 * Active object which listens for console messages.
 *
 * @author Philip Aston
 * @version $Revision$
 * @see net.grinder.engine.process.GrinderProcess
 **/
final class ConsoleListener {
  /**
   * Constant that represents start message.
   * @see #received
   **/
  public static final int START = 1 << 0;

  /**
   * Constant that represents a a reset message.
   * @see #received
   **/
  public static final int RESET = 1 << 1;

  /**
   * Constant that represents a stop message.
   * @see #received
   **/
  public static final int STOP =  1 << 2;

  /**
   * Constant that represent any message.
   * @see #received
   **/
  public static final int ANY = START | RESET | STOP;

  private final Monitor m_notifyOnMessage;
  private final Logger m_logger;
  private int m_messagesReceived = 0;

  /**
   * Constructor that creates an appropriate {@link ConsoleListener}
   * based on the passed {@link GrinderProperties}
   *
   * <p>If <code>properties</code> specifies that this process
   * should receive console signals, a thread is created to listen
   * for messages. Otherwise we simply do nothing and {@link
   * #received} will always return 0. </p>
   *
   * @param properties The {@link GrinderProperties}
   * @param notifyOnMessage A {@link Monitor} to notify when a
   * message arrives.
   * @param logger A {@link net.grinder.common.Logger} to log receive
   * event messages to.
   * @exception CommunicationException If a multicast error occurs.
   **/
  public ConsoleListener(GrinderProperties properties,
                         Monitor notifyOnMessage, Logger logger)
    throws CommunicationException {
    m_notifyOnMessage = notifyOnMessage;
    m_logger = logger;

    // Parse console configuration.
    final String grinderAddress =
      properties.getProperty("grinder.grinderAddress",
                             CommunicationDefaults.GRINDER_ADDRESS);

    final int grinderPort =
      properties.getInt("grinder.grinderPort",
                        CommunicationDefaults.GRINDER_PORT);

    final ReceiverThread receiverThread =
      new ReceiverThread(grinderAddress, grinderPort);

    receiverThread.setDaemon(true);
    receiverThread.start();
  }

  /**
   * The <code>ConsoleListener</code> has a bit mask representing
   * messages received but not acknowledged. This method returns a
   * bit mask representing the messages received that match the
   * <code>mask</code> parameter and acknowledges the messages
   * represented by <code>mask</code>.
   *
   * @param mask The messages to check for.
   * @return The subset of <code>mask</code> received.
   **/
  public synchronized int received(int mask) {
    final int intersection = m_messagesReceived & mask;

    try {
      return intersection;
    }
    finally {
      m_messagesReceived ^= intersection;
    }
  }

  /**
   * Thread that uses a {@link net.grinder.communication.Receiver}
   * to receive console messages.
   **/
  private final class ReceiverThread extends Thread {
    private final Receiver m_receiver;

    /**
     * Creates a new <code>ReceiverThread</code> instance.
     *
     * @param address Console multicast address.
     * @param port Console multicast port.
     * @exception CommunicationException If an error occurs
     * binding to the multicast port.
     **/
    private ReceiverThread(String address, int port)
      throws CommunicationException {
      super("Console Listener");

      m_receiver = null; // new MulticastReceiver(address, port);
    }

    /**
     * Event loop that receives messages from the console.
     **/
    public void run() {
      while (true) {
        final Message message;

        try {
          message = m_receiver.waitForMessage();
        }
        catch (CommunicationException e) {
          m_logger.error("error receiving console signal: " + e,
                         Logger.LOG | Logger.TERMINAL);
          continue;
        }

        if (message instanceof StartGrinderMessage) {
          m_logger.output("got a start message from console");
          setReceived(START);
        }
        else if (message instanceof StopGrinderMessage) {
          m_logger.output("got a stop message from console");
          setReceived(STOP);
        }
        else if (message instanceof ResetGrinderMessage) {
          m_logger.output("got a reset message from console");
          setReceived(RESET);
        }
        else {
          m_logger.output("got an unknown message from console");
        }
      }
    }

    private void setReceived(int message) {
      synchronized (ConsoleListener.this) {
        m_messagesReceived |= message;
      }

      synchronized (m_notifyOnMessage) {
        m_notifyOnMessage.notifyAll();
      }
    }
  }
}

