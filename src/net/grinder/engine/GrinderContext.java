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

import java.util.Properties;

/**
 * This class is used to share data between the Grinder and the 
 * plug-in.
 * 
 * @author <a href="mailto:paco.gomez@terra.com">Paco Gomez</a>.
 * @author Copyright © 2000
 * @version 1.6.0
 */
public class GrinderContext{
    
  /**
   * The default constructor.
   * 
   */    
  public GrinderContext(){
    setProperties(null);
    stopIteration = false;
    skipIteration = false;
    errorIteration = false;
  }

  /**
   * The constructor with properties.
   * 
   */
  public GrinderContext(Properties p){
    setProperties(p);
    stopIteration = false;
    skipIteration = false;
    errorIteration = false;
  }
    
  public void setProperties(Properties properties){
    this.properties = new Properties(properties);
  }
    
  public Properties getProperties(){
    return this.properties;
  }
    
  public void setStopIteration(boolean stopIteration){
    this.stopIteration = stopIteration;
  }

  public boolean getSkipIteration(){
    return this.skipIteration;
  }

  public void setSkipIteration(boolean skipIteration){
    this.skipIteration = skipIteration;
  }

  public boolean getStopIteration(){
    return this.stopIteration;
  }

  public void setErrorIteration(boolean errorIteration){
    this.errorIteration = errorIteration;
  }

  public boolean getErrorIteration(){
    return this.errorIteration;
  }
            
  public int paramAsInt(String s){
    return Integer.parseInt(properties.getProperty(s));
  }
  
  public double paramAsDouble(String s){
    return Double.valueOf(properties.getProperty(s)).doubleValue();
  }
  
  public String paramAsString(String s){
    return properties.getProperty(s);
  }
  
  public boolean paramAsBoolean(String s){
    return (Boolean.valueOf(properties.getProperty(s)).booleanValue());
  }    
    
  public String toString(){
    return properties.toString() 
           + ", stopIteration: " + stopIteration
           + ", skipIteration: " + skipIteration
           + ", errorIteration: " + errorIteration;
  }
    
  private Properties properties;
  private boolean stopIteration;
  private boolean skipIteration;
  private boolean errorIteration;
    
}
