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
 * @author Philip Aston
 * @version $Revision$
 */
public class TestSignificantFigureFormat extends TestCase
{
    public static void main(String[] args)
    {
	TestRunner.run(TestSignificantFigureFormat.class);
    }

    public TestSignificantFigureFormat(String name)
    {
	super(name);
    }

    protected void setUp()
    {
    }

    public void testSignificantFigureFormat() throws Exception
    {
	final SignificantFigureFormat f = new SignificantFigureFormat(4);

	assertEquals("1.000", f.format(1d));
	assertEquals("1.000", f.format(1));
	assertEquals("-1.000", f.format(-1d));
	assertEquals("0.1000", f.format(0.1));
	assertEquals("123.0", f.format(123d));
	assertEquals("123.0", f.format(123));
	assertEquals("10.00", f.format(10d));
	assertEquals("10.00", f.format(10));
	assertEquals("0.9900", f.format(.99d));
	assertEquals("0.002320", f.format(.00232));
	assertEquals("12350", f.format(12345d));
	assertEquals("12350", f.format(12345));
	assertEquals("1235", f.format(1234.5));
	assertEquals("1234", f.format(1234));
	assertEquals("12.35", f.format(12.345));
	assertEquals("0.1235", f.format(0.12345));
	// Interestingly .012345 -> 0.01234, but I think this is a
	// floating point thing.
	assertEquals("0.01234", f.format(0.012345));
	assertEquals("0.001235", f.format(0.0012345));
	assertEquals("0.000", f.format(0));
	assertEquals("0.000", f.format(-0));
	assertEquals("0.000", f.format(0.0));
	assertEquals("0.000", f.format(-0.0));
	assertEquals("\u221e", f.format(Double.POSITIVE_INFINITY));
	assertEquals("-\u221e", f.format(Double.NEGATIVE_INFINITY));
	assertEquals("\ufffd", f.format(Double.NaN));
    }
}
