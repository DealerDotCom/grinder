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

package net.grinder.common;

import java.util.Comparator;

import net.grinder.engine.agent.PublicAgentIdentityImplementation;
import net.grinder.testutility.RandomStubFactory;

import junit.framework.TestCase;


/**
 * Unit tests for {@link ProcessReport}.
 *
 * @author Philip Aston
 * @version $Revision:$
 */
public class TestProcessReport extends TestCase {

  public void testStateThenNameComparator() throws Exception {
    final Comparator comparator =
      new ProcessReport.StateThenNameComparator();

    final AgentIdentity agentIdentity1 =
      new PublicAgentIdentityImplementation("my agent");

    final RandomStubFactory agentProcessReportStubFactory1 =
      new RandomStubFactory(AgentProcessReport.class);
    final AgentProcessReport agentProcessReport1 =
      (AgentProcessReport)agentProcessReportStubFactory1.getStub();
    agentProcessReportStubFactory1.setResult("getIdentity", agentIdentity1);
    agentProcessReportStubFactory1.setResult("getAgentIdentity", agentIdentity1);
    agentProcessReportStubFactory1.setResult(
      "getState", new Short(AgentProcessReport.STATE_RUNNING));

    assertEquals(0,
      comparator.compare(agentProcessReport1, agentProcessReport1));

    final RandomStubFactory agentProcessReportStubFactory2 =
      new RandomStubFactory(AgentProcessReport.class);
    final AgentProcessReport agentProcessReport2 =
      (AgentProcessReport)agentProcessReportStubFactory2.getStub();
    agentProcessReportStubFactory2.setResult("getIdentity", agentIdentity1);
    agentProcessReportStubFactory2.setResult("getAgentIdentity", agentIdentity1);
    agentProcessReportStubFactory2.setResult(
      "getState", new Short(AgentProcessReport.STATE_FINISHED));

    assertTrue(
      comparator.compare(agentProcessReport1, agentProcessReport2) < 0);

    assertTrue(
      comparator.compare(agentProcessReport2, agentProcessReport1) > 0);

    final AgentIdentity agentIdentity2 =
      new PublicAgentIdentityImplementation("zzzagent");

    final RandomStubFactory agentProcessReportStubFactory3 =
      new RandomStubFactory(AgentProcessReport.class);
    final AgentProcessReport agentProcessReport3 =
      (AgentProcessReport)agentProcessReportStubFactory3.getStub();
    agentProcessReportStubFactory3.setResult("getIdentity", agentIdentity2);
    agentProcessReportStubFactory3.setResult("getAgentIdentity", agentIdentity2);
    agentProcessReportStubFactory3.setResult(
      "getState", new Short(AgentProcessReport.STATE_FINISHED));

    assertTrue(
      comparator.compare(agentProcessReport3, agentProcessReport2) > 0);

    assertTrue(
      comparator.compare(agentProcessReport2, agentProcessReport3) < 0);
  }
}
