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
import net.grinder.statistics.ExpressionView;
import net.grinder.statistics.StatisticsIndexMap;
import net.grinder.statistics.StatisticsServices;
import net.grinder.statistics.StatisticsServicesImplementation;
import net.grinder.statistics.StatisticsServicesTestFactory;
import net.grinder.statistics.StatisticsSet;
import net.grinder.testutility.RandomStubFactory;
import junit.framework.TestCase;


public class TestThreadDataWriter extends TestCase {

  private static final StatisticsIndexMap.LongIndex s_errorsIndex;
  private static final StatisticsIndexMap.LongSampleIndex s_timedTestsIndex;
  private static final StatisticsIndexMap.DoubleIndex s_userDouble0Index;

  static {
    final StatisticsIndexMap indexMap =
      StatisticsServicesImplementation.getInstance().getStatisticsIndexMap();

    s_errorsIndex = indexMap.getLongIndex("errors");
    s_timedTestsIndex = indexMap.getLongSampleIndex("timedTests");
    s_userDouble0Index = indexMap.getDoubleIndex("userDouble0");
  }

  private final ByteArrayOutputStream m_dataOutput =
    new ByteArrayOutputStream();

  private final PrintWriter m_dataFilePrintWriter =
    new PrintWriter(m_dataOutput, true);

  public void testReport() throws Exception {

    final StatisticsServices statisticsServices =
      StatisticsServicesTestFactory.createTestInstance();

    final ThreadDataWriter threadDataWriter =
      new ThreadDataWriter(
          m_dataFilePrintWriter,
          statisticsServices.getDetailStatisticsView().getExpressionViews(),
          33);

    final Test test1 = new StubTest(1, "A description");
    final Test test3 = new StubTest(3, "Another test");

    final StatisticsSet statistics =
      statisticsServices.getStatisticsSetFactory().create();

    final RandomStubFactory dispatchContextStubFactory =
      new RandomStubFactory(DispatchContext.class);
    final DispatchContext dispatchContext =
      (DispatchContext)dispatchContextStubFactory.getStub();

    dispatchContextStubFactory.setResult("getTest", test1);
    dispatchContextStubFactory.setResult("getStartTime", new Long(123));
    dispatchContextStubFactory.setResult("getStatistics", statistics);
    statistics.addSample(s_timedTestsIndex, 99);

    threadDataWriter.report(dispatchContext, 10);

    assertEquals("33, 10, 1, 123, 99, 0", m_dataOutput.toString().trim());
    m_dataOutput.reset();

    dispatchContextStubFactory.setResult("getStartTime", new Long(125));

    threadDataWriter.report(dispatchContext, 10);

    assertEquals("33, 10, 1, 125, 99, 0", m_dataOutput.toString().trim());
    m_dataOutput.reset();

    dispatchContextStubFactory.setResult("getTest", test3);
    dispatchContextStubFactory.setResult("getStartTime", new Long(300));

    threadDataWriter.report(dispatchContext, 11);

    assertEquals("33, 11, 3, 300, 99, 0", m_dataOutput.toString().trim());
    m_dataOutput.reset();

    statistics.reset();
    statistics.setValue(s_errorsIndex, 1);
    dispatchContextStubFactory.setResult("getStartTime", new Long(301));

    threadDataWriter.report(dispatchContext, 11);

    assertEquals("33, 11, 3, 301, 0, 1", m_dataOutput.toString().trim());
    m_dataOutput.reset();

    statisticsServices.getDetailStatisticsView().add(
      new ExpressionView("foo", "foo", "userDouble0"));
    statistics.reset();
    statistics.addSample(s_timedTestsIndex, 5);
    statistics.addValue(s_userDouble0Index, 1.5);
    dispatchContextStubFactory.setResult("getStartTime", new Long(530));

    final ThreadDataWriter threadDataWriter2 =
      new ThreadDataWriter(
          m_dataFilePrintWriter,
          statisticsServices.getDetailStatisticsView().getExpressionViews(),
          33);

    threadDataWriter2.report(dispatchContext, 11);

    assertEquals("33, 11, 3, 530, 5, 0, 1.5", m_dataOutput.toString().trim());
    m_dataOutput.reset();
  }
}