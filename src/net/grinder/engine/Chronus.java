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

/**
 * This class is used to get the elapsed time of each method call.
 * 
 * @author <a href="mailto:paco.gomez@terra.com">Paco Gomez</a>.
 * @author Copyright © 2000
 * @version 1.6.0
 */
public class Chronus{
    
  public void begin(){
    _msBegin = System.currentTimeMillis();
    _begin = true;
    _end = false;
  }
    
  public void end(){
    if (_begin){
      _msEnd = System.currentTimeMillis();
      _end = true;
      _begin = false;
    }
  }
    
  public long getElapsed(){
    if (_begin){
       end();
    }
    return (_msEnd - _msBegin);
  }
    
  private long _msBegin = 0;
  private long _msEnd = 0;
  private boolean _begin = false;
  private boolean _end = true;
}
