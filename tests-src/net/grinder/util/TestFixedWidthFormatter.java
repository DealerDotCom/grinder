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

package net.grinder.util;

import junit.framework.TestCase;
import junit.swingui.TestRunner;
//import junit.textui.TestRunner;


/**
 * Unit test case for <code>FixedWidthFormatter</code>.
 *
 * TO DO - test centre formatting, wrapping and word wrapping.
 *
 * @author Philip Aston
 * @version $Revision$
 */
public class TestFixedWidthFormatter extends TestCase
{
    public static void main(String[] args)
    {
	TestRunner.run(TestFixedWidthFormatter.class);
    }

    public TestFixedWidthFormatter(String name)
    {
	super(name);
    }

    public void testTruncate() throws Exception
    {
	final String text = "They walked in a line, they wallked in a line";
	final String text2 = "Be on my side and I'll be on your side";

	for (int width=1; width<20; ++width) {
	    final FixedWidthFormatter leftFormatter =
		new FixedWidthFormatter(FixedWidthFormatter.ALIGN_LEFT,
					FixedWidthFormatter.FLOW_TRUNCATE,
					width);
	    
	    for (int i=0; i<text.length(); ++i) {
		final StringBuffer buffer =
		    new StringBuffer(text.substring(0, i));
		final StringBuffer remainder = new StringBuffer(text2);
		
		leftFormatter.transform(buffer, remainder);
		
		final String result = buffer.toString();
		
		assertEquals(width, result.length());

		if (i<width) {
		    assertEquals(text.substring(0, i), result.substring(0, i));

		    for (int j=i; j<width; ++j) {
			assertEquals(' ', result.charAt(j));
		    }
		}
		else {
		    assertEquals(text.substring(0, width),
				 result.substring(0, width));
		}
	    
		assertEquals(text2, remainder.toString());
	    }

	    final FixedWidthFormatter rightFormatter =
		new FixedWidthFormatter(FixedWidthFormatter.ALIGN_RIGHT,
					FixedWidthFormatter.FLOW_TRUNCATE,
					width);

	    for (int i=1; i<text.length(); ++i) {
		final StringBuffer buffer =
		    new StringBuffer(text.substring(0, i));
		final StringBuffer remainder = new StringBuffer(text2);

		rightFormatter.transform(buffer, remainder);

		final String result = buffer.toString();

		assertEquals(width, result.length());

		if (i<width) {
		    assertEquals(text.substring(0, i),
				 result.substring(width-i));

		    for (int j=0; j<width-i; ++j) {
			assertEquals(' ', result.charAt(j));
		    }
		}
		else {
		    assertEquals(text.substring(0, width),
				 result.substring(0, width));
		}
	    
		assertEquals(text2, remainder.toString());
	    }
	}
    }
}
