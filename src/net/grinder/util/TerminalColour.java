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

/**
 * Creates ANSI colour control strings.
 *
 * @author Philip Aston
 * @version $Revision$
 */
public class TerminalColour
{
    public final static String BLACK = controlString("30");
    public final static String RED = controlString("31");
    public final static String GREEN = controlString("32");
    public final static String YELLOW = controlString("33");
    public final static String BLUE = controlString("34");
    public final static String MAGENTA = controlString("35");
    public final static String CYAN = controlString("36");
    public final static String WHITE = controlString("37");
    public final static String BLACK_BACKGROUND = controlString("40");
    public final static String RED_BACKGROUND = controlString("41");
    public final static String GREEN_BACKGROUND = controlString("42");
    public final static String YELLOW_BACKGROUND = controlString("43");
    public final static String BLUE_BACKGROUND = controlString("44");
    public final static String MAGENTA_BACKGROUND = controlString("45");
    public final static String CYAN_BACKGROUND = controlString("46");
    public final static String WHITE_BACKGROUND = controlString("47");

    // Quoting from Thomas E. Dickey's vttest: "Some terminals will
    // reset colors with SGR-0; I've added the 39, 49 codes for those
    // that are ISO compliant. (The black/white codes are for
    // emulators written by people who don't bother reading
    // standards)."
    public final static String NONE = controlString("0;40;37;39;49");

    private static String controlString(String body)
    {
	return (char)0033 + "[" + body + 'm';
    }
    
    public static void main(String[] args){
	System.out.println(BLACK + "black");
	System.out.println(RED + "red");
	System.out.println(BLUE + "blue");
	System.out.println(GREEN_BACKGROUND + "green background");
	System.out.println(MAGENTA + "magenta");
	System.out.println(BLUE_BACKGROUND + "blue background");
	System.out.println(WHITE + "white");
	System.out.println(BLACK_BACKGROUND + "black background");
	System.out.println(GREEN + "green");
	System.out.println(RED_BACKGROUND + "red background");
	System.out.println(YELLOW_BACKGROUND + "yellow background");
	System.out.println(MAGENTA_BACKGROUND + "magenta background");
	System.out.println(YELLOW + "yellow");
	System.out.println(CYAN_BACKGROUND + "cyan background");
	System.out.println(WHITE_BACKGROUND + "white background");
	System.out.println(CYAN + "cyan");
	System.out.println(NONE + "none");

	System.out.println();
    }
} // Colour
