// Copyright (C) 2000 Paco Gomez
// Copyright (C) 2000, 2001, 2002 Philip Aston
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

import java.util.ArrayList;
import java.util.List;

import net.grinder.common.GrinderException;


/**
 *
 * @author Philip Aston
 * @version $Revision$
 * @stereotype singleton
 */
public final class StatisticExpressionFactory
{
    private final static StatisticExpressionFactory s_instance =
	new StatisticExpressionFactory();

    /**
     * @link dependency 
     * @stereotype instantiate
     **/
    /*#StatisticExpression lnkStatisticExpression;*/

    /**
     * @link aggregation
     * @supplierCardinality 1 
     */
    private final StatisticsIndexMap m_indexMap =
	StatisticsIndexMap.getInstance();

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

	normaliseExpressionString(parseContext, result);

	if (parseContext.hasMoreCharacters()) {
	    throw parseContext.new ParseException(
		"Additional characters found");
	}

	return result.toString();
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

    public final StatisticExpression createExpression(String expression)
	throws GrinderException
    {
	final ParseContext parseContext = new ParseContext(expression);

	final StatisticExpression result = createExpression(parseContext);

	if (parseContext.hasMoreCharacters()) {
	    throw parseContext.new ParseException(
		"Additional characters found");
	}

	return result;
    }

    private final
	StatisticExpression createExpression(ParseContext parseContext)
	throws GrinderException
    {
	if (parseContext.peekCharacter() == '(') {
	    parseContext.readCharacter();

	    final String operation = parseContext.readToken();
	    final StatisticExpression result;

	    if ("+".equals(operation)) {
		result = createSum(readOperands(parseContext, 2));
	    }
	    else if ("*".equals(operation)) {
		result =
		    createProduct(readOperands(parseContext, 2));
	    }
	    else if ("/".equals(operation)) {
		result = createDivision(createExpression(parseContext),
					createExpression(parseContext));
	    }
	    else {
		throw parseContext.new ParseException(
		    "Unknown operation '" + operation + "'");
	    }

	    if (parseContext.readCharacter() != ')') {
		throw parseContext.new ParseException("Expecting ')'");
	    }

	    return result;
	}
	else {
	    final String token = parseContext.readToken();

	    try {
		return createConstant(Long.parseLong(token));
	    }
	    catch (NumberFormatException e) {
		try {
		    return createConstant(Double.parseDouble(token));
		}
		catch (NumberFormatException e2) {
		    if (m_indexMap.isLongIndex(token)) {
			return createPrimitive(
			    m_indexMap.getIndexForLong(token));
		    }
		    else if (m_indexMap.isDoubleIndex(token)) {
			return createPrimitive(
			    m_indexMap.getIndexForDouble(token));
		    }
		}
	    }

	    throw parseContext.new ParseException("Unknown token '" + token +
						  "'");
	}
    }

    public final StatisticExpression createConstant(final long value)
    {
	return new LongStatistic() {
		public final long getValue(RawStatistics rawStatistics) {
		    return value;
		}
	    };
    }

    public final StatisticExpression createConstant(final double value)
    {
	return new DoubleStatistic() {
		public final double getValue(RawStatistics rawStatistics) {
		    return value;
		}
	    };
    }

    public final StatisticExpression
	createPrimitive(StatisticsIndexMap.DoubleIndex index) 
    {
	return new PrimitiveDoubleStatistic(index);
    }

    public final StatisticExpression 
	createPrimitive(StatisticsIndexMap.LongIndex index) 
    {
	return new PrimitiveLongStatistic(index);
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
	createPeak(StatisticsIndexMap.DoubleIndex peakIndex,
		   StatisticExpression monitoredStatistic)
    {
	return new PeakDoubleStatistic(peakIndex, monitoredStatistic);
    }

    public final PeakStatisticExpression
	createPeak(StatisticsIndexMap.LongIndex peakIndex,
		   StatisticExpression monitoredStatistic)
    {
	return new PeakLongStatistic(peakIndex, monitoredStatistic);
    }

    private final StatisticExpression[]
	readOperands(ParseContext parseContext, int minimumSize)
	throws GrinderException
    {
	final List arrayList = new ArrayList();

	while (parseContext.peekCharacter() != ')') {
	    arrayList.add(createExpression(parseContext));
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

	protected abstract double getValue(RawStatistics rawStatistics);
    }

    private static class PrimitiveDoubleStatistic extends DoubleStatistic
    {
	private final StatisticsIndexMap.DoubleIndex m_index;

	public PrimitiveDoubleStatistic(StatisticsIndexMap.DoubleIndex index)
	{
	    m_index = index;
	}

	public final double getValue(RawStatistics rawStatistics) {
	    return rawStatistics.getValue(m_index);
	}

	protected final void setValue(RawStatistics rawStatistics,
				      double value)
	{
	    rawStatistics.setValue(m_index, value);
	}
    }

    private static class PeakDoubleStatistic
	extends PrimitiveDoubleStatistic implements PeakStatisticExpression 
    {
	private final StatisticExpression m_monitoredStatistic;

	public PeakDoubleStatistic(StatisticsIndexMap.DoubleIndex peakIndex,
				   StatisticExpression monitoredStatistic)
	{
	    super(peakIndex);
	    m_monitoredStatistic = monitoredStatistic;
	}

	public void update(RawStatistics monitoredStatistics,
			   RawStatistics peakStorageStatistics) {
	    setValue(peakStorageStatistics,
		     Math.max(getValue(peakStorageStatistics),
			      m_monitoredStatistic.getDoubleValue(
				  monitoredStatistics)));
	}
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

	protected abstract long getValue(RawStatistics rawStatistics);
    }

    private static class PrimitiveLongStatistic extends LongStatistic
    {
	private final StatisticsIndexMap.LongIndex m_index;

	public PrimitiveLongStatistic(StatisticsIndexMap.LongIndex index)
	{
	    m_index = index;
	}

	public final long getValue(RawStatistics rawStatistics) {
	    return rawStatistics.getValue(m_index);
	}

	protected final void setValue(RawStatistics rawStatistics, long value)
	{
	    rawStatistics.setValue(m_index, value);
	}
    }

    private static class PeakLongStatistic
	extends PrimitiveLongStatistic implements PeakStatisticExpression 
    {
	private final StatisticExpression m_monitoredStatistic;

	public PeakLongStatistic(StatisticsIndexMap.LongIndex peakIndex,
				 StatisticExpression monitoredStatistic)
	{
	    super(peakIndex);
	    m_monitoredStatistic = monitoredStatistic;
	}

	public void update(RawStatistics monitoredStatistics,
			   RawStatistics peakStorageStatistics) {
	    setValue(peakStorageStatistics,
		     Math.max(getValue(peakStorageStatistics),
			      m_monitoredStatistic.getLongValue(
				  monitoredStatistics)));
	}
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
