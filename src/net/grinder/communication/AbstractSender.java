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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.security.MessageDigest;


/**
 * Abstract class that manages the sending of messages.
 *
 * @author Philip Aston
 * @version $Revision$
 **/
abstract class AbstractSender implements Sender {

  private final String m_grinderID;
  private final String m_senderID;
  private long m_nextSequenceID = 0;
  private boolean m_shutdown = false;

  /**
   * Constructor.
   *
   * <p>The <code>grinderID</code> and <code>senderID</code>
   * parameters are used to initialise Messages that originate from
   * this <code>Server</code>.</p>
   *
   * @param grinderID Process identity.
   * @param senderID Unique sender identity.
   */
  protected AbstractSender(String grinderID, String senderID)
    throws CommunicationException {

    m_grinderID = grinderID;

    try {
      final ByteArrayOutputStream byteStream = new ByteArrayOutputStream();

      final Writer writer = new OutputStreamWriter(byteStream);
      writer.write(senderID);
      writer.write(grinderID);
      writer.write(Long.toString(System.currentTimeMillis()));
      writer.close();

      m_senderID =
        new String(MessageDigest.getInstance("MD5").digest(
                     byteStream.toByteArray()));
    }
    catch (Exception e) {
      throw new CommunicationException("Could not calculate sender ID", e);
    }
  }

  /**
   * Constructor for <code>Senders</code> that only route messages.
   */
  protected AbstractSender() {
    m_grinderID = null;
    m_senderID = null;
  }

  /**
   * First flush any pending messages queued with {@link #queue} and
   * then send the given message.
   *
   * @param message A {@link Message}.
   * @exception CommunicationException If an error occurs.
   */
  public final void send(Message message) throws CommunicationException {

    if (m_shutdown) {
      throw new CommunicationException("Shut down");
    }

    if (!message.isInitialised()) {
      if (m_grinderID == null || m_senderID == null) {
        throw new CommunicationException(
          "This Sender can only route messages");
      }
      else {
        synchronized (this) {
          message.setSenderInformation(
            m_grinderID, m_senderID, m_nextSequenceID++);
        }
      }
    }

    try {
      writeMessage(message);
    }
    catch (IOException e) {
      throw new CommunicationException("Exception whilst sending message", e);
    }
  }

  protected abstract void writeMessage(Message message) throws IOException;

  /**
   * Cleanly shutdown the <code>Sender</code>.
   *
   * @throws CommunicationException If an error occurs.
   */
  public void shutdown() throws CommunicationException {
    try {
      final Message message = new CloseCommunicationMessage();
      message.setSenderInformation("internal", "internal", -1);
      send(message);
    }
    catch (CommunicationException e) {
      // Ignore.
    }

    // Keep track of whether we've been closed. Can't rely on delegate
    // as some implementations don't do anything with close(), e.g.
    // ByteArrayOutputStream.
    m_shutdown = true;
  }
}
