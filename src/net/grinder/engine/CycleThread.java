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
import java.lang.reflect.*;
import java.net.*;
import java.io.*;

/**
 * The class executed by each thread.
 * The total number of threads per JVM is specified in the property "grinder.threads".
 * This class is responsible for instantiating an object of the class specified in the
 * property "grinder.cycleClass". It also invokes the methods specified in the
 * property "grinder.cycleMethods". It records the time spent in each method invocation.
 * The elapsed time is printed out in the "dat" file and stored in the shared space 
 * of the GrinderProcess object, shared by all threads.
 * 
 * @author <a href="mailto:paco.gomez@terra.com">Paco Gomez</a>.
 * @author Copyright © 2000
 * @version 1.6.0
 * @see net.grinder.engine.GrinderProcess
 * @see net.grinder.engine.GrinderContext
 * @see net.grinder.engine.Chronus
 */
class CycleThread implements java.lang.Runnable{
    
  /**
   * The constructor.
   * It starts a new thread that will execute the run method.   
   * 
   */        
  public CycleThread(GrinderProcess gp, int threadId){
    _gp = gp;
    _threadId = threadId;
    Thread t = new Thread(this, "threadId=" + _threadId);
    t.start(); 
  }
    
  /**
   * The application's main loop.
   * 
   */     
  public void run(){
    Chronus ch = new Chronus();
    try{
      Class c = Class.forName(System.getProperty("grinder.cycleClass"));               
	        
      //Object grinderPlugin;
      GrinderPlugin grinderPlugin;
      //float a[];
      long t[];
      long errors[];
            
      //a = new float[_gp._methods.size()];
      t = new long[_gp._methods.size()];           
      errors = new long[_gp._methods.size()];
      for (int mi=0; mi<_gp._methods.size(); mi++){
        //a[mi] = 0;
        t[mi] = 0;
        errors[mi] = 0;
      }
            
      grinderPlugin = (GrinderPlugin)c.newInstance();
            
      Object obj[]=new Object[1];
      GrinderContext gCtx = new GrinderContext(_gp._params);
      Properties p = gCtx.getProperties();
      p.put("grinder.threadId", "" + _threadId);
      gCtx.setProperties(p);
      gCtx.setStopIteration(false);
      obj[0] = gCtx;

      int times = Integer.getInteger("grinder.times").intValue();
      int sleepMillis = Integer.getInteger("grinder.sleepMillis").intValue();
      String hostId = System.getProperty("grinder.hostId");
      String jvmId = System.getProperty("grinder.jvmId");

      synchronized(_gp){
        _gp._aliveThreads++;
        try{_gp.wait();}catch(Exception e){System.err.println(e);}
      }    
            
      //random initial wait
      if (sleepMillis != 0){
        int initialSleepTimes = Integer.getInteger("grinder.initialSleepTimes", 4).intValue();
        int alea = (int)((sleepMillis*initialSleepTimes)*Math.random());
        System.out.println("<I> <CycleThread-run> Random initial wait (millis)="+
                           alea+" current=["+
                           hostId + "," + 
                           jvmId + "," +
                           _threadId +
                           "]");
        Thread.sleep(alea);
      }               
            
      try{
        boolean bStop = false;
        _gp._m[0].invoke(grinderPlugin, obj);
        for (int i=0; i<times; i++){           
          for(int j=0; j<(_gp._m.length-1); j++){
            if(j==0){
              //_gp._m[j].invoke(grinderPlugin, obj);
            }
            else{                  
              ch.begin();
              _gp._m[j].invoke(grinderPlugin, null);
              ch.end();
              //
              //log, notify
              //
              if (gCtx.getErrorIteration()){
                errors[j]++;
                _gp.addError(j, 1);                             
                System.err.println("Grinder: the plug-in reports an error. This method is logged as an error...(current=["+
                                   hostId + "," + 
                                   jvmId + "," +
                                   _threadId + "," +
                                   i + "," +
                                   _gp._m[j].getName() + "])"
                                   );                                                    
                gCtx.setErrorIteration(false);
              }                	                                    
              else{                  
                t[j] += ch.getElapsed();                        
                //a[j] += t[j];
                //a[j] /= 2;
                _gp.printlnStats(
                    hostId + "," + 
                    jvmId + "," +
                    _threadId + "," +
                    i + "," +
                    _gp._m[j].getName() + "," +
                    ch.getElapsed()
                    );                                
                _gp.add(j, ch.getElapsed(), 1);       
              }
                    
              //sleep  
              if (sleepMillis != 0){
                Thread.sleep(sleepMillis);
              }           
            }
            if (gCtx.getStopIteration()){
              bStop = true;
              System.err.println("Grinder: the plug-in doesn't want to continue. Exiting...(current=["+
                                 hostId + "," + 
                                 jvmId + "," +
                                 _threadId + "," +
                                 i + "," +
                                 _gp._m[j].getName() + "])"
                                 );                                                    
              break;                	
            }                	                  
            if (gCtx.getSkipIteration()){
              System.err.println("Grinder: the plug-in wants to skip this iteration. Jumping to the next iteration...(current=["+
                                 hostId + "," + 
                                 jvmId + "," +
                                 _threadId + "," +
                                 i + "," +
                                 _gp._m[j].getName() + "])"
                                 );                                                    
              gCtx.setSkipIteration(false);
              break;                	
            }                	                                    
          }
          if (bStop){
            break; 
          }	
        }
        if (! gCtx.getStopIteration()){
          _gp._m[_gp._m.length-1].invoke(grinderPlugin, null);              
        }
      }
      catch(java.lang.reflect.InvocationTargetException e){
        //do something more
        e.printStackTrace(System.err);
        System.err.println(e);
      }             
      catch(Exception e){
        e.printStackTrace(System.err);
        System.err.println(e);
      }
    }
    catch(ClassNotFoundException e){
      e.printStackTrace(System.err);
      System.err.println(e);
    }
    catch(InstantiationException e){
      e.printStackTrace(System.err);
      System.err.println(e);
    }            
    catch(Exception e){
      e.printStackTrace(System.err);
      System.err.println(e);
    }
    finally{
      synchronized(_gp){
        _gp._aliveThreads--;
      }  
    }                        
  }

  /**
   * The calling process.
   * 
   */     
  protected GrinderProcess _gp = null;

  /**
   * This thread ID.
   * 
   */     
  protected int _threadId = 0;
  
}
