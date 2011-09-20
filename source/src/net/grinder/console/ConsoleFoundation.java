// Copyright (C) 2000 Paco Gomez
// Copyright (C) 2000 - 2011 Philip Aston
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

package net.grinder.console;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import net.grinder.common.GrinderException;
import net.grinder.common.Logger;
import net.grinder.common.processidentity.WorkerIdentity;
import net.grinder.common.processidentity.WorkerProcessReport;
import net.grinder.communication.CommunicationException;
import net.grinder.communication.MessageDispatchRegistry;
import net.grinder.communication.MessageDispatchRegistry.AbstractHandler;
import net.grinder.console.common.ErrorHandler;
import net.grinder.console.common.ErrorQueue;
import net.grinder.console.common.Resources;
import net.grinder.console.communication.ConsoleCommunication;
import net.grinder.console.communication.ConsoleCommunicationImplementation;
import net.grinder.console.communication.DistributionControlImplementation;
import net.grinder.console.communication.ProcessControl;
import net.grinder.console.communication.ProcessControlImplementation;
import net.grinder.console.communication.ProcessControl.ProcessReports;
import net.grinder.console.communication.server.DispatchClientCommands;
import net.grinder.console.distribution.FileDistribution;
import net.grinder.console.distribution.FileDistributionImplementation;
import net.grinder.console.model.ConsoleProperties;
import net.grinder.console.model.SampleModel;
import net.grinder.console.model.SampleModelImplementation;
import net.grinder.console.model.SampleModelViews;
import net.grinder.console.model.SampleModelViewsImplementation;
import net.grinder.messages.console.RegisterExpressionViewMessage;
import net.grinder.messages.console.RegisterTestsMessage;
import net.grinder.messages.console.ReportStatisticsMessage;
import net.grinder.statistics.StatisticsServicesImplementation;
import net.grinder.synchronisation.AbstractBarrierGroups;
import net.grinder.synchronisation.BarrierGroup;
import net.grinder.synchronisation.BarrierGroups;
import net.grinder.synchronisation.LocalBarrierGroups;
import net.grinder.synchronisation.BarrierGroup.BarrierIdentityGenerator;
import net.grinder.synchronisation.BarrierGroup.Listener;
import net.grinder.synchronisation.messages.AddBarrierMessage;
import net.grinder.synchronisation.messages.AddWaiterMessage;
import net.grinder.synchronisation.messages.BarrierIdentity;
import net.grinder.synchronisation.messages.CancelWaiterMessage;
import net.grinder.synchronisation.messages.OpenBarrierMessage;
import net.grinder.synchronisation.messages.RemoveBarriersMessage;
import net.grinder.util.Directory;

import org.picocontainer.DefaultPicoContainer;
import org.picocontainer.MutablePicoContainer;
import org.picocontainer.Parameter;
import org.picocontainer.behaviors.Caching;
import org.picocontainer.parameters.ComponentParameter;
import org.picocontainer.parameters.ConstantParameter;


/**
 * This is the entry point of The Grinder console.
 *
 * @author Paco Gomez
 * @author Philip Aston
 **/
public final class ConsoleFoundation {

  private final MutablePicoContainer m_container;
  private final Timer m_timer;

  /**
   * Constructor. Locates the console properties in the user's home directory.
   *
   * @param resources Console resources
   * @param logger Logger.
   *
   * @exception GrinderException If an error occurs.
   */
  public ConsoleFoundation(Resources resources, Logger logger)
    throws GrinderException {

    this(resources,
         logger,
         new Timer(true),
         new ConsoleProperties(
            resources,
            // Some platforms do not have user home directories, fall back
            // to java.home.
            new File(System.getProperty("user.home",
                       System.getProperty("java.home")),
                     ".grinder_console")));
  }

  /**
   * Constructor. Allows properties to be specified.
   *
   * @param resources Console resources
   * @param logger Logger.
   * @param timer A timer.
   * @param properties The properties.
   *
   * @exception GrinderException If an error occurs.
   */
  public ConsoleFoundation(Resources resources,
                           Logger logger,
                           Timer timer,
                           ConsoleProperties properties)
    throws GrinderException {

    m_timer = timer;

    m_container = new DefaultPicoContainer(new Caching());
    m_container.addComponent(logger);
    m_container.addComponent(resources);
    m_container.addComponent(properties);
    m_container.addComponent(timer);
    m_container.addComponent(StatisticsServicesImplementation.getInstance());

    m_container.addComponent(SampleModelImplementation.class);
    m_container.addComponent(SampleModelViewsImplementation.class);
    m_container.addComponent(ConsoleCommunicationImplementation.class);
    m_container.addComponent(DistributionControlImplementation.class);
    m_container.addComponent(ProcessControlImplementation.class);

    m_container.addComponent(
      FileDistributionImplementation.class,
      FileDistributionImplementation.class,
      new Parameter[] {
        new ComponentParameter(DistributionControlImplementation.class),
        new ComponentParameter(ProcessControlImplementation.class),
        new ConstantParameter(properties.getDistributionDirectory()),
        new ConstantParameter(properties.getDistributionFileFilterPattern()),
      });

    m_container.addComponent(DispatchClientCommands.class);

    m_container.addComponent(WireFileDistribution.class);

    m_container.addComponent(WireMessageDispatch.class);

    m_container.addComponent(ErrorQueue.class);

    m_container.addComponent(ConsoleBarrierGroups.class);
    m_container.addComponent(WireDistributedBarriers.class);
  }

  /**
   * Factory method to create a console user interface implementation.
   * PicoContainer is used to satisfy the requirements of the implementation's
   * constructor.
   *
   * @param uiClass
   *            The implementation class - must implement
   *            {@link ConsoleFoundation.UI}.
   * @return An instance of the user interface class.
   */
  public UI createUI(Class<? extends UI> uiClass) {
    m_container.addComponent(uiClass);

    final UI ui = m_container.getComponent(uiClass);

    final ErrorQueue errorQueue = m_container.getComponent(ErrorQueue.class);

    errorQueue.setErrorHandler(ui.getErrorHandler());

    return ui;
  }

  /**
   * Shut down the console.
   *
   */
  public void shutdown() {
    final ConsoleCommunication communication =
      m_container.getComponent(ConsoleCommunication.class);

    communication.shutdown();

    m_timer.cancel();
  }

  /**
   * Console message event loop. Dispatches communication messages
   * appropriately. Blocks until we are {@link #shutdown()}.
   */
  public void run() {
    m_container.start();

    final ConsoleCommunication communication =
      m_container.getComponent(ConsoleCommunication.class);

    // Need to request components, or they won't be instantiated.
    m_container.getComponent(WireMessageDispatch.class);
    m_container.getComponent(WireFileDistribution.class);
    m_container.getComponent(WireDistributedBarriers.class);

    while (communication.processOneMessage()) {
      // Process until communication is shut down.
    }
  }

  /**
   * Contract for user interfaces.
   *
   * @author Philip Aston
   */
  public interface UI {
    /**
     * Return an error handler to which errors should be reported.
     *
     * @return The error handler.
     */
    ErrorHandler getErrorHandler();
  }

  /**
   * Factory that wires up the FileDistribution. As far as I can see, Pico
   * forces us to use a constructor. Would be nicer if we could say
   * <pre>
   *    container.call(MyFactory.class, "myMethod");
   * </pre>
   *
   * <p>Must be public for PicoContainer.</p>
   */
  public static class WireFileDistribution {

    /**
     * Constructor for WireFileDistribution.
     *
     * @param fileDistribution A file distribution.
     * @param properties The console properties.
     * @param timer A timer.
     */
    public WireFileDistribution(final FileDistribution fileDistribution,
                                ConsoleProperties properties,
                                Timer timer) {

      timer.schedule(new TimerTask() {
          public void run() {
            fileDistribution.scanDistributionFiles();
          }
        },
        properties.getScanDistributionFilesPeriod(),
        properties.getScanDistributionFilesPeriod());


      properties.addPropertyChangeListener(
        new PropertyChangeListener() {
          public void propertyChange(PropertyChangeEvent e) {
            final String propertyName = e.getPropertyName();

            if (propertyName.equals(
              ConsoleProperties.DISTRIBUTION_DIRECTORY_PROPERTY)) {
              fileDistribution.setDirectory((Directory)e.getNewValue());
            }
            else if (propertyName.equals(
              ConsoleProperties.DISTRIBUTION_FILE_FILTER_EXPRESSION_PROPERTY)) {
              fileDistribution.setFileFilterPattern((Pattern) e.getNewValue());
            }
          }
        });
    }
  }

  /**
   * Factory that wires up the message dispatch.
   *
   * <p>Must be public for PicoContainer.</p>
   *
   * @see ConsoleFoundation.WireFileDistribution
   */
  public static class WireMessageDispatch {

    /**
     * Constructor for WireFileDistribution.
     *
     * @param communication Console communication.
     * @param model Console sample model.
     * @param sampleModelViews Console sample model views
     * @param dispatchClientCommands Client command dispatcher.
     */
    public WireMessageDispatch(ConsoleCommunication communication,
                               final SampleModel model,
                               final SampleModelViews sampleModelViews,
                               DispatchClientCommands dispatchClientCommands) {

      final MessageDispatchRegistry messageDispatchRegistry =
        communication.getMessageDispatchRegistry();

      messageDispatchRegistry.set(
        RegisterTestsMessage.class,
        new AbstractHandler<RegisterTestsMessage>() {
          public void handle(RegisterTestsMessage message) {
            model.registerTests(message.getTests());
          }
        });

      messageDispatchRegistry.set(
        ReportStatisticsMessage.class,
        new AbstractHandler<ReportStatisticsMessage>() {
          public void handle(ReportStatisticsMessage message) {
            model.addTestReport(message.getStatisticsDelta());
          }
        });

      messageDispatchRegistry.set(
        RegisterExpressionViewMessage.class,
        new AbstractHandler<RegisterExpressionViewMessage>() {
          public void handle(RegisterExpressionViewMessage message) {
            sampleModelViews.registerStatisticExpression(
              message.getExpressionView());
          }
        });

      dispatchClientCommands.registerMessageHandlers(messageDispatchRegistry);
    }
  }

  /**
   * Factory that wires up the support for global barriers.
   *
   * <p>Must be public for PicoContainer.</p>
   *
   * @see ConsoleFoundation.WireFileDistribution
   */
  public static class WireDistributedBarriers {
    // Guarded by self.
    private final Map<WorkerIdentity, ProcessBarrierGroups>
      m_barrierProcessState =
        new HashMap<WorkerIdentity, ProcessBarrierGroups>();

    private final ConsoleBarrierGroups m_consoleBarrierGroups;

    /**
     * Constructor for WireFileDistribution.
     *
     * @param communication Console communication.
     * @param consoleBarrierGroups
     *          The central barrier groups, owned by the console.
     * @param processControl Console process control.
     */
    public WireDistributedBarriers(ConsoleCommunication communication,
                                   ConsoleBarrierGroups consoleBarrierGroups,
                                   ProcessControl processControl) {

      m_consoleBarrierGroups = consoleBarrierGroups;

      final MessageDispatchRegistry messageDispatch =
        communication.getMessageDispatchRegistry();

      messageDispatch.set(
        AddBarrierMessage.class,
        new AbstractHandler<AddBarrierMessage>() {
          public void handle(AddBarrierMessage message)
            throws CommunicationException {

            getBarrierGroupsForProcess(message.getProcessIdentity())
              .getGroup(message.getName()).addBarrier();
          }
        });

      messageDispatch.set(
        RemoveBarriersMessage.class,
        new AbstractHandler<RemoveBarriersMessage>() {
          public void handle(RemoveBarriersMessage message)
            throws CommunicationException {

            getBarrierGroupsForProcess(message.getProcessIdentity())
              .getGroup(message.getName())
              .removeBarriers(message.getNumberOfBarriers());
          }
        });

      messageDispatch.set(
        AddWaiterMessage.class,
        new AbstractHandler<AddWaiterMessage>() {
          public void handle(AddWaiterMessage message)
            throws CommunicationException {

            getBarrierGroupsForProcess(message.getProcessIdentity())
              .getGroup(message.getName())
              .addWaiter(message.getBarrierIdentity());
          }
        });

      messageDispatch.set(
        CancelWaiterMessage.class,
        new AbstractHandler<CancelWaiterMessage>() {
          public void handle(CancelWaiterMessage message)
            throws CommunicationException {

            getBarrierGroupsForProcess(message.getProcessIdentity())
              .getGroup(message.getName())
              .cancelWaiter(message.getBarrierIdentity());
          }
        });

      processControl.addProcessStatusListener(
        new ProcessControl.Listener() {

          public void update(ProcessReports[] processReports) {
            final Set<WorkerIdentity> liveWorkers =
              new HashSet<WorkerIdentity>();

            for (ProcessReports agentReport : processReports) {
              for (WorkerProcessReport workerReport :
                  agentReport.getWorkerProcessReports()) {
                liveWorkers.add(workerReport.getWorkerIdentity());
              }
            }

            final Set<ProcessBarrierGroups> deadProcesses =
              new HashSet<ProcessBarrierGroups>();

            synchronized (m_barrierProcessState) {
              for (Entry<WorkerIdentity, ProcessBarrierGroups> p :
                   m_barrierProcessState.entrySet()) {
                if (!liveWorkers.contains(p.getKey())) {
                  deadProcesses.add(p.getValue());
                }
              }
            }

            for (ProcessBarrierGroups p : deadProcesses) {
              try {
                p.cancelAll();
              }
              catch (CommunicationException e) {
                throw new AssertionError(e);
              }
            }
          }
        });
    }

    private BarrierGroups
      getBarrierGroupsForProcess(WorkerIdentity processIdentity) {

      synchronized (m_barrierProcessState) {
        final BarrierGroups existing =
          m_barrierProcessState.get(processIdentity);

        if (existing != null) {
          return existing;
        }

        final ProcessBarrierGroups newState = new ProcessBarrierGroups();
        m_barrierProcessState.put(processIdentity, newState);

        return newState;
      }
    }

    private final class ProcessBarrierGroups
      extends AbstractBarrierGroups {

      /**
       * {@inheritDoc}
       */
      @Override protected InternalBarrierGroup createBarrierGroup(String name) {
        return new ProcessBarrierGroup(name);
      }

      /**
       * {@inheritDoc}
       */
      public BarrierIdentityGenerator getIdentityGenerator() {
        throw new UnsupportedOperationException();
      }

      private final class ProcessBarrierGroup
        extends AbstractBarrierGroup {

        public ProcessBarrierGroup(String name) {
          super(name);
        }

        @Override public void addBarrier() throws CommunicationException {
          super.addBarrier();
          delegate().addBarrier();
        }

        @Override public void removeBarriers(long n)
          throws CommunicationException {

          super.removeBarriers(n);
          delegate().removeBarriers(n);
        }


        @Override public void addWaiter(BarrierIdentity barrierIdentity)
          throws CommunicationException {

          super.addWaiter(barrierIdentity);
          delegate().addWaiter(barrierIdentity);
        }

        @Override public void cancelWaiter(BarrierIdentity barrierIdentity)
          throws CommunicationException {

          super.cancelWaiter(barrierIdentity);
          delegate().cancelWaiter(barrierIdentity);
        }

        private BarrierGroup delegate() {
          return m_consoleBarrierGroups.getGroup(getName());
        }
      }
    }
  }

  /**
   * Centralised record of distributed barriers.
   */
  public static final class ConsoleBarrierGroups extends LocalBarrierGroups {

    private final ConsoleCommunication m_communication;

    /**
     * Constructor for WireFileDistribution.
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
    protected InternalBarrierGroup createBarrierGroup(final String name) {
      final InternalBarrierGroup group = super.createBarrierGroup(name);

      group.addListener(new Listener() {
        public void awaken() {
          m_communication.sendToAgents(new OpenBarrierMessage(name));
        }
      });

      return group;
    }

    /**
     * {@inheritDoc}
     */
    @Override public BarrierIdentityGenerator getIdentityGenerator() {
      throw new UnsupportedOperationException();
    }
  }
}
