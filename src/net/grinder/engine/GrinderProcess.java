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
import java.net.*;
import java.lang.reflect.*;
import java.text.*;

/**
 * The class executed by the main thread of each JVM.
 * The total number of JVM is specified in the property "grinder.jvms".
 * This class is responsible for creating as many threads as configured in the
 * property "grinder.threads". Each thread is an object of class "CycleThread".
 * It is responsible for storing the statistical information from the threads
 * and also for send it to the console and print it at the end.
 * 
 * @author <a href="mailto:paco.gomez@terra.com">Paco Gomez</a>.
 * @author Copyright © 2000
 * @version 1.6.0
 * @see net.grinder.engine.GrinderThread
 * @see net.grinder.engin.CycleThread
 */
public class GrinderProcess extends net.grinder.engine.PropsLoader{
    
  /**
   * The application's entry point.
   * 
   */    
  public static void main(String args[]){
    GrinderProcess gp = new GrinderProcess();
    gp.run();
  }

  /**
   * The application's main loop.
   * 
   */        
  protected void run(){
    loadProperties();
    String s = new java.util.Date().toString() + ": ";
    System.out.println(s + "Grinder process started with the following properties:");
    showProperties();
        
    if (Boolean.getBoolean("grinder.fileStats")){
      String fileName = System.getProperty("grinder.logDir")+ "/grinder_log_" +
      System.getProperty("grinder.hostId") + "_" + 
      System.getProperty("grinder.jvmId");            
                
      try{                
        _datFile = new PrintWriter(new BufferedWriter(
                       new FileWriter(fileName + ".dat", 
                           Boolean.getBoolean("grinder.appendLog")))); 
        _datFile.println("hostId,jvmId,threadId,timesId,methodId,millis");                                   
      }
      catch(Exception e){
        _datFile = null;
        System.err.println(e);
      }
    }
    
    int threads = 1;
    s = System.getProperty("grinder.threads");
    if (s != null){
      threads = Integer.parseInt(s);
    }
    CycleThread ct[];
    ct = new CycleThread[threads];
    _aTime = new long[_methods.size()];
    _aTrans = new long[_methods.size()];
    _aErrors = new long[_methods.size()];
    _iTime = new long[_methods.size()];
    _iTrans = new long[_methods.size()];
    for (int i=0; i<_methods.size(); i++){
      _aTime[i] = 0;
      _aTrans[i] = 0;
      _aErrors[i] = 0;
      _iTime[i] = 0;
      _iTrans[i] = 0;
    }
    String methodName = "";
    try{
      final Class pluginClass =
	  Class.forName(System.getProperty("grinder.cycleClass"));    
      
      if (!GrinderPlugin.class.isAssignableFrom(pluginClass)) {
      	throw new Exception("The specified plug-in class (\""+
      	                    pluginClass.getName() +
      	                    "\") does not implement the interface: "+
      	                    "\"" + GrinderPlugin.class.getName() + "\"");
      }	                 
                 
      _m = new Method[_methods.size()];
            
      final Class contextClass[] = new Class[1];
      contextClass[0] = GrinderContext.class;
            
      for (int i=0; i<_methods.size(); i++){
        methodName = (String)_methods.elementAt(i);
        if (i == 0){
          _m[i] = pluginClass.getMethod((String) _methods.elementAt(i),
					 contextClass);
        }
        else{
          _m[i] = pluginClass.getMethod((String) _methods.elementAt(i),
					 null);
        }
      }
    }
    catch(ClassNotFoundException e){
      System.err.println("<E> <GrinderProcess-run> Could not initialize the Grinder, the plug-in class was not found.");
      e.printStackTrace(System.err);
      System.exit(-1);      
    }
    catch(NoSuchMethodException e){
      System.err.println("<E> <GrinderProcess-run> Could not initialize the Grinder with the plug-in, method: " + 
                         methodName);        	
      e.printStackTrace(System.err);
      System.exit(-1);
    }
    catch(SecurityException e){
      System.err.println("<E> <GrinderProcess-run> Could not initialize the Grinder with the plug-in, method: " + 
                         methodName);        	
      e.printStackTrace(System.err);
      System.exit(-1);      
    }                
    catch(Exception e){
      System.err.println("<E> <GrinderProcess-run> Could not initialize the Grinder with the plug-in.");
      e.printStackTrace(System.err);
      System.exit(-1);
    }
    finally{
    }   
                
    for (int i=0; i<threads; i++){
      ct[i] = new CycleThread(this, i);
      //Thread t = new Thread(ct[i], "threadId=" + i);
      //t.start();             
      //1synchronized(ct[i]){
      //1    try{ct[i].wait();}catch(Exception e){System.err.println(e);}
      //1}
    }      
        
    if (Boolean.getBoolean("grinder.initialWait")){
      System.out.println("jvmId=" + System.getProperty("grinder.jvmId") + ": waiting...");
      waitForSignal();
    }
    System.out.println("jvmId=" + System.getProperty("grinder.jvmId") + ": started at  " + new Date());                    
        
    //wait until the last thread is blocked
    while (true){
      synchronized(this){
        if(_aliveThreads == threads){
          //all the threads are blocked
          break;
        }
        else{
          Thread.yield();
        }
      }
    }

    synchronized(this){
      this.notifyAll();
    }
              
    boolean onceMore = true;
    float art = 0.0f;
    float tps = 0.0f;
    StatInfo si = null;
    String msg = "";
    byte b[] = null;

    DatagramSocket socket = null;
    InetAddress groupAddr = null;
    DatagramPacket packet = null;       
    int port = 0;
    String jvmId = System.getProperty("grinder.jvmId");
    String hostId = System.getProperty("grinder.hostId");

    if (Boolean.getBoolean("grinder.reportToConsole")){
      try{
        socket = new DatagramSocket();
        groupAddr = InetAddress.getByName(
                    System.getProperty("grinder.console.multicastAddress"));
        port = Integer.getInteger("grinder.console.multicastPort").intValue();
      }
      catch(Exception e){
        System.err.println(e);
      }
    }
                    
    while ((_aliveThreads > 0) || onceMore){
      if (_aliveThreads <= 0){
        onceMore = false;
      }
      
      try{
        Thread.sleep(_interval);
                
        if (Boolean.getBoolean("grinder.reportToConsole")){
                        
          for (int i=1; i<(_methods.size()-1); i++){
            si = getStatInfo(i);
            art = 0;
            tps = 0;
            if ((si._trans != 0) && (si._time != 0)){
              art = ((float)si._time / (float)si._trans)/1000.0f;
              tps = 1.0f / art; 
            }

            msg = hostId + "," + 
                  jvmId + "," + 
                  threads + "," + 
                  //_methods.elementAt(i) + "," +
                  //be aware of this!!!
                  (i-1) + "," +
                  si._time + "," +
                  si._trans + "," +
                  Float.toString(art) + "," +
                  Float.toString(tps) + ",";

            //Java 1.1 (it doesn't work)
            //b = msg.getBytes();
            // Java 1.0 Deprecated in Java 1.1, but it works:
            b = new byte[msg.length() + 1];                           
            msg.getBytes(0, msg.length(), b, 0);

            try{
              packet = new DatagramPacket(b, b.length, groupAddr, port);
              socket.send(packet);
            }
            catch(SocketException e){
              System.err.println(e);
            }
            catch(IOException e){
              System.err.println(e);
            }
            catch(Exception e){
              System.err.println(e);
            }
          }
        }
      }
      catch(Exception e){
        System.err.println(e);
      }
    }
        
    if (_datFile != null){
      _datFile.flush();
      _datFile.close();
    }
        
    System.out.println("jvmId=" + System.getProperty("grinder.jvmId") + ": finished at " + new Date());                            
            
    System.out.println("Final statistics for this JVM: [hostId="+ hostId + ",jvmId=" + jvmId + "]");
    System.out.println("\t(TST=Total Successful Transactions)");
    System.out.println("\t(TPT=Total Processing Time (miliseconds))");
    System.out.println("\t(ART=Average Response Time (seconds))");
    System.out.println("\t(TPS=Transactions Per Second)");
    System.out.println("\t(TUT=Total Unsuccessful Transactions)");        
    //System.out.println("\tmethod,\t\tTPT,\t\tTST,\t\tART,\t\tTPS");
    System.out.println("\tper method:");//,\tTPT,\tTT,\tART,\tTPS");
        
    long totalTST = 0;
    float totalTPT = 0.0f;
    float totalART = 0.0f;
    long totalTUT = 0;
        
    DecimalFormat frmt1 = new DecimalFormat("0.##");
    DecimalFormat frmt2 = new DecimalFormat("0.00");
    for (int i=1; i<(_methods.size()-1); i++){
            
      art = 0;
      tps = 0;
      if ((_aTrans[i] != 0) && (_aTime[i] != 0)){
        art = ((float)_aTime[i] / (float)_aTrans[i])/1000.0f;
        tps = 1.0f / art; 
      }            

      msg = "\t" + _methods.elementAt(i) + "," +
            "\tTST:" + frmt1.format(_aTrans[i]) + "," +
            "\tTPT:" + frmt1.format(_aTime[i]) + "," +
            "\tART:" + frmt2.format(art) + "," +
            "\tTPS:" + frmt2.format(tps) + "," +
            "\tTUT:" + frmt1.format(_aErrors[i]);                              
                        
      System.out.println(msg);
                
      totalTST += _aTrans[i];
      totalTPT += _aTime[i];            
      totalTUT += _aErrors[i];
      totalART += art;
    }
        
    System.out.println("total TST=" + frmt1.format(totalTST));
    System.out.println("total TPT=" + frmt1.format(totalTPT));
    System.out.println("total ART=" + frmt2.format(totalART));
    System.out.println("total TUT=" + frmt1.format(totalTUT));

    s = new java.util.Date().toString() + ": ";
    System.out.println(s + "Grinder process finished");   
    //System.out.println();
        
  }
    
  protected void loadProperties(){
    super.loadProperties();
        
    StringTokenizer st;
    
    st = new StringTokenizer(System.getProperty("grinder.cycleMethods"), "\t\n\r,");
    _methods = new java.util.Vector();
    while(st.hasMoreElements()){
      _methods.addElement(st.nextElement());
    }

    st = new StringTokenizer(System.getProperty("grinder.cycleParams"), "\t\n\r,[]");
    _params = new Properties();
    String s1 = "";
    String s2 = "";
    
    while(st.hasMoreElements()){
      //st.nextElement(); //[
      if (st.hasMoreElements()){
        s1 = st.nextToken();
        if (st.hasMoreElements()){
          s2 = st.nextToken();
          _params.put(s1, s2);
        }
      }
    }           
  }    
	
  public void waitForSignal(){
        
    byte[] inbuf = new byte[1024];
    try{
      MulticastSocket msocket = new MulticastSocket(
        Integer.getInteger("grinder.multicastPort").intValue());
      InetAddress group = InetAddress.getByName(
        System.getProperty("grinder.multicastAddress"));
      msocket.joinGroup(group);
            
      DatagramPacket packet = new DatagramPacket(inbuf, inbuf.length);
      msocket.receive(packet);
      int numBytesReceived = packet.getLength();
    }
    catch(SocketException e){
      System.err.println(e);
    }
    catch(IOException e){
      System.err.println(e);
    }
    catch(Exception e){
      System.err.println(e);
    }
        
  }	

  public synchronized void addError(int method, long error){
    _aErrors[method] += error;
  }	
	
  public synchronized void add(int method, long time, long trans){
    _aTime[method] += time;
    _aTrans[method] += trans;
    _iTime[method] += time;
    _iTrans[method] += trans;
  }
    
  public synchronized StatInfo getStatInfo(int method){
    StatInfo si = new StatInfo(_iTime[method], _iTrans[method]);
    _iTime[method] = 0;
    _iTrans[method] = 0;
    return si;
  }    
    
  protected void printlnStats(String s){
    if (_datFile != null){
      _datFile.println(s);
    }
  }
    
    
  protected Vector _methods;  
  protected Method _m[];
  protected Properties _params;  
    
  /**
   * Interval time, per method
   */
  private long _iTime[];
  
  /** 
   * Interval transactions, per method
   */
  private long _iTrans[];
  
  /**
   * Accumulated time, per method
   */
  private long _aTime[];
  
  /**
   * Accumulated transactions, per method
   */
  private long _aTrans[];  
  
  /**
   * Accumulated errors, per method
   */
  private long _aErrors[];
    

  protected int _aliveThreads = 0;

  /**
   * In milliseconds, time between reports to console
   */
  protected int _interval = 500;    
    
  PrintWriter _datFile = null;
    
}

