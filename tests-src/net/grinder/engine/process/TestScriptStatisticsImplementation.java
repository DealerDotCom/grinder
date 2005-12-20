// Copyright (C) 2005 Philip Aston
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

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;

import net.grinder.common.StubTest;
import net.grinder.common.Test;
import net.grinder.script.InvalidContextException;
import net.grinder.script.Statistics;
import net.grinder.statistics.ExpressionView;
import net.grinder.statistics.StatisticsIndexMap;
import net.grinder.statistics.StatisticsServices;
import net.grinder.statistics.StatisticsServicesImplementation;
import net.grinder.statistics.StatisticsServicesTestFactory;
import net.grinder.testutility.RandomStubFactory;

import junit.framework.TestCase;


/**
 * Unit test case for <code>ScriptStatisticsImplementation</code>.
 *
 * @author Philip Aston
 * @version $Revision$
 */
public class TestScriptStatisticsImplementation extends TestCase {

  private static final StatisticsIndexMap.LongIndex s_errorsIndex;
  private static final StatisticsIndexMap.LongIndex s_untimedTestsIndex;
  private static final StatisticsIndexMap.DoubleIndex s_userDouble0Index;

  static {
    final StatisticsIndexMap indexMap =
      StatisticsServicesImplementation.getInstance().getStatisticsIndexMap();

    s_errorsIndex = indexMap.getLongIndex("errors");
    s_untimedTestsIndex = indexMap.getLongIndex("untimedTests");
    s_userDouble0Index = indexMap.getDoubleIndex("userDouble0");
  }

  private final ThreadContextStubFactory m_threadContextFactory =
    new ThreadContextStubFactory();

  private final StubThreadContextLocator m_threadContextLocator =
    new StubThreadContextLocator();

  private final ByteArrayOutputStream m_dataOutput =
    new ByteArrayOutputStream();

  private final PrintWriter m_dataWriter = new PrintWriter(m_dataOutput, true);

  public void testContextChecks() throws Exception {

    final ScriptStatisticsImplementation scriptStatisticsImplementation =
      new ScriptStatisticsImplementation(
          m_threadContextLocator,
          m_dataWriter,
          StatisticsServicesImplementation.getInstance(),
          3,
          false);

    assertFalse(scriptStatisticsImplementation.availableForUpdate());

    try {
      scriptStatisticsImplementation.report();
      fail("Expected InvalidContextException");
    }
    catch (InvalidContextException e) {
      assertTrue(e.getMessage().indexOf("worker threads") > 0);
    }

    m_threadContextLocator.set(m_threadContextFactory.getThreadContext());
    assertFalse(scriptStatisticsImplementation.availableForUpdate());

    try {
      scriptStatisticsImplementation.report();
      fail("Expected InvalidContextException");
    }
    catch (InvalidContextException e) {
      assertTrue(e.getMessage().indexOf("which they are acquired") > 0);
    }

    m_threadContextFactory.setScriptStatistics(scriptStatisticsImplementation);
    assertFalse(scriptStatisticsImplementation.availableForUpdate());

    try {
      scriptStatisticsImplementation.report();
      fail("Expected InvalidContextException");
    }
    catch (InvalidContextException e) {
      assertTrue(e.getMessage().indexOf("not yet performed") > 0);
    }
  }

  public void testReport() throws Exception {

    final StatisticsServices statisticsServices =
      StatisticsServicesTestFactory.createTestInstance();

    final ScriptStatisticsImplementation scriptStatisticsImplementation =
      new ScriptStatisticsImplementation(
          m_threadContextLocator,
          m_dataWriter,
          statisticsServices,
          0,
          true);

    m_threadContextLocator.set(m_threadContextFactory.getThreadContext());
    m_threadContextFactory.setScriptStatistics(scriptStatisticsImplementation);

    assertFalse(scriptStatisticsImplementation.availableForUpdate());

    final Test test = new StubTest(1, "A description");
    final TestData testData =
      new TestData(null, m_threadContextLocator,
                   statisticsServices.getStatisticsSetFactory(), test);

    scriptStatisticsImplementation.beginRun();

    scriptStatisticsImplementation.beginTest(testData, 10);
    assertEquals("No previous data to flush", 0, m_dataOutput.size());
    assertTrue(scriptStatisticsImplementation.availableForUpdate());
    assertEquals(-1, scriptStatisticsImplementation.getTime());

    scriptStatisticsImplementation.endTest(123, 99);
    assertEquals("0, 10, 1, 123, 99, 0", m_dataOutput.toString().trim());
    assertFalse(scriptStatisticsImplementation.availableForUpdate());
    assertEquals(99, scriptStatisticsImplementation.getTime());

    m_dataOutput.reset();

    // No op.
    scriptStatisticsImplementation.report();
    assertEquals(0, m_dataOutput.size());

    scriptStatisticsImplementation.endRun();

    scriptStatisticsImplementation.beginRun();

    scriptStatisticsImplementation.beginTest(testData, 12);
    scriptStatisticsImplementation.setSuccess(false);
    scriptStatisticsImplementation.endTest(300, 10);
    assertEquals("0, 12, 1, 300, 0, 1", m_dataOutput.toString().trim());
    assertEquals(-1, scriptStatisticsImplementation.getTime());

    m_dataOutput.reset();

    try {
      scriptStatisticsImplementation.setSuccess(false);
      fail("Expected InvalidContextException");
    }
    catch (InvalidContextException e) {
    }

    scriptStatisticsImplementation.setDelayReports(true);

    scriptStatisticsImplementation.beginTest(testData, 12);
    scriptStatisticsImplementation.endTest(410, 231);
    assertEquals("Report delayed", 0, m_dataOutput.size());
    assertEquals(231, scriptStatisticsImplementation.getTime());

    scriptStatisticsImplementation.report();
    assertEquals("0, 12, 1, 410, 231, 0", m_dataOutput.toString().trim());
    assertEquals(231, scriptStatisticsImplementation.getTime());

    m_dataOutput.reset();

    scriptStatisticsImplementation.beginTest(testData, 12);
    scriptStatisticsImplementation.endTest(410, 231);
    assertEquals("Report delayed", 0, m_dataOutput.size());

    scriptStatisticsImplementation.setDelayReports(false);
    assertEquals("0, 12, 1, 410, 231, 0", m_dataOutput.toString().trim());

    scriptStatisticsImplementation.endRun();
  }

  public void testReportWithRecordTimeFalse() throws Exception {

    final StatisticsServices statisticsServices =
      StatisticsServicesTestFactory.createTestInstance();

    final ScriptStatisticsImplementation scriptStatisticsImplementation =
      new ScriptStatisticsImplementation(
          m_threadContextLocator,
          m_dataWriter,
          statisticsServices,
          3,
          false);

    m_threadContextLocator.set(m_threadContextFactory.getThreadContext());
    m_threadContextFactory.setScriptStatistics(scriptStatisticsImplementation);

    final Test test = new StubTest(22, "A description");
    final TestData testData =
      new TestData(null, m_threadContextLocator,
                   statisticsServices.getStatisticsSetFactory(), test);

    scriptStatisticsImplementation.beginRun();

    scriptStatisticsImplementation.beginTest(testData, 10);
    assertEquals("No previous data to flush", 0, m_dataOutput.size());

    scriptStatisticsImplementation.endTest(123, 99);
    assertEquals("3, 10, 22, 123, 0, 0", m_dataOutput.toString().trim());
    assertEquals(99, scriptStatisticsImplementation.getTime());

    m_dataOutput.reset();

    // No op.
    scriptStatisticsImplementation.report();
    assertEquals(0, m_dataOutput.size());

    scriptStatisticsImplementation.endRun();

    scriptStatisticsImplementation.beginRun();

    scriptStatisticsImplementation.beginTest(testData, 12);
    scriptStatisticsImplementation.setSuccess(false);
    scriptStatisticsImplementation.endTest(300, 10);
    assertEquals("3, 12, 22, 300, 0, 1", m_dataOutput.toString().trim());

    m_dataOutput.reset();

    try {
      scriptStatisticsImplementation.setSuccess(false);
      fail("Expected InvalidContextException");
    }
    catch (InvalidContextException e) {
    }

    scriptStatisticsImplementation.setDelayReports(true);

    scriptStatisticsImplementation.beginTest(testData, 12);
    scriptStatisticsImplementation.endTest(410, 231);
    assertEquals("Report delayed", 0, m_dataOutput.size());

    scriptStatisticsImplementation.report();
    assertEquals("3, 12, 22, 410, 0, 0", m_dataOutput.toString().trim());

    m_dataOutput.reset();

    scriptStatisticsImplementation.beginTest(testData, 12);
    scriptStatisticsImplementation.endTest(410, 231);
    assertEquals("Report delayed", 0, m_dataOutput.size());

    scriptStatisticsImplementation.endRun();

    assertEquals("endRun() has flushed report",
                 "3, 12, 22, 410, 0, 0", m_dataOutput.toString().trim());
    assertEquals(231, scriptStatisticsImplementation.getTime());
  }

  public void testStatistics() throws Exception {

    final StatisticsServices statisticsServices =
      StatisticsServicesTestFactory.createTestInstance();

    statisticsServices.getDetailStatisticsView().add(
      new ExpressionView("foo", "foo", "userDouble0"));

    final ScriptStatisticsImplementation scriptStatisticsImplementation =
      new ScriptStatisticsImplementation(
          m_threadContextLocator,
          m_dataWriter,
          statisticsServices,
          3,
          false);

    m_threadContextLocator.set(m_threadContextFactory.getThreadContext());
    m_threadContextFactory.setScriptStatistics(scriptStatisticsImplementation);

    final Test test = new StubTest(22, "A description");
    final TestData testData =
      new TestData(null, m_threadContextLocator,
                   statisticsServices.getStatisticsSetFactory(), test);

    scriptStatisticsImplementation.beginRun();

    scriptStatisticsImplementation.beginTest(testData, 10);

    scriptStatisticsImplementation.setValue(s_errorsIndex, 10);
    scriptStatisticsImplementation.addValue(s_untimedTestsIndex, 7);
    scriptStatisticsImplementation.addValue(s_untimedTestsIndex, 1);
    scriptStatisticsImplementation.addValue(s_userDouble0Index, 1.5);
    scriptStatisticsImplementation.setValue(s_userDouble0Index, 1.5);

    assertEquals(10, scriptStatisticsImplementation.getValue(s_errorsIndex));
    assertEquals(8,
                 scriptStatisticsImplementation.getValue(s_untimedTestsIndex));
    assertEquals(1.5d,
                 scriptStatisticsImplementation.getValue(s_userDouble0Index),
                 0.0001d);

    scriptStatisticsImplementation.endTest(123, 99);

    // Errors should be overriden to 1 and
    // untimed tests should be overridden to 0.
    assertEquals(1, scriptStatisticsImplementation.getValue(s_errorsIndex));
    assertEquals(0,
                 scriptStatisticsImplementation.getValue(s_untimedTestsIndex));
    assertEquals(1.5d,
                 scriptStatisticsImplementation.getValue(s_userDouble0Index),
                 0.0001d);

    assertEquals("3, 10, 22, 123, 0, 1, 1.5", m_dataOutput.toString().trim());

    m_dataOutput.reset();

    scriptStatisticsImplementation.beginTest(testData, 10);

    scriptStatisticsImplementation.setValue(s_errorsIndex, 10);
    scriptStatisticsImplementation.addValue(s_untimedTestsIndex, 7);
    scriptStatisticsImplementation.addValue(s_untimedTestsIndex, 1);
    scriptStatisticsImplementation.addValue(s_userDouble0Index, 1.5);
    scriptStatisticsImplementation.setSuccess(true);

    assertEquals(0, scriptStatisticsImplementation.getValue(s_errorsIndex));
    assertEquals(8,
                 scriptStatisticsImplementation.getValue(s_untimedTestsIndex));
    assertEquals(1.5d,
                 scriptStatisticsImplementation.getValue(s_userDouble0Index),
                 0.0001d);

    scriptStatisticsImplementation.endTest(123, 99);

    // Errors should be overriden to 0 and
    // untimed tests should be overridden to 1.
    assertEquals(0, scriptStatisticsImplementation.getValue(s_errorsIndex));
    assertEquals(1,
                 scriptStatisticsImplementation.getValue(s_untimedTestsIndex));
    assertEquals(1.5d,
                 scriptStatisticsImplementation.getValue(s_userDouble0Index),
                 0.0001d);

    assertEquals("3, 10, 22, 123, 0, 0, 1.5", m_dataOutput.toString().trim());
  }

  /**
   * Must be public so that override_ methods can be called
   * externally.
   */
  public static class ThreadContextStubFactory extends RandomStubFactory {

    private Statistics m_scriptStatistics;

    public ThreadContextStubFactory() {
      super(ThreadContext.class);
    }

    public void setScriptStatistics(Statistics statistics) {
      m_scriptStatistics = statistics;
    }

    public final ThreadContext getThreadContext() {
      return (ThreadContext)getStub();
    }

    public Statistics override_getScriptStatistics(Object proxy) {
      return m_scriptStatistics;
    }
  }
}