// Copyright (C) 2003 Philip Aston
// All rights reserved.
//
// This file is part of The Grinder software distribution. Refer to
// the file LICENSE which is part of The Grinder distribution for
// licensing details. The Grinder distribution is available on the
// Internet at http://grinder.sourceforge.net/
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
// "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
// LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
// FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
// REGENTS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
// INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
// (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
// SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
// HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
// STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
// ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
// OF THE POSSIBILITY OF SUCH DAMAGE.

package net.grinder.console.swingui;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.HashSet;
import java.util.Set;
import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.ImageIcon;


/**
 * Customised Action.
 *
 * @author Philip Aston
 * @version $Revision$
 */
abstract class CustomAction extends AbstractAction {

  /** Property name used to indicate a set action event. */
  protected static final String SET_ACTION_PROPERTY = "setAction";

  private final String m_key;
  private final Set m_propertyChangeListenersByButton = new HashSet();

  public CustomAction(Resources resources, String key) {
    this(resources, key, false);
  }

  public CustomAction(Resources resources, String key,
		      boolean isDialogAction) {
    super();

    m_key = key;

    final String label = resources.getString(m_key + ".label", false);

    if (label != null) {
      if (isDialogAction) {
	putValue(Action.NAME, label + "...");
      }
      else {
	putValue(Action.NAME, label);
      }
    }

    final String tip = resources.getString(m_key + ".tip", false);

    if (tip != null) {
      putValue(Action.SHORT_DESCRIPTION, tip);
    }

    final ImageIcon imageIcon = resources.getImageIcon(m_key + ".image");

    if (imageIcon != null) {
      putValue(Action.SMALL_ICON, imageIcon);
    }

    final ImageIcon rolloverImageIcon =
      resources.getImageIcon(m_key + ".rollover-image");

    if (rolloverImageIcon != null) {
      putValue(CustomJButton.ROLLOVER_ICON, rolloverImageIcon);
    }
  }

  public final String getKey() {
    return m_key;
  }

  public final void registerButton(final AbstractButton button) {
    if (!m_propertyChangeListenersByButton.contains(button)) {
      addPropertyChangeListener(
	new PropertyChangeListener() {
	  public void propertyChange(PropertyChangeEvent e) {
	    if (e.getPropertyName().equals(SET_ACTION_PROPERTY)) {

	      final CustomAction newAction = (CustomAction)e.getNewValue();

	      button.setAction(newAction);
	      newAction.registerButton(button);
	    }
	  }
	}
	);

      m_propertyChangeListenersByButton.add(button);
    }
  }
}
