// The Grinder
// Copyright (C) 2000  Paco Gomez

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

/**
 * This class knows how to start a Java Virtual Machine with parameters.
 * The virtual machine will execute the class GrinderProcess.
 * It redirects the standard ouput and error to disk files.
 *
 * @see net.grinder.Grinder
 * @see net.grinder.engine.Redirector
 * @see net.grinder.engine.GrinderProcess
 * @author <a href="mailto:paco.gomez@terra.com">Paco Gomez</a>.
 * @author Copyright © 2000
 * @version 1.6.0
 */
public class GrinderThread implements java.lang.Runnable{
    
  /**
   * The constructor.
   * It starts a new thread that will execute the run method.
   */    
  public GrinderThread(String execArgs, String fileName){
    _execArgs = execArgs;
    _fileName = fileName;
    Thread t = new Thread(this, _execArgs);
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
      p = Runtime.getRuntime().exec(_execArgs); 
      String s = new java.util.Date().toString() + ": ";
      System.out.println(s + "[" + _execArgs + "]"+ " [Started]");       
      
      in = new BufferedReader(new InputStreamReader(p.getInputStream()));
      er = new BufferedReader(new InputStreamReader(p.getErrorStream()));
      
      OutputStream outFile;
      outFile = new PrintStream(
        new BufferedOutputStream(
        new FileOutputStream(_fileName + ".out", b)));      

      OutputStream errFile;
      errFile = new PrintStream(
        new BufferedOutputStream(
        new FileOutputStream(_fileName + ".err", b)));      
      
      Redirector rOut = new Redirector((java.io.PrintStream)outFile, in);
      Redirector rErr = new Redirector((java.io.PrintStream)errFile, er);

      p.waitFor();
      int completionStatus = p.exitValue(); 
      s = new java.util.Date().toString() + ": ";
      
      System.out.println(s + "[" + _execArgs + "]"+ " [Exit Status: " + completionStatus + "]"); 
               
    }
    catch(Exception _){
      _.printStackTrace();
    }
  }

  protected String _execArgs = "";
  protected String _fileName = "";
}
