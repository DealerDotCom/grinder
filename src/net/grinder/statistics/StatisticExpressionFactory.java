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
 * @stereotype singleton
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

    public final String normaliseExpressionString(String expression)
	throws GrinderException
    {
	final ParseContext parseContext = new ParseContext(expression);
	final StringBuffer result = new StringBuffer(expression.length());

	try {
	    normaliseExpressionString(parseContext, result);
	    return result.toString();
	}
	finally {
	    if (parseContext.hasMoreCharacters()) {
		throw parseContext.new ParseException(
		    "Additional characters found");
	    }
	}	
    }

    private final void normaliseExpressionString(ParseContext parseContext,
						 StringBuffer result)
	throws GrinderException
    {
	if (parseContext.peekCharacter() == '(') {
	    // Compound expression.
	    result.append(parseContext.readCharacter());
	    result.append(parseContext.readToken());

	    while (parseContext.peekCharacter() != ')') {
		result.append(' ');
		normaliseExpressionString(parseContext, result);
	    }

	    result.append(parseContext.readCharacter());
	}
	else {
	    result.append(parseContext.readToken());
	}
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
	if (parseContext.peekCharacter() == '(') {
	    parseContext.readCharacter();

	    final String operation = parseContext.readToken();

	    try {
		if ("+".equals(operation)) {
		    return createSum(readOperands(parseContext, indexMap, 2));
		}
		else if ("*".equals(operation)) {
		    return
			createProduct(readOperands(parseContext, indexMap, 2));
		}
		else if ("/".equals(operation)) {
		    return
			createDivision(
			    createExpression(parseContext, indexMap),
			    createExpression(parseContext, indexMap));
		}
	    }
	    finally {
		if (parseContext.readCharacter() != ')') {
		    throw parseContext.new ParseException("Expecting ')'");
		}
	    }

	    throw parseContext.new ParseException(
		"Unknown operation '" + operation + "'");
	}
	else {
	    final String token = parseContext.readToken();

	    try {
		return createConstantExpression(Long.parseLong(token));
	    }
	    catch (NumberFormatException e) {
		try {
		    return createConstantExpression(Double.parseDouble(token));
		}
		catch (NumberFormatException e2) {
		    // Raw statistic name.
		    return createPrimitiveStatistic(
			indexMap.getIndexFor(token));
		}
	    }
	}
    }

    public final StatisticExpression createConstantExpression(final long value)
    {
	return new LongStatistic() {
		public final long getValue(RawStatistics rawStatistics) {
		    return value;
		}
	    };
    }

    public final StatisticExpression
	createConstantExpression(final double value)
    {
	return new DoubleStatistic() {
		public final double getValue(RawStatistics rawStatistics) {
		    return value;
		}
	    };
    }

    public final StatisticExpression
	createPrimitiveStatistic(final int processStatisticsIndex) 
    {
	return new PrimitiveStatistic(processStatisticsIndex);
    }

    public final StatisticExpression
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

    public final StatisticExpression
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

    public final StatisticExpression
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

    public final PeakStatisticExpression
	createPeak(final StatisticExpression operand)
    {
	if (operand.isDouble()) {
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

    private final StatisticExpression[]
	readOperands(ParseContext parseContext,
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

	public final boolean isDouble()
	{
	    return true;
	}

	/** Default to false so only PrimitiveStatistic needs to override. **/
	public boolean isPrimitive()
	{
	    return false;
	}

	protected abstract double getValue(RawStatistics rawStatistics);
    }

    private static abstract class PeakDoubleStatistic
	extends DoubleStatistic implements PeakStatisticExpression 
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

	public final boolean isDouble()
	{
	    return false;
	}

	/** Default to false so only PrimitiveStatistic needs to override. **/
	public boolean isPrimitive()
	{
	    return false;
	}

	protected abstract long getValue(RawStatistics rawStatistics);
    }

    private final static class PrimitiveStatistic extends LongStatistic
    {
	private final int m_processStatisticsIndex;

	public PrimitiveStatistic(int processStatisticsIndex)
	{
	    m_processStatisticsIndex = processStatisticsIndex;
	}

	public final long getValue(RawStatistics rawStatistics) {
	    return rawStatistics.getValue(m_processStatisticsIndex);
	}

	public boolean isPrimitive()
	{
	    return true;
	}
    }

    private static abstract class PeakLongStatistic
	extends LongStatistic implements PeakStatisticExpression 
    {
    }

    private static abstract class VariableArgumentsExpression
    {
	final StatisticExpression m_expression;

	public VariableArgumentsExpression(
	    final double initialValue, final StatisticExpression[] operands) 
	{
	    boolean doubleResult = false;

	    for (int i=0; i<operands.length && !doubleResult; ++i) {
		if (operands[i].isDouble()) {
		    doubleResult = true;
		}
	    }

	    if (doubleResult) {
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
