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
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;

import net.grinder.console.ConsoleException;
import net.grinder.statistics.Statistics;


/**
 * This class is used graphically show statistics.
 *
 * @author Paco Gomez
 * @author Philip Aston
 * @version $Revision$
 */
class LabelledGraph extends JPanel
{
    private static double m_peak = 0;

    final static Border s_blackLine =
	BorderFactory.createLineBorder(Color.black);

    private Color[] m_colors =
    {
	new Color(0xFF, 0x00, 0x00),
	new Color(0xFF, 0x20, 0x00),
	new Color(0xFF, 0x40, 0x00),
	new Color(0xFF, 0x60, 0x00),
	new Color(0xFF, 0x80, 0x00),
	new Color(0xFF, 0xA0, 0x00),
	new Color(0xFF, 0xC0, 0x00),
	new Color(0xFF, 0xE0, 0x00),
	new Color(0xFF, 0xFF, 0x00),
    };

    private Color m_color;

    private Graph m_graph;

    private static class Label extends JLabel
    {
	private final static Font s_labelFont =
	    new Font("helvetica", Font.PLAIN, 9);

	private final static NumberFormat s_decimalFormat =
	    new DecimalFormat("0.00");

	private final String m_prefix;
	private final String m_suffix;

	private Label(String prefix, String suffix)
	{
	    m_prefix = prefix;
	    m_suffix = suffix;
	    setFont(s_labelFont);
	    setPreferredSize(new Dimension(100, 14));
	    set(0);
	}

	private void set(long value)
	{
	    super.setText(m_prefix + value + m_suffix);
	}

	void set(double value)
	{
	    super.setText(m_prefix + s_decimalFormat.format(value) + m_suffix);
	}

	private void set(String value)
	{
	    super.setText(m_prefix + value + m_suffix);
	}
    }

    private final Label m_tpsLabel = new Label(" ", " tps");
    private final Label m_averageTPSLabel = new Label("Avg: ", " tps");
    private final Label m_peakTPSLabel = new Label("Peak: ", " tps");
    private final Label m_averageTimeLabel = new Label("", " ms (avg)");
    private final Label m_transactionsLabel = new Label("", " transactions");
    private final Label m_errorsLabel = new Label("", " errors");
    private final Label m_abortionsLabel = new Label("", " abortions");

    public LabelledGraph(String title)
	throws ConsoleException
    {
	this(title, null);
    }

    public LabelledGraph(String title, Color color)
	throws ConsoleException
    {
	m_color = color;
	m_graph = new Graph(25);
	m_graph.setPreferredSize(new Dimension(150, 90));
	m_graph.setBorder(s_blackLine);
        setPreferredSize(new Dimension(250, 120));

	final JPanel labelPanel = new JPanel();
	labelPanel.setLayout(new BoxLayout(labelPanel, BoxLayout.Y_AXIS));

	final JLabel titleLabel= new JLabel();
	titleLabel.setText(title);
	titleLabel.setForeground(Color.black);

	labelPanel.add(m_tpsLabel);
	labelPanel.add(m_averageTPSLabel);
	labelPanel.add(m_peakTPSLabel);
	labelPanel.add(m_averageTimeLabel);
	labelPanel.add(m_transactionsLabel);
	labelPanel.add(m_errorsLabel);
	labelPanel.add(m_abortionsLabel);

	setLayout(new BorderLayout());

	add(titleLabel, BorderLayout.NORTH);
	add(labelPanel, BorderLayout.WEST);
	add(m_graph);

	final Border border = getBorder();
	final Border margin = new EmptyBorder(10, 10, 10, 10);
	setBorder(new CompoundBorder(border, margin));
    }

    private JLabel createLabel() 
    {
	final JLabel result = new JLabel();
	return result;
    }

    public void add(double tps, double averageTPS, double peakTPS,
		    Statistics total)
    {
	m_graph.setMaximum(peakTPS);
	m_graph.setColor(calculateColour(tps));
	m_graph.add(tps);

	final double averageTime = total.getAverageTransactionTime();

	m_tpsLabel.set(tps);
	m_averageTPSLabel.set(averageTPS);
	m_peakTPSLabel.set(peakTPS);

	if (!Double.isNaN(averageTime)) {
	    m_averageTimeLabel.set(averageTime);
	}
	else {
	    m_averageTimeLabel.set("----");
	}

	m_transactionsLabel.set(total.getTransactions());
	m_errorsLabel.set(total.getErrors());
	m_abortionsLabel.set(total.getAbortions());
    }

    private Color calculateColour(double tps)
    {
	if (m_color != null) {
	    return m_color;
	}
	else {
	    if (tps > m_peak) { // Not worth the cost of synchornization.
		m_peak = tps;
	    }

	    final int colorIndex = (int)(m_colors.length * (tps/m_peak));

	    if (colorIndex >= m_colors.length) {
		return m_colors[m_colors.length - 1];
	    }

	    return m_colors[colorIndex];
	}
    }

    public static void resetPeak()
    {
	m_peak = 0d;
    }
}
