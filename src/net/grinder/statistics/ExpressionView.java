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

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;


/**
 * <p>Associate a statistic expression with display information.</p>
 *
 * <p>Statistic expressions are composed of statistic names (see
 * {@link StatisticsIndexMap}) in a simple post-fix format using the
 * symbols <code>+</code>, <code>-</code>, <code>/</code> and
 * <code>*</code>, which have their usual meanings, in conjunction with
 * simple statistic names or sub-expressions. Precedence can be controlled by
 * grouping expressions in parentheses. For example, the error rate is
 * <code>(* (/ errors period) 1000)</code> errors per second.</p>
 *
 * <p>Sample statistics, such as timedTests, must be introduced with one
 * of <code>sum</code>, <code>count</code>, or <code>variance</code>,
 * depending on the attribute of interest.</p>
 *
 * <p>For example, the statistic expression "<code>(/ (sum timedTests)
 * (count timedTests))</code>" represents the mean test time in milliseconds.
 *
 * @author Philip Aston
 * @version $Revision$
 */
public class ExpressionView {

  private static int s_creationOrder;
  private static StatisticExpressionFactory s_statisticExpressionFactory =
    StatisticExpressionFactory.getInstance();

  private final String m_displayName;
  private final String m_displayNameResourceKey;
  private final String m_expressionString;
  private final int m_hashCode;
  private final int m_creationOrder;

  private final StatisticExpression m_expression;

  static {
    // Ensure that the standard ExpressionViews are initialised
    // before any user ExpressionViews.
    final Class dummy = CommonStatisticsViews.class;
  }

  /**
   * Creates a new <code>ExpressionView</code> instance.
   *
   * @param displayName A common display name.
   * @param displayNameResourceKey A resource key to use to look up
   * an internationalised display name.
   * @param expressionString An expression string, used to create
   * the {@link StatisticExpression}.
   * @exception StatisticsException If the expression is invalid.
   * @see StatisticExpressionFactory
   */
  public ExpressionView(String displayName, String displayNameResourceKey,
                        String expressionString)
    throws StatisticsException {
    this(displayName, displayNameResourceKey,
         s_statisticExpressionFactory.normaliseExpressionString(
           expressionString),
         s_statisticExpressionFactory.createExpression(expressionString));
  }

  /**
   * Creates a new <code>ExpressionView</code> instance.
   *
   * @param displayName A common display name.
   * @param displayNameResourceKey A resource key to use to look up
   * an internationalised display name.
   * @param expression A {@link StatisticExpression}.
   */
  public ExpressionView(String displayName, String displayNameResourceKey,
                        StatisticExpression expression) {
    this(displayName, displayNameResourceKey, "", expression);
  }

  private ExpressionView(String displayName, String displayNameResourceKey,
                         String expressionString,
                         StatisticExpression expression) {
    m_displayName = displayName;
    m_displayNameResourceKey = displayNameResourceKey;
    m_expressionString = expressionString;
    m_expression = expression;

    m_hashCode =
      m_displayName.hashCode() ^
      m_displayNameResourceKey.hashCode() ^
      m_expressionString.hashCode();

    synchronized (ExpressionView.class) {
      m_creationOrder = s_creationOrder++;
    }
  }

  /**
   * @see StatisticsView#readExternal
   */
  ExpressionView(ObjectInput in) throws IOException, StatisticsException {
    this(in.readUTF(), in.readUTF(), in.readUTF());
  }

  /**
   * @see StatisticsView#writeExternal
   */
  final void myWriteExternal(ObjectOutput out) throws IOException {
    if (m_expressionString == "") {
      throw new IOException(
        "This expression view is not externalisable");
    }

    out.writeUTF(m_displayName);
    out.writeUTF(m_displayNameResourceKey);
    out.writeUTF(m_expressionString);
  }

  /**
   * Get the common display name.
   *
   * @return The display name.
   */
  public final String getDisplayName() {
    return m_displayName;
  }

  /**
   * Get the display name resource key.
   *
   * @return A key that might be used to look up an
   * internationalised display name.
   */
  public final String getDisplayNameResourceKey() {
    return m_displayNameResourceKey;
  }

  /**
   * Return the {@link StatisticExpression}.
   *
   * @return The {@link StatisticExpression}.
   */
  public final StatisticExpression getExpression() {
    return m_expression;
  }

  /**
   * Value based equality.
   *
   * @param other An <code>Object</code> to compare.
   * @return <code>true</code> => <code>other</code> is equal to this object.
   */
  public boolean equals(Object other) {
    if (other == this) {
      return true;
    }

    if (!(other instanceof ExpressionView)) {
      return false;
    }

    final ExpressionView otherView = (ExpressionView)other;

    return
      m_hashCode == otherView.m_hashCode &&
      m_displayName.equals(otherView.m_displayName) &&
      m_displayNameResourceKey.equals(
        otherView.m_displayNameResourceKey) &&

      // If either expression string is null, one of the views
      // is not externalisable. We then only compare on the
      // display names.
      (m_expressionString.length() == 0 ||
       otherView.m_expressionString.length() == 0 ||
       m_expressionString.equals(otherView.m_expressionString));
  }

  /**
   * Implement {@link Object#hashCode}.
   *
   * @return an <code>int</code> value
   */
  public final int hashCode() {
    return m_hashCode;
  }

  /**
   * Return a <code>String</code> representation of this
   * <code>ExpressionView</code>.
   *
   * @return The <code>String</code>
   */
  public final String toString() {
    return
      "ExpressionView(" + m_displayName + ", " +
      m_displayNameResourceKey + ", " + m_expressionString + ")";
  }

  final int getCreationOrder() {
    return m_creationOrder;
  }
}
