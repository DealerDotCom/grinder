// Copyright (C) 2001, 2002, 2003 Philip Aston
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

package net.grinder.console.swingui;

import java.io.IOException;
import java.io.Writer;
import java.util.Set;
import javax.swing.table.AbstractTableModel;

import net.grinder.console.common.ConsoleException;
import net.grinder.console.model.Model;
import net.grinder.console.model.ModelListener;
import net.grinder.console.model.ModelTestIndex;
import net.grinder.statistics.ExpressionView;
import net.grinder.statistics.StatisticExpression;
import net.grinder.statistics.StatisticsView;
import net.grinder.statistics.TestStatistics;


/**
 * @author Philip Aston
 * @version $Revision$
 */
abstract class DynamicStatisticsTableModel
  extends AbstractTableModel implements ModelListener, Table.TableModel {

  private final Model m_model;
  private final Resources m_resources;

  private ModelTestIndex m_lastModelTestIndex = new ModelTestIndex();

  private final String m_testString;
  private final String m_testColumnString;
  private final String m_testDescriptionColumnString;

  private StatisticsView m_statisticsView = new StatisticsView();
  private ExpressionView[] m_columnViews = new ExpressionView[0];
  private String[] m_columnLabels;

  protected DynamicStatisticsTableModel(Model model, Resources resources,
					boolean setColumns)
    throws ConsoleException {

    m_model = model;
    m_resources = resources;

    m_testString = resources.getString("table.test.label") + " ";
    m_testColumnString = resources.getString("table.testColumn.label");
    m_testDescriptionColumnString =
      resources.getString("table.descriptionColumn.label");

    m_model.addModelListener(new SwingDispatchedModelListener(this));
  }

  protected abstract TestStatistics getStatistics(int row);

  protected final Model getModel() {
    return m_model;
  }

  protected final ModelTestIndex getLastModelTestIndex() {
    return m_lastModelTestIndex;
  }

  public synchronized void addColumns(StatisticsView statisticsView) {
    m_statisticsView.add(statisticsView);

    final int originalNumberOfColumns = m_columnViews.length;

    final ExpressionView[] newViews =
      m_statisticsView.getExpressionViews();

    if (newViews.length != originalNumberOfColumns) {
      m_columnViews = newViews;

      m_columnLabels = new String[m_columnViews.length];

      for (int i=0; i<m_columnLabels.length; ++i) {
	final String resource =
	  m_resources.getString(
	    m_columnViews[i].getDisplayNameResourceKey(), false);

	m_columnLabels[i] =
	  resource != null ?
	  resource : m_columnViews[i].getDisplayName();
      }

      fireTableStructureChanged();
    }
  }

  public final synchronized void newTests(Set newTests,
					  ModelTestIndex modelTestIndex) {

    m_lastModelTestIndex = modelTestIndex;

    // We've been reset, number of rows may have changed.
    fireTableDataChanged();
  }

  public final synchronized void update() {
    fireTableRowsUpdated(0, getRowCount());
  }

  /**
   * {@link net.grinder.console.model.ModelListener} interface. New
   * <code>StatisticsView</code>s have been added. 
   **/
  public final synchronized void newStatisticsViews(
    StatisticsView intervalStatisticsView,
    StatisticsView cumulativeStatisticsView) {
    addColumns(cumulativeStatisticsView);
  }

  public final synchronized int getColumnCount() {
    return 2 + m_columnLabels.length;
  }

  public final synchronized String getColumnName(int column) {
    switch (column) {
    case 0:
      return m_testColumnString;

    case 1:
      return m_testDescriptionColumnString;

    default:
      return m_columnLabels[column - 2];
    }
  }

  public synchronized int getRowCount() {
    return m_lastModelTestIndex.getNumberOfTests();
  }

  public synchronized Object getValueAt(int row, int column) {
    if (column == 0) {
      return m_testString + m_lastModelTestIndex.getTest(row).getNumber();
    }
    else if (column == 1) {
      return m_lastModelTestIndex.getTest(row).getDescription();
    }
    else {
      return getDynamicField(getStatistics(row), column - 2);
    }
  }

  protected synchronized String getDynamicField(TestStatistics statistics,
						int dynamicColumn) {
    if (dynamicColumn < m_columnViews.length) {
      final StatisticExpression expression =
	m_columnViews[dynamicColumn].getExpression();

      if (expression.isDouble()) {
	final double value = expression.getDoubleValue(statistics);

	if (Double.isNaN(value)) {
	  return "";
	}
	else {
	  return m_model.getNumberFormat().format(value);
	}
      }
      else {
	return String.valueOf(expression.getLongValue(statistics));
      }
    }
    else {
      return "?";
    }
  }

  public boolean isBold(int row, int column) {
    return isRed(row, column);
  }

  public boolean isRed(int row, int column) {
    return column == 3 && getStatistics(row).getErrors() > 0;
  }

  public synchronized void write(Writer writer, String columnDelimiter,
				 String lineDelimeter)
    throws IOException {
    final int numberOfRows = getRowCount();
    final int numberOfColumns = getColumnCount();

    writer.write(m_testColumnString);
    writer.write(columnDelimiter);
    writer.write(m_testDescriptionColumnString);
    writer.write(columnDelimiter);
	
    for (int dynamicColumn=0; dynamicColumn<numberOfColumns-2;
	 dynamicColumn++) {
      writer.write(m_columnLabels[dynamicColumn]);
      writer.write(columnDelimiter);
    }

    writer.write(lineDelimeter);

    for (int row=0; row<numberOfRows; row++) {
      for (int column=0; column<numberOfColumns; column++) {
	final Object o = getValueAt(row, column);
	writer.write(o != null ? o.toString() : "");
	writer.write(columnDelimiter);
      }

      writer.write(lineDelimeter);
    }
  }
}
