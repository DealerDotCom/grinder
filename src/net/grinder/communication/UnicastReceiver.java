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


/**
 * Class that manages the receipt of unicast messages.
 *
 * @author Philip Aston
 * @version $Revision$
 */
public final class UnicastReceiver extends AbstractReceiver {

  private final Acceptor m_acceptor;

  /**
   * Constructor.
   *
   * @param addressString The TCP address to listen on. Zero-length
   * string => listen on all interfaces.
   * @param port The TCP port to listen to.
   * @throws CommunicationException If server socket could not be
   * bound.
   */
  public UnicastReceiver(String addressString, int port)
    throws CommunicationException {

    this(new Acceptor(addressString, port), 5);
  }

  /**
   * Constructor.
   *
   * @param acceptor Acceptor that manages connections to our server socket.
   * @param numberOfThreads Number of listen threads to use.
   * @throws CommunicationException If server socket could not be
   * bound.
   */
  UnicastReceiver(Acceptor acceptor, int numberOfThreads)
    throws CommunicationException {

    super(false);        // TCP guarantees message sequence so we
                         // don't have to.

    m_acceptor = acceptor;

    final ThreadGroup threadGroup = acceptor.getThreadGroup();

    for (int i = 0; i < numberOfThreads; ++i) {
      new ListenThread(threadGroup, i).start();
    }
  }

  /**
   * Shut down this reciever.
   *
   * @throws CommunicationException If an IO exception occurs.
   */
  public void shutdown() throws CommunicationException {

    super.shutdown();

    // Will also shut down our ListenThreads.
    m_acceptor.shutdown();
  }

  private final class ListenThread extends Thread {

    public ListenThread(ThreadGroup threadGroup, int listenThreadIndex) {
      super(threadGroup, "Unicast listen thread " + listenThreadIndex);
      setDaemon(true);
    }

    public void run() {
      final MessageQueue messageQueue = getMessageQueue();

      try {
        // Did we do some work on the last pass?
        boolean idle = false;

        while (true) {
          final SocketSet.Handle socketHandle =
            m_acceptor.getSocketSet().reserveNextHandle();

          try {
            if (socketHandle.isSentinel()) {
              if (idle) {
                Thread.sleep(500);
              }

              idle = true;
            }
            else {
              final Message m = socketHandle.pollForMessage();

              if (m instanceof CloseCommunicationMessage) {
                socketHandle.close();
              }
              else if (m != null) {
                messageQueue.queue(m);
              }

              idle = false;
            }
          }
          catch (IOException e) {
            socketHandle.close();
            messageQueue.queue(e);
          }
          catch (ClassNotFoundException e) {
            socketHandle.close();
            messageQueue.queue(e);
          }
          finally {
            socketHandle.free();
          }
        }
      }
      catch (MessageQueue.ShutdownException e) {
        // We've been shutdown, exit this thread.
      }
      catch (InterruptedException e) {
        // Ignore.
      }
    }
  }
}
