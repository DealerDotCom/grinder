// Copyright (C) 2011 Philip Aston
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
// COPYRIGHT HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
// INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
// (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
// SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
// HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
// STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
// ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
// OF THE POSSIBILITY OF SUCH DAMAGE.

package net.grinder.synchronisation;

import java.util.concurrent.atomic.AtomicInteger;

import net.grinder.common.processidentity.WorkerIdentity;
import net.grinder.communication.CommunicationException;
import net.grinder.communication.MessageDispatchRegistry;
import net.grinder.communication.QueuedSender;
import net.grinder.communication.MessageDispatchRegistry.AbstractHandler;
import net.grinder.synchronisation.BarrierGroup.BarrierIdentity;
import net.grinder.synchronisation.BarrierGroup.BarrierIdentityGenerator;
import net.grinder.synchronisation.messages.AddBarrierMessage;
import net.grinder.synchronisation.messages.AddWaiterMessage;
import net.grinder.synchronisation.messages.CancelWaiterMessage;
import net.grinder.synchronisation.messages.OpenBarrierMessage;
import net.grinder.synchronisation.messages.RemoveBarriersMessage;


/**
 * {@link BarrierGroups} implementation which delegates to a remote instance.
 *
 * @author Philip Aston
 * @version $Revision:$
 */
public class GlobalBarrierGroups extends AbstractBarrierGroups {

  private final BarrierIdentityGenerator m_identityGenerator =
    new GlobalIdentityGenerator();

  private final QueuedSender m_sender;
  private final WorkerIdentity m_workerIdentity;

  /**
   * Constructor.
   *
   * @param sender Used to send messages to the remote instance (the console).
   * @param messageDispatch Used to receive messages from the remote instance.
   * @param workerIdentity Unique process identity.
   */
  public GlobalBarrierGroups(QueuedSender sender,
                             MessageDispatchRegistry messageDispatch,
                             WorkerIdentity workerIdentity) {
    m_sender = sender;
    m_workerIdentity = workerIdentity;

    messageDispatch.set(
      OpenBarrierMessage.class,
      new AbstractHandler<OpenBarrierMessage>() {
        public void handle(OpenBarrierMessage message) {
          final InternalBarrierGroup existingGroup =
            getExistingGroup(message.getName());

          if (existingGroup != null) {
            existingGroup.fireAwaken();
          }
        }
      });
  }

  /**
   * {@inheritDoc}
   */
  public BarrierIdentityGenerator getIdentityGenerator() {
    return m_identityGenerator;
  }

  /**
   * {@inheritDoc}
   */
  @Override protected InternalBarrierGroup createBarrierGroup(String name) {
    return new GlobalBarrierGroup(name);
  }

  /**
   * Extends {@link AbstractBarrierGroups} which verifies the consistency of the
   * local partition of the barrier group.
   */
  private class GlobalBarrierGroup extends AbstractBarrierGroup {

    /**
     * Constructor.
     *
     * @param name Barrier group name.
     */
    public GlobalBarrierGroup(String name) {
      super(name);
    }

    /**
     * {@inheritDoc}
     */
    public void addBarrier() {
      super.addBarrier();

      try {
        m_sender.queue(new AddBarrierMessage(getName()));
      }
      catch (CommunicationException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }

    /**
     * {@inheritDoc}
     */
    public void removeBarriers(int n) {
      super.removeBarriers(n);

      try {
        m_sender.queue(new RemoveBarriersMessage(getName(), n));
      }
      catch (CommunicationException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }

    /**
     * {@inheritDoc}
     */
    public void addWaiter(BarrierIdentity barrierIdentity) {
      super.addWaiter(barrierIdentity);

      try {
        m_sender.queue(new AddWaiterMessage(getName(), barrierIdentity));
      }
      catch (CommunicationException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }

    /**
     * {@inheritDoc}
     */
    public void cancelWaiter(BarrierIdentity barrierIdentity) {
      super.cancelWaiter(barrierIdentity);

      try {
        m_sender.queue(new CancelWaiterMessage(getName(), barrierIdentity));
      }
      catch (CommunicationException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }
  }

  private class GlobalIdentityGenerator implements BarrierIdentityGenerator {

    private final AtomicInteger m_next = new AtomicInteger();

    public BarrierIdentity next() {
      return new GlobalBarrierIdentity(m_workerIdentity,
                                       m_next.getAndIncrement());
    }
  }

  private static final class GlobalBarrierIdentity
    extends LocalBarrierIdentity {

    private final WorkerIdentity m_workerIdentity;

    public GlobalBarrierIdentity(WorkerIdentity workerIdentity,
                                 int value) {
      super(value);
      m_workerIdentity = workerIdentity;
    }

    @Override public int hashCode() {
      return super.hashCode() * 17 + m_workerIdentity.hashCode();
    }

    @Override public boolean equals(Object o) {
      if (this == o) {
        return true;
      }

      if (o == null || getClass() != o.getClass()) {
        return false;
      }

      final GlobalBarrierIdentity other = (GlobalBarrierIdentity)o;

      return m_workerIdentity.equals(other.m_workerIdentity) &&
             super.equals(o);
    }
  }
}
