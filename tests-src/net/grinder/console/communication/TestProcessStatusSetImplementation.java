// Copyright (C) 2004 Philip Aston
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

import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import net.grinder.common.WorkerProcessStatus;
import net.grinder.testutility.AssertUtilities;
import net.grinder.testutility.CallData;
import net.grinder.testutility.RandomStubFactory;


/**
 *  Unit test case for {@link ProcessStatusSetImplementation}.
 *
 * @author Philip Aston
 * @version $Revision$
 */
public class TestProcessStatusSetImplementation extends TestCase {

  public void testConstruction() throws Exception {
    final MyTimer myTimer = new MyTimer();

    final ProcessStatusSetImplementation processStatusSet =
      new ProcessStatusSetImplementation(myTimer);

    assertEquals(2, myTimer.getNumberOfScheduledTasks());
  }

  public void testUpdate() throws Exception {

    final RandomStubFactory listenerStubFactory =
      new RandomStubFactory(ProcessStatusListener.class);
    final ProcessStatusListener listener =
      (ProcessStatusListener)listenerStubFactory.getStub();

    final MyTimer myTimer = new MyTimer();

    final ProcessStatusSetImplementation processStatusSet =
      new ProcessStatusSetImplementation(myTimer);

    final TimerTask updateTask = myTimer.getTaskByPeriod(500L);

    processStatusSet.addListener(listener);

    updateTask.run();
    listenerStubFactory.assertNoMoreCalls();

    final WorkerProcessStatus processStatus =
      new ProcessStatusImplementation("identity", "name",
                                      WorkerProcessStatus.STATE_RUNNING, 3, 5);

    processStatusSet.addWorkerStatusReport(processStatus);

    updateTask.run();
    final CallData callData =
      listenerStubFactory.assertSuccess("update",
                                        new WorkerProcessStatus[0].getClass(),
                                        Integer.class,
                                        Integer.class);

    final WorkerProcessStatus[] processStatusArray =
      (WorkerProcessStatus[])callData.getParameters()[0];

    assertEquals(1, processStatusArray.length);
    assertEquals(processStatus, processStatusArray[0]);
    assertEquals(new Integer(3), callData.getParameters()[1]);
    assertEquals(new Integer(5), callData.getParameters()[2]);

    updateTask.run();
    listenerStubFactory.assertNoMoreCalls();
  }

  public void testUpdateWithManyProcessStatusesAndFlush() throws Exception {
    final RandomStubFactory listenerStubFactory =
      new RandomStubFactory(ProcessStatusListener.class);
    final ProcessStatusListener listener =
      (ProcessStatusListener)listenerStubFactory.getStub();

    final MyTimer myTimer = new MyTimer();

    final ProcessStatusSetImplementation processStatusSet =
      new ProcessStatusSetImplementation(myTimer);

    final TimerTask updateTask = myTimer.getTaskByPeriod(500L);
    final TimerTask flushTask = myTimer.getTaskByPeriod(2000L);

    processStatusSet.addListener(listener);

    updateTask.run();
    listenerStubFactory.assertNoMoreCalls();

    final WorkerProcessStatus[] processStatusArray = {
      new ProcessStatusImplementation(
        "a", "a process A", WorkerProcessStatus.STATE_STARTED, 1, 1),
      new ProcessStatusImplementation(
        "b", "a process B", WorkerProcessStatus.STATE_STARTED, 1, 1),
      new ProcessStatusImplementation(
        "a", "a process A", WorkerProcessStatus.STATE_RUNNING, 5, 10),
      new ProcessStatusImplementation(
        "e", "another process name", WorkerProcessStatus.STATE_RUNNING, 1, 1),
      new ProcessStatusImplementation(
        "d", "name", WorkerProcessStatus.STATE_FINISHED, 1, 1),
      new ProcessStatusImplementation(
        "c", "some name", WorkerProcessStatus.STATE_FINISHED, 3, 10),
    };

    for (int i = 0; i < processStatusArray.length; ++i) {
      processStatusSet.addWorkerStatusReport(processStatusArray[i]);
    }

    updateTask.run();

    final CallData callData =
      listenerStubFactory.assertSuccess("update",
                                        new WorkerProcessStatus[0].getClass(),
                                        Integer.class,
                                        Integer.class);

    final WorkerProcessStatus[] expectedProcessStatusArray = {
      new ProcessStatusImplementation(
        "b", "a process B", WorkerProcessStatus.STATE_STARTED, 1, 1),
      new ProcessStatusImplementation(
        "a", "a process A", WorkerProcessStatus.STATE_RUNNING, 5, 10),
      new ProcessStatusImplementation(
        "e", "another process name", WorkerProcessStatus.STATE_RUNNING, 1, 1),
      new ProcessStatusImplementation(
        "d", "name", WorkerProcessStatus.STATE_FINISHED, 1, 1),
      new ProcessStatusImplementation(
        "c", "some name", WorkerProcessStatus.STATE_FINISHED, 3, 10),
    };

    AssertUtilities.assertArraysEqual(
      expectedProcessStatusArray,
      (WorkerProcessStatus[])callData.getParameters()[0]);

    assertEquals(new Integer(11), callData.getParameters()[1]);
    assertEquals(new Integer(23), callData.getParameters()[2]);

    updateTask.run();
    listenerStubFactory.assertNoMoreCalls();

    flushTask.run();
    updateTask.run();
    listenerStubFactory.assertNoMoreCalls();

    processStatusSet.processEvent();

    flushTask.run();
    updateTask.run();
    listenerStubFactory.assertNoMoreCalls();

    final WorkerProcessStatus[] processStatusArray2 = {
      new ProcessStatusImplementation(
        "b", "a process B", WorkerProcessStatus.STATE_RUNNING, 1, 1),
      new ProcessStatusImplementation(
        "a", "a process A", WorkerProcessStatus.STATE_RUNNING, 5, 10),
      new ProcessStatusImplementation(
        "e", "another process name", WorkerProcessStatus.STATE_FINISHED, 1, 1),
    };

    for (int i = 0; i < processStatusArray2.length; ++i) {
      processStatusSet.addWorkerStatusReport(processStatusArray2[i]);
    }

    // Second flush after processEvent will remove processes that
    // haven't reported.
    flushTask.run();
    updateTask.run();

    final CallData callData2 =
      listenerStubFactory.assertSuccess("update",
                                        new WorkerProcessStatus[0].getClass(),
                                        Integer.class,
                                        Integer.class);

    final WorkerProcessStatus[] expectedProcessStatusArray2 = {
      new ProcessStatusImplementation(
        "a", "a process A", WorkerProcessStatus.STATE_RUNNING, 5, 10),
      new ProcessStatusImplementation(
        "b", "a process B", WorkerProcessStatus.STATE_RUNNING, 1, 1),
      new ProcessStatusImplementation(
        "e", "another process name", WorkerProcessStatus.STATE_FINISHED, 1, 1),
    };

    AssertUtilities.assertArraysEqual(
      expectedProcessStatusArray2,
      (WorkerProcessStatus[])callData2.getParameters()[0]);

    assertEquals(new Integer(7), callData2.getParameters()[1]);
    assertEquals(new Integer(12), callData2.getParameters()[2]);

    updateTask.run();
    listenerStubFactory.assertNoMoreCalls();
  }

  private static final class ProcessStatusImplementation
    implements WorkerProcessStatus {

    private final String m_identity;
    private final String m_name;
    private final short m_state;
    private final short m_totalNumberOfThreads;
    private final short m_numberOfRunningThreads;

    public ProcessStatusImplementation(String identity,
                                       String name,
                                       short state,
                                       int runningThreads,
                                       int totalThreads) {
      m_identity = identity;
      m_name = name;
      m_state = state;
      m_numberOfRunningThreads = (short)runningThreads;
      m_totalNumberOfThreads = (short)totalThreads;
    }

    public String getIdentity() {
      return m_identity;
    }

    public String getName() {
      return m_name;
    }

    public short getState() {
      return m_state;
    }

    public short getNumberOfRunningThreads() {
      return m_numberOfRunningThreads;
    }

    public short getTotalNumberOfThreads() {
      return m_totalNumberOfThreads;
    }

    public int hashCode() {
      return m_identity.hashCode();
    }

    public boolean equals(Object o) {
      if (o == this) {
        return true;
      }

      if (!(o instanceof WorkerProcessStatus)) {
        return false;
      }

      final WorkerProcessStatus other = (WorkerProcessStatus)o;

      return
        this.getState() == other.getState() &&
        this.getNumberOfRunningThreads() == other.getNumberOfRunningThreads()
        &&
        this.getTotalNumberOfThreads() == other.getTotalNumberOfThreads() &&
        this.getIdentity().equals(other.getIdentity()) &&
        this.getName().equals(other.getName());
    }

    public String toString() {
      return
        "ProcessStatusImplementation(" +
        getIdentity() + ", " +
        getName() + ", " +
        getState() + ", " +
        getNumberOfRunningThreads() + ", " +
        getTotalNumberOfThreads() + ")";
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
}
