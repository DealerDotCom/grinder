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

import java.io.*;

import net.grinder.util.FilenameFactory;

/**
 * This class knows how to start a Java Virtual Machine with parameters.
 * The virtual machine will execute the class GrinderProcess.
 * It redirects the standard ouput and error to disk files.
 *
 * @see net.grinder.Grinder
 * @see net.grinder.engine.Redirector
 * @see net.grinder.engine.GrinderProcess
 *
 * @author Paco Gomez
 * @author Philip Aston
 * @version $Revision$
 */
public class LauncherThread implements java.lang.Runnable {
    
  /**
   * The constructor.
   * It starts a new thread that will execute the run method.
   */    
  public LauncherThread(String execArgs, FilenameFactory filenameFactory){
    m_execArguments = execArgs;
    m_filenameFactory = filenameFactory;
    Thread t = new Thread(this, m_execArguments);
    t.start(); 
  }
  
  /**
   * This method will start a process with the JVM.
   * It redirects standard output and error to disk files.
   */    
  public void run(){
    
    Process p;
    BufferedReader in;
    BufferedReader er;

    try{
      boolean b = Boolean.getBoolean("grinder.appendLog");
      p = Runtime.getRuntime().exec(m_execArguments); 
      String s = new java.util.Date().toString() + ": ";
      System.out.println(s + "[" + m_execArguments + "]"+ " [Started]");       
      
      in = new BufferedReader(new InputStreamReader(p.getInputStream()));
      er = new BufferedReader(new InputStreamReader(p.getErrorStream()));
      
      final PrintWriter outFile =
	  new PrintWriter(
	      new BufferedOutputStream(
		  new FileOutputStream(
		      m_filenameFactory.createFilename("out"), b)), true);  

      final PrintWriter errFile =
	  new PrintWriter(
	      new BufferedOutputStream(
		  new FileOutputStream(
		      m_filenameFactory.createFilename("error"), b)), true);
      
      Redirector rOut = new Redirector(outFile, in);
      Redirector rErr = new Redirector(errFile, er);

      p.waitFor();
      int completionStatus = p.exitValue(); 
      s = new java.util.Date().toString() + ": ";
      
      System.out.println(s + "[" + m_execArguments + "]" +
			 " [Exit Status: " + completionStatus + "]"); 
               
    }
    catch(Exception e){
      e.printStackTrace();
    }
  }

    private final String m_execArguments;
    private final FilenameFactory m_filenameFactory;
}
