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


/**
 * QueuedSender implementation.
 *
 * @author Philip Aston
 * @version $Revision$
 **/
public final class QueuedSenderDecorator implements QueuedSender {

  private final Sender m_delegate;
  private final MessageQueue m_messageQueue = new MessageQueue(false);

  /**
   * Constructor.
   *
   * @param delegate Sender to decorate.
   */
  public QueuedSenderDecorator(Sender delegate) {
    m_delegate = delegate;
  }

  /**
   * First flush any pending messages queued with {@link #queue} and
   * then send the given message.
   *
   * @param message A {@link Message}.
   * @exception CommunicationException If an error occurs.
   **/
  public void send(Message message) throws CommunicationException {
    synchronized (m_messageQueue.getMutex()) {
      queue(message);
      flush();
    }
  }

  /**
   * Queue the given message for later sending.
   *
   * @param message A {@link Message}.
   * @exception CommunicationException If an error occurs.
   * @see #flush
   * @see #send
   */
  public void queue(Message message) throws CommunicationException {

    try {
      m_messageQueue.queue(message);
    }
    catch (ThreadSafeQueue.ShutdownException e) {
      // Assertion failure.
      throw new RuntimeException("MessageQueue unexpectedly shutdown");
    }
  }

  /**
   * Flush any pending messages queued with {@link #queue}.
   *
   * @exception CommunicationException if an error occurs
   */
  public void flush() throws CommunicationException {

    try {
      synchronized (m_messageQueue.getMutex()) {
        Message message;

        while ((message = m_messageQueue.dequeue(false)) != null) {
          m_delegate.send(message);
        }
      }
    }
    catch (ThreadSafeQueue.ShutdownException e) {
      // Assertion failure.
      throw new RuntimeException("MessageQueue unexpectedly shutdown");
    }
  }

  /**
   * Cleanly shutdown the <code>Sender</code>.
   *
   * <p>Any queued messages are discarded.</p>
   *
   * @exception CommunicationException If an error occurs.
   */
  public void shutdown() throws CommunicationException {
    m_messageQueue.shutdown();
    m_delegate.shutdown();
  }
}
