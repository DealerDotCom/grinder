// Copyright (C) 2003 Philip Aston
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
import java.io.OutputStream;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;


/**
 * Manages the sending of messages to many streams.
 *
 * @author Philip Aston
 * @version $Revision$
 */
public final class FanOutStreamSender extends AbstractSender {

  private final Kernel m_kernel;
  private final Set m_outputStreams = new HashSet();

  /**
   * Constructor.
   */
  public FanOutStreamSender() {
    this(new Kernel(3));
  }

  /**
   * Constructor.
   *
   * @param Kernel Kernel to use.
   */
  private FanOutStreamSender(Kernel kernel) {
    m_kernel = kernel;
  }

  /**
   * Add a stream.
   *
   * @param stream The stream.
   */
  public void add(OutputStream stream) {
    synchronized (m_outputStreams) {
      m_outputStreams.add(stream);
    }
  }

  /**
   * Remove a stream.
   *
   * @param stream The stream.
   */
  public void remove(OutputStream stream) {
    synchronized (m_outputStreams) {
      m_outputStreams.remove(stream);
    }
  }

  /**
   * Send a message.
   *
   * @param message The message.
   * @exception IOException If an error occurs.
   */
  protected void writeMessage(Message message) throws IOException {

    try {
      synchronized (m_outputStreams) {
        final Iterator iterator = m_outputStreams.iterator();

        while (iterator.hasNext()) {
          m_kernel.execute(
            new WriteMessageToStream(message, (OutputStream) iterator.next()));
        }
      }
    }
    catch (Kernel.ShutdownException e) {
      // Assertion failure.
      throw new RuntimeException("Kernel unexpectedly shutdown");
    }
  }

  /**
   * Shut down this sender.
   *
   * @throws CommunicationException If an IO exception occurs.
   */
  public void shutdown() throws CommunicationException {
    super.shutdown();
    m_kernel.forceShutdown();
  }

  private static final class WriteMessageToStream implements Runnable {
    private final Message m_message;
    private final OutputStream m_stream;

    public WriteMessageToStream(Message message, OutputStream stream) {
      m_message = message;
      m_stream = stream;
    }

    public void run() {
      try {
        writeMessageToStream(m_message, m_stream);
      }
      catch (IOException e) {
        //            m_messageQueue.queue(e);
        e.printStackTrace();
      }
    }
  }
}
