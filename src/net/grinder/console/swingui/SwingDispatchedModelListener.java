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

import java.util.Set;
import javax.swing.SwingUtilities;

import net.grinder.console.model.ModelListener;


/**
 * ModelListener Decorator that disptaches the reset() and update()
 * notifications via a Swing thread.
 *
 * @author Philip Aston
 * @version $Revision$
 */
class SwingDispatchedModelListener implements ModelListener
{
    private final ModelListener m_delegate;
    private final Runnable m_updateRunnable;

    public SwingDispatchedModelListener(ModelListener delegate)
    {
	m_delegate = delegate;

	m_updateRunnable =
	    new Runnable() {
		public void run() { m_delegate.update(); }
	    };
    }

    public void reset(final Set newTests)
    {
	SwingUtilities.invokeLater(
	    new Runnable() {
		public void run() { m_delegate.reset(newTests); }
	    }
	    );
    }

    public void update()
    {
	SwingUtilities.invokeLater(m_updateRunnable);
    }
}
