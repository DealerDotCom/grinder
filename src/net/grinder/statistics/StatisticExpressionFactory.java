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

import java.util.ArrayList;
import java.util.List;


/**
 * Factory for StatisticExpressions.
 *
 * @author Philip Aston
 * @version $Revision$
 */
public final class StatisticExpressionFactory {

  private final StatisticsIndexMap m_indexMap;

  StatisticExpressionFactory(StatisticsIndexMap statisticsIndexMap) {
    m_indexMap = statisticsIndexMap;
  }

  /**
   * Apply standard formatting to an expression.
   *
   * @param expression The expression.
   * @return The formatted expression.
   * @exception StatisticsException If the expression is invalid.
   */
  public String normaliseExpressionString(String expression)
    throws StatisticsException {
    final ParseContext parseContext = new ParseContext(expression);
    final StringBuffer result = new StringBuffer(expression.length());

    normaliseExpressionString(parseContext, result);

    if (parseContext.hasMoreCharacters()) {
      throw parseContext.new ParseException("Additional characters found");
    }

    return result.toString();
  }

  private void normaliseExpressionString(ParseContext parseContext,
                                         StringBuffer result)
    throws StatisticsException {
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

  /**
   * Parse an expression.
   *
   * @param expression The expression.
   * @return The parsed expression.
   * @exception StatisticsException If the expression is invalid.
   */
  public StatisticExpression createExpression(String expression)
    throws StatisticsException {

    final ParseContext parseContext = new ParseContext(expression);

    final StatisticExpression result = createExpression(parseContext);

    if (parseContext.hasMoreCharacters()) {
      throw parseContext.new ParseException(
        "Additional characters found");
    }

    return result;
  }

  private StatisticExpression createExpression(ParseContext parseContext)
    throws ParseContext.ParseException {

    if (parseContext.peekCharacter() == '(') {
      parseContext.readCharacter();

      final String operation = parseContext.readToken();
      final StatisticExpression result;

      if ("+".equals(operation)) {
        result = createSum(readOperands(parseContext, 2));
      }
      else if ("*".equals(operation)) {
        result = createProduct(readOperands(parseContext, 2));
      }
      else if ("/".equals(operation)) {
        result = createDivision(createExpression(parseContext),
                                createExpression(parseContext));
      }
      else if ("sum".equals(operation)) {
        result = createSampleSum(parseContext);
      }
      else if ("count".equals(operation)) {
        result = createSampleCount(parseContext);
      }
      else if ("variance".equals(operation)) {
        result = createSampleVariance(parseContext);
      }
      else if ("sqrt".equals(operation)) {
        result = createSquareRoot(createExpression(parseContext));
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
          final StatisticsIndexMap.LongIndex longIndex =
            m_indexMap.getLongIndex(token);

          if (longIndex != null) {
            return createPrimitive(longIndex);
          }

          final StatisticsIndexMap.DoubleIndex doubleIndex =
            m_indexMap.getDoubleIndex(token);

          if (doubleIndex != null) {
            return createPrimitive(doubleIndex);
          }
        }
      }

      throw parseContext.new ParseException("Unknown token '" + token + "'");
    }
  }

  /**
   * Create a constant long expression.
   *
   * @param value The value.
   * @return The <code>StatisticExpression</code>.
   */
  public StatisticExpression createConstant(final long value) {
    return new LongStatistic() {
        public long getValue(StatisticsSet statisticsSet) {
          return value;
        }
      };
  }

  /**
   *  Create a constant float expression.
   *
   * @param value The value.
   * @return The <code>StatisticExpression</code>.
   */
  public StatisticExpression createConstant(final double value) {
    return new DoubleStatistic() {
        public double getValue(StatisticsSet statisticsSet) {
          return value;
        }
      };
  }

  /**
   * Create a primitive double expression.
   *
   * @param index The expression index.
   * @return The <code>StatisticExpression</code>.
   */
  public StatisticExpression
    createPrimitive(StatisticsIndexMap.DoubleIndex index) {
    return new PrimitiveDoubleStatistic(index);
  }

  /**
   * Create a primitive long expression.
   *
   * @param index The expression index.
   * @return The <code>StatisticExpression</code>.
   */
  public StatisticExpression
    createPrimitive(StatisticsIndexMap.LongIndex index) {
    return new PrimitiveLongStatistic(index);
  }

  /**
   * Create a sum.
   *
   * @param operands The things to add.
   * @return The resulting expression.
   */
  public StatisticExpression
    createSum(final StatisticExpression[] operands) {

    return new VariableArgumentsExpression(0, operands) {
        public double doDoubleOperation(
          double result, StatisticExpression operand,
          StatisticsSet statisticsSet) {
          return result + operand.getDoubleValue(statisticsSet);
        }

        public long doLongOperation(
          long result, StatisticExpression operand,
          StatisticsSet statisticsSet) {
          return result + operand.getLongValue(statisticsSet);
        }
      }
      .getExpression();
  }

  /**
   * Create a product.
   *
   * @param operands The things to multiply.
   * @return The resulting expression.
   */
  public StatisticExpression
    createProduct(final StatisticExpression[] operands) {

    return new VariableArgumentsExpression(1, operands) {
        public double doDoubleOperation(
          double result, StatisticExpression operand,
          StatisticsSet statisticsSet) {
          return result * operand.getDoubleValue(statisticsSet);
        }

        public long doLongOperation(
          long result, StatisticExpression operand,
          StatisticsSet statisticsSet) {
          return result * operand.getLongValue(statisticsSet);
        }
      }
      .getExpression();
  }

  /**
   * Create a division.
   *
   * @param numerator The numerator.
   * @param denominator The denominator.
   * @return The resulting expression.
   */
  public StatisticExpression
    createDivision(final StatisticExpression numerator,
                   final StatisticExpression denominator) {

    return new DoubleStatistic() {
        public double getValue(StatisticsSet statisticsSet) {
          return
            numerator.getDoubleValue(statisticsSet) /
            denominator.getDoubleValue(statisticsSet);
        }
      };
  }

  /**
   * Create an accessor for a sample's sum attribute.
   *
   * @param parseContext The parse context.
   * @return The resulting expression.
   * @throws ParseException If the parse failed.
   */
  private StatisticExpression createSampleSum(ParseContext parseContext)
    throws ParseContext.ParseException {

    final StatisticExpression result;

    final String token = parseContext.readToken();

    final StatisticsIndexMap.DoubleSampleIndex doubleSampleIndex =
      m_indexMap.getDoubleSampleIndex(token);

    if (doubleSampleIndex != null) {
      result = createPrimitive(doubleSampleIndex.getSumIndex());
    }
    else {
      final StatisticsIndexMap.LongSampleIndex longSampleIndex =
        m_indexMap.getLongSampleIndex(token);

      if (longSampleIndex != null) {
        result = createPrimitive(longSampleIndex.getSumIndex());
      }
      else {
        throw parseContext.new ParseException(
            "Can't apply sum to unknown sample index '" + token + "'");
      }
    }

    return result;
  }

  /**
   * Create an accessor for a sample's count attribute.
   *
   * @param parseContext The parse context.
   * @return The resulting expression.
   * @throws ParseException If the parse failed.
   */
  private StatisticExpression createSampleCount(ParseContext parseContext)
    throws ParseContext.ParseException {

    final StatisticExpression result;

    final String token = parseContext.readToken();

    final StatisticsIndexMap.DoubleSampleIndex doubleSampleIndex =
      m_indexMap.getDoubleSampleIndex(token);

    if (doubleSampleIndex != null) {
      result = createPrimitive(doubleSampleIndex.getCountIndex());
    }
    else {
      final StatisticsIndexMap.LongSampleIndex longSampleIndex =
        m_indexMap.getLongSampleIndex(token);

      if (longSampleIndex != null) {
        result = createPrimitive(longSampleIndex.getCountIndex());
      }
      else {
        throw parseContext.new ParseException(
            "Can't apply count to unknown sample index '" + token + "'");
      }
    }

    return result;
  }

  /**
   * Create an accessor for a sample's variance attribute.
   *
   * @param parseContext The parse context.
   * @return The resulting expression.
   * @throws ParseException If the parse failed.
   */
  private StatisticExpression createSampleVariance(ParseContext parseContext)
    throws ParseContext.ParseException {

    final StatisticExpression result;

    final String token = parseContext.readToken();

    final StatisticsIndexMap.DoubleSampleIndex doubleSampleIndex =
      m_indexMap.getDoubleSampleIndex(token);

    if (doubleSampleIndex != null) {
      result = createPrimitive(doubleSampleIndex.getVarianceIndex());
    }
    else {
      final StatisticsIndexMap.LongSampleIndex longSampleIndex =
        m_indexMap.getLongSampleIndex(token);

      if (longSampleIndex != null) {
        result = createPrimitive(longSampleIndex.getVarianceIndex());
      }
      else {
        throw parseContext.new ParseException(
            "Can't apply variance to unknown sample index '" + token + "'");
      }
    }

    return result;
  }

  /**
   * Create a square root.
   *
   * @param operand The operand.
   * @return The resulting expression.
   */
  public StatisticExpression
    createSquareRoot(final StatisticExpression operand) {

    return new DoubleStatistic() {
        public double getValue(StatisticsSet statisticsSet) {
          return Math.sqrt(operand.getDoubleValue(statisticsSet));
        }
      };
  }

  /**
   * Create a peak double statistic.
   *
   * @param peakIndex Index of a slot to store peak information in.
   * @param monitoredStatistic Statistic to monitor.
   * @return The resulting expression.
   */
  public PeakStatisticExpression
    createPeak(StatisticsIndexMap.DoubleIndex peakIndex,
               StatisticExpression monitoredStatistic) {
    return new PeakDoubleStatistic(peakIndex, monitoredStatistic);
  }

  /**
   * Create a peak long statistic.
   *
   * @param peakIndex Index of a slot to store peak information in.
   * @param monitoredStatistic Statistic to monitor.
   * @return The resulting expression.
   */
  public PeakStatisticExpression
    createPeak(StatisticsIndexMap.LongIndex peakIndex,
               StatisticExpression monitoredStatistic) {
    return new PeakLongStatistic(peakIndex, monitoredStatistic);
  }

  private StatisticExpression[] readOperands(ParseContext parseContext,
                                             int minimumSize)
    throws ParseContext.ParseException {
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

  private abstract static class DoubleStatistic
    implements StatisticExpression {
    public final double getDoubleValue(StatisticsSet statisticsSet) {
      return getValue(statisticsSet);
    }

    public final long getLongValue(StatisticsSet statisticsSet) {
      return (long)getValue(statisticsSet);
    }

    public final boolean isDouble() {
      return true;
    }

    protected abstract double getValue(StatisticsSet statisticsSet);
  }

  private static class PrimitiveDoubleStatistic extends DoubleStatistic {

    private final StatisticsIndexMap.DoubleIndex m_index;

    public PrimitiveDoubleStatistic(StatisticsIndexMap.DoubleIndex index) {
      m_index = index;
    }

    public final double getValue(StatisticsSet statisticsSet) {
      return statisticsSet.getValue(m_index);
    }

    protected final void setValue(StatisticsSet statisticsSet, double value) {
      statisticsSet.setValue(m_index, value);
    }
  }

  private static class PeakDoubleStatistic
    extends PrimitiveDoubleStatistic implements PeakStatisticExpression {

    private final StatisticExpression m_monitoredStatistic;

    public PeakDoubleStatistic(StatisticsIndexMap.DoubleIndex peakIndex,
                               StatisticExpression monitoredStatistic) {
      super(peakIndex);
      m_monitoredStatistic = monitoredStatistic;
    }

    public void update(StatisticsSet monitoredStatistics,
                       StatisticsSet peakStorageStatistics) {
      setValue(peakStorageStatistics,
               Math.max(getValue(peakStorageStatistics),
                        m_monitoredStatistic.getDoubleValue(
                          monitoredStatistics)));
    }
  }

  private abstract static class LongStatistic implements StatisticExpression {

    public final double getDoubleValue(StatisticsSet statisticsSet) {
      return getValue(statisticsSet);
    }

    public final long getLongValue(StatisticsSet statisticsSet) {
      return getValue(statisticsSet);
    }

    public final boolean isDouble() {
      return false;
    }

    protected abstract long getValue(StatisticsSet statisticsSet);
  }

  private static class PrimitiveLongStatistic extends LongStatistic {

    private final StatisticsIndexMap.LongIndex m_index;

    public PrimitiveLongStatistic(StatisticsIndexMap.LongIndex index) {
      m_index = index;
    }

    public final long getValue(StatisticsSet statisticsSet) {
      return statisticsSet.getValue(m_index);
    }

    protected final void setValue(StatisticsSet statisticsSet, long value) {
      statisticsSet.setValue(m_index, value);
    }
  }

  private static class PeakLongStatistic
    extends PrimitiveLongStatistic implements PeakStatisticExpression {
    private final StatisticExpression m_monitoredStatistic;

    public PeakLongStatistic(StatisticsIndexMap.LongIndex peakIndex,
                             StatisticExpression monitoredStatistic) {
      super(peakIndex);
      m_monitoredStatistic = monitoredStatistic;
    }

    public void update(StatisticsSet monitoredStatistics,
                       StatisticsSet peakStorageStatistics) {
      setValue(peakStorageStatistics,
               Math.max(getValue(peakStorageStatistics),
                        m_monitoredStatistic.getLongValue(
                          monitoredStatistics)));
    }
  }

  private abstract static class VariableArgumentsExpression {

    private final StatisticExpression m_expression;

    public VariableArgumentsExpression(
      final double initialValue, final StatisticExpression[] operands) {

      boolean doubleResult = false;

      for (int i = 0; i < operands.length && !doubleResult; ++i) {
        if (operands[i].isDouble()) {
          doubleResult = true;
        }
      }

      if (doubleResult) {
        m_expression = new DoubleStatistic() {
            public final double getValue(
              StatisticsSet statisticsSet) {
              double result = initialValue;

              for (int i = 0; i < operands.length; ++i) {
                result = doDoubleOperation(result, operands[i], statisticsSet);
              }

              return result;
            }
          };
      }
      else {
        m_expression = new LongStatistic() {
            public final long getValue(
              StatisticsSet statisticsSet) {
              long result = (long)initialValue;

              for (int i = 0; i < operands.length; ++i) {
                result = doLongOperation(result, operands[i], statisticsSet);
              }

              return result;
            }
          };
      }
    }

    protected abstract double
      doDoubleOperation(double result, StatisticExpression operand,
                        StatisticsSet statisticsSet);

    protected abstract long
      doLongOperation(long result, StatisticExpression operand,
                      StatisticsSet statisticsSet);

    final StatisticExpression getExpression() {
      return m_expression;
    }
  }

  /**
   * Package scope for unit tests.
   */
  static final class ParseContext {

    private static final char EOS_SENTINEL = 0;
    private final char[] m_expression;
    private int m_index;

    public ParseContext(String expression) {
      m_expression = expression.toCharArray();
      m_index = 0;
    }

    public boolean hasMoreCharacters() {
      eatWhiteSpace();
      return m_index < m_expression.length;
    }

    public char peekCharacter() {
      eatWhiteSpace();
      return peekCharacterNoEat();
    }

    private char peekCharacterNoEat() {
      if (m_index >= m_expression.length) {
        return EOS_SENTINEL;
      }

      return m_expression[m_index];
    }

    public char readCharacter() {
      final char result = peekCharacter();

      if (result != EOS_SENTINEL) {
        ++m_index;
      }

      return result;
    }

    public String readToken() throws ParseException {
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

    private boolean isTokenCharacter(char c) {
      return
        c != EOS_SENTINEL &&
        c != '(' &&
        c != ')' &&
        !Character.isWhitespace(c);
    }

    private void eatWhiteSpace() {
      while (Character.isWhitespace(peekCharacterNoEat())) {
        ++m_index;
      }
    }

    /**
     * Exception representing a failure to parse an expression.
     */
    public final class ParseException extends StatisticsException {
      private ParseException(String message) {
        this(message, m_index);
      }

      public ParseException(String message, int where) {
        super("Parse exception: " + message + ", at character " + where +
              " of '" + new String(m_expression) + "'");
      }
    }
  }
}
