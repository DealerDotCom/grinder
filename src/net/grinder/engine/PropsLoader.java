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

import java.util.*;
import java.io.*;

/**
 * This class knows how to load properties from a property file.
 * 
 * @author Paco Gomez
 * @version $Revision$
 * @deprecated Use GrinderProperties instead.
 */
public class PropsLoader{
 
  /**
   * This method loads and shows the properties.
   *
   */ 
  protected void run(){
    loadProperties();
  }
   
  /**
   * This method loads the properties from a properties file.
   * The properties file must be located in in a directory in the classpath.
   *
   */      
  protected void loadProperties(){
    
    Properties defProps;
    defProps = getDefaultProperties();
    
    try{
      Properties systemProps = System.getProperties();
      	            
      InputStream is = ClassLoader.getSystemResourceAsStream(_propertyFile);
  
      Properties p = new Properties(defProps);
      if (is != null){
        p.load(is);
      }               
      else{
        System.err.println("property file not found: " + _propertyFile);
        System.err.println("using default properties");
      }

      for (Enumeration e = systemProps.propertyNames() ; e.hasMoreElements() ;){
        String pn = (String)e.nextElement();
        p.put(pn, systemProps.getProperty(pn));
      }
                        
      System.setProperties(p);
          
    }
    catch(Exception e){
      System.err.println(e);
    }
  }
    
  /**
   * This method returns the default values for the properties.
   *
   */  
  protected Properties getDefaultProperties(){
    Properties p = new Properties();
    p.put("grinder.hostId", "0");
    p.put("grinder.cycleClass", "net.grinder.plugin.simple.SimpleBmk");
    p.put("grinder.cycleMethods", "init,methodA,methodB,methodC,end");
    p.put("grinder.cycleParams", "[paramA]a,[paramB]500,[paramC]10.2");
    p.put("grinder.jvm.path", "c:\\jdk1.2.2\\bin\\java");        
    p.put("grinder.jvm.args", "");
    p.put("grinder.processes", "1");
    p.put("grinder.threads", "2");
    p.put("grinder.times", "3");
    p.put("grinder.initialWait", "false");
    p.put("grinder.multicastAddress", "228.1.1.1");
    p.put("grinder.multicastPort", "1234");
    p.put("grinder.reportToConsole", "false");
    p.put("grinder.console.multicastAddress", "228.1.1.2");
    p.put("grinder.console.multicastPort", "1234");
    p.put("grinder.logDirectory", ".");
    p.put("grinder.appendLog", "false");
    p.put("grinder.fileStats", "true");       
    p.put("grinder.sleepMillis", "0"); 
    p.put("grinder.initialSleepTimes", "0");
       
    return p;
        
  }

  /**
   * The name of the properties file.
   * The properties file must be located in a directory in the classpath.   
   *
   */        
  protected String _propertyFile = "grinder.properties";
    
}
