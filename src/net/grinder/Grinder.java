// The Grinder
// Copyright (C) 2000  Paco Gomez
// Copyright (C) 2000  Philip Aston

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

import java.io.File;

import net.grinder.engine.LauncherThread;
import net.grinder.engine.process.GrinderProcess;
import net.grinder.util.GrinderProperties;


/**
 * This is the entry point of The Grinder.
 * 
 * @author Paco Gomez
 * @author Philip Aston
 * @version $Revision$
 */
public class Grinder
{
    /**
     * The Grinder entry point.
     *
     */
    public static void main(String args[]){
	Grinder g = new Grinder();
	g.run();
    }
    
    protected void run()
    {
	final GrinderProperties properties = GrinderProperties.getProperties();

	System.out.println("Grinder (version FIXME) started");        
        
	final int numberOfProcesses =
	    properties.getInt("grinder.processes", 1);

	final String jvm = properties.getProperty("grinder.jvm", "java");

	final String additionalClasspath =
	    properties.getProperty("grinder.jvm.classpath", null);

	final String classpath =
	    (additionalClasspath != null ? additionalClasspath + ";" : "") +
	    System.getProperty("java.class.path");


	final String commandPrefix =
	    jvm + " -classpath " + classpath + " " +
	    properties.getProperty("grinder.jvm.arguments", "");

	final String commandSuffix = " " + GrinderProcess.class.getName();
	final boolean appendLog = Boolean.getBoolean("grinder.appendLog");

	for (int i=0; i<numberOfProcesses; i++){
	    final String command =
		commandPrefix + " -Dgrinder.jvmID=" + i + commandSuffix;

	    new LauncherThread(Integer.toString(i), command, appendLog);
	}
    }
}

