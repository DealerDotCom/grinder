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
import java.awt.Container;
import java.awt.Cursor;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;


/**
 * <code>JDialog</code> that is more useful than that returned by
 * <code>JOptionPane.createDialog()</code>.
 *
 * @author Philip Aston
 * @version $Revision$
 */
public class JOptionPaneDialog extends JDialog
{
    public JOptionPaneDialog(JFrame frame, final JOptionPane optionPane,
			     String title, boolean modal) 
    {
	super(frame, title, modal);

	setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);

	final Container contentPane = getContentPane();
	contentPane.setLayout(new BorderLayout());
	contentPane.add(optionPane, BorderLayout.CENTER);
	pack();
	setLocationRelativeTo(frame);

	addWindowListener(
	    new WindowAdapter() {
		private boolean m_gotFocus = false;

		public void windowClosing(WindowEvent e)
		{
		    optionPane.setValue(null);
		}

		public void windowActivated(WindowEvent e)
		{
		    // Once window gets focus, set initial focus
		    if (!m_gotFocus) {
			optionPane.selectInitialValue();
			m_gotFocus = true;
		    }
		}
	    });

	optionPane.addPropertyChangeListener(
	    new PropertyChangeListener()
	    {
		private boolean m_disable = false;

		public void propertyChange(PropertyChangeEvent e) {
		    if(isVisible() && 
		       e.getSource() == optionPane &&
		       !m_disable &&
		       (e.getPropertyName().equals(JOptionPane.VALUE_PROPERTY)
			||
			e.getPropertyName().equals(
			    JOptionPane.INPUT_VALUE_PROPERTY))) {

			final Cursor oldCursor = getCursor();
			setCursor(
			    Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

			try {
			    if (shouldClose()) {
				setVisible(false);
				dispose();
			    }
			}
			finally {
			    m_disable = true;
			    optionPane.setValue(null);
			    m_disable = false;
			    setCursor(oldCursor);
			}
		    }
		}
	    });

	optionPane.setValue(null);
    }

    protected boolean shouldClose()
    {
	return true;
    }
}
