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

package net.grinder;

import java.util.*;
import java.io.*;
import net.grinder.engine.*;

/**
 * This is the entry point of The Grinder.
 * 
 * @author <a href="mailto:paco.gomez@terra.com">Paco Gomez</a>.
 * @author Copyright © 2000
 * @version 1.6.0
 */
public class Grinder extends PropsLoader{

  protected static String _version = "1.6.0";
 
  /**
   * The Grinder entry point.
   *
   */
  public static void main(String args[]){
    Grinder g = new Grinder();
    g.run();
  }
    
  /**
   * Starts as many threads as JVM especified.
   * Each thread is a GrinderThread object.
   *
   */
  protected void run(){
    String propsCL = getGrinderPropertiesFromCommandLine();
    loadProperties();
    String s = new java.util.Date().toString() + ": ";
    System.out.println(s + "Grinder (v" + _version + 
                           ") started with the following properties:");        
    showProperties();
        
    String sArgs = "";
    String sExe = "";
        
    int jvms = 1;
    s = System.getProperty("grinder.jvms");
    if (s != null){
      jvms = Integer.parseInt(s);
    }

    GrinderThread gt[] = new GrinderThread[jvms];
      
    sExe = System.getProperty("grinder.jvm.path", 
                              "c:\\jdk1.1.7B\\bin\\java") + " ";
    for (int i=0; i<jvms; i++){
      sArgs  = "";
      sArgs += System.getProperty("grinder.jvm.args", "") + " ";
      sArgs += System.getProperty("grinder.ms.arg", "-ms");
      sArgs += System.getProperty("grinder.ms", "16") + "m" + " ";
      sArgs += System.getProperty("grinder.mx.arg", "-mx");            
      sArgs += System.getProperty("grinder.mx", "32") + "m" + " ";
      sArgs += "-Dgrinder.jvmId=" + i + " ";
      sArgs += propsCL;
      sArgs += GrinderProcess.class.getName();
            
      s = System.getProperty("grinder.logDir")+ "/grinder_log_" +
          System.getProperty("grinder.hostId") + "_" + i;
            
      gt[i] = new GrinderThread(sExe + sArgs, s);
    }
  }
 
  /**
   * This method loads properties from the command line.
   *
   */ 
  protected String getGrinderPropertiesFromCommandLine(){
    Properties systemProps = System.getProperties();
    String s = "";
    for (Enumeration e = systemProps.propertyNames() ; e.hasMoreElements() ;){
      String pn = (String)e.nextElement();
      if (pn.startsWith("grinder.")){
        s += "-D" + pn + "=" + systemProps.getProperty(pn) + " ";
      }
    }
    return s;        
  }
        
}
