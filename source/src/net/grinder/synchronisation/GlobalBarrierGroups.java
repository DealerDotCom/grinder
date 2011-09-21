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

import net.grinder.common.processidentity.WorkerIdentity;
import net.grinder.communication.CommunicationException;
import net.grinder.communication.MessageDispatchRegistry;
import net.grinder.communication.QueuedSender;
import net.grinder.communication.MessageDispatchRegistry.AbstractHandler;
import net.grinder.synchronisation.BarrierGroup.BarrierIdentityGenerator;
import net.grinder.synchronisation.messages.AddBarrierMessage;
import net.grinder.synchronisation.messages.AddWaiterMessage;
import net.grinder.synchronisation.messages.BarrierIdentity;
import net.grinder.synchronisation.messages.CancelWaiterMessage;
import net.grinder.synchronisation.messages.OpenBarrierMessage;
import net.grinder.synchronisation.messages.RemoveBarriersMessage;


/**
 * {@link BarrierGroups} implementation which delegates to a remote instance.
 *
 * @author Philip Aston
 */
public class GlobalBarrierGroups extends AbstractBarrierGroups {

  private final BarrierIdentityGenerator m_identityGenerator;

  private final QueuedSender m_sender;

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
    m_identityGenerator = new IdentityGeneratorImplementation(workerIdentity);

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

  // TODO could be a wrapper.
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
    @Override public void addBarrier() throws CommunicationException {
      super.addBarrier();

      m_sender.queue(new AddBarrierMessage(getName()));
    }

    /**
     * {@inheritDoc}
     */
    @Override public void removeBarriers(long n) throws CommunicationException {
      super.removeBarriers(n);

      m_sender.queue(new RemoveBarriersMessage(getName(), n));
    }

    /**
     * {@inheritDoc}
     */
    @Override public void addWaiter(BarrierIdentity barrierIdentity)
      throws CommunicationException {

      super.addWaiter(barrierIdentity);

      m_sender.queue(new AddWaiterMessage(getName(), barrierIdentity));
    }

    /**
     * {@inheritDoc}
     */
    @Override public void cancelWaiter(BarrierIdentity barrierIdentity)
      throws CommunicationException {

      super.cancelWaiter(barrierIdentity);

      m_sender.queue(new CancelWaiterMessage(getName(), barrierIdentity));
    }
  }
}
