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

package net.grinder.console.swingui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;

import net.grinder.console.common.ConsoleException;


/**
 * A read-only JTable that works in conjunction with an extended
 * TableModel specifies some cell rendering.
 *
 * @author Philip Aston
 * @version $Revision$
 **/
final class Table extends JTable {

  /**
   * Interface for our extended TableModel.
   */
  public interface TableModel extends javax.swing.table.TableModel {
    boolean isBold(int row, int column);
    boolean isRed(int row, int column);
  }

  private final MyCellRenderer m_myCellRenderer;
  private final Color m_defaultForeground;
  private final Font m_boldFont;
  private final Font m_defaultFont;

  public Table(TableModel tableModel) throws ConsoleException {
    super(tableModel);

    setRowSelectionAllowed(false);

    m_myCellRenderer = new MyCellRenderer();

    m_defaultForeground = m_myCellRenderer.getForeground();
    m_defaultFont = m_myCellRenderer.getFont();
    m_boldFont = m_defaultFont.deriveFont(Font.BOLD);
  }

  public final TableCellRenderer getCellRenderer(int row, int column) {
    final TableModel model = (TableModel)getModel();

    final boolean red = model.isRed(row, column);
    final boolean bold = model.isBold(row, column);

    if (red | bold) {
      m_myCellRenderer.setForeground(red ? Colours.RED : m_defaultForeground);
      m_myCellRenderer.setTheFont(bold ? m_boldFont : m_defaultFont);

      return m_myCellRenderer;
    }
    else {
      return super.getCellRenderer(row, column);
    }
  }

  private final class MyCellRenderer extends DefaultTableCellRenderer {
    private Font m_font;

    public final
      Component getTableCellRendererComponent(JTable table,
                                              Object value,
                                              boolean isSelected,
                                              boolean hasFocus,
                                              int row,
                                              int column) {
      final DefaultTableCellRenderer defaultRenderer =
        (DefaultTableCellRenderer)
        super.getTableCellRendererComponent(table, value, isSelected,
                                            hasFocus, row, column);

      // DefaultTableCellRenderer strangely only supports a
      // single font per Table. We override to set font on a per
      // cell basis.
      defaultRenderer.setFont(m_font);

      return defaultRenderer;
    }

    public final void setTheFont(Font f) {
      m_font = f;
    }
  }
}
