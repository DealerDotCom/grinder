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

import net.grinder.common.AgentIdentity;
import net.grinder.common.AgentProcessReport;
import net.grinder.common.StubAgentProcessReport;
import net.grinder.console.communication.ProcessStatus.ProcessReports;
import net.grinder.engine.agent.PublicAgentIdentityImplementation;
import net.grinder.testutility.RandomStubFactory;

import junit.framework.TestCase;

/**
 * Unit tests for {@link ProcessStatus}.
 *
 * @author Philip Aston
 * @version $Revision:$
 */
public class TestProcessStatus extends TestCase {

  public void testProcessReportsComparator() throws Exception {
    final Comparator comparator = new ProcessStatus.ProcessReportsComparator();

    final AgentIdentity agentIdentity1 =
      new PublicAgentIdentityImplementation("my agent");

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
}