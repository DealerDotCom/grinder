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
import java.net.InetAddress;
import java.net.UnknownHostException;

import net.grinder.common.GrinderException;
import net.grinder.common.GrinderProperties;
import net.grinder.engine.LauncherThread;
import net.grinder.engine.process.GrinderProcess;


/**
 * This is the entry point of The Grinder agent process.
 * 
 * @author Paco Gomez
 * @author Philip Aston
 * @version $Revision$
 */
public class Grinder
{
    /**
     * The Grinder agent process entry point.
     *
     * @param args[] Command line arguments.
     * @exception GrinderException If an error occurred.
     */
    public static void main(String args[])
	throws GrinderException
    {
	if (args.length > 1) {
	    System.err.println("Usage: java " + Grinder.class.getName() +
			       " [alternatePropertiesFilename]");
	    System.exit(1);
	}

	new Grinder(args.length != 0 ? new File(args[0]) : null).run();
    }

    private final File m_alternateFile;

    private Grinder(File alternateFile) 
    {
	m_alternateFile = alternateFile;
    }
    
    /**
     * Run the Grinder agent process.
     *
     * @exception GrinderException if an error occurs
     */
    protected void run() throws GrinderException
    {
	boolean ignoreInitialSignal = false;

	while (true) {
	    final GrinderProperties properties =
		new GrinderProperties(m_alternateFile);

	    final int numberOfProcesses =
		properties.getInt("grinder.processes", 1);

	    final String jvm = properties.getProperty("grinder.jvm", "java");

	    final String additionalClasspath =
		properties.getProperty("grinder.jvm.classpath", null);

	    final String classpath =
		(additionalClasspath != null ? additionalClasspath + ";" : "")
		+ System.getProperty("java.class.path");

	    classpath.replace(';', File.pathSeparatorChar);
	    classpath.replace(':', File.pathSeparatorChar);

	    final String hostIDString =
		properties.getProperty("grinder.hostID", getHostName());

	    final String ignoreInitialSignalHack =
		ignoreInitialSignal ?
		("-D" +
		 GrinderProcess.DONT_WAIT_FOR_SIGNAL_PROPERTY_NAME + "=true ")
		: "";

	    final String command =
		jvm + " " +
		properties.getProperty("grinder.jvm.arguments", "") +
		" -classpath " + classpath + " " +
		ignoreInitialSignalHack + GrinderProcess.class.getName();

	    final LauncherThread[] threads =
		new LauncherThread[numberOfProcesses];

	    for (int i=0; i<numberOfProcesses; i++) {
		threads[i] = new LauncherThread(hostIDString + "-" +
						Integer.toString(i),
						command, m_alternateFile);
		threads[i].start();
	    }

	    System.out.println("The Grinder version @version@ started");

	    int combinedExitStatus = 0;

	    for (int i=0; i<numberOfProcesses;) {
		try {
		    threads[i].join();
		
		    final int exitStatus = threads[i].getExitStatus();

		    if (exitStatus > 0) { // Not an error
			if (combinedExitStatus == 0) {
			    combinedExitStatus = exitStatus;
			}
			else if (combinedExitStatus != exitStatus) {
			    System.out.println(
				"WARNING, threads disagree on exit status");
			}
		    }
		
		    i++;
		}
		catch (InterruptedException e) {
		}
	    }

	    System.out.println("The Grinder version @version@ finished");

	    if (combinedExitStatus == GrinderProcess.EXIT_START_SIGNAL) {
		System.out.println("Start signal received");
		ignoreInitialSignal = true;
	    }
	    else if (combinedExitStatus == GrinderProcess.EXIT_RESET_SIGNAL) {
		System.out.println("Reset signal received");
		ignoreInitialSignal = false;
	    }
	    else {
		break;
	    }

	    System.out.println();
	}
    }

    private String getHostName()
    {
	try {
	    return InetAddress.getLocalHost().getHostName();
	}
	catch (UnknownHostException e) {
	    return "UNNAMED HOST";
	}
    }
}

