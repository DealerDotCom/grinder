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
    public static void main(String args[])
    {
	Grinder grinder = null;

	if (args.length == 0) {
	    grinder = new Grinder(null);
	}
	else if (args.length == 1) {
	    final String propertiesFilename = args[0];

	    GrinderProperties.setPropertiesFileName(propertiesFilename);
	    grinder = new Grinder(propertiesFilename);
	}
	else {
	    System.err.println("Usage: java " + Grinder.class.getName() +
			       " [alternatePropertiesFilename]");
	    System.exit(1);
	}

	grinder.run();
    }

    private final String m_alternateFilename;

    private Grinder(String alternateFilename) 
    {
	m_alternateFilename = alternateFilename;
    }
    
    protected void run()
    {
	final GrinderProperties properties = GrinderProperties.getProperties();

	if (properties == null) {
	    // GrinderProperties will have output a message to stderr.
	    return;
	}

	final int numberOfProcesses =
	    properties.getInt("grinder.processes", 1);

	final String jvm = properties.getProperty("grinder.jvm", "java");

	final String additionalClasspath =
	    properties.getProperty("grinder.jvm.classpath", null);

	final String classpath =
	    (additionalClasspath != null ? additionalClasspath + ";" : "") +
	    System.getProperty("java.class.path");

	final String hostIDString =
	    properties.getProperty("grinder.hostID", getHostName());

	final String command =
	    jvm + " -classpath " + classpath + " " +
	    properties.getProperty("grinder.jvm.arguments", "") + " " +
	    GrinderProcess.class.getName();

	final boolean appendLog = properties.getBoolean("grinder.appendLog",
							false);

	final Thread[] threads = new Thread[numberOfProcesses];

	for (int i=0; i<numberOfProcesses; i++) {
	    threads[i] = new LauncherThread(hostIDString, Integer.toString(i),
					    command, m_alternateFilename,
					    appendLog);
	    threads[i].start();
	}

	System.out.println("The Grinder version @version@ started");

	for (int i=0; i<numberOfProcesses;) {
	    try {
		threads[i].join();
		i++;
	    }
	    catch (InterruptedException e) {
	    }
	}

	System.out.println("The Grinder version @version@ finished");
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

