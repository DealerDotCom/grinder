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
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Iterator;


/**
 * Manages the sending of messages to many clients.
 *
 * @author Philip Aston
 * @version $Revision$
 */
public final class ServerSender extends AbstractSender {

  private final Acceptor m_acceptor;
  private final ThreadSafeQueue m_workQueue = new ThreadSafeQueue();

  /**
   * Factory method that creates a <code>ServerSender</code> that
   * listens on the given address.
   *
   * @param grinderID A string describing our Grinder process.
   * @param addressString The TCP address to listen on. Zero-length
   * string => listen on all interfaces.
   * @param port The TCP port to listen to. 0 => any local port.
   * @return The ServerSender.
   * @throws CommunicationException If server socket could not be
   * bound.
   */
  public static ServerSender bindTo(String grinderID, String addressString,
                                    int port)
    throws CommunicationException {

    final Acceptor acceptor = new Acceptor(addressString, port);

    try {
      final String senderID =
        addressString + ":" + acceptor.getPort() + ":" +
        InetAddress.getLocalHost().getHostName();

      return new ServerSender(grinderID, senderID, acceptor, 3);
    }
    catch (UnknownHostException e) {
      throw new CommunicationException("Can't get local host", e);
    }
  }

  /**
   * Constructor.
   *
   * @param grinderID A string describing our Grinder process.
   * @param senderID Unique string identifying sender.
   * @param acceptor Acceptor that manages connections to our server socket.
   * @param numberOfThreads Number of sender threads to use.
   * @throws CommunicationException If server socket could not be
   * bound.
   */
  private ServerSender(String grinderID, String senderID, Acceptor acceptor,
                       int numberOfThreads)
    throws CommunicationException {

    super(grinderID, senderID);

    m_acceptor = acceptor;

    final ThreadGroup threadGroup = m_acceptor.getThreadGroup();

    for (int i = 0; i < numberOfThreads; ++i) {
      new SenderThread(threadGroup, i).start();
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
      final Iterator iterator =
        m_acceptor.getSocketSet().reserveAllHandles().iterator();

      while (iterator.hasNext()) {
        m_workQueue.queue(
          new MessageHandlePair(message, (SocketSet.Handle) iterator.next()));
      }
    }
    catch (ThreadSafeQueue.ShutdownException e) {
      // Assertion failure.
      throw new RuntimeException("MessageQueue unexpectedly shutdown");
    }
    catch (InterruptedException e) {
      // Assertion failure.
      throw new RuntimeException("Unexpectedly shutdown");
    }
  }

  /**
   * Shut down this reciever.
   *
   * @throws CommunicationException If an IO exception occurs.
   */
  public void shutdown() throws CommunicationException {

    super.shutdown();

    m_workQueue.shutdown();

    // Will also shut down our SenderThreads.
    m_acceptor.shutdown();
  }

  /**
   * Return the Acceptor. Used by the unit tests.
   *
   * @return The number of connections.
   */
  public Acceptor getAcceptor() {
    return m_acceptor;
  }

  private final class SenderThread extends Thread {

    public SenderThread(ThreadGroup threadGroup, int senderThreadIndex) {
      super(threadGroup, "Sender thread " + senderThreadIndex);
      setDaemon(true);
    }

    public void run() {

      try {
        while (true) {
          final MessageHandlePair pair =
            (MessageHandlePair) m_workQueue.dequeue(true);

          final Message message = pair.getMessage();
          final SocketSet.Handle socketHandle = pair.getHandle();

          try {
            // See note in ClientSender.writeMessage regarding why we
            // create an ObjectOutputStream for every message.
            final ObjectOutputStream objectStream =
              new ObjectOutputStream(socketHandle.getOutputStream());

            objectStream.writeObject(message);
            objectStream.flush();
          }
          catch (IOException e) {
            socketHandle.close();
            //            m_messageQueue.queue(e);
            e.printStackTrace();
          }
          finally {
            socketHandle.free();
          }
        }
      }
      catch (ThreadSafeQueue.ShutdownException e) {
        // We've been shutdown, exit this thread.
      }
    }
  }

  private static final class MessageHandlePair {
    private final Message m_message;
    private final SocketSet.Handle m_handle;

    public MessageHandlePair(Message message, SocketSet.Handle handle) {
      m_message = message;
      m_handle = handle;
    }

    public Message getMessage() {
      return m_message;
    }

    public SocketSet.Handle getHandle() {
      return m_handle;
    }
  }
}
