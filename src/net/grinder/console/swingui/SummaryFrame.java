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

import java.awt.Dimension;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTable;

import net.grinder.console.ConsoleException;
import net.grinder.console.model.Model;
import net.grinder.plugininterface.Test;
import net.grinder.statistics.Statistics;
import net.grinder.statistics.StatisticsTable;


/**
 * @author Philip Aston
 * @version $Revision$
 */
public class SummaryFrame extends JFrame
{
    private final StatisticsTableModel m_tableModel;

    public SummaryFrame(Model model, String title)
    {
	super(title);

	m_tableModel = new StatisticsTableModel(model);

	final JTable table = new JTable(m_tableModel);
	table.setPreferredScrollableViewportSize(new Dimension(800, 400));
        final JScrollPane scrollPane = new JScrollPane(table);

	getContentPane().add(scrollPane);
	pack();
    }

    public void displaySummary()
    {
	/* Fix to refresh
	*/
	show();
    }
}
