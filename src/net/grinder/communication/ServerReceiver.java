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

import java.io.InputStream;
import java.io.IOException;
import java.io.ObjectInputStream;

import net.grinder.util.thread.ThreadPool;
import net.grinder.util.thread.ThreadSafeQueue;


/**
 * Manages the receipt of messages from many clients.
 *
 * @author Philip Aston
 * @version $Revision$
 */
public final class ServerReceiver implements Receiver {

  private final ResourcePool m_acceptedSocketSet;
  private final MessageQueue m_messageQueue = new MessageQueue(true);
  private final ThreadPool m_threadPool;

  /**
   * Constructor.
   *
   * @param acceptor Acceptor.
   * @param connectionType Connection type.
   * @param numberOfThreads Number of listen threads to use.
   */
  public ServerReceiver(Acceptor acceptor, ConnectionType connectionType,
                        int numberOfThreads) {

    m_acceptedSocketSet = acceptor.getSocketSet(connectionType);

    final ThreadPool.RunnableFactory runnableFactory =
      new ThreadPool.RunnableFactory() {
        public Runnable create() {
          return new Runnable() {
              public void run() { process(); }
            };
        }
      };

    m_threadPool =
      new ThreadPool("Server receiver", numberOfThreads, runnableFactory);

    m_threadPool.start();
  }

  /**
   * Block until a message is available, or another thread has called
   * {@link #shutdown}. Typically called from a message dispatch loop.
   *
   * <p>Multiple threads can call this method, but only one thread
   * will receive a given message.</p>
   *
   * @return The message or <code>null</code> if shut down.
   * @throws CommunicationException If an error occured receiving a message.
   */
  public Message waitForMessage() throws CommunicationException {

    try {
      return m_messageQueue.dequeue(true);
    }
    catch (ThreadSafeQueue.ShutdownException e) {
      return null;
    }
  }

  /**
   * Shut down this receiver.
   */
  public void shutdown() {

    m_messageQueue.shutdown();
    m_threadPool.stop();
  }

  /**
   * Return the thread group used for our threads. Package scope; used
   * by the unit tests.
   *
   * @return The thread group.
   */
  ThreadGroup getThreadGroup() {
    return m_threadPool.getThreadGroup();
  }

  private void process() {

    try {
      // Did we do some work on the last pass?
      boolean idle = false;

      while (true) {
        final ResourcePool.Reservation reservation =
          m_acceptedSocketSet.reserveNext();

        try {
          if (reservation.isSentinel()) {
            if (idle) {
              Thread.sleep(500);
            }

            idle = true;
          }
          else {
            final SocketWrapper socketResource =
              (SocketWrapper)reservation.getResource();

            final InputStream inputStream = socketResource.getInputStream();

            if (inputStream.available() > 0) {

              final ObjectInputStream objectStream =
                new ObjectInputStream(inputStream);

              final Message message = (Message)objectStream.readObject();

              if (message instanceof CloseCommunicationMessage) {
                reservation.close();
              }
              else {
                m_messageQueue.queue(message);
              }

              idle = false;
            }
          }
        }
        catch (IOException e) {
          reservation.close();
          m_messageQueue.queue(e);
        }
        catch (ClassNotFoundException e) {
          reservation.close();
          m_messageQueue.queue(e);
        }
        finally {
          reservation.free();
        }
      }
    }
    catch (ThreadSafeQueue.ShutdownException e) {
      // We've been shutdown, exit this thread.
    }
    catch (InterruptedException e) {
      // We've been shutdown, exit this thread.
    }
  }
}
