// The Grinder
// Copyright (C) 2000, 2001  Paco Gomez
// Copyright (C) 2000, 2001  Philip Aston

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

import java.util.ArrayList;
import java.util.List;

import net.grinder.common.GrinderException;


/**
 *
 * @author Philip Aston
 * @version $Revision$
 */
final class StatisticExpressionFactory
{
    private final static StatisticExpressionFactory s_instance =
	new StatisticExpressionFactory();

    /**
     * @link dependency 
     * @stereotype instantiate
     **/
    /*#StatisticExpression lnkStatisticExpression;*/

    private StatisticExpressionFactory()
    {
    }

    public final static StatisticExpressionFactory getInstance()
    {
	return s_instance;
    }

    public final StatisticExpression
	createExpression(String expression, ProcessStatisticsIndexMap indexMap)
	throws GrinderException
    {
	final ParseContext parseContext = new ParseContext(expression);

	try {
	    return createExpression(parseContext, indexMap);
	}
	finally {
	    if (parseContext.hasMoreCharacters()) {
		throw parseContext.new ParseException(
		    "Additional characters found");
	    }
	}
    }

    private final StatisticExpression
	createExpression(ParseContext parseContext,
			 ProcessStatisticsIndexMap indexMap)
	throws GrinderException
    {
	final StatisticExpression result;

	if (parseContext.peekCharacter() == '(') {
	    parseContext.readCharacter();

	    final String operation = parseContext.readToken();

	    if ("+".equals(operation)) {
		result = createSum(parseOperands(parseContext, indexMap, 2));
	    }
	    else if ("*".equals(operation)) {
		result =
		    createProduct(parseOperands(parseContext, indexMap, 2));
	    }
	    else if ("/".equals(operation)) {
		result = createDivision(
		    createExpression(parseContext, indexMap),
		    createExpression(parseContext, indexMap));
	    }
	    else {
		throw parseContext.new ParseException(
		    "Unknown operation '" + operation + "'");
	    }

	    if (parseContext.readCharacter() != ')') {
		throw parseContext.new ParseException("Expecting ')'");
	    }
	}
	else {
	    // Raw statistic name. Should extend to handle constants.
	    result =
		createRawStatistic(
		    indexMap.getIndexFor(parseContext.readToken()));
	}

	return result;
    }

    public StatisticExpression
	createRawStatistic(final int processStatisticsIndex) 
    {
	return new LongStatistic() {
		public long getValue(RawStatistics rawStatistics) {
		    return rawStatistics.getValue(processStatisticsIndex);
		}
	    };
    }

    public StatisticExpression
	createSum(final StatisticExpression[] operands)
    {
	return new VariableArgumentsExpression(0, operands) {
		public final double doDoubleOperation(
		    double result, StatisticExpression operand,
		    RawStatistics rawStatistics) {
		    return result + operand.getDoubleValue(rawStatistics);
		}

		public final long doLongOperation(
		    long result, StatisticExpression operand,
		    RawStatistics rawStatistics) {
		    return result + operand.getLongValue(rawStatistics);
		}
	    }.getExpression();
    }

    public StatisticExpression
	createProduct(final StatisticExpression[] operands)
    {
	return new VariableArgumentsExpression(1, operands) {
		public final double doDoubleOperation(
		    double result, StatisticExpression operand,
		    RawStatistics rawStatistics) {
		    return result * operand.getDoubleValue(rawStatistics);
		}

		public final long doLongOperation(
		    long result, StatisticExpression operand,
		    RawStatistics rawStatistics) {
		    return result * operand.getLongValue(rawStatistics);
		}
	    }.getExpression();
    }

    public StatisticExpression
	createDivision(final StatisticExpression numerator,
		       final StatisticExpression denominator)
    {
	return new DoubleStatistic() {
		public double getValue(RawStatistics rawStatistics) {
		    return
			numerator.getDoubleValue(rawStatistics)/
			denominator.getDoubleValue(rawStatistics);
		}
	    };
    }

    public PeakStatistic createPeak(final StatisticExpression operand)
    {
	if (shouldPromoteResultToDouble(operand)) {
	    return new PeakDoubleStatistic() {
		    private double m_value = 0;

		    public double getValue(RawStatistics rawStatistics) {
			return m_value;
		    }

		    public void reset(RawStatistics rawStatistics) {
			m_value = operand.getDoubleValue(rawStatistics);
		    }

		    public void update(RawStatistics rawStatistics) {
			m_value =
			    Math.max(m_value,
				     operand.getDoubleValue(rawStatistics));
		    }
		};
	}
	else {
	    return new PeakLongStatistic() {
		    private long m_value = 0;

		    public long getValue(RawStatistics rawStatistics) {
			return m_value;
		    }

		    public void reset(RawStatistics rawStatistics) {
			m_value = operand.getLongValue(rawStatistics);
		    }

		    public void update(RawStatistics rawStatistics) {
			m_value =
			    Math.max(m_value,
				     operand.getLongValue(rawStatistics));
		    }
		};
	}
    }

    private StatisticExpression[]
	parseOperands(ParseContext parseContext,
		      ProcessStatisticsIndexMap indexMap, int minimumSize)
	throws GrinderException
    {
	final List arrayList = new ArrayList();

	while (parseContext.peekCharacter() != ')') {
	    arrayList.add(createExpression(parseContext, indexMap));
	}

	if (arrayList.size() < minimumSize) {
	    throw parseContext.new ParseException(
		"Operation must have at least two operands");
	}

	return (StatisticExpression[])
	    arrayList.toArray(new StatisticExpression[0]);
    }

    private static final boolean
	shouldPromoteResultToDouble(StatisticExpression operand)
    {
	return operand instanceof DoubleStatistic;
    }

    private static final boolean
	shouldPromoteResultToDouble(StatisticExpression[] operands)
    {
	for (int i=0; i<operands.length; ++i) {
	    if (shouldPromoteResultToDouble(operands[i]))
		return true;
	}

	return false;
    }

    private static abstract class DoubleStatistic
	implements StatisticExpression
    {
	public final double getDoubleValue(RawStatistics rawStatistics)
	{
	    return getValue(rawStatistics);
	}

	public final long getLongValue(RawStatistics rawStatistics)
	{
	    return (long)getValue(rawStatistics);
	}

	abstract double getValue(RawStatistics rawStatistics);
    }

    private static abstract class PeakDoubleStatistic
	extends DoubleStatistic implements PeakStatistic 
    {
    }

    private static abstract class LongStatistic implements StatisticExpression
    {
	public final double getDoubleValue(RawStatistics rawStatistics)
	{
	    return (double)getValue(rawStatistics);
	}

	public final long getLongValue(RawStatistics rawStatistics)
	{
	    return getValue(rawStatistics);
	}

	abstract long getValue(RawStatistics rawStatistics);
    }

    private static abstract class PeakLongStatistic
	extends LongStatistic implements PeakStatistic 
    {
    }

    private static abstract class VariableArgumentsExpression
    {
	final StatisticExpression m_expression;

	VariableArgumentsExpression(final double initialValue,
				    final StatisticExpression[] operands) 
	{
	    if (shouldPromoteResultToDouble(operands)) {
		m_expression = new DoubleStatistic() {
			public final double getValue(
			    RawStatistics rawStatistics) {
			    double result = initialValue;

			    for (int i=0; i<operands.length; ++i) {
				result =
				    doDoubleOperation(result, operands[i],
						      rawStatistics);
			    }

			    return result;
			}
		    };
	    }
	    else {
		m_expression = new LongStatistic() {
			public final long getValue(
			    RawStatistics rawStatistics) {
			    long result = (long)initialValue;

			    for (int i=0; i<operands.length; ++i) {
				result =
				    doLongOperation(result, operands[i],
						    rawStatistics);
			    }

			    return result;
			}
		    };
	    }
	}

	protected abstract double
	    doDoubleOperation(double result, StatisticExpression operand,
			      RawStatistics rawStatistics);

	protected abstract long
	    doLongOperation(long result, StatisticExpression operand,
			    RawStatistics rawStatistics);

	final StatisticExpression getExpression()
	{
	    return m_expression;
	}
    }

    private final static class ParseContext
    {
	private static final char EOS_SENTINEL = 0;
	private final char[] m_expression;
	private int m_index;

	public ParseContext(String expression)
	{
	    m_expression = expression.toCharArray();
	    m_index = 0;
	}

	public final boolean hasMoreCharacters()
	{
	    eatWhiteSpace();
	    return m_index < m_expression.length;
	}

	public final char peekCharacter()
	{
	    eatWhiteSpace();
	    return peekCharacterNoEat();
	}

	private final char peekCharacterNoEat()
	{
	    if (m_index >= m_expression.length) {
		return EOS_SENTINEL;
	    }

	    return m_expression[m_index];
	}

	public final char readCharacter()
	{
	    final char result = peekCharacter();

	    if (result != EOS_SENTINEL) {
		++m_index;
	    }

	    return result;
	}

	public final String readToken() throws ParseException
	{
	    eatWhiteSpace();

	    final int start = m_index;

	    while (isTokenCharacter(peekCharacterNoEat())) {
		++m_index;
	    }

	    final int stringLength = m_index - start;

	    if (stringLength == 0) {
		throw new ParseException("Expected a token", start);
	    }

	    return new String(m_expression, start, stringLength);
	}

	private final boolean isTokenCharacter(char c)
	{
	    return
		c != EOS_SENTINEL &&
		c != '(' &&
		c != ')' &&
		!Character.isWhitespace(c);
	}

	private final void eatWhiteSpace()
	{
	    while (Character.isWhitespace(peekCharacterNoEat())) {
		++m_index;
	    }
	}

	public final class ParseException extends GrinderException
	{
	    private ParseException(String message)
	    {
		this(message, m_index);
	    }

	    public ParseException(String message, int where)
	    {
		super("Parse exception: " + message + ", at character " +
		      where + " of '" + m_expression + "'");
	    }
	}
    }
}
