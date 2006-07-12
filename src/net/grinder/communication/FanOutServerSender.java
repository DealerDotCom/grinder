// Copyright (C) 2003, 2004, 2005, 2006 Philip Aston
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

import java.io.OutputStream;
import java.util.Iterator;

import net.grinder.util.thread.Kernel;


/**
 * Manages the sending of messages to many TCP clients.
 *
 * @author Philip Aston
 * @version $Revision$
 */
public final class FanOutServerSender
  extends AbstractFanOutSender implements CheckIfPeerShutdown {

  /**
   * Constructor.
   *
   * @param acceptor Acceptor.
   * @param connectionType Connection type.
   * @param numberOfThreads Number of sender threads to use.
   */
  public FanOutServerSender(Acceptor acceptor, ConnectionType connectionType,
                            int numberOfThreads) {

    this(acceptor.getSocketSet(connectionType), new Kernel(numberOfThreads));
  }

  /**
   * Constructor.
   *
   * @param acceptedSocketSet Socket set.
   * @param kernel A kernel to use.
   * @throws CommunicationException If server socket could not be
   * bound.
   */
  private FanOutServerSender(ResourcePool acceptedSocketSet, Kernel kernel) {

    super(kernel, acceptedSocketSet);
  }

  /**
   * Return an output stream from a socket resource.
   *
   * @param resource The resource.
   * @return The output stream.
   * @throws CommunicationException If the output stream could not be
   * obtained from the socket.
   */
  protected OutputStream resourceToOutputStream(
    ResourcePool.Resource resource) throws CommunicationException {

    // We don't need to synchronise access to the SocketWrapper;
    // access is protected through the socket set and only we hold
    // the reservation.
    return ((SocketWrapper)resource).getOutputStream();
  }

  /**
   * Check whether any peer connection has been shut down. If so,
   * clean it up and return <code>true</code>.
   *
   * @return boolean <code>true</code> => at least one peer has been shut
   * down.
   */
  public boolean isPeerShutdown() {
    boolean result = false;

    // Reserve the lot.
    final Iterator iterator = getResourcePool().reserveAll().iterator();

    try {
      while (iterator.hasNext()) {
        final ResourcePool.Reservation reservation =
          (ResourcePool.Reservation) iterator.next();

        try {
          if (((SocketWrapper)reservation.getResource()).isPeerShutdown()) {
            result = true;
            // Don't break, we want to clean them all up.
          }
        }
        finally {
          reservation.free();
        }
      }
    }
    finally {
      while (iterator.hasNext()) {
        ((ResourcePool.Reservation) iterator.next()).free();
      }
    }

    return result;
  }
}
