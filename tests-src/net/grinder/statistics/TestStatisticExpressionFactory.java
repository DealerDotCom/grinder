// The Grinder
// Copyright (C) 2001  Paco Gomez
// Copyright (C) 2001  Philip Aston

// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.

// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.

// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.

package net.grinder.statistics;

import junit.framework.TestCase;
import junit.swingui.TestRunner;
//import junit.textui.TestRunner;

import net.grinder.common.GrinderException;
import net.grinder.statistics.ProcessStatisticsIndexMap;
import net.grinder.statistics.RawStatistics;


/**
 * Unit test case for <code>StatisticExpressionFactory</code>.
 *
 * @author Philip Aston
 * @version $Revision$
 * @see RawStatistics
 */
public class TestStatisticExpressionFactory extends TestCase
{
    public static void main(String[] args)
    {
	TestRunner.run(TestStatisticExpressionFactory.class);
    }

    public TestStatisticExpressionFactory(String name)
    {
	super(name);
    }

    private final StatisticExpressionFactory m_factory =
	StatisticExpressionFactory.getInstance();

    private final ProcessStatisticsIndexMap m_indexMap =
	new ProcessStatisticsIndexMap();

    private final RawStatistics m_rawStatistics = new RawStatistics();

    private final double m_accuracy = 0.00001d;

    protected void setUp()
    {
	m_rawStatistics.addValue(m_indexMap.getIndexFor("One"), 1);
	m_rawStatistics.addValue(m_indexMap.getIndexFor("Two"), 2);
    }

    public void testRawStatistic() throws Exception
    {
	myAssertEquals(1, m_factory.createRawStatistic(
			   m_indexMap.getIndexFor("One")));

	myAssertEquals(0, m_factory.createExpression("  Test ", m_indexMap));

	myAssertEquals(2, m_factory.createExpression("Two", m_indexMap));

	try {
	    m_factory.createExpression("", m_indexMap);
	    fail("Expected a GrinderException");
	}
	catch (GrinderException e) {
	}

	try {
	    m_factory.createExpression("One Two", m_indexMap);
	    fail("Expected a GrinderException");
	}
	catch (GrinderException e) {
	}
    }

    public void testSum() throws Exception
    {
	myAssertEquals(2, m_factory.createExpression(
			   "(+ One One)", m_indexMap));

	myAssertEquals(4, m_factory.createExpression(
			   "(+ One Two One)", m_indexMap));

	myAssertEquals(5, m_factory.createExpression(
			   "(+ One (+ One Two) One)", m_indexMap));

	try {
	    m_factory.createExpression("(+)", m_indexMap);
	    fail("Expected a GrinderException");
	}
	catch (GrinderException e) {
	}

	try {
	    m_factory.createExpression("(+ One)", m_indexMap);
	    fail("Expected a GrinderException");
	}
	catch (GrinderException e) {
	}
    }

    public void testProduct() throws Exception
    {
	myAssertEquals(1, m_factory.createExpression(
			   "(* One One)", m_indexMap));

	myAssertEquals(4, m_factory.createExpression(
			   "(* One Two Two)", m_indexMap));

	myAssertEquals(8, m_factory.createExpression(
			   "(* Two (* Two Two) One)", m_indexMap));

	try {
	    m_factory.createExpression("(*)", m_indexMap);
	    fail("Expected a GrinderException");
	}
	catch (GrinderException e) {
	}

	try {
	    m_factory.createExpression("(* One)", m_indexMap);
	    fail("Expected a GrinderException");
	}
	catch (GrinderException e) {
	}
    }

    public void testDivision() throws Exception
    {
	myAssertEquals(1d, m_factory.createExpression(
			   "(/ One One)", m_indexMap));

	myAssertEquals(0.5d, m_factory.createExpression(
			   "(/ One Two)", m_indexMap));

	myAssertEquals(2d, m_factory.createExpression(
			   "(/ Two One)", m_indexMap));

	try {
	    m_factory.createExpression("(/)", m_indexMap);
	    fail("Expected a GrinderException");
	}
	catch (GrinderException e) {
	}

	try {
	    m_factory.createExpression("(/ One)", m_indexMap);
	    fail("Expected a GrinderException");
	}
	catch (GrinderException e) {
	}

	try {
	    m_factory.createExpression("(/ One One One)", m_indexMap);
	    fail("Expected a GrinderException");
	}
	catch (GrinderException e) {
	}
    }

    public void testLongPeak() throws Exception
    {
	final int statIndex = m_indexMap.getIndexFor("testPeak");

	final RawStatistics rawStatistics2 = new RawStatistics();
	rawStatistics2.addValue(statIndex, 2);

	final RawStatistics rawStatistics9 = new RawStatistics();
	rawStatistics9.addValue(statIndex, 9);

	final RawStatistics rawStatistics33 = new RawStatistics();
	rawStatistics33.addValue(statIndex, 33);

	final PeakStatistic peak = 
	    m_factory.createPeak(m_factory.createExpression("testPeak",
							    m_indexMap));

	myAssertEquals(0, peak, rawStatistics9);
	peak.update(rawStatistics9);
	myAssertEquals(9, peak, rawStatistics9);

	myAssertEquals(9, peak, rawStatistics33);
	peak.update(rawStatistics33);
	myAssertEquals(33, peak, rawStatistics33);

	myAssertEquals(33, peak, rawStatistics2);
	peak.update(rawStatistics2);
	myAssertEquals(33, peak, rawStatistics2);
	peak.reset(rawStatistics2);
	myAssertEquals(2, peak, rawStatistics2);
    }

    public void testDoublePeak() throws Exception
    {
	final int xIndex = m_indexMap.getIndexFor("x");
	final int yIndex = m_indexMap.getIndexFor("y");

	final RawStatistics rawStatisticsHalf = new RawStatistics();
	rawStatisticsHalf.addValue(xIndex, 1);
	rawStatisticsHalf.addValue(yIndex, 2);

	final RawStatistics rawStatistics9 = new RawStatistics();
	rawStatistics9.addValue(xIndex, 9);
	rawStatistics9.addValue(yIndex, 1);

	final RawStatistics rawStatistics33 = new RawStatistics();
	rawStatistics33.addValue(xIndex, 33);
	rawStatistics33.addValue(yIndex, 1);

	final PeakStatistic peak = 
	    m_factory.createPeak(m_factory.createExpression("(/ x y)",
							    m_indexMap));

	myAssertEquals(0d, peak, rawStatistics9);
	peak.update(rawStatistics9);
	myAssertEquals(9d, peak, rawStatistics9);

	myAssertEquals(9d, peak, rawStatistics33);
	peak.update(rawStatistics33);
	myAssertEquals(33d, peak, rawStatistics33);

	myAssertEquals(33d, peak, rawStatisticsHalf);
	peak.update(rawStatisticsHalf);
	myAssertEquals(33d, peak, rawStatisticsHalf);
	peak.reset(rawStatisticsHalf);
	myAssertEquals(0.5d, peak, rawStatisticsHalf);
    }

    public void testParseCompoundExpessions() throws Exception
    {
	myAssertEquals(0.5,
		       m_factory.createExpression(
			   "(/ One (+ One One))", m_indexMap));

	myAssertEquals(2.25d,
		       m_factory.createExpression(
			   "(+ One (/ One (+ Two Two)) One)", m_indexMap));

	myAssertEquals(2.25d,
		       m_factory.createExpression(
			   "(+ One (/ One (* Two Two)) One)", m_indexMap));


	try {
	    m_factory.createExpression("(+", m_indexMap);
	    fail("Expected a GrinderException");
	}
	catch (GrinderException e) {
	}

	try {
	    m_factory.createExpression("+)", m_indexMap);
	    fail("Expected a GrinderException");
	}
	catch (GrinderException e) {
	}
    }

    private void myAssertEquals(long expected, StatisticExpression expression)
    {
	myAssertEquals(expected, expression, m_rawStatistics);
    }

    private void myAssertEquals(long expected, StatisticExpression expression,
				RawStatistics rawStatistics)
    {
	assertEquals(expected, expression.getLongValue(rawStatistics));
	myAssertEquals((double)expected, expression, rawStatistics);
    }

    private void myAssertEquals(double expected,
				StatisticExpression expression)
    {
	myAssertEquals(expected, expression, m_rawStatistics);
    }

    private void myAssertEquals(double expected,
				StatisticExpression expression,
				RawStatistics rawStatistics)
    {
	assertEquals(expected, expression.getDoubleValue(rawStatistics),
		     0.00001d);
    }
}
