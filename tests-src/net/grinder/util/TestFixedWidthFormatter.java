// Copyright (C) 2000 Paco Gomez
// Copyright (C) 2000, 2001, 2002 Philip Aston
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
