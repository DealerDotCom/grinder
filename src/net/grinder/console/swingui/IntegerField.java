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

import java.awt.Toolkit;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.PlainDocument;


/**
 * @author Philip Aston
 * @version $Revision$
 */
class IntegerField extends JTextField
{
    private final static double s_log10 = Math.log(10);
    private final int m_minimumValue;
    private final int m_maximumValue;

    private static int log10(long x)
    {
	final double d = Math.floor(Math.log(x)/s_log10);
	return new Double(d).intValue();
    }

    private static int maxFieldWidth(int minimumValue, int maximumValue)
    {
	final long min = minimumValue < 0 ? 10 * -minimumValue : minimumValue;
	final long max = maximumValue < 0 ? 10 * -maximumValue : maximumValue;
	
	return log10(Math.max(min, max));
    }

    public IntegerField(int minimumValue, int maximumValue)
    {
	super(maxFieldWidth(minimumValue, maximumValue));

	if (minimumValue > maximumValue) {
	    throw new IllegalArgumentException(
		"Minimum value exceeds maximum value");
	}

	m_minimumValue = minimumValue;
	m_maximumValue = maximumValue;

	setDocument(new FormattedDocument());
    }

    public int getValue()
    {
	try {
	    return Integer.parseInt(getText());
	}
	catch (NumberFormatException e) {
	    // Occurs if field is blank or "-".
	    return 0;
	}
    }

    public void setValue(int value)
    {
	if (value < m_minimumValue || value > m_maximumValue) {
	    throw new IllegalArgumentException("Value out of bounds");
	}
	
	setText(Integer.toString(value));
    }

    public class FormattedDocument extends PlainDocument
    {
	private static final Toolkit s_toolkit = Toolkit.getDefaultToolkit();

	public void insertString(int offset, String string,
				 AttributeSet attributeSet)
	    throws BadLocationException
	{
	    final String currentText = super.getText(0, getLength());
	    final String result =
		currentText.substring(0, offset) + string +
		currentText.substring(offset);

	    if (m_minimumValue >= 0 || !result.equals("-")) {
		try {
		    final int x = Integer.parseInt(result);

		    if (x < m_minimumValue || x > m_maximumValue) {
			s_toolkit.beep();
			return;
		    }
		}
		catch (NumberFormatException e) {
		    s_toolkit.beep();
		    return;
		}
	    }

	    super.insertString(offset, string, attributeSet);
	}
    }

    public void addChangeListener(final ChangeListener listener)
    {
	getDocument().addDocumentListener(new DocumentListener() {

		private void notifyChangeListener() 
		{
		    listener.stateChanged(new ChangeEvent(this));
		}
		    
		public void changedUpdate(DocumentEvent e) 
		{
		    notifyChangeListener();
		}

		public void insertUpdate(DocumentEvent e) 
		{
		    notifyChangeListener();
		}

		public void removeUpdate(DocumentEvent e) 
		{
		    notifyChangeListener();
		}
	    });
    }
}
