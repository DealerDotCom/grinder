// The Grinder
// Copyright (C) 2000  Paco Gomez
// Copyright (C) 2000  Philip Aston

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

package net.grinder.engine;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.util.Date;

/**
 * This class is used to redirect the standard output and error
 * to disk files. 
 * It reads characters from a BufferedRead and prints them out
 * in a PrintStream.
 * 
 * @author Paco Gomez
 * @author Philip Aston
 * @version $Revision$
 */
class Redirector implements java.lang.Runnable
{
    /**
     * The constructor. It starts a thread that executes 
     * the <tt>run</tt> method.
     * 
     */      
    public Redirector(PrintWriter printWriter, BufferedReader bufferedReader)
    {
	m_printWriter = printWriter;
	m_bufferedReader = bufferedReader;

	final Thread t = new Thread(this, m_printWriter.toString());
	t.start(); 
    }
    
    /**
     * This method reads characters from a BufferedReader and prints
     * them out in a PrintWriter.
     */    
    public void run(){
       
	try{
	    String s;

	    while ((s = m_bufferedReader.readLine()) != null) {
		m_printWriter.println(m_dateFormat.format(new Date()) +
				      ": " + s);
	    }

	    m_bufferedReader.close();
	}
	catch(Exception e){
	    System.err.println(e);
	}
    }

    private final PrintWriter m_printWriter;
    private final BufferedReader m_bufferedReader;
    private final DateFormat m_dateFormat =
	DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM);
}
