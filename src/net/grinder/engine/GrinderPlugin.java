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
 * The interface a valid Grinder plug-in must implement.
 *
 * @author <a href="mailto:paco.gomez@terra.com">Paco Gomez</a>.
 * @author Copyright © 2000
 * @version 1.6.0
 */ 
public interface GrinderPlugin{

  /**
   * This method is executed the first time the plug-in is 
   * loaded. It is executed only once. It allows the 
   * plug-in initialization passing the GrinderContext
   * from the Grinder to the plug-in.
   */
  public void init(GrinderContext grinderContext);

  /**
   * This method is executed at the end of the iterations.
   * It is executed only once. It allows the plug-in 
   * finalization.
   */  
  public void end();

}
