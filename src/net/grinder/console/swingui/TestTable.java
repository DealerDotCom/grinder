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

package net.grinder.console.swingui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;

import net.grinder.console.common.ConsoleException;


/**
 * @author Philip Aston
 * @version $Revision$
 */
public class TestTable extends JTable
{
    private final MyCellRenderer m_myCellRenderer;
    private final Color m_defaultForeground;
    private final Font m_boldFont;
    private final Font m_defaultFont;

    public TestTable(AbstractStatisticsTableModel model)
	throws ConsoleException
    {
	super(model);

	setRowSelectionAllowed(false);

	m_myCellRenderer = new MyCellRenderer();

	m_defaultForeground = m_myCellRenderer.getForeground();
	m_defaultFont = m_myCellRenderer.getFont();
	m_boldFont = m_defaultFont.deriveFont(Font.BOLD);
    }

    public TableCellRenderer getCellRenderer(int row, int column)
    {
	final AbstractStatisticsTableModel model =
	    (AbstractStatisticsTableModel)getModel();

	final boolean red = model.isRed(row, column);
	final boolean bold = model.isBold(row, column);

	if (red | bold) {
	    m_myCellRenderer.setForeground(
		red ? Color.red : m_defaultForeground);

	    m_myCellRenderer.setTheFont(bold ? m_boldFont : m_defaultFont);

	    return m_myCellRenderer;
	}
	else {
	    return super.getCellRenderer(row, column);
	}
    }

    private class MyCellRenderer extends DefaultTableCellRenderer
    {
	private Font m_font;

	public Component getTableCellRendererComponent(JTable table,
						       Object value,
						       boolean isSelected,
						       boolean hasFocus,
						       int row,
						       int column) {
	    final DefaultTableCellRenderer defaultRenderer =
		(DefaultTableCellRenderer)
		super.getTableCellRendererComponent(table, value, isSelected,
						    hasFocus, row, column);
	    defaultRenderer.setFont(m_font);

	    return defaultRenderer;
	}

	public void setTheFont(Font f) 
	{
	    m_font = f;
	}
    }
}
