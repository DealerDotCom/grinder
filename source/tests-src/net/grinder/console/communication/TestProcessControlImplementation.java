// Copyright (C) 2008 Philip Aston
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

import java.util.Comparator;

import net.grinder.communication.Address;
import net.grinder.console.communication.ProcessControl.ProcessReports;
import net.grinder.engine.agent.StubAgentIdentity;
import net.grinder.messages.agent.CacheHighWaterMark;
import net.grinder.messages.agent.StubCacheHighWaterMark;
import net.grinder.messages.console.AgentIdentity;
import net.grinder.messages.console.AgentProcessReport;
import net.grinder.messages.console.StubAgentProcessReport;
import net.grinder.testutility.RandomStubFactory;
import net.grinder.testutility.StubTimer;

import junit.framework.TestCase;

/**
 * Unit tests for {@link ProcessStatus}.
 *
 * @author Philip Aston
 * @version $Revision:$
 */
public class TestProcessControlImplementation extends TestCase {

  private final StubTimer m_timer = new StubTimer();

  private final RandomStubFactory m_consoleCommunicationStubFactory =
    new RandomStubFactory(ConsoleCommunication.class);
  private final ConsoleCommunication m_consoleCommunication =
    (ConsoleCommunication)m_consoleCommunicationStubFactory.getStub();

  public void testProcessReportsComparator() throws Exception {
    final Comparator comparator = new ProcessControl.ProcessReportsComparator();

    final AgentIdentity agentIdentity1 =
      new StubAgentIdentity("my agent");

    final AgentProcessReport agentProcessReport1 =
      new StubAgentProcessReport(agentIdentity1,
                                 AgentProcessReport.STATE_RUNNING);

    final RandomStubFactory processReportsStubFactory1 =
      new RandomStubFactory(ProcessReports.class);
    final ProcessReports processReports1 =
      (ProcessReports)processReportsStubFactory1.getStub();
    processReportsStubFactory1.setResult("getAgentProcessReport",
                                         agentProcessReport1);

    assertEquals(0, comparator.compare(processReports1, processReports1));

    final AgentProcessReport agentProcessReport2 =
      new StubAgentProcessReport(agentIdentity1,
                                 AgentProcessReport.STATE_FINISHED);

    final RandomStubFactory processReportsStubFactory2 =
      new RandomStubFactory(ProcessReports.class);
    final ProcessReports processReports2 =
      (ProcessReports)processReportsStubFactory2.getStub();
    processReportsStubFactory2.setResult("getAgentProcessReport",
                                         agentProcessReport2);

    assertEquals(0, comparator.compare(processReports2, processReports2));
    assertTrue(comparator.compare(processReports1, processReports2) < 0);
    assertTrue(comparator.compare(processReports2, processReports1) > 0);
  }

  public void testAgentsWithOutOfDateCaches() throws Exception {
    final ProcessControlImplementation processControl =
      new ProcessControlImplementation(m_timer, m_consoleCommunication);

    final CacheHighWaterMark cacheState1 = new StubCacheHighWaterMark(9);

    final Address address =
      processControl.agentsWithOutOfDateCaches(cacheState1);

    assertNotNull(address);
    assertFalse(address.includes(
      new Address() {
        public boolean includes(Address address) { return false; }
      }
    ));

    assertNotSame(address,
                  processControl.agentsWithOutOfDateCaches(cacheState1));
  }
}