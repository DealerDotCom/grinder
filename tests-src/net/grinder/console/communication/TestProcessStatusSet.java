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

import net.grinder.common.ProcessStatus;
import net.grinder.testutility.AssertUtilities;
import net.grinder.testutility.CallData;
import net.grinder.testutility.RandomStubFactory;


/**
 *  Unit test case for {@link ProcessStatusSet}.
 *
 * @author Philip Aston
 * @version $Revision$
 */
public class TestProcessStatusSet extends TestCase {

  public void testUpdate() throws Exception {

    final RandomStubFactory listenerStubFactory =
      new RandomStubFactory(ProcessStatusListener.class);
    final ProcessStatusListener listener =
      (ProcessStatusListener)listenerStubFactory.getStub();

    final ProcessStatusSet processStatusSet = new ProcessStatusSet();
    processStatusSet.addListener(listener);

    processStatusSet.update();
    listenerStubFactory.assertNoMoreCalls();

    final ProcessStatus processStatus =
      new ProcessStatusImplementation("identity", "name",
                                      ProcessStatus.STATE_RUNNING, 3, 5);

    processStatusSet.addStatusReport(processStatus);

    processStatusSet.update();
    final CallData callData =
      listenerStubFactory.assertSuccess("update",
                                        new ProcessStatus[0].getClass(),
                                        Integer.class,
                                        Integer.class);

    final ProcessStatus[] processStatusArray =
      (ProcessStatus[])callData.getParameters()[0];

    assertEquals(1, processStatusArray.length);
    assertEquals(processStatus, processStatusArray[0]);
    assertEquals(new Integer(3), callData.getParameters()[1]);
    assertEquals(new Integer(5), callData.getParameters()[2]);

    processStatusSet.update();
    listenerStubFactory.assertNoMoreCalls();
  }

  public void testUpdateWithManyProcessStatusesAndFlush() throws Exception {
    final RandomStubFactory listenerStubFactory =
      new RandomStubFactory(ProcessStatusListener.class);
    final ProcessStatusListener listener =
      (ProcessStatusListener)listenerStubFactory.getStub();

    final ProcessStatusSet processStatusSet = new ProcessStatusSet();
    processStatusSet.addListener(listener);

    processStatusSet.update();
    listenerStubFactory.assertNoMoreCalls();

    final ProcessStatus[] processStatusArray = {
      new ProcessStatusImplementation(
        "a", "a process", ProcessStatus.STATE_STARTED, 1, 1),
      new ProcessStatusImplementation(
        "b", "a process", ProcessStatus.STATE_STARTED, 1, 1),
      new ProcessStatusImplementation(
        "a", "a process", ProcessStatus.STATE_RUNNING, 5, 10),
      new ProcessStatusImplementation(
        "e", "another process name", ProcessStatus.STATE_RUNNING, 1, 1),
      new ProcessStatusImplementation(
        "d", "name", ProcessStatus.STATE_FINISHED, 1, 1),
      new ProcessStatusImplementation(
        "c", "some name", ProcessStatus.STATE_FINISHED, 3, 10),
    };

    for (int i = 0; i < processStatusArray.length; ++i) {
      processStatusSet.addStatusReport(processStatusArray[i]);
    }

    processStatusSet.update();

    final CallData callData =
      listenerStubFactory.assertSuccess("update",
                                        new ProcessStatus[0].getClass(),
                                        Integer.class,
                                        Integer.class);

    final ProcessStatus[] expectedProcessStatusArray = {
      new ProcessStatusImplementation(
        "b", "a process", ProcessStatus.STATE_STARTED, 1, 1),
      new ProcessStatusImplementation(
        "a", "a process", ProcessStatus.STATE_RUNNING, 5, 10),
      new ProcessStatusImplementation(
        "e", "another process name", ProcessStatus.STATE_RUNNING, 1, 1),
      new ProcessStatusImplementation(
        "d", "name", ProcessStatus.STATE_FINISHED, 1, 1),
      new ProcessStatusImplementation(
        "c", "some name", ProcessStatus.STATE_FINISHED, 3, 10),
    };

    AssertUtilities.assertArraysEqual(
      expectedProcessStatusArray,
      (ProcessStatus[])callData.getParameters()[0]);

    assertEquals(new Integer(11), callData.getParameters()[1]);
    assertEquals(new Integer(23), callData.getParameters()[2]);

    processStatusSet.update();
    listenerStubFactory.assertNoMoreCalls();

    processStatusSet.flush();
    processStatusSet.update();
    listenerStubFactory.assertNoMoreCalls();

    processStatusSet.processEvent();

    processStatusSet.flush();
    processStatusSet.update();
    listenerStubFactory.assertNoMoreCalls();

    final ProcessStatus[] processStatusArray2 = {
      new ProcessStatusImplementation(
        "b", "a process", ProcessStatus.STATE_RUNNING, 1, 1),
      new ProcessStatusImplementation(
        "a", "a process", ProcessStatus.STATE_RUNNING, 5, 10),
      new ProcessStatusImplementation(
        "e", "another process name", ProcessStatus.STATE_FINISHED, 1, 1),
    };

    for (int i = 0; i < processStatusArray2.length; ++i) {
      processStatusSet.addStatusReport(processStatusArray2[i]);
    }

    // Second flush after processEvent will remove processes that
    // haven't reported.
    processStatusSet.flush();
    processStatusSet.update();

    final CallData callData2 =
      listenerStubFactory.assertSuccess("update",
                                        new ProcessStatus[0].getClass(),
                                        Integer.class,
                                        Integer.class);

    final ProcessStatus[] expectedProcessStatusArray2 = {
      new ProcessStatusImplementation(
        "b", "a process", ProcessStatus.STATE_RUNNING, 1, 1),
      new ProcessStatusImplementation(
        "a", "a process", ProcessStatus.STATE_RUNNING, 5, 10),
      new ProcessStatusImplementation(
        "e", "another process name", ProcessStatus.STATE_FINISHED, 1, 1),
    };

    AssertUtilities.assertArraysEqual(
      expectedProcessStatusArray2,
      (ProcessStatus[])callData2.getParameters()[0]);

    assertEquals(new Integer(7), callData2.getParameters()[1]);
    assertEquals(new Integer(12), callData2.getParameters()[2]);

    processStatusSet.update();
    listenerStubFactory.assertNoMoreCalls();
  }

  private static final class ProcessStatusImplementation
    implements ProcessStatus {

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

      if (!(o instanceof ProcessStatus)) {
        return false;
      }

      final ProcessStatus other = (ProcessStatus)o;

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
}
