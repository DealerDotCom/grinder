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

import java.text.DecimalFormat;
import java.text.FieldPosition;


/**
 * Java doesn't provide a NumberFormatter which understands
 * significant figures, this is a cheap and cheerful one. Not
 * extensively tested.
 *
 * @author Philip Aston
 * @version $Revision$
 */
public class SignificantFigureFormat extends DecimalFormat
{
    private final static int s_decimalPlaces = 40;

    private final int m_significantFigures;

    public SignificantFigureFormat(int significantFigures)
    {
	// 40 DP, should match value of s_decimalPlaces.
	super("0.0000000000000000000000000000000000000000");
	
	m_significantFigures = significantFigures;
    }

    private static int boundingPowerOfTen(double number)
    {
	if (number == 0d ||
	    Double.isInfinite(number) ||
	    Double.isNaN(number)) {
	    return 1;
	}

	final double abs = Math.abs(number);

	int i = 0;
	double x = 1;

	if (abs < 1) {
	    while (x > abs) {
		x /= 10;
		--i;
	    }

	    return i + 1;
	}
	else {
	    while (!(x > abs)) {
		x *= 10;
		++i;
	    }

	    return i;
	}
    }
    
    /**
     * Almost certainly doesn't set position correctly
     **/
    public StringBuffer format(double number, StringBuffer buffer,
			FieldPosition position) 
    {
	if (Double.isInfinite(number) ||
	    Double.isNaN(number)) {
	    return super.format(number, buffer, position);
	}

	final int shift = boundingPowerOfTen(number) - m_significantFigures;
	final double factor = Math.pow(10, shift);
	
	super.format(factor * Math.round(number/factor), buffer, position);

	final int truncate =
	    shift < 0 ? s_decimalPlaces + shift : s_decimalPlaces + 1;

	buffer.setLength(buffer.length() - truncate);

	return buffer;
    }

    /**
     * Almost certainly doesn't set position correctly
     **/
    public StringBuffer format(long number, StringBuffer buffer,
			       FieldPosition position) 
    {
	return format((double)number, buffer, position);
    }
    

    /**
     * Tests that really should be a JUnit test case.
     * Interestingly .012345 -> 0.01234, but I think this is a floating point thing.
     **/
    public static void main(String[] args)
    {
	final SignificantFigureFormat f = new SignificantFigureFormat(4);

	final double[] numbers = {
	    1,
	    -1,
	    0.1,
	    123,
	    10,
	    .99,
	    0.00232,
	    12345,
	    1234.5,
	    123.45,
	    12.345,
	    1.2345,
	    .12345,
	    .012345,
	    .0012345,
	    -0.0,
	    0d,
	    Double.POSITIVE_INFINITY,
	    Double.NEGATIVE_INFINITY,
	    Double.NaN,
	};

	for (int i=0; i<numbers.length; i++) {
	    final double d = numbers[i];
	    final long l = (long)numbers[i];

	    System.out.println(d + " -> " + f.format(d) + ", " +
	    			       boundingPowerOfTen(d));
	    System.out.println(l + " -> " + f.format(l));
	}
    }
}
