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

package net.grinder.engine.agent;

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
    private final String[] m_commandArray;
    private final String m_commandString;
    private int m_exitStatus = 0;
    
    /**
     * The constructor.
     * It starts a new thread that will execute the run method.
     */    
    public LauncherThread(String grinderID, String[] commandArray)
	throws GrinderException
    {
	super(grinderID);

	m_grinderID = grinderID;
	m_commandArray = commandArray;

	final StringBuffer buffer = new StringBuffer(commandArray.length * 10);

	for (int i=0; i<commandArray.length; ++i) {
	    if (i != 0) {
		buffer.append(" ");
	    }

	    buffer.append(commandArray[i]);
	}

	m_commandString = buffer.toString();
    }
  
    /**
     * This method will start a process with the JVM.
     * It redirects standard output and error to disk files.
     */    
    public void run(){

	try{
	    System.out.println("Worker process (" + m_grinderID +
			       ") started with command line: " +
			       m_commandString);

	    final Process process = Runtime.getRuntime().exec(m_commandArray);
      
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
