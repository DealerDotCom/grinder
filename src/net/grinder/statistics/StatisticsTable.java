// Copyright (C) 2000, 2001, 2002, 2003 Philip Aston
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

import java.io.PrintStream;
import java.io.PrintWriter;
import java.text.DecimalFormat;

import net.grinder.common.GrinderException;
import net.grinder.common.Test;
import net.grinder.util.FixedWidthFormatter;


/**
 * Package scope
 *
 * @author Philip Aston
 * @version $Revision$
 */
public class StatisticsTable {
  /**
   * @supplierCardinality 1
   **/
  private final TestStatisticsMap m_testStatisticsMap;

  /**
   * @clientRole totals 
   * @supplierCardinality 1
   * @link aggregation
   **/
  private final RawStatistics m_totals;
  private final DecimalFormat m_twoDPFormat = new DecimalFormat("0.00");
  private final int m_columnWidth = 12;
  private final String m_columnSeparator = " ";

  private final FixedWidthFormatter m_headingFormatter =
    new FixedWidthFormatter(FixedWidthFormatter.ALIGN_LEFT,
			    FixedWidthFormatter.FLOW_WORD_WRAP,
			    m_columnWidth);

  private final FixedWidthFormatter m_rowLabelFormatter =
    new FixedWidthFormatter(FixedWidthFormatter.ALIGN_LEFT,
			    FixedWidthFormatter.FLOW_TRUNCATE,
			    m_columnWidth);

  private final FixedWidthFormatter m_rowCellFormatter =
    new FixedWidthFormatter(FixedWidthFormatter.ALIGN_LEFT,
			    FixedWidthFormatter.FLOW_TRUNCATE,
			    m_columnWidth);

  /**
   * @supplierCardinality 1
   **/
  private final StatisticsView m_statisticsView;
    
  /**
   * Creates a new <code>StatisticsTable</code> instance.
   *
   * @param statisticsView Views.
   * @param testStatisticsMap Tests and associated statistics.
   */
  public StatisticsTable(StatisticsView statisticsView,
			 TestStatisticsMap testStatisticsMap) {
    m_statisticsView = statisticsView;
    m_testStatisticsMap = testStatisticsMap;

    m_totals = m_testStatisticsMap.getTotal();
  }

  /**
   * Write the table out an output stream.
   *
   * @param out The output stream.
   * @exception GrinderException if an error occurs
   */
  public final void print(PrintStream out) throws GrinderException {
    final PrintWriter writer = new PrintWriter(out);
    print(writer);
    writer.flush();
  }

  /**
   * Write the table out an output writer
   *
   * @param out The output writer
   * @exception GrinderException if an error occurs
   */
  public final void print(PrintWriter out) throws GrinderException {
    final ExpressionView[] expressionViews =
      m_statisticsView.getExpressionViews();

    final int numberOfHeaderColumns = expressionViews.length + 1;

    StringBuffer[] cells = new StringBuffer[numberOfHeaderColumns];
    StringBuffer[] remainders = new StringBuffer[numberOfHeaderColumns];

    for (int i=0; i<numberOfHeaderColumns; i++) {
      cells[i] = new StringBuffer(
	i == 0 ? "" : expressionViews[i-1].getDisplayName());

      remainders[i] = new StringBuffer();
    }

    boolean wrapped = false;

    do {
      wrapped = false;

      for (int i=0; i<numberOfHeaderColumns; ++i) {
	remainders[i].setLength(0);
	m_headingFormatter.transform(cells[i], remainders[i]);

	out.print(cells[i].toString());
	out.print(m_columnSeparator);

	if (remainders[i].length() > 0) {
	  wrapped = true;
	}
      }

      out.println();

      final StringBuffer[] otherArray = cells;
      cells = remainders;
      remainders = otherArray;
    }
    while (wrapped);

    out.println();

    final TestStatisticsMap.Iterator iterator =
      m_testStatisticsMap.new Iterator();

    while (iterator.hasNext()) {
      final TestStatisticsMap.Pair pair = iterator.next();

      final Test test = pair.getTest();

      StringBuffer output = formatLine("Test " + test.getNumber(),
				       pair.getStatistics(),
				       expressionViews);

      final String testDescription = test.getDescription();

      if (testDescription != null) {
	output.append(" \"" + testDescription + "\"");
      }

      out.println(output.toString());
    }

    out.println();
    out.println(formatLine("Totals", m_totals, expressionViews));
  }

  private StringBuffer formatLine(String rowLabel,
				  RawStatistics rawStatistics,
				  ExpressionView[] expressionViews)
    throws GrinderException {
    final StringBuffer result = new StringBuffer();

    final StringBuffer cell = new StringBuffer(rowLabel);
    final StringBuffer remainder = new StringBuffer();

    m_rowLabelFormatter.transform(cell, remainder);
    result.append(cell.toString());
    result.append(m_columnSeparator);

    for (int i=0; i<expressionViews.length; ++i) {

      final StatisticExpression expression =
	expressionViews[i].getExpression();

      final String text;

      if (expression.isDouble()) {
	text = m_twoDPFormat.format(expression.getDoubleValue(
				      rawStatistics));
      }
      else {
	text = String.valueOf(expression.getLongValue(rawStatistics));
      }

      cell.replace(0, cell.length(), text);
      m_rowCellFormatter.transform(cell, remainder);
      result.append(cell.toString());
      result.append(m_columnSeparator);
    }

    return result;
  }
}
