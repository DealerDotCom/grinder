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

package net.grinder.console.swing;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.Border;

import net.grinder.console.ConsoleException;


/**
 * This class is used graphically show statistics.
 *
 * @author Paco Gomez
 * @author Philip Aston
 * @version $Revision$
 */
class LabelledGraph extends JPanel
{
    final static Border s_blackLine =
	BorderFactory.createLineBorder(Color.black);

    final static Font s_labelFont = new Font("helvetica", Font.PLAIN, 9);
    final static Font s_titleFont = new Font("helvetica", Font.BOLD, 12);
    final static NumberFormat s_decimalFormat = new DecimalFormat("0.00");

    private Graph m_graph;
    private final JLabel m_valueLabel;
    private final JLabel m_maximumLabel;
    private final JLabel m_transactionsLabel;

    public LabelledGraph(String title) throws ConsoleException
    {
	m_graph = new Graph(150, 50, 25, 1000);
	m_graph.setBorder(s_blackLine);
        setPreferredSize(new Dimension(250, 70));

	final JPanel labelPanel = new JPanel();
	labelPanel.setLayout(new BoxLayout(labelPanel, BoxLayout.Y_AXIS));

	final JLabel titleLabel= new JLabel();
	titleLabel.setForeground(Color.black);
	titleLabel.setFont(s_titleFont);
	titleLabel.setText(title);

	m_maximumLabel = createLabel();
	labelPanel.add(m_maximumLabel);
	setMaximumLabel(0);

	m_valueLabel = createLabel();
	labelPanel.add(m_valueLabel);
	setValueLabel(0);

	m_transactionsLabel = createLabel();
	labelPanel.add(m_transactionsLabel);
	setTransactionsLabel(0);

	setLayout(new BorderLayout());

	add(titleLabel, BorderLayout.NORTH);
	add(labelPanel, BorderLayout.WEST);
	add(m_graph, BorderLayout.EAST);
    }

    private JLabel createLabel() 
    {
	final JLabel result = new JLabel();
	result.setForeground(Color.black);
	result.setFont(s_labelFont);
	return result;
    }

    private void setMaximumLabel(double value) 
    {
	m_maximumLabel.setText("Max: " + s_decimalFormat.format(value)
			       + " ms");
    }

    private void setValueLabel(double value) 
    {
	m_valueLabel.setText(s_decimalFormat.format(value)+ " ms");
    }

    private void setTransactionsLabel(long value) 
    {
	m_transactionsLabel.setText(value + " transactions");
    }

    public void add(double averageTransactionTime, long numberOfTransactions)
    {
	m_graph.add(averageTransactionTime);
	setValueLabel(averageTransactionTime);
	setMaximumLabel(m_graph.getMaximum());
	setTransactionsLabel(numberOfTransactions);
    }
}
