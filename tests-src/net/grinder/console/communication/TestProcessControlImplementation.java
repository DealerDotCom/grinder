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

import net.grinder.console.messages.ReportStatusMessage;
import net.grinder.engine.messages.ResetGrinderMessage;

import net.grinder.testutility.CallData;
import net.grinder.testutility.DelegatingStubFactory;
import net.grinder.testutility.RandomStubFactory;




/**
 *  Unit test case for {@link ProcessControlImplementation}.
 *
 * @author Philip Aston
 * @version $Revision$
 */
public class TestProcessControlImplementation extends TestCase {

  RandomStubFactory m_consoleCommunicationStubFactory;
  ConsoleCommunication m_consoleCommunication;
  DistributionStatus m_distributionStatus;
  DelegatingStubFactory m_distributionStatusStubFactory;
  RandomStubFactory m_processStatusSetStubFactory;
  ProcessStatusSet m_processStatusSet;

  protected void setUp() throws Exception {
    m_consoleCommunicationStubFactory =
      new RandomStubFactory(ConsoleCommunication.class);
    m_consoleCommunication =
      (ConsoleCommunication)m_consoleCommunicationStubFactory.getStub();

    m_distributionStatus = new DistributionStatus();
    m_distributionStatusStubFactory =
      new DelegatingStubFactory(m_distributionStatus);

    m_processStatusSetStubFactory =
      new RandomStubFactory(ProcessStatusSet.class);
    m_processStatusSet =
      (ProcessStatusSet)m_processStatusSetStubFactory.getStub();
  }

  public void testConstruction() throws Exception {

    final ProcessControlImplementation
      processControlImplementation =
      new ProcessControlImplementation(m_consoleCommunication,
                                       m_processStatusSet,
                                       m_distributionStatus);

    m_processStatusSetStubFactory.assertSuccess("startProcessing");
    m_distributionStatusStubFactory.assertNoMoreCalls();
    
    // Check ConsoleCommunication has been given a MessageHandler that
    // wires ReportStatusMessages to our ProcessStatusSet.
    final CallData addMessageHandlerCallData =
      m_consoleCommunicationStubFactory.assertSuccess(
        "addMessageHandler", ConsoleCommunication.MessageHandler.class);

    final ConsoleCommunication.MessageHandler messageHandler =
      (ConsoleCommunication.MessageHandler)
      addMessageHandlerCallData.getParameters()[0];

    assertFalse(messageHandler.process(new ResetGrinderMessage()));
    m_processStatusSetStubFactory.assertNoMoreCalls();

    final ReportStatusMessage reportStatusMessage =
      new ReportStatusMessage("id", "test", (short)1, (short)2, (short)3);

    assertTrue(messageHandler.process(reportStatusMessage));
    final CallData processStatusSetCallData =
      m_processStatusSetStubFactory.assertSuccess("addStatusReport",
                                                  ReportStatusMessage.class);

    final ReportStatusMessage passedReportStatusMessage =
      (ReportStatusMessage)processStatusSetCallData.getParameters()[0];

    assertEquals(reportStatusMessage.getIdentity(),
                 passedReportStatusMessage.getIdentity());

    assertEquals(reportStatusMessage.getName(),
                 passedReportStatusMessage.getName());

    assertEquals(reportStatusMessage.getState(),
                 passedReportStatusMessage.getState());

    assertEquals(reportStatusMessage.getNumberOfRunningThreads(),
                 passedReportStatusMessage.getNumberOfRunningThreads());

    assertEquals(reportStatusMessage.getTotalNumberOfThreads(),
                 passedReportStatusMessage.getTotalNumberOfThreads());

    m_consoleCommunicationStubFactory.assertNoMoreCalls();
    m_processStatusSetStubFactory.assertNoMoreCalls();
    m_distributionStatusStubFactory.assertNoMoreCalls();
  }
}
