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

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.security.MessageDigest;


/**
 * Abstract class that manages the sending of messages.
 *
 * @author Philip Aston
 * @version $Revision$
 **/
abstract class AbstractSender implements Sender {

  private final String m_grinderID;
  private String m_senderID;
  private long m_nextSequenceID = 0;
  private MessageQueue m_messageQueue = new MessageQueue(false);

  private final MyByteArrayOutputStream m_scratchByteStream =
    new MyByteArrayOutputStream();

  protected AbstractSender(String grinderID) {
    m_grinderID = grinderID;
  }

  protected final void setSenderID(String uniqueString)
    throws CommunicationException {
    try {
      final BufferedWriter bufferedWriter =
	new BufferedWriter(new OutputStreamWriter(m_scratchByteStream));

      bufferedWriter.write(uniqueString);
      bufferedWriter.flush();

      m_senderID =
	new String(MessageDigest.getInstance("MD5").digest(
		     m_scratchByteStream.getBytes()));
    }
    catch (Exception e) {
      throw new CommunicationException("Could not calculate sender ID", e);
    }
  }

  /**
   * First flush any pending messages queued with {@link #queue} and
   * then send the given message.
   *
   * @param message A {@link Message}.
   * @exception CommunicationException If an error occurs.
   **/
  public final void send(Message message) throws CommunicationException {
    synchronized(m_messageQueue.getMutex()) {
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
   **/
  public final void queue(Message message) throws CommunicationException {
    synchronized (this) {
      message.setSenderInformation(m_grinderID, m_senderID,
				   m_nextSequenceID++);
    }

    try {
      m_messageQueue.queue(message);
    }
    catch (MessageQueue.ShutdownException e) {
      // Assertion failure.
      throw new RuntimeException(
	"MessageQueue unexpectedly shutdown");
    }
  }

  /**
   * Flush any pending messages queued with {@link #queue}.
   *
   * @exception CommunicationException if an error occurs
   **/
  public final void flush() throws CommunicationException {
    try {
      synchronized (m_messageQueue.getMutex()) {
	Message message;

	while ((message = m_messageQueue.dequeue(false)) != null) {
	  writeMessage(message);
	}
      }
    }
    catch (IOException e) {
      throw new CommunicationException(
	"Exception whilst sending message", e);
    }
    catch (MessageQueue.ShutdownException e) {
      // Assertion failure.
      throw new RuntimeException(
	"MessageQueue unexpectedly shutdown");
    }
  }

  protected abstract void writeMessage(Message message) throws IOException;

  /**
   * Cleanly shutdown the <code>Sender</code>.
   *
   * <p>Any queued messages are discarded.</p>
   *
   * @exception CommunicationException If an error occurs.
   **/
  public void shutdown() throws CommunicationException {
    m_messageQueue.shutdown();
  }

  protected final MyByteArrayOutputStream getScratchByteStream() {
    return m_scratchByteStream;
  }

  /**
   * Abuse Java API to avoid needless proliferation of temporary
   * objects.
   * @author Philip Aston
   **/
  protected static final class MyByteArrayOutputStream
    extends ByteArrayOutputStream {
    public byte[] getBytes() {
      return buf;
    }
  }
}
