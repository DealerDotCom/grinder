// Copyright (C) 2000 Paco Gomez
// Copyright (C) 2000, 2001, 2002, 2003, 2004, 2005 Philip Aston
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

package net.grinder.statistics;

import junit.framework.TestCase;


/**
 * Unit test case for <code>TestStatisticsImplementation</code>.
 *
 * @author Philip Aston
 * @version $Revision$
 * @see RawStatistics
 */
public class TestTestStatisticsImplementation extends TestCase {
  public TestTestStatisticsImplementation(String name) {
	super(name);
  }

  public void testTestStatisticsImplementation() throws Exception {
    final StatisticsIndexMap statisticsIndexMap =
      StatisticsIndexMap.getInstance();
    final StatisticsIndexMap.LongIndex errorStatisticIndex =
      statisticsIndexMap.getIndexForLong("errors");
    final StatisticsIndexMap.LongIndex untimedTestsIndex =
      statisticsIndexMap.getIndexForLong("untimedTests");
    final StatisticsIndexMap.LongSampleIndex timedTestsIndex =
      statisticsIndexMap.getIndexForLongSample("timedTests");

	final TestStatistics testStatistics1 =
	    new TestStatisticsImplementation();

	assertEquals(0, testStatistics1.getErrors());
	assertEquals(0, testStatistics1.getTests());
	assertTrue(Double.isNaN(testStatistics1.getAverageTestTime()));

	final TestStatistics testStatistics2 =
	    new TestStatisticsImplementation();

	assertTrue(testStatistics1 != testStatistics2);

	assertEquals(testStatistics1, testStatistics2);

	testStatistics1.addValue(errorStatisticIndex, 1);
	assertEquals(1, testStatistics1.getErrors());
	assertTrue(!testStatistics1.equals(testStatistics2));

	testStatistics2.addValue(errorStatisticIndex, 1);
	assertEquals(testStatistics1, testStatistics2);

	testStatistics1.addValue(untimedTestsIndex, 1);
	assertEquals(1, testStatistics1.getTests());
	assertTrue(!testStatistics1.equals(testStatistics2));

	testStatistics2.addValue(untimedTestsIndex, 1);
	assertEquals(testStatistics1, testStatistics2);

	testStatistics1.addSample(timedTestsIndex, 5);
	testStatistics2.addSample(timedTestsIndex, 10);
	assertEquals(2, testStatistics1.getTests());
	assertTrue(!testStatistics1.equals(testStatistics2));

	testStatistics1.addSample(timedTestsIndex, 10);
	testStatistics2.addSample(timedTestsIndex, 5);
	assertEquals(testStatistics1, testStatistics2);
	assertEquals(7.5d, testStatistics2.getAverageTestTime(), 0.01);
  }
}
