package net.grinder.console.synchronisation;

import net.grinder.console.communication.ConsoleCommunication;
import net.grinder.synchronisation.LocalBarrierGroups;
import net.grinder.synchronisation.BarrierGroup.Listener;
import net.grinder.synchronisation.messages.OpenBarrierMessage;


/**
 * Centralised record of distributed barriers.
 *
 * @author Philip Aston
 */
final class ConsoleBarrierGroups extends LocalBarrierGroups {

  private final ConsoleCommunication m_communication;

  /**
   * Constructor.
   *
   * @param communication Console communication.
   */
  public ConsoleBarrierGroups(ConsoleCommunication communication) {
    m_communication = communication;

//      timer.schedule(new TimerTask() {
//
//        @Override
//        public void run() {
//          System.out.printf("%s%n", ConsoleBarrierGroups.this);
//
//        }}, 3000, 3000);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected BarrierGroupImplementation createBarrierGroup(final String name) {
    final BarrierGroupImplementation group = super.createBarrierGroup(name);

    group.addListener(new Listener() {
      public void awaken() {
        m_communication.sendToAgents(new OpenBarrierMessage(name));
      }
    });

    return group;
  }
}
