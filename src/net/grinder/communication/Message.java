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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;


/**
 * Base class for messages.
 *
 * @author Philip Aston
 * @version $Revision$
 */
public abstract class Message implements Serializable {
  private static final long serialVersionUID = 6389542594440493966L;

  /**  The ID of the Grinder process which owns this {@link Sender}. **/
  private transient String m_senderGrinderID = null;

  /** Unique ID of {@link Sender}. **/
  private transient String m_senderUniqueID = null;

  /** Sequence ID of message. **/
  private transient long m_sequenceNumber = -1;

  /**
   * Called by {@link Sender} before dispatching the Message.
   **/
  final void setSenderInformation(String grinderID, String senderUniqueID,
                                  long sequenceNumber) {
    m_senderGrinderID = grinderID;
    m_senderUniqueID = senderUniqueID;
    m_sequenceNumber = sequenceNumber;
  }

  /**
   * Returns a string describing the Grinder process associated of the
   * {@link Sender}.
   *
   * @throws RuntimeException If {@link #setSenderInformation} has not
   * been called.
   **/
  final String getSenderGrinderID() {
    assertInitialised();
    return m_senderGrinderID;
  }

  /**
   * Returns a unique ID for the {@link Sender}.
   *
   * @return The unique ID.
   * @exception RuntimeException If {@link #setSenderInformation} has not
   * been called.
   */
  public final String getSenderUniqueID() {
    assertInitialised();
    return m_senderUniqueID;
  }

  /**
   * Get the message sequence ID.
   *
   * @throws RuntimeException If {@link #setSenderInformation} has not
   * been called.
   **/
  final long getSequenceNumber() {
    assertInitialised();
    return m_sequenceNumber;
  }

  /**
   * @throws RuntimeException If {@link #setSenderInformation} has not
   * been called.
   **/
  private final void assertInitialised() {
    if (m_senderUniqueID == null) {
      throw new RuntimeException("Message not initialised");
    }
  }

  /**
   * Customise serialisation for efficiency.
   *
   * @param out The stream to write our data to.
   **/
  private void writeObject(ObjectOutputStream out) throws IOException {
    out.defaultWriteObject();
    out.writeUTF(m_senderGrinderID);
    out.writeUTF(m_senderUniqueID);
    out.writeLong(m_sequenceNumber);
  }

  /**
   * Customise serialisation for efficiency.
   *
   * @param in The stream to read our data from.
   **/
  private void readObject(ObjectInputStream in)
    throws IOException, ClassNotFoundException {
    in.defaultReadObject();
    m_senderGrinderID = in.readUTF();
    m_senderUniqueID = in.readUTF();
    m_sequenceNumber = in.readLong();
  }

  /**
   * Compare two Messages. Sent messages have enhanced equality
   * semantics - they are equivalent if they have the same sender ID
   * and sequence number.
   *
   * @param o The other object.
   * @return <code>true</code> => Messages are equal.
   */
  public final boolean equals(Object o) {
    if (o == this) {
      return true;
    }

    if (!(o instanceof Message)) {
      return false;
    }

    final Message message = (Message)o;

    return
      m_sequenceNumber != -1 &&
      m_sequenceNumber == message.m_sequenceNumber &&
      m_senderUniqueID.equals(message.m_senderUniqueID);
  }

  /**
   * Implement hash code.
   *
   * @return The hash code.
   */
  public final int hashCode() {
    return
      (int) m_sequenceNumber ^ (int) (m_sequenceNumber >> 32) ^
      (m_senderUniqueID != null ? m_senderUniqueID.hashCode() : 0);
  }
}
