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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import javax.swing.JComponent;

import net.grinder.console.ConsoleException;


/**
 * This class is used graphically show statistics.
 *
 * @author Paco Gomez
 * @author Philip Aston
 * @version $Revision$
 */
class Graph extends JComponent
{
    private final int m_width;
    private final int m_height;
    private final int m_numberOfValues;
    private final double m_expectedMaximum;

    private final double[] m_values;
    private double m_maximum = 0d;
    private int m_cursor = 0;

    private final int[] m_polygonX;
    private final int[] m_polygonY;
    private boolean m_recalculate = true;

    private Color[] m_colors =
    {
	Color.white,
	Color.yellow,
	Color.orange,
	Color.red,
    };

    Graph(int width, int height, int numberOfValues, double expectedMaximum)
	throws ConsoleException
    {
	if (width <= 0) {
	    throw new ConsoleException("Invalid width (" + width + ")");
	}

	if (height <= 0) {
	    throw new ConsoleException("Invalid height (" + height + ")");
	}

	if (numberOfValues <= 0) {
	    throw new ConsoleException(
		"Invalid number of values (" + numberOfValues + ")");
	}

	if (expectedMaximum <= 0) {
	    throw new ConsoleException(
		"Invalid expected maximum values (" + expectedMaximum + ")");
	}

	m_width = width;
	m_height = height;
	m_numberOfValues = numberOfValues;
	m_expectedMaximum = expectedMaximum;

        m_values = new double[m_width];

        setPreferredSize(new Dimension(m_width, m_height));

	// Add 2 for the end points of the polygon.
	m_polygonX = new int[m_numberOfValues + 2];
	m_polygonY = new int[m_numberOfValues + 2];

	m_polygonX[m_numberOfValues + 1] = m_width;

	final double step = (m_width/(double)numberOfValues);

	if (step < 1) {
	    throw new ConsoleException(
		"width=" + width + ", numberOfValues=" + numberOfValues +
		" gives invalid step value " + step);
	}

	for (int i=0; i<m_numberOfValues; i++) {
	    m_polygonX[i+1] = (int)(i * step);
	}

	m_polygonX[0] = 0;
	m_polygonX[m_numberOfValues + 1] = m_polygonX[m_numberOfValues];
    }

    public void add(double newValue)
    {
	m_values[m_cursor] = newValue;

	if (newValue > m_maximum) {
	    m_maximum = newValue;
	}

	if (++m_cursor >= m_numberOfValues) {
	    m_cursor = 0;
	}

	m_recalculate = true;
        repaint();
    }

    private Color calculateColour()
    {
	final int colorIndex =
	    (int)(m_colors.length * (m_maximum/m_expectedMaximum));

	if (colorIndex >= m_colors.length) {
	    return m_colors[m_colors.length - 1];
	}

	return m_colors[colorIndex];
    }

    public void paintComponent(Graphics graphics)
    {    
        super.paintComponent(graphics);

        graphics.setColor(calculateColour());

	final double scale = m_maximum > 0 ? m_height/(double)m_maximum : 0d;

	int cursor = m_cursor;

	if (m_recalculate) {
	    for (int i=0; i<m_numberOfValues; i++) {
		m_polygonY[i+1] =
		    (int)((m_maximum - m_values[cursor]) * scale);

		if (++cursor >= m_numberOfValues) {
		    cursor = 0;
		}
	    }

	    m_polygonY[0] = (int)(m_maximum * scale);
	    m_polygonY[m_numberOfValues + 1] = m_polygonY[0];

	    m_recalculate = false;
	}

	graphics.fillPolygon(m_polygonX, m_polygonY, m_numberOfValues + 2);
    }

    public double getMaximum()
    {
	return m_maximum;
    }
}
