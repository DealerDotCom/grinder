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
    private final int m_numberOfValues;

    private final double[] m_values;
    private double m_maximum = 0d;
    private int m_cursor = 0;
    private Color m_color;

    private final int[] m_polygonX;
    private final int[] m_polygonY;
    private boolean m_recalculate = true;

    Graph(int numberOfValues)
	throws ConsoleException
    {
	if (numberOfValues <= 0) {
	    throw new ConsoleException(
		"Invalid number of values (" + numberOfValues + ")");
	}

	m_numberOfValues = numberOfValues;

        m_values = new double[numberOfValues];

	// Add 2 for the end points of the polygon.
	m_polygonX = new int[2*m_numberOfValues + 2];
	m_polygonY = new int[2*m_numberOfValues + 2];

	// Set default so we're visable.
	setPreferredSize(new Dimension(200, 100));
    }

    public void add(double newValue)
    {
	m_values[m_cursor] = newValue;

	if (++m_cursor >= m_numberOfValues) {
	    m_cursor = 0;
	}

	m_recalculate = true;
        repaint();
    }

    public void setColor(Color color) 
    {
	m_color = color;
    }

    public void setMaximum(double maximum)
    {
	m_maximum = maximum;
    }

    public void paintComponent(Graphics graphics)
    {    
        super.paintComponent(graphics);

        graphics.setColor(m_color);

	if (m_recalculate) {
	    final double xScale = (getWidth()/(double)m_numberOfValues);

	    for (int i=0; i<=m_numberOfValues; i++) {
		final int x = (int)(i * xScale);
		m_polygonX[2*i] = x;
		m_polygonX[2*i+1] = x;
	    }

	    final double yScale =
		m_maximum > 0 ? getHeight()/(double)m_maximum : 0d;

	    int cursor = m_cursor;

	    for (int i=0; i<m_numberOfValues; i++) {
		int y = (int)((m_maximum - m_values[cursor]) * yScale);

		if (y == 0 && m_maximum != m_values[cursor]) {
		    y = 1;
		}

		m_polygonY[2*i+1] = y;
		m_polygonY[2*i+2] = y;

		if (++cursor >= m_numberOfValues) {
		    cursor = 0;
		}
	    }

	    m_polygonY[0] = (int)(m_maximum * yScale);
	    m_polygonY[2*m_numberOfValues + 1] = m_polygonY[0];

	    m_recalculate = false;
	}

	graphics.fillPolygon(m_polygonX, m_polygonY, 2*m_numberOfValues + 2);
    }
}
