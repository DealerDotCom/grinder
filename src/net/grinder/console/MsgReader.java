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

package net.grinder.console;

import java.io.*;
import java.net.*;
import java.util.*;


/**
 * This class reads performance data from the Grinder processes.
 * 
 * @author Paco Gomez
 * @version $Revision$
 */
public class MsgReader implements java.lang.Runnable{
        
  public MsgReader(StatInfo[] si){
    _si = si;
    Thread t = new Thread(this);
    t.start();
  }
    
  public void run(){
    byte[] inbuf = new byte[1024];
       
    try{
            
      MulticastSocket msocket = new MulticastSocket(
          Integer.getInteger("grinder.console.multicastPort").intValue());
      InetAddress group = InetAddress.getByName(
          System.getProperty("grinder.console.multicastAddress"));
      msocket.joinGroup(group);
            
      DatagramPacket packet = new DatagramPacket(inbuf, inbuf.length);
      long ti = 0;
      long tt = 0;
            
      while (true){
        msocket.receive(packet);
        decode(packet);
        packet.setLength(inbuf.length);                    
      }
    }
    catch(Exception e){
      e.printStackTrace();
    }
  }
    
  protected void decode(DatagramPacket packet){
      
    _s = new String(packet.getData(), 0, packet.getLength());

    _st = new StringTokenizer(_s, "\t\n\r,");
    	            
    if (Character.isDigit(_s.charAt(0))){ 	            
      if (_st.hasMoreElements()){
        _hostid = Integer.parseInt(_st.nextToken());
        if (_st.hasMoreElements()){
          _jvmid = Integer.parseInt(_st.nextToken());
          if (_st.hasMoreElements()){
            _threads = Integer.parseInt(_st.nextToken());
            if (_st.hasMoreElements()){
              _method =Integer.parseInt(_st.nextToken());
              if (_st.hasMoreElements()){
                _time = Long.parseLong(_st.nextToken());
                if (_st.hasMoreElements()){
                  _trans = Long.parseLong(_st.nextToken());
                  if (_st.hasMoreElements()){
                    _art = Float.valueOf(_st.nextToken()).floatValue();
                  }
                  else{
                    _art = 0.0f;
                  }
                }
                else{
                  _trans = 0;
                }
              }
              else{
                _time = 0;
              }
            }
            else{
              _method = 0;
            }
          }
          else{
            _threads = 0;
          }
        }
        else{
          _jvmid = -1;                               
        }
      }
      else{
        _hostid = -1;
      }        
    }
    else{
      _hostid = 0;
      _jvmid = 0;
      _threads = 0;
      _method = 0;
      _time = 0;
      _trans = 0;
      _art = 0.0f;
    }   
        
    synchronized(_si[_method]){
      _si[_method]._time += _time;
      _si[_method]._trans += _trans;
      _si[_method]._art = _art;
    }
 
  }

  String _s = "";
  StringTokenizer _st = null;
  int _hostid = 0;
  int _jvmid = 0;
  int _threads = 0;
  int _method = 0;
  long _time = 0;
  long _trans = 0;
  float _art = 0.0f;
  StatInfo _si[] = null;
    
}
