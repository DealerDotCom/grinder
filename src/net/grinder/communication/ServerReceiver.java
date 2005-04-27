// Copyright (C) 2000, 2001, 2002, 2003, 2004, 2005 Philip Aston
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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.grinder.communication.ResourcePool.Reservation;
import net.grinder.util.thread.ThreadPool;
import net.grinder.util.thread.ThreadSafeQueue;
import net.grinder.util.thread.ThreadSafeQueue.ShutdownException;


/**
 * Manages the receipt of messages from many clients.
 *
 * @author Philip Aston
 * @version $Revision$
 */
public final class ServerReceiver implements Receiver {

  private final MessageQueue m_messageQueue = new MessageQueue(true);
  private final List m_threadPools = new ArrayList();

  /**
   * Constructor.
   */
  public ServerReceiver() {
  }

  /**
   * Registers a new (socket, connection type) pair which the
   * <code>ServerReceiver</code> should process messages from.
   *
   * <p>
   * A single <code>ServerReceiver</code> can listen to multiple (socket,
   * connection type). You can register the same (socket, connection type) pair
   * with multiple <code>ServerReceiver</code>s, but there is no way of
   * controlling which receiver will receive messages from the pair.
   * </p>
   *
   * @param acceptor
   *          Acceptor.
   * @param connectionType
   *          Connection type.
   * @param numberOfThreads
   *          How many threads to dedicate to processing the (socket,
   *          connectionType) pair.
   * @exception CommunicationException
   *              If this <code>ServerReceiver</code> has been shutdown.
   */
  public void receiveFrom(Acceptor acceptor,
                          ConnectionType connectionType,
                          int numberOfThreads)
    throws CommunicationException {


    final ResourcePool acceptedSocketSet =
      acceptor.getSocketSet(connectionType);

    final ThreadPool.RunnableFactory runnableFactory =
      new ThreadPool.RunnableFactory() {
        public Runnable create() {
          return new Runnable() {
              public void run() { process(acceptedSocketSet); }
            };
        }
      };

    final ThreadPool threadPool =
      new ThreadPool("ServerReceiver (" + acceptor.getPort() + ", " +
                     connectionType + ")",
                     numberOfThreads,
                     runnableFactory);

    synchronized (this) {
      try {
        m_messageQueue.checkIfShutdown();
      }
      catch (ShutdownException e) {
        throw new CommunicationException("Shut down", e);
      }

      m_threadPools.add(threadPool);
    }

    threadPool.start();
  }

  /**
   * Block until a message is available, or another thread has called
   * {@link #shutdown}. Typically called from a message dispatch loop.
   *
   * <p>Multiple threads can call this method, but only one thread
   * will receive a given message.</p>
   *
   * @return The message or <code>null</code> if shut down.
   * @throws CommunicationException If an error occurred receiving a message.
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
  public synchronized void shutdown() {

    m_messageQueue.shutdown();

    final Iterator iterator = m_threadPools.iterator();

    while (iterator.hasNext()) {
      ((ThreadPool)iterator.next()).stop();
    }
  }

  /**
   * Return the number of active threads. Package scope; used by the unit tests.
   *
   * @return The number of active threads.
   */
  synchronized int getActveThreadCount() {
    int result = 0;

    final Iterator iterator = m_threadPools.iterator();

    while (iterator.hasNext()) {
      result += ((ThreadPool)iterator.next()).getThreadGroup().activeCount();
    }

    return result;
  }

  private void process(ResourcePool acceptedSocketSet) {

    try {
      // Did we do some work on the last pass?
      boolean idle = false;

      while (true) {
        final Reservation reservation = acceptedSocketSet.reserveNext();

        try {
          if (reservation.isSentinel()) {
            if (idle) {
              Thread.sleep(500);
            }

            idle = true;
          }
          else {
            // TODO Find a way of tunneling connection identity through to
            // receiver. Can then use this in ProcessStatusSetImplementation.
            final SocketWrapper socketWrapper =
              (SocketWrapper)reservation.getResource();

            // We don't need to synchronise access to the SocketWrapper;
            // access is protected through the socket set and only we hold
            // the reservation.
            final InputStream inputStream = socketWrapper.getInputStream();

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
