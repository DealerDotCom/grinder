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

package net.grinder.communication;

import java.util.HashMap;
import java.util.Map;


/**
 * Abstract class that manages the receipt of messages.
 *
 * @author Philip Aston
 * @version $Revision$
 **/
abstract class AbstractReceiver implements Receiver {

  private final Map m_sequenceValues;

  private final MessageQueue m_messageQueue = new MessageQueue(true);

  /**
   * Constructor.
   *
   * @param checkSequence If <code>true</code>, check the sequence
   * numbers of received messages.
   */
  protected AbstractReceiver(boolean checkSequence) {
    m_sequenceValues = checkSequence ? new HashMap() : null;
  }

  protected final MessageQueue getMessageQueue() {
    return m_messageQueue;
  }

  /**
   * Block until a message is available, or another thread has
   * called {@link #shutdown}. Typically called from a message
   * dispatch loop.
   *
   * <p>Multiple threads can call this method, but only one thread
   * will receive a given message.</p>
   *
   * @return The message or <code>null</code> if shut down.
   * @throws CommunicationException If an error occured receiving a message.
   **/
  public final synchronized Message waitForMessage()
    throws CommunicationException {
    final Message message;

    try {
      message = m_messageQueue.dequeue(true);
    }
    catch (MessageQueue.ShutdownException e) {
      return null;
    }

    final String senderID = message.getSenderUniqueID();
    final long sequenceNumber = message.getSequenceNumber();

    if (m_sequenceValues != null) {
      final SequenceValue sequenceValue =
        (SequenceValue)m_sequenceValues.get(senderID);

      if (sequenceValue != null) {
        sequenceValue.nextValue(sequenceNumber, message);
      }
      else {
        m_sequenceValues.put(senderID, new SequenceValue(sequenceNumber));
      }
    }

    return message;
  }

  /**
   * Shut down this reciever.
   * @throws CommunicationException If an IO exception occurs.
   **/
  public void shutdown() throws CommunicationException {
    m_messageQueue.shutdown();
  }

  /**
   * Numeric sequence checker. Relies on caller for synchronisation.
   **/
  private static final class SequenceValue {
    private long m_value;

    /**
     * Constructor.
     * @param initialValue The initial sequence value.
     **/
    public SequenceValue(long initialValue) {
      m_value = initialValue;
    }

    /**
     * Check the next value in the sequence, and store it for next time.
     *
     * @param newValue The next value.
     * @throws CommunicationException If the message is out of sequence.
     **/
    public final void nextValue(long newValue, Message message)
      throws CommunicationException {
      if (newValue != ++m_value) {
        final CommunicationException e = new CommunicationException(
          "Out of sequence message from Sender '" +
          message.getSenderGrinderID() + "' (received " + newValue +
          ", expected " + m_value + ")");

        m_value = newValue;

        throw e;
      }
    }
  }
}
