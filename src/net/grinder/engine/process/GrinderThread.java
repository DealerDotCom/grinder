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

package net.grinder.engine.process;

import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Properties;

import net.grinder.plugininterface.GrinderPlugin;
import net.grinder.plugininterface.PluginException;
import net.grinder.util.GrinderProperties;


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
 */
class GrinderThread implements java.lang.Runnable
{
    /** m_numberOfThreads is incremented in constructor rather than in
     * run to avoid pathological race conditions. Hence it really
     * means "the number of GrinderThread's that have been created but
     * not run to completion" */
    private static int m_numberOfThreads = 0;

    private final Class m_pluginClass;
    private final Method[] m_methods;
    private final MethodStatistics[] m_methodStatistics;
    private final PrintWriter m_dataPrintWriter;

    private final GrinderContextImplementation m_grinderContext;
    private int m_sleepTime;
    private int m_initialSleepTime;
    private int m_numberOfCycles;

    /**
     * The constructor.
     */        
    public GrinderThread(Class pluginClass,
			 GrinderContextImplementation grinderContext,
			 PrintWriter dataPrintWriter, Method[] methods,
			 MethodStatistics[] methodStatistics)
    {
	m_pluginClass = pluginClass;
	m_grinderContext = grinderContext;
	m_dataPrintWriter = dataPrintWriter;
	m_methods = methods;
	m_methodStatistics = methodStatistics;


	// Should really wrap all of this in a configuration class.
	final GrinderProperties properties = GrinderProperties.getProperties();
	
	m_sleepTime = properties.getInt("grinder.thread.sleepTime", 0);
	m_initialSleepTime =
	    properties.getInt("grinder.thread.initialSleepTime", 0);

	m_numberOfCycles = properties.getInt("grinder.cycles", 1);

	incrementThreadCount();	// See m_numberOfThreads javadoc.
  }
    
    /**
     * The thread's main loop.
     */     
    public void run()
    {
	try{
	    final GrinderPlugin pluginInstance =
		(GrinderPlugin)m_pluginClass.newInstance();
            
	    // Random initial wait
	    if (m_initialSleepTime != 0) {
		final int randomTime =
		    (int)(m_initialSleepTime * Math.random());

		logMessage("waiting for " + randomTime + " milliseconds");

		Thread.sleep(randomTime);
	    }               

	    try {
		pluginInstance.initialize(m_grinderContext);
	    }
	    catch (PluginException e) {
		logError("Plug-in initialize() threw " + e);
		e.printStackTrace();
		return;
	    }
	    
	    logMessage("initilized plug-in, starting cycles");

	    CYCLE_LOOP:
	    for (int cycle=0; cycle<m_numberOfCycles; cycle++) {

		try {
		    pluginInstance.beginCycle();
		}
		catch (PluginException e) {
		    logError("Aborting cycle - plug-in beginCycle() threw " +
			     e, cycle);
		    e.printStackTrace();
		    continue CYCLE_LOOP;
		}
		
		METHOD_LOOP:
		for (int method=0; method<m_methods.length; method++) {
		    m_grinderContext.reset();

		    boolean success = false;
			
		    m_grinderContext.startTimer();

		    try {
			try {
			    final Boolean successBoolean = (Boolean)
				m_methods[method].invoke(pluginInstance, null);

			    success = successBoolean.booleanValue();
			}
			finally {
			    m_grinderContext.stopTimer();		
			}
		    }
		    catch (InvocationTargetException e) {
			logError("Aborting cycle - plug-in threw " +
				 e.getTargetException(), cycle, method);
			e.printStackTrace();
			continue CYCLE_LOOP;
		    }

		    if (m_grinderContext.getAborted()){
			logError("Plug-in aborted thread", cycle, method);
			break CYCLE_LOOP;
		    }

		    if (m_grinderContext.getAbortedCycle()){
			logError("Plug-in aborted cycle", cycle, method);
			continue CYCLE_LOOP;
		    }

		    final long time = m_grinderContext.getElapsedTime();
			
		    if (success) {
			m_methodStatistics[method].addTransaction(time);
		    }
		    else {
			// Abortions don't count as errors.
			m_methodStatistics[method].addError();
			logError("Plug-in reported an error", cycle, method);
		    }

		    if (m_dataPrintWriter != null) {
			m_dataPrintWriter.println(
			    m_grinderContext.getThreadID() + ", " +
			    cycle + ", " + method + ", " + time);
		    }
                    
		    if (m_sleepTime != 0) {
			Thread.sleep(m_sleepTime);
		    }
		}

		try {
		    pluginInstance.endCycle();
		}
		catch (PluginException e) {
		    logError("Plugin endCycle() threw: " + e, cycle);
		    e.printStackTrace();
		}
	    }

	    logMessage("finished");
	}
	catch(Exception e) {
	    logError(" threw an exception:");
	    e.printStackTrace(System.err);
	}
	finally {
	    decrementThreadCount();
	}
    }

    private String formatMessage(String message, int cycle, int method) 
    {
	final StringBuffer buffer = new StringBuffer(
	    "Thread (Host " + m_grinderContext.getHostIDString() +
	    " Process " + m_grinderContext.getProcessIDString() +
	    " Thread " + m_grinderContext.getProcessIDString());

	if (cycle > 0) {
	    buffer.append(" Cycle " + cycle);
	}
	
	if (method > 0) {
	    buffer.append(" Method " + m_methods[method].getName());
	}

	buffer.append("): " + message);

	return buffer.toString();
    }

    private void logMessage(String message, int cycle, int method)
    {
	System.out.println(formatMessage(message, cycle, method));
    }

    private void logMessage(String message, int cycle) 
    {
	System.out.println(formatMessage(message, cycle, -1));
    }

    private void logMessage(String message) 
    {
	System.out.println(formatMessage(message, -1, -1));
    }


    private void logError(String message, int cycle, int method)
    {
	System.err.println(formatMessage(message, cycle, method));
    }

    private void logError(String message, int cycle) 
    {
	System.err.println(formatMessage(message, cycle, -1));
    }

    private void logError(String message) 
    {
	System.err.println(formatMessage(message, -1, -1));
    }

    private static synchronized void incrementThreadCount() 
    {
	m_numberOfThreads++;
    }

    private static synchronized void decrementThreadCount() 
    {
	m_numberOfThreads--;
    }

    public static int numberOfUncompletedThreads()
    {
	return m_numberOfThreads;
    }
}
