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

    private final RawStatistics m_rawStatistics =
	new RawStatisticsImplementation();

    private final double m_accuracy = 0.00001d;

    protected void setUp()
    {
	m_rawStatistics.addValue(m_indexMap.getIndexFor("One"), 1);
	m_rawStatistics.addValue(m_indexMap.getIndexFor("Two"), 2);
    }

    public void testConstant() throws Exception
    {
	final StatisticExpression longExpression =
	    m_factory.createConstantExpression(-22);

	myAssertEquals(-22, longExpression);
	assert(!longExpression.isPrimitive());
	assert(!longExpression.isDouble());

	final StatisticExpression doubleExpression =
	    m_factory.createConstantExpression(2.3);

	myAssertEquals(2.3d, doubleExpression);
	assert(!doubleExpression.isPrimitive());
	assert(doubleExpression.isDouble());

	myAssertEquals(0, m_factory.createExpression("0", m_indexMap));
	myAssertEquals(99d, m_factory.createExpression("99f", m_indexMap));

	try {
	    m_factory.createExpression("1 2", m_indexMap);
	    fail("Expected a GrinderException");
	}
	catch (GrinderException e) {
	}
    }

    public void testPrimitive() throws Exception
    {
	final StatisticExpression expression =
	    m_factory.createPrimitiveStatistic(m_indexMap.getIndexFor("One"));

	myAssertEquals(1, expression);
	assert(expression.isPrimitive());
	assert(!expression.isDouble());

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
	final StatisticExpression[] expressions = {
	    m_factory.createExpression("One", m_indexMap),
	    m_factory.createExpression("Two", m_indexMap),
	    m_factory.createExpression("Two", m_indexMap),
	};

	final StatisticExpression expression =
	    m_factory.createSum(expressions);

	myAssertEquals(5, expression);
	assert(!expression.isPrimitive());
	assert(!expression.isDouble());

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
	final StatisticExpression[] expressions = {
	    m_factory.createExpression("One", m_indexMap),
	    m_factory.createExpression("Two", m_indexMap),
	    m_factory.createExpression("Two", m_indexMap),
	};

	final StatisticExpression expression =
	    m_factory.createProduct(expressions);

	myAssertEquals(4, expression);
	assert(!expression.isPrimitive());
	assert(!expression.isDouble());

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
	final StatisticExpression expression =
	    m_factory.createDivision(
		m_factory.createExpression("Two", m_indexMap),
		m_factory.createExpression("Two", m_indexMap));

	myAssertEquals(1, expression);
	assert(!expression.isPrimitive());
	assert(expression.isDouble());

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
	final StatisticExpression expression =
	    m_factory.createPeak(
		m_factory.createExpression("Two", m_indexMap));

	myAssertEquals(0, expression);
	assert(!expression.isPrimitive());
	assert(!expression.isDouble());

	final int statIndex = m_indexMap.getIndexFor("testPeak");

	final RawStatistics rawStatistics2 =
	    new RawStatisticsImplementation();

	rawStatistics2.addValue(statIndex, 2);

	final RawStatistics rawStatistics9 =
	    new RawStatisticsImplementation();
	rawStatistics9.addValue(statIndex, 9);

	final RawStatistics rawStatistics33 =
	    new RawStatisticsImplementation();
	rawStatistics33.addValue(statIndex, 33);

	final PeakStatisticExpression peak = 
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
	final StatisticExpression expression =
	    m_factory.createPeak(
		m_factory.createExpression("(/ Two One)", m_indexMap));

	myAssertEquals(0, expression);
	assert(!expression.isPrimitive());
	assert(expression.isDouble());

	final int xIndex = m_indexMap.getIndexFor("x");
	final int yIndex = m_indexMap.getIndexFor("y");

	final RawStatistics rawStatisticsHalf =
	    new RawStatisticsImplementation();
	rawStatisticsHalf.addValue(xIndex, 1);
	rawStatisticsHalf.addValue(yIndex, 2);

	final RawStatistics rawStatistics9 =
	    new RawStatisticsImplementation();
	rawStatistics9.addValue(xIndex, 9);
	rawStatistics9.addValue(yIndex, 1);

	final RawStatistics rawStatistics33 =
	    new RawStatisticsImplementation();
	rawStatistics33.addValue(xIndex, 33);
	rawStatistics33.addValue(yIndex, 1);

	final PeakStatisticExpression peak = 
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

	myAssertEquals(9d,
		       m_factory.createExpression(
			   "(* 4 (+ One (/ One (* Two Two)) One))",
			   m_indexMap));

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

    public void testNormaliseExpressionString() throws Exception
    {
	assertEquals("Test",
		     m_factory.normaliseExpressionString(" Test "));

	assertEquals("(+ One Two (* One Two))",
		     m_factory.normaliseExpressionString(
			 "\t(+ One Two( \n  * One Two) )"));
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
