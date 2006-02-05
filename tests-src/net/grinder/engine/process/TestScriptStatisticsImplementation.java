// Copyright (C) 2006 Philip Aston
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

package net.grinder.engine.process;

import net.grinder.script.InvalidContextException;
import net.grinder.statistics.StatisticsIndexMap;
import net.grinder.statistics.StatisticsServices;
import net.grinder.statistics.StatisticsServicesImplementation;
import net.grinder.statistics.StatisticsServicesTestFactory;
import net.grinder.statistics.StatisticsSet;
import net.grinder.testutility.AssertUtilities;
import net.grinder.testutility.RandomStubFactory;

import junit.framework.TestCase;


/**
 * Unit test case for {@link ScriptStatisticsImplementation}.
 *
 * @author Philip Aston
 * @version $Revision$
 */
public class TestScriptStatisticsImplementation extends TestCase {

  private static final StatisticsIndexMap.LongIndex s_errorsIndex;
  private static final StatisticsIndexMap.DoubleIndex s_userDouble0Index;

  static {
    final StatisticsIndexMap indexMap =
      StatisticsServicesImplementation.getInstance().getStatisticsIndexMap();

    s_errorsIndex = indexMap.getLongIndex("errors");
    s_userDouble0Index = indexMap.getDoubleIndex("userDouble0");
  }

  private final RandomStubFactory m_testStatisticsHelperStubFactory =
    new RandomStubFactory(TestStatisticsHelper.class);
  private final TestStatisticsHelper m_testStatisticsHelper =
    (TestStatisticsHelper)m_testStatisticsHelperStubFactory.getStub();

  private final RandomStubFactory m_threadContextStubFactory =
    new RandomStubFactory(ThreadContext.class);
  private final ThreadContext m_threadContext =
    (ThreadContext)m_threadContextStubFactory.getStub();

  private final RandomStubFactory m_dispatchContextStubFactory =
    new RandomStubFactory(DispatchContext.class);
  private final DispatchContext m_dispatchContext =
    (DispatchContext)m_dispatchContextStubFactory.getStub();

  private final StubThreadContextLocator m_threadContextLocator =
    new StubThreadContextLocator();

  private final StatisticsServices m_statisticsServices =
    StatisticsServicesTestFactory.createTestInstance();
  private StatisticsSet m_statisticsSet =
    m_statisticsServices.getStatisticsSetFactory().create();

  public void testContextChecks() throws Exception {

    final ScriptStatisticsImplementation scriptStatistics =
      new ScriptStatisticsImplementation(
          m_threadContextLocator,
          m_testStatisticsHelper,
          m_statisticsServices.getStatisticsIndexMap());

    assertSame(
      scriptStatistics.getStatisticsIndexMap(),
      m_statisticsServices.getStatisticsIndexMap());

    // 1. Null thread context.
    assertFalse(scriptStatistics.availableForUpdate());

    try {
      scriptStatistics.report();
      fail("Expected InvalidContextException");
    }
    catch (InvalidContextException e) {
      AssertUtilities.assertContains(e.getMessage(), "worker threads");
    }

    // 2. Different script statistics.
    m_threadContextLocator.set(m_threadContext);
    m_threadContextStubFactory.setResult("getScriptStatistics",
      new ScriptStatisticsImplementation(
        m_threadContextLocator,
        m_testStatisticsHelper,
        m_statisticsServices.getStatisticsIndexMap()));

    assertFalse(scriptStatistics.availableForUpdate());

    try {
      scriptStatistics.report();
      fail("Expected InvalidContextException");
    }
    catch (InvalidContextException e) {
      AssertUtilities.assertContains(e.getMessage(), "which they are acquired");
    }

    // 3. Null dispatch context.
    m_threadContextStubFactory.setResult("getScriptStatistics", scriptStatistics);
    m_threadContextStubFactory.setResult("getDispatchContext", null);
    assertFalse(scriptStatistics.availableForUpdate());

    try {
      scriptStatistics.getSuccess();
      fail("Expected InvalidContextException");
    }
    catch (InvalidContextException e) {
      AssertUtilities.assertContains(
        e.getMessage(), "should have called setDelayReports");
    }

    m_threadContextStubFactory.setResult("getDispatchContext", m_dispatchContext);
    m_dispatchContextStubFactory.setResult("getStatistics", m_statisticsSet);

    final boolean result = scriptStatistics.getSuccess();

    assertEquals(
      new Boolean(result),
      m_testStatisticsHelperStubFactory
      .assertSuccess("getSuccess", m_statisticsSet).getResult());
    m_testStatisticsHelperStubFactory.assertNoMoreCalls();
  }

  public void testThreadContextPassThrough() throws Exception {
    final ScriptStatisticsImplementation scriptStatistics =
      new ScriptStatisticsImplementation(
          m_threadContextLocator,
          m_testStatisticsHelper,
          m_statisticsServices.getStatisticsIndexMap());
    m_threadContextLocator.set(m_threadContext);
    m_threadContextStubFactory.setResult("getScriptStatistics", scriptStatistics);

    scriptStatistics.setDelayReports(true);
    m_threadContextStubFactory.assertSuccess("getScriptStatistics");
    m_threadContextStubFactory.assertSuccess("setDelayReports", Boolean.TRUE);

    scriptStatistics.setDelayReports(false);
    m_threadContextStubFactory.assertSuccess("getScriptStatistics");
    m_threadContextStubFactory.assertSuccess("setDelayReports", Boolean.FALSE);

    scriptStatistics.report();
    m_threadContextStubFactory.assertSuccess("getScriptStatistics");
    m_threadContextStubFactory.assertSuccess("flushPendingDispatchContext");

    m_threadContextStubFactory.assertNoMoreCalls();
  }

  public void testStatisticsPassThrough() throws Exception {
    final ScriptStatisticsImplementation scriptStatistics =
      new ScriptStatisticsImplementation(
          m_threadContextLocator,
          m_testStatisticsHelper,
          m_statisticsServices.getStatisticsIndexMap());
    m_threadContextLocator.set(m_threadContext);
    m_threadContextStubFactory.setResult("getScriptStatistics", scriptStatistics);

    m_threadContextStubFactory.setResult("getDispatchContext", m_dispatchContext);
    m_dispatchContextStubFactory.setResult("getStatistics", m_statisticsSet);

    scriptStatistics.setValue(s_errorsIndex, 1);
    assertEquals(1, scriptStatistics.getValue(s_errorsIndex));
    assertEquals(1, m_statisticsSet.getValue(s_errorsIndex));

    scriptStatistics.setValue(s_userDouble0Index, 5.3);
    assertEquals(5.3d, scriptStatistics.getValue(s_userDouble0Index), 0.01);
    assertEquals(5.3d, m_statisticsSet.getValue(s_userDouble0Index), 0.01);

    scriptStatistics.addValue(s_userDouble0Index, 1.0);
    assertEquals(6.3d, scriptStatistics.getValue(s_userDouble0Index), 0.01);
    assertEquals(6.3d, m_statisticsSet.getValue(s_userDouble0Index), 0.01);

    scriptStatistics.addValue(s_errorsIndex, 2);
    assertEquals(3, scriptStatistics.getValue(s_errorsIndex));
    assertEquals(3, m_statisticsSet.getValue(s_errorsIndex));

    scriptStatistics.setValue(s_errorsIndex, 0);
    assertEquals(0, scriptStatistics.getValue(s_errorsIndex));
    assertEquals(0, m_statisticsSet.getValue(s_errorsIndex));

    scriptStatistics.setSuccess(false);
    m_testStatisticsHelperStubFactory.assertSuccess(
      "setSuccess", m_statisticsSet, Boolean.FALSE);
    scriptStatistics.setSuccess(true);
    m_testStatisticsHelperStubFactory.assertSuccess(
      "setSuccess", m_statisticsSet, Boolean.TRUE);
  }

  public void testDispatchContextPassThrough() throws Exception {
    final ScriptStatisticsImplementation scriptStatistics =
      new ScriptStatisticsImplementation(
          m_threadContextLocator,
          m_testStatisticsHelper,
          m_statisticsServices.getStatisticsIndexMap());
    m_threadContextLocator.set(m_threadContext);
    m_threadContextStubFactory.setResult("getScriptStatistics", scriptStatistics);

    m_threadContextStubFactory.setResult("getDispatchContext", m_dispatchContext);

    final long time = 213214L;
    m_dispatchContextStubFactory.setResult("getElapsedTime", new Long(time));
    assertEquals(time, scriptStatistics.getTime());
    m_dispatchContextStubFactory.assertSuccess("getElapsedTime");
    m_dispatchContextStubFactory.assertNoMoreCalls();
  }
}