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
import java.text.NumberFormat;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;

import net.grinder.statistics.CumulativeStatistics;
import net.grinder.statistics.IntervalStatistics;


/**
 * This class is used graphically show statistics.
 *
 * @author Paco Gomez
 * @author Philip Aston
 * @version $Revision$
 */
class LabelledGraph extends JPanel
{
    private static double s_peak = 0d;
    private static double s_lastPeak = 0d;

    final static Border s_blackLine =
	BorderFactory.createLineBorder(Color.black);

    private Color[] m_colors =
    {
	new Color(0xFF, 0xFF, 0x00),
	new Color(0xFF, 0xF0, 0x00),
	new Color(0xFF, 0xE0, 0x00),
	new Color(0xFF, 0xD0, 0x00),
	new Color(0xFF, 0xC0, 0x00),
	new Color(0xFF, 0xB0, 0x00),
	new Color(0xFF, 0xA0, 0x00),
	new Color(0xFF, 0x90, 0x00),
	new Color(0xFF, 0x80, 0x00),
	new Color(0xFF, 0x70, 0x00),
	new Color(0xFF, 0x60, 0x00),
	new Color(0xFF, 0x50, 0x00),
	new Color(0xFF, 0x40, 0x00),
	new Color(0xFF, 0x30, 0x00),
	new Color(0xFF, 0x20, 0x00),
	new Color(0xFF, 0x10, 0x00),
	new Color(0xFF, 0x00, 0x00),
    };

    private Color m_color;

    private Graph m_graph;

    private static class Label extends JLabel
    {
	private static final Font s_plainFont;
	private static final Font s_boldFont;
	private static final Color s_defaultForeground;

	static 
	{
	    final JLabel label = new JLabel();
	    final Font defaultFont = label.getFont();
	    final float size = defaultFont.getSize2D() - 1;

	    s_plainFont = defaultFont.deriveFont(Font.PLAIN, size);
	    s_boldFont = defaultFont.deriveFont(Font.BOLD, size);
	    s_defaultForeground = label.getForeground();
	}

	private final String m_suffix;
	private final String m_unit;
	private final String m_units;

	public Label(String unit, String units, String suffix)
	{
	    m_suffix = " " + suffix;
	    m_unit = " " + unit;
	    m_units = " " + units;
	    setFont(s_plainFont);
	    set(0);
	}

	public void set(long value)
	{
	    super.setText(Long.toString(value) +
			  (value == 1 ? m_unit : m_units) +
			  m_suffix);
	}

	public void set(double value, NumberFormat numberFormat)
	{
	    super.setText(numberFormat.format(value) +
			  m_units +
			  m_suffix);
	}

	public void set(String value)
	{
	    super.setText(value + m_units + m_suffix);
	}

	/**
	 * Make all labels the same width.
	 * Pack more tightly vertically.
	 **/
	public Dimension getPreferredSize()
	{
	    final Dimension d = super.getPreferredSize();
	    d.width = 120;
	    d.height -= 2;
	    return d;
	}

	public Dimension getMaximumSize()
	{
	    return getPreferredSize();
	}

	public void setHighlight(boolean highlight)
	{
	    if (highlight) {
		setForeground(Color.red);
		setFont(s_boldFont);
	    }
	    else {
		setForeground(s_defaultForeground);
		setFont(s_plainFont);
	    }
	}
    }

    private final Label m_averageTimeLabel;
    private final Label m_averageTPSLabel;
    private final Label m_peakTPSLabel;
    private final Label m_transactionsLabel;
    private final Label m_errorsLabel;
    private final Dimension m_preferredSize = new Dimension(300, 130);

    public LabelledGraph(String title, Resources resources)
    {
	this(title, resources, null);
    }

    public LabelledGraph(String title, Resources resources, Color color)
    {
	final String msUnit = resources.getString("ms.unit");
	final String msUnits = resources.getString("ms.units");
	final String tpsUnits = resources.getString("tps.units");
	final String transactionUnit = resources.getString("transaction.unit");
	final String transactionUnits =
	    resources.getString("transaction.units");
	final String errorUnit = resources.getString("error.unit");
	final String errorUnits = resources.getString("error.units");

	final String averageSuffix =
	    resources.getString("graph.averageSuffix.label");
	final String peakSuffix =
	    resources.getString("graph.peakSuffix.label");

	m_averageTimeLabel = new Label(msUnit, msUnits, averageSuffix);
	m_averageTPSLabel = new Label(tpsUnits, tpsUnits, averageSuffix);
	m_peakTPSLabel = new Label(tpsUnits, tpsUnits, peakSuffix);
	m_transactionsLabel = new Label(transactionUnit, transactionUnits, "");
	m_errorsLabel = new Label(errorUnit, errorUnits, "");

	m_color = color;
	m_graph = new Graph(25);
	m_graph.setPreferredSize(null); // We are the master now.
	m_graph.setBorder(s_blackLine);

	final JPanel labelPanel = new JPanel();
	labelPanel.setLayout(new BoxLayout(labelPanel, BoxLayout.Y_AXIS));
	labelPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 0, 0));

	final JLabel titleLabel= new JLabel();
	titleLabel.setText(title);
	titleLabel.setForeground(Color.black);

	labelPanel.add(m_averageTimeLabel);
	labelPanel.add(m_averageTPSLabel);
	labelPanel.add(m_peakTPSLabel);
	labelPanel.add(m_transactionsLabel);
	labelPanel.add(m_errorsLabel);

	setLayout(new BorderLayout());

	add(titleLabel, BorderLayout.NORTH);
	add(labelPanel, BorderLayout.WEST);
	add(m_graph, BorderLayout.CENTER);

	final Border border = getBorder();
	final Border margin = new EmptyBorder(10, 10, 10, 10);
	setBorder(new CompoundBorder(border, margin));
    }

    public Dimension getPreferredSize()
    {
	return m_preferredSize;
    }

    public void add(IntervalStatistics intervalStatistics,
		    CumulativeStatistics cumulativeStatistics,
		    NumberFormat numberFormat)
    {
	final double averageTime =
	    cumulativeStatistics.getAverageTransactionTime();
	final long errors = cumulativeStatistics.getErrors();
	final double peakTPS = cumulativeStatistics.getPeakTPS();

	m_graph.setMaximum(peakTPS);
	m_graph.add(intervalStatistics.getTPS());
	m_graph.setColor(calculateColour(averageTime));

	if (!Double.isNaN(averageTime)) {
	    m_averageTimeLabel.set(averageTime, numberFormat);
	}
	else {
	    m_averageTimeLabel.set("----");
	}

	m_averageTPSLabel.set(cumulativeStatistics.getTPS(), numberFormat);
	m_peakTPSLabel.set(peakTPS, numberFormat);

	m_transactionsLabel.set(cumulativeStatistics.getTransactions());

	m_errorsLabel.set(errors);
	m_errorsLabel.setHighlight(errors > 0);
    }

    private Color calculateColour(double time)
    {
	if (m_color != null) {
	    return m_color;
	}
	else {
	    if (time > s_peak) { // Not worth the cost of synchornization.
		s_peak = time;
	    }

	    final int colorIndex = (int)(m_colors.length * (time/s_lastPeak));

	    if (colorIndex >= m_colors.length) {
		return m_colors[m_colors.length - 1];
	    }

	    return m_colors[colorIndex];
	}
    }

    public static void resetPeak()
    {
	s_lastPeak = s_peak;
	s_peak = 0;
    }
}
