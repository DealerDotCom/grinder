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
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Collection;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;


/**
 * @author Philip Aston
 * @version $Revision$
 */
public class ConsoleUI
{
    final JFrame m_frame;

    public ConsoleUI(Collection testSet, ActionListener startHandler)
    {
	m_frame = new JFrame("Grinder Console");

        m_frame.addWindowListener(
	    new WindowAdapter() {
		    public void windowClosing(WindowEvent e) {
			System.exit(0);
		    }
		}
	    );
       
        final JPanel topLevelPanel = new JPanel();
	final BoxLayout boxLayout = new BoxLayout(topLevelPanel,
						  BoxLayout.Y_AXIS);

        final JButton startButton = new JButton("Start Grinder");
	startButton.addActionListener(startHandler);
        topLevelPanel.add(startButton);

        final JButton exitButton = new JButton("Exit");

        topLevelPanel.add(exitButton);

        final JPanel testPanel = new JPanel();
	

        final JScrollPane scrollPane = new JScrollPane(testPanel);
	topLevelPanel.add(scrollPane);

        m_frame.getContentPane().add(topLevelPanel);
        m_frame.pack();
        m_frame.setVisible(true);
    }
    
}
