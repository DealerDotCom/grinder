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
