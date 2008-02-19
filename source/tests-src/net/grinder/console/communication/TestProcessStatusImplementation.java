// Copyright (C) 2004 - 2008 Philip Aston
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

package net.grinder.console.communication;

import junit.framework.TestCase;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import net.grinder.console.communication.ProcessStatusImplementation.AgentAndWorkers;
import net.grinder.engine.agent.StubAgentIdentity;
import net.grinder.messages.console.AgentProcessReport;
import net.grinder.messages.console.ProcessReport;
import net.grinder.messages.console.StubAgentProcessReport;
import net.grinder.messages.console.StubWorkerProcessReport;
import net.grinder.messages.console.WorkerIdentity;
import net.grinder.messages.console.WorkerProcessReport;
import net.grinder.testutility.AssertUtilities;
import net.grinder.testutility.CallData;
import net.grinder.testutility.RandomStubFactory;
import net.grinder.util.AllocateLowestNumber;


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

  private final MyTimer m_timer = new MyTimer();

  private final RandomStubFactory m_allocateLowestNumberStubFactory =
    new RandomStubFactory(AllocateLowestNumber.class);
  private final AllocateLowestNumber m_allocateLowestNumber =
    (AllocateLowestNumber)m_allocateLowestNumberStubFactory.getStub();

  protected void tearDown() {
    m_timer.cancel();
  }

  public void testConstruction() throws Exception {
    new ProcessStatusImplementation(m_timer, m_allocateLowestNumber);

    assertEquals(2, m_timer.getNumberOfScheduledTasks());

    m_allocateLowestNumberStubFactory.assertNoMoreCalls();
  }

  public void testUpdate() throws Exception {

    final RandomStubFactory listenerStubFactory =
      new RandomStubFactory(ProcessStatus.Listener.class);
    final ProcessStatus.Listener listener =
      (ProcessStatus.Listener)listenerStubFactory.getStub();

    final ProcessStatusImplementation processStatusSet =
      new ProcessStatusImplementation(m_timer, m_allocateLowestNumber);

    final TimerTask updateTask = m_timer.getTaskByPeriod(500L);

    processStatusSet.addListener(listener);

    updateTask.run();
    listenerStubFactory.assertNoMoreCalls();
    m_allocateLowestNumberStubFactory.assertNoMoreCalls();

    updateTask.run();
    listenerStubFactory.assertNoMoreCalls();
    m_allocateLowestNumberStubFactory.assertNoMoreCalls();

    final StubAgentIdentity agentIdentity = new StubAgentIdentity("agent");
    final WorkerIdentity workerIdentity =
      agentIdentity.createWorkerIdentity(10);

    final WorkerProcessReport workerProcessReport =
      new StubWorkerProcessReport(workerIdentity,
                                  WorkerProcessReport.STATE_RUNNING,
                                  3,
                                  5);

    processStatusSet.addWorkerStatusReport(workerProcessReport);
    m_allocateLowestNumberStubFactory.assertSuccess("add", Object.class);
    m_allocateLowestNumberStubFactory.assertNoMoreCalls();

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
    m_allocateLowestNumberStubFactory.assertNoMoreCalls();
  }

  public void testUpdateWithManyProcessStatusesAndFlush() throws Exception {
    final RandomStubFactory listenerStubFactory =
      new RandomStubFactory(ProcessStatus.Listener.class);
    final ProcessStatus.Listener listener =
      (ProcessStatus.Listener)listenerStubFactory.getStub();

    final ProcessStatusImplementation processStatus =
      new ProcessStatusImplementation(m_timer, m_allocateLowestNumber);

    final TimerTask updateTask = m_timer.getTaskByPeriod(500L);
    final TimerTask flushTask = m_timer.getTaskByPeriod(2000L);

    processStatus.addListener(listener);

    updateTask.run();
    listenerStubFactory.assertNoMoreCalls();
    m_allocateLowestNumberStubFactory.assertNoMoreCalls();

    final StubAgentIdentity agentIdentityA =
      new StubAgentIdentity("Agent A");
    final WorkerIdentity workerIdentityA1 =
      agentIdentityA.createWorkerIdentity(9);
    final WorkerIdentity workerIdentityA2 =
      agentIdentityA.createWorkerIdentity(9);
    final WorkerIdentity workerIdentityA3 =
      agentIdentityA.createWorkerIdentity(9);
    final WorkerIdentity workerIdentityA4 =
        agentIdentityA.createWorkerIdentity(9);
    assertEquals(9, workerIdentityA4.getAgentID());

    final StubAgentIdentity agentIdentityB =
      new StubAgentIdentity("Agent B");
    final WorkerIdentity workerIdentityB1 =
      agentIdentityB.createWorkerIdentity(19);
    assertEquals(19, workerIdentityB1.getAgentID());

    final WorkerProcessReport[] workerProcessReportArray = {
      new StubWorkerProcessReport(
        workerIdentityA3, WorkerProcessReport.STATE_STARTED, 1, 1),
      new StubWorkerProcessReport(
        workerIdentityA4, WorkerProcessReport.STATE_STARTED, 1, 1),
      new StubWorkerProcessReport(
        workerIdentityA3, WorkerProcessReport.STATE_RUNNING, 5, 10),
      new StubWorkerProcessReport(
        workerIdentityB1, WorkerProcessReport.STATE_RUNNING, 1, 1),
      new StubWorkerProcessReport(
        workerIdentityA2, WorkerProcessReport.STATE_FINISHED, 1, 1),
      new StubWorkerProcessReport(
        workerIdentityA1, WorkerProcessReport.STATE_FINISHED, 3, 10),
    };

    for (int i = 0; i < workerProcessReportArray.length; ++i) {
      processStatus.addWorkerStatusReport(workerProcessReportArray[i]);
    }

    assertEquals(2, processStatus.getNumberOfLiveAgents());
    m_allocateLowestNumberStubFactory.assertSuccess("add", Object.class);
    m_allocateLowestNumberStubFactory.assertSuccess("add", Object.class);
    m_allocateLowestNumberStubFactory.assertNoMoreCalls();

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
      new StubWorkerProcessReport(
        workerIdentityA4, WorkerProcessReport.STATE_STARTED, 1, 1),
      new StubWorkerProcessReport(
        workerIdentityA3, WorkerProcessReport.STATE_RUNNING, 5, 10),
      new StubWorkerProcessReport(
        workerIdentityA1, WorkerProcessReport.STATE_FINISHED, 3, 10),
      new StubWorkerProcessReport(
        workerIdentityA2, WorkerProcessReport.STATE_FINISHED, 1, 1),
    };

    AssertUtilities.assertArraysEqual(expectedAgent1WorkerProcessReports,
                                      agent1WorkerReports);

    final WorkerProcessReport[] agent2WorkerReports =
      processReports[1].getWorkerProcessReports();
    Arrays.sort(agent2WorkerReports, m_processReportComparator);

    final WorkerProcessReport[] expectedAgent2WorkerProcessReports = {
        new StubWorkerProcessReport(
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
    m_allocateLowestNumberStubFactory.assertNoMoreCalls();

    final StubAgentIdentity agentIdentityC =
      new StubAgentIdentity("Agent C");
    final WorkerIdentity workerIdentityC1 =
      agentIdentityC.createWorkerIdentity(22);

    final WorkerProcessReport[] processStatusArray2 = {
      new StubWorkerProcessReport(
        workerIdentityB1, WorkerProcessReport.STATE_RUNNING, 1, 1),
      new StubWorkerProcessReport(
        workerIdentityA1, WorkerProcessReport.STATE_RUNNING, 5, 10),
      new StubWorkerProcessReport(
        workerIdentityC1, WorkerProcessReport.STATE_FINISHED, 1, 1),
    };

    for (int i = 0; i < processStatusArray2.length; ++i) {
      processStatus.addWorkerStatusReport(processStatusArray2[i]);
    }

    assertEquals(3, processStatus.getNumberOfLiveAgents());
    m_allocateLowestNumberStubFactory.assertSuccess("add", Object.class);
    m_allocateLowestNumberStubFactory.assertNoMoreCalls();

    processStatus.addAgentStatusReport(
      new StubAgentProcessReport(agentIdentityA,
                                 AgentProcessReport.STATE_RUNNING));
    processStatus.addAgentStatusReport(
      new StubAgentProcessReport(agentIdentityB,
                                 AgentProcessReport.STATE_RUNNING));

    assertEquals(3, processStatus.getNumberOfLiveAgents());
    m_allocateLowestNumberStubFactory.assertNoMoreCalls();

    // Second flush will remove processes that haven't reported.
    // It won't remove any agents, because there's been at least one
    // report for each.
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
    m_allocateLowestNumberStubFactory.assertNoMoreCalls();

    final WorkerProcessReport[] expectedAgent1WorkerProcessReports2 = {
      new StubWorkerProcessReport(
        workerIdentityA1, WorkerProcessReport.STATE_RUNNING, 5, 10),
    };

    AssertUtilities.assertArraysEqual(
      expectedAgent1WorkerProcessReports2,
      processReports2[0].getWorkerProcessReports());

    final WorkerProcessReport[] expectedAgent2WorkerProcessReports2 = {
      new StubWorkerProcessReport(
        workerIdentityB1, WorkerProcessReport.STATE_RUNNING, 1, 1),
    };

    AssertUtilities.assertArraysEqual(
      expectedAgent2WorkerProcessReports2,
      processReports2[1].getWorkerProcessReports());

    final WorkerProcessReport[] expectedAgent3WorkerProcessReports2 = {
      new StubWorkerProcessReport(
        workerIdentityC1, WorkerProcessReport.STATE_FINISHED, 1, 1),
    };

    AssertUtilities.assertArraysEqual(
      expectedAgent3WorkerProcessReports2,
      processReports2[2].getWorkerProcessReports());

    assertEquals(Boolean.TRUE, callData.getParameters()[1]);

    updateTask.run();
    listenerStubFactory.assertNoMoreCalls();

    // Third flush.
    flushTask.run();

    assertEquals(0, processStatus.getNumberOfLiveAgents());
    m_allocateLowestNumberStubFactory.assertSuccess("remove", Object.class);
    m_allocateLowestNumberStubFactory.assertSuccess("remove", Object.class);
    m_allocateLowestNumberStubFactory.assertSuccess("remove", Object.class);
    m_allocateLowestNumberStubFactory.assertNoMoreCalls();
  }

  public void testAgentAndWorkers() throws Exception {
    final ProcessStatusImplementation processStatusSet =
      new ProcessStatusImplementation(m_timer, m_allocateLowestNumber);

    final StubAgentIdentity agentIdentity =
      new StubAgentIdentity("agent");

    final AgentAndWorkers agentAndWorkers =
      processStatusSet.new AgentAndWorkers(agentIdentity);

    final AgentProcessReport initialReport =
      agentAndWorkers.getAgentProcessReport();

    assertEquals(agentIdentity, initialReport.getAgentIdentity());
  }

  private static final class MyTimer extends Timer {
    private final Map m_taskByPeriod = new HashMap();
    private int m_numberOfScheduledTasks;

    MyTimer() {
      super(true);
    }

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
