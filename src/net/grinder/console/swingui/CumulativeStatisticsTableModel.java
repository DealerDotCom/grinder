// Copyright (C) 2001, 2002, 2003, 2004 Philip Aston
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

import net.grinder.console.common.ConsoleException;
import net.grinder.console.model.Model;
import net.grinder.statistics.StatisticsSet;
import net.grinder.statistics.StatisticsView;
import net.grinder.statistics.TestStatisticsQueries;


/**
 * Table model for cumulative statistics table.
 *
 * @author Philip Aston
 * @version $Revision$
 */
final class CumulativeStatisticsTableModel
  extends DynamicStatisticsTableModel {

  private boolean m_includeTotals = true;
  private final String m_totalString;

  public CumulativeStatisticsTableModel(Model model) throws ConsoleException {

    super(model);

    m_totalString = model.getResources().getString("table.total.label");
  }

  /**
   * {@link net.grinder.console.model.ModelListener} interface. New
   * <code>StatisticsView</code>s have been added.
   **/
  public synchronized void newStatisticsViews(
    StatisticsView intervalStatisticsView,
    StatisticsView cumulativeStatisticsView) {
    addColumns(cumulativeStatisticsView);
  }

  public synchronized void resetTestsAndStatisticsViews() {
    super.resetTestsAndStatisticsViews();
    addColumns(getModel().getCumulativeStatisticsView());
  }

  protected StatisticsSet getStatistics(int row) {
    return getLastModelTestIndex().getCumulativeStatistics(row);
  }

  public int getRowCount() {
    return super.getRowCount() + (m_includeTotals ? 1 : 0);
  }

  public synchronized Object getValueAt(int row, int column) {

    if (row < getLastModelTestIndex().getNumberOfTests()) {
      return super.getValueAt(row, column);
    }
    else {
      switch (column) {
      case 0:
        return m_totalString;

      case 1:
        return "";

      default:
        return getDynamicField(
          getModel().getTotalCumulativeStatistics(), column - 2);
      }
    }
  }

  public boolean isBold(int row, int column) {

    if (row < getLastModelTestIndex().getNumberOfTests()) {
      return super.isBold(row, column);
    }
    else {
      return true;
    }
  }

  public boolean isRed(int row, int column) {

    if (row < getLastModelTestIndex().getNumberOfTests()) {
      return super.isRed(row, column);
    }
    else {
      return
        column == 3 &&
        TestStatisticsQueries.getInstance().getNumberOfErrors(
            getModel().getTotalCumulativeStatistics()) > 0;
    }
  }

  public synchronized void write(Writer writer, String columnDelimiter,
                                 String lineDelimeter)
    throws IOException {

    try {
      m_includeTotals = false;

      super.write(writer, columnDelimiter, lineDelimeter);
    }
    finally {
      m_includeTotals = true;
    }
  }
}
