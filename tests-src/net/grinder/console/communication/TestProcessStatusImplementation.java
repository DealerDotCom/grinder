// Copyright (C) 2004, 2005 Philip Aston
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

package net.grinder.console.communication;

import junit.framework.TestCase;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import net.grinder.common.AgentIdentity;
import net.grinder.common.AgentProcessReport;
import net.grinder.common.ProcessReport;
import net.grinder.common.WorkerIdentity;
import net.grinder.common.WorkerProcessReport;
import net.grinder.engine.agent.PublicAgentIdentityImplementation;
import net.grinder.testutility.AssertUtilities;
import net.grinder.testutility.CallData;
import net.grinder.testutility.RandomStubFactory;


/**
 * Unit test case for {@link ProcessStatusImplementation}.
 *
 * @author Philip Aston
 * @version $Revision$
 */
public class TestProcessStatusImplementation extends TestCase {
  private final ProcessReportComparator m_processReportComparator =
    new ProcessReportComparator();

  private final Comparator m_processReportsComparator =
    new ProcessReportsComparator();

  public void testConstruction() throws Exception {
    final MyTimer myTimer = new MyTimer();

    new ProcessStatusImplementation(myTimer);

    assertEquals(2, myTimer.getNumberOfScheduledTasks());
  }

  public void testUpdate() throws Exception {

    final RandomStubFactory listenerStubFactory =
      new RandomStubFactory(ProcessStatus.Listener.class);
    final ProcessStatus.Listener listener =
      (ProcessStatus.Listener)listenerStubFactory.getStub();

    final MyTimer myTimer = new MyTimer();

    final ProcessStatusImplementation processStatusSet =
      new ProcessStatusImplementation(myTimer);

    final TimerTask updateTask = myTimer.getTaskByPeriod(500L);

    processStatusSet.addListener(listener);

    updateTask.run();
    listenerStubFactory.assertNoMoreCalls();

    final PublicAgentIdentityImplementation agentIdentity =
      new PublicAgentIdentityImplementation("agent");
    final WorkerIdentity workerIdentity =
      agentIdentity.createWorkerIdentity();

    final WorkerProcessReport workerProcessReport =
      new WorkerProcessReportImplementation(workerIdentity,
                                            WorkerProcessReport.STATE_RUNNING,
                                            3,
                                            5);

    processStatusSet.addWorkerStatusReport(workerProcessReport);

    updateTask.run();
    final CallData callData =
      listenerStubFactory.assertSuccess(
        "update",
        new ProcessStatus.ProcessReports[0].getClass(),
        Boolean.class);

    final ProcessStatus.ProcessReports[] processReportsArray =
      (ProcessStatus.ProcessReports[])callData.getParameters()[0];

    assertEquals(1, processReportsArray.length);
    final WorkerProcessReport[] workerProcessReports =
      processReportsArray[0].getWorkerProcessReports();
    assertEquals(1, workerProcessReports.length);
    assertEquals(workerProcessReport, workerProcessReports[0]);
    assertEquals(Boolean.TRUE, callData.getParameters()[1]);

    updateTask.run();
    listenerStubFactory.assertNoMoreCalls();
  }

  public void testUpdateWithManyProcessStatusesAndFlush() throws Exception {
    final RandomStubFactory listenerStubFactory =
      new RandomStubFactory(ProcessStatus.Listener.class);
    final ProcessStatus.Listener listener =
      (ProcessStatus.Listener)listenerStubFactory.getStub();

    final MyTimer myTimer = new MyTimer();

    final ProcessStatusImplementation processStatus =
      new ProcessStatusImplementation(myTimer);

    final TimerTask updateTask = myTimer.getTaskByPeriod(500L);
    final TimerTask flushTask = myTimer.getTaskByPeriod(2000L);

    processStatus.addListener(listener);

    updateTask.run();
    listenerStubFactory.assertNoMoreCalls();

    final PublicAgentIdentityImplementation agentIdentityA =
      new PublicAgentIdentityImplementation("Agent A");
    final WorkerIdentity workerIdentityA1 =
      agentIdentityA.createWorkerIdentity();
    final WorkerIdentity workerIdentityA2 =
      agentIdentityA.createWorkerIdentity();
    final WorkerIdentity workerIdentityA3 =
      agentIdentityA.createWorkerIdentity();
    final WorkerIdentity workerIdentityA4 =
        agentIdentityA.createWorkerIdentity();

    final PublicAgentIdentityImplementation agentIdentityB =
      new PublicAgentIdentityImplementation("Agent B");
    final WorkerIdentity workerIdentityB1 =
      agentIdentityB.createWorkerIdentity();

    final WorkerProcessReport[] workerProcessReportArray = {
      new WorkerProcessReportImplementation(
        workerIdentityA3, WorkerProcessReport.STATE_STARTED, 1, 1),
      new WorkerProcessReportImplementation(
        workerIdentityA4, WorkerProcessReport.STATE_STARTED, 1, 1),
      new WorkerProcessReportImplementation(
        workerIdentityA3, WorkerProcessReport.STATE_RUNNING, 5, 10),
      new WorkerProcessReportImplementation(
        workerIdentityB1, WorkerProcessReport.STATE_RUNNING, 1, 1),
      new WorkerProcessReportImplementation(
        workerIdentityA2, WorkerProcessReport.STATE_FINISHED, 1, 1),
      new WorkerProcessReportImplementation(
        workerIdentityA1, WorkerProcessReport.STATE_FINISHED, 3, 10),
    };

    for (int i = 0; i < workerProcessReportArray.length; ++i) {
      processStatus.addWorkerStatusReport(workerProcessReportArray[i]);
    }

    updateTask.run();

    final CallData callData =
      listenerStubFactory.assertSuccess(
        "update",
        new ProcessStatus.ProcessReports[0].getClass(),
        Boolean.class);

    final ProcessStatus.ProcessReports[] processReports =
      (ProcessStatus.ProcessReports[])callData.getParameters()[0];
    Arrays.sort(processReports, m_processReportsComparator);

    assertEquals(2, processReports.length);

    final WorkerProcessReport[] agent1WorkerReports =
      processReports[0].getWorkerProcessReports();
    Arrays.sort(agent1WorkerReports, m_processReportComparator);

    final WorkerProcessReport[] expectedAgent1WorkerProcessReports = {
      new WorkerProcessReportImplementation(
        workerIdentityA4, WorkerProcessReport.STATE_STARTED, 1, 1),
      new WorkerProcessReportImplementation(
        workerIdentityA3, WorkerProcessReport.STATE_RUNNING, 5, 10),
      new WorkerProcessReportImplementation(
        workerIdentityA1, WorkerProcessReport.STATE_FINISHED, 3, 10),
      new WorkerProcessReportImplementation(
        workerIdentityA2, WorkerProcessReport.STATE_FINISHED, 1, 1),
    };

    AssertUtilities.assertArraysEqual(expectedAgent1WorkerProcessReports,
                                      agent1WorkerReports);

    final WorkerProcessReport[] agent2WorkerReports =
      processReports[1].getWorkerProcessReports();
    Arrays.sort(agent2WorkerReports, m_processReportComparator);

    final WorkerProcessReport[] expectedAgent2WorkerProcessReports = {
        new WorkerProcessReportImplementation(
          workerIdentityB1, WorkerProcessReport.STATE_RUNNING, 1, 1),
      };

    AssertUtilities.assertArraysEqual(expectedAgent2WorkerProcessReports,
                                      agent2WorkerReports);

    assertEquals(Boolean.TRUE, callData.getParameters()[1]);

    updateTask.run();
    listenerStubFactory.assertNoMoreCalls();

    // Nothing's changed, reports are new, first flush should do nothing.
    flushTask.run();
    updateTask.run();
    listenerStubFactory.assertNoMoreCalls();

    final PublicAgentIdentityImplementation agentIdentityC =
      new PublicAgentIdentityImplementation("Agent C");
    final WorkerIdentity workerIdentityC1 =
      agentIdentityC.createWorkerIdentity();

    final WorkerProcessReport[] processStatusArray2 = {
      new WorkerProcessReportImplementation(
        workerIdentityB1, WorkerProcessReport.STATE_RUNNING, 1, 1),
      new WorkerProcessReportImplementation(
        workerIdentityA1, WorkerProcessReport.STATE_RUNNING, 5, 10),
      new WorkerProcessReportImplementation(
        workerIdentityC1, WorkerProcessReport.STATE_FINISHED, 1, 1),
    };

    for (int i = 0; i < processStatusArray2.length; ++i) {
      processStatus.addWorkerStatusReport(processStatusArray2[i]);
    }

    processStatus.addAgentStatusReport(
      new AgentProcessReportImplementation(agentIdentityA,
                                           AgentProcessReport.STATE_RUNNING));
    processStatus.addAgentStatusReport(
      new AgentProcessReportImplementation(agentIdentityB,
                                           AgentProcessReport.STATE_RUNNING));

    // Second flush will remove processes that haven't reported.
    flushTask.run();
    updateTask.run();

    final CallData callData2 =
      listenerStubFactory.assertSuccess(
        "update",
        new ProcessStatus.ProcessReports[0].getClass(),
        Boolean.class);

    final ProcessStatus.ProcessReports[] processReports2 =
      (ProcessStatus.ProcessReports[])callData2.getParameters()[0];
    Arrays.sort(processReports2, m_processReportsComparator);

    assertEquals(3, processReports2.length);

    final WorkerProcessReport[] expectedAgent1WorkerProcessReports2 = {
      new WorkerProcessReportImplementation(
        workerIdentityA1, WorkerProcessReport.STATE_RUNNING, 5, 10),
    };

    AssertUtilities.assertArraysEqual(
      expectedAgent1WorkerProcessReports2,
      processReports2[0].getWorkerProcessReports());

    final WorkerProcessReport[] expectedAgent2WorkerProcessReports2 = {
        new WorkerProcessReportImplementation(
          workerIdentityB1, WorkerProcessReport.STATE_RUNNING, 1, 1),
      };

    AssertUtilities.assertArraysEqual(
      expectedAgent2WorkerProcessReports2,
      processReports2[1].getWorkerProcessReports());

    final WorkerProcessReport[] expectedAgent3WorkerProcessReports2 = {
        new WorkerProcessReportImplementation(
          workerIdentityC1, WorkerProcessReport.STATE_FINISHED, 1, 1),
      };

    AssertUtilities.assertArraysEqual(
      expectedAgent3WorkerProcessReports2,
      processReports2[2].getWorkerProcessReports());

    assertEquals(Boolean.TRUE, callData.getParameters()[1]);

    updateTask.run();
    listenerStubFactory.assertNoMoreCalls();
  }

  private static final class AgentProcessReportImplementation
    implements AgentProcessReport {

    private final AgentIdentity m_identity;
    private final short m_state;

    public AgentProcessReportImplementation(AgentIdentity identity,
                                            short state) {
      m_identity = identity;
      m_state = state;
    }

    public AgentIdentity getAgentIdentity() {
      return m_identity;
    }

    public ProcessIdentity getIdentity() {
      return m_identity;
    }

    public short getState() {
      return m_state;
    }
  }

  private static final class WorkerProcessReportImplementation
    implements WorkerProcessReport {

    private final WorkerIdentity m_workerIdentity;
    private final short m_state;
    private final short m_totalNumberOfThreads;
    private final short m_numberOfRunningThreads;

    public WorkerProcessReportImplementation(WorkerIdentity workerIdentity,
                                             short state,
                                             int runningThreads,
                                             int totalThreads) {
      m_workerIdentity = workerIdentity;
      m_state = state;
      m_numberOfRunningThreads = (short)runningThreads;
      m_totalNumberOfThreads = (short)totalThreads;
    }

    public ProcessIdentity getIdentity() {
      return m_workerIdentity;
    }

    public WorkerIdentity getWorkerIdentity() {
      return m_workerIdentity;
    }

    public short getState() {
      return m_state;
    }

    public short getNumberOfRunningThreads() {
      return m_numberOfRunningThreads;
    }

    public short getMaximumNumberOfThreads() {
      return m_totalNumberOfThreads;
    }

    public int hashCode() {
      return m_workerIdentity.hashCode();
    }

    public boolean equals(Object o) {
      if (o == this) {
        return true;
      }

      if (!(o instanceof WorkerProcessReport)) {
        return false;
      }

      final WorkerProcessReport other = (WorkerProcessReport)o;

      return
        this.getState() == other.getState() &&
        this.getNumberOfRunningThreads() == other.getNumberOfRunningThreads() &&
        this.getMaximumNumberOfThreads() == other.getMaximumNumberOfThreads() &&
        this.getWorkerIdentity().equals(other.getWorkerIdentity());
    }

    public String toString() {
      return
        "WorkerProcessReportImplementation(" +
        getWorkerIdentity() + ", " +
        getState() + ", " +
        getNumberOfRunningThreads() + ", " +
        getMaximumNumberOfThreads() + ")";
    }
  }

  private final class MyTimer extends Timer {
    private final Map m_taskByPeriod = new HashMap();
    private int m_numberOfScheduledTasks;

    public void schedule(TimerTask timerTask, long delay, long period) {
      assertEquals(0, delay);

      m_taskByPeriod.put(new Long(period), timerTask);
      ++m_numberOfScheduledTasks;
    }

    public TimerTask getTaskByPeriod(long period) {
      return (TimerTask)m_taskByPeriod.get(new Long(period));
    }

    public int getNumberOfScheduledTasks() {
      return m_numberOfScheduledTasks;
    }
  }


  private static final class ProcessReportComparator implements Comparator {
    public int compare(Object o1, Object o2) {
      final ProcessReport processReport1 = (ProcessReport)o1;
      final ProcessReport processReport2 = (ProcessReport)o2;

      final int compareState =
        processReport1.getState() - processReport2.getState();

      if (compareState == 0) {
        return processReport1.getIdentity().getName().compareTo(
               processReport2.getIdentity().getName());
      }
      else {
        return compareState;
      }
    }
  }

  private final class ProcessReportsComparator implements Comparator {
    public int compare(Object o1, Object o2) {
      return m_processReportComparator.compare(
        ((ProcessStatus.ProcessReports)o1).getAgentProcessReport(),
        ((ProcessStatus.ProcessReports)o2).getAgentProcessReport());
    }
  }
}
