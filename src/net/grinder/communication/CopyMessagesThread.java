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


/**
 * Thread that copies messages from a {@link Receiver} to a {@link
 * Sender}.
 *
 * @author Philip Aston
 * @version $Revision$
 */
public final class CopyMessagesThread extends Thread {

  private final Receiver m_receiver;
  private final Sender m_sender;
  private boolean m_shutdown = false;

  /**
   * Constructor.
   *
   * @param receiver Receiver to read messages from.
   * @param sender Sender to send messages to.
   */
  public CopyMessagesThread(Receiver receiver, Sender sender) {
    super("CopyMessagesThread");

    m_receiver = receiver;
    m_sender = sender;

    setDaemon(true);
    start();
  }

  /**
   * Main loop.
   */
  public void run() {
    while (!m_shutdown) {
      try {
        final Message message = m_receiver.waitForMessage();
        m_sender.send(message);
      }
      catch (CommunicationException e) {
        if (!m_shutdown) {
          e.printStackTrace();
        }
      }
    }
  }

  /**
   * Shutdown the thread.
   *
   * @throws InterruptedException If the calling thread is interrupted
   * whilst waiting for this thread to shut down.
   */
  public void shutdown() throws InterruptedException {

    m_shutdown = true;

    try {
      m_receiver.shutdown();
    }
    catch (CommunicationException e) {
      // Ignore.
    }

    try {
      m_sender.shutdown();
    }
    catch (CommunicationException e) {
      // Ignore.
    }

    interrupt();

    join();
  }
}
