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
import java.util.Date;

/**
 * This class is used to redirect the standard output and error
 * to disk files. 
 * It reads characters from a BufferedRead and prints them out
 * in a PrintStream.
 * 
 * @author <a href="mailto:paco.gomez@terra.com">Paco Gomez</a>.
 * @author Copyright © 2000
 * @version 1.6.0
 */
class Redirector implements java.lang.Runnable{
    
  /**
   * The constructor. It starts a thread that executes 
   * the <tt>run</tt> method.
   * 
   */      
  public Redirector(PrintStream ps, BufferedReader br){
    _ps = ps;
    _br = br;
    Thread t = new Thread(this, _ps.toString());
    t.start(); 
  }
    
  /**
   * This method reads characters from a BufferedRead and prints 
   * them out in a PrintStream.
   */    
  public void run(){
       
    String s = "";            
       
      try{
        while (s != null){
          s = _br.readLine();
          if (s != null){
            _ps.println(new Date() + ": " + s);
          }
        }
        _ps.flush();
        _br.close();
      }
      catch(Exception e){
        System.err.println(e);
      }
                  
  }

  protected PrintStream _ps;
  protected BufferedReader _br;
}
