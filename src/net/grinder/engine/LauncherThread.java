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
import java.io.InputStreamReader;
import java.io.PrintWriter;

import net.grinder.common.GrinderException;


/**
 * This class knows how to start a Java Virtual Machine with parameters.
 * The virtual machine will execute the class GrinderProcess.
 * It redirects the standard ouput and error to disk files.
 *
 * @see net.grinder.Grinder
 * @see net.grinder.engine.Redirector
 * @see net.grinder.engine.process.GrinderProcess
 *
 * @author Paco Gomez
 * @author Philip Aston
 * @version $Revision$
 */
public class LauncherThread extends Thread
{
    private final String m_grinderID;
    private final String m_commandLine;
    private int m_exitStatus = 0;
    
    /**
     * The constructor.
     * It starts a new thread that will execute the run method.
     */    
    public LauncherThread(String grinderID, String commandLine,
			  String alternatePropertiesFilename)
	throws GrinderException
    {
	super(grinderID);

	m_grinderID = grinderID;

	final StringBuffer stringBuffer = new StringBuffer(commandLine);
	stringBuffer.append(" ");
	stringBuffer.append(grinderID);

	if (alternatePropertiesFilename != null) {
	    stringBuffer.append(" ");
	    stringBuffer.append(alternatePropertiesFilename);
	}

	m_commandLine = stringBuffer.toString();
    }
  
    /**
     * This method will start a process with the JVM.
     * It redirects standard output and error to disk files.
     */    
    public void run(){

	try{
	    System.out.println("Grinder Process (" + m_grinderID +
			       ") started with command line: " +
			       m_commandLine);

	    final Process process = Runtime.getRuntime().exec(m_commandLine);
      
	    final BufferedReader outputReader =
		new BufferedReader(
		    new InputStreamReader(process.getInputStream()));

	    final BufferedReader errorReader =
		new BufferedReader(
		    new InputStreamReader(process.getErrorStream()));
      
	    final Redirector r1 =
		new Redirector(new PrintWriter(System.out, true),
			       outputReader);

	    final Redirector r2 =
		new Redirector(new PrintWriter(System.err, true), errorReader);

	    process.waitFor();

	    m_exitStatus = process.exitValue();   
	}
	catch(Exception e){
	    e.printStackTrace();
	}
    }

    public int getExitStatus()
    {
	return m_exitStatus;
    }
}
