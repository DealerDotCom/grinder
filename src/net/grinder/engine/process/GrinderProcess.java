// The Grinder
// Copyright (C) 2000, 2001  Paco Gomez
// Copyright (C) 2000, 2001  Philip Aston

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

import java.io.File;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import net.grinder.common.GrinderException;
import net.grinder.common.GrinderProperties;
import net.grinder.common.Logger;
import net.grinder.common.Test;
import net.grinder.common.TestImplementation;
import net.grinder.communication.CommunicationException;
import net.grinder.communication.ReportStatisticsMessage;
import net.grinder.communication.Sender;
import net.grinder.engine.EngineException;
import net.grinder.plugininterface.GrinderPlugin;
import net.grinder.plugininterface.ThreadCallbacks;
import net.grinder.statistics.CommonStatisticsViews;
import net.grinder.statistics.StatisticsTable;
import net.grinder.statistics.StatisticsView;
import net.grinder.statistics.TestStatistics;
import net.grinder.statistics.TestStatisticsMap;


/**
 * The class executed by the main thread of each JVM.
 * The total number of JVM is specified in the property "grinder.jvms".
 * This class is responsible for creating as many threads as configured in the
 * property "grinder.threads".
 * 
 * @author Paco Gomez
 * @author Philip Aston
 * @version $Revision$
 * @see net.grinder.engine.process.GrinderThread
 **/
public final class GrinderProcess implements Monitor
{
    // Return values used to indicate to the parent process the
    // last console signal that has been received.
    public static int EXIT_NATURAL_DEATH = 0;
    public static int EXIT_RESET_SIGNAL = 16;
    public static int EXIT_START_SIGNAL = 17;
    public static int EXIT_STOP_SIGNAL = 18;

    /** Hack extra information from parent process in system properties **/
    public static String DONT_WAIT_FOR_SIGNAL_PROPERTY_NAME =
	"grinder.dontWaitForSignal";

    private static final String TEST_PREFIX = "grinder.test";

    /**
     * The application's entry point.
     **/    
    public static void main(String[] args)
    {
	if (args.length < 1 || args.length > 2) {
	    System.err.println("Usage: java " +
			       GrinderProcess.class.getName() +
			       " <grinderID> [ propertiesFile ]");
	    System.exit(-1);
	}

	try {
	    final GrinderProcess grinderProcess =
		new GrinderProcess(args[0],
				   args.length == 2 ?
				   new File(args[1]) : null);

	    final int status = grinderProcess.run();
	    
	    System.exit(status);
	}
	catch (GrinderException e) {
	    System.err.println("Error initialising grinder process: " + e);
	    e.printStackTrace();
	    System.exit(-2);
	}
    }

    private final ProcessContext m_context;
    private final PrintWriter m_dataWriter;
    private final int m_numberOfThreads;
    private final BSFProcessContext m_bsfContext;

    private final ConsoleListener m_consoleListener;
    private final int m_reportToConsoleInterval;

    private final GrinderPlugin m_plugin;

    private int m_lastMessagesReceived = 0;

    public GrinderProcess(String grinderID, File propertiesFile)
	throws GrinderException
    {
	final GrinderProperties properties =
	    new GrinderProperties(propertiesFile);

	ProcessContext.initialiseSingleton(grinderID, properties);

	m_context = ProcessContext.getInstance();

	final LoggerImplementation loggerImplementation =
	    m_context.getLoggerImplementation();

	m_dataWriter = loggerImplementation.getDataWriter();

	m_numberOfThreads = properties.getInt("grinder.threads", 1);

	m_reportToConsoleInterval =
	    properties.getInt("grinder.reportToConsole.interval", 500);

	// Parse plugin class.
	m_plugin = instantiatePlugin();

	m_context.initialiseDataWriter();

	final String scriptFilename = properties.getProperty("grinder.script");

	m_bsfContext =
	    scriptFilename != null ?
	    new BSFProcessContext(new File(scriptFilename)) : null;

	m_consoleListener = new ConsoleListener(properties, this, m_context);
    }

    private final GrinderPlugin instantiatePlugin() throws GrinderException
    {
	final String pluginClassName =
	    m_context.getProperties().getMandatoryProperty("grinder.plugin");

	try {
	    final Class pluginClass = Class.forName(pluginClassName);

	    if (!GrinderPlugin.class.isAssignableFrom(pluginClass)) {
		throw new GrinderException(
		    "The specified plugin class ('" + pluginClass.getName() +
		    "') does not implement the interface '" +
		    GrinderPlugin.class.getName() + "'");
	    }

	    final GrinderPlugin plugin =
		(GrinderPlugin)pluginClass.newInstance();

	    plugin.initialize(m_context, getPropertiesTestSet());

	    return plugin;
	}
	catch(ClassNotFoundException e){
	    throw new GrinderException(
		"The specified plug-in class was not found.", e);
	}
	catch (Exception e){
	    throw new GrinderException(
		"An instance of the specified plug-in class " +
		"could not be created.", e);
	}
    }

    private final Set getPropertiesTestSet() throws GrinderException
    {
	final Map tests = new HashMap();
	final GrinderProperties properties = m_context.getProperties();

	final Iterator nameIterator = properties.keySet().iterator();

	while (nameIterator.hasNext()) {
	    final String name = (String)nameIterator.next();
		
	    if (!name.startsWith(TEST_PREFIX)) {
		continue;	// Not a test property.
	    }

	    final int nextSeparator = name.indexOf('.', TEST_PREFIX.length());

	    final int testNumber;

	    try {
		testNumber =
		    Integer.parseInt(name.substring(TEST_PREFIX.length(),
						    nextSeparator));
	    }
	    catch (Exception e) {
		throw new GrinderException(
		    "Could not resolve test number from property '" +
		    name + ".");
	    }

	    final Integer testNumberInteger = new Integer(testNumber);

	    if (tests.containsKey(testNumberInteger)) {
		continue;	// Already parsed.
	    }

	    final String description =
		properties.getProperty(
		    getTestPropertyName(testNumber, "description"), null);

	    final GrinderProperties parameters =
		properties.getPropertySubset(
		    getTestPropertyName(testNumber, "parameter") + '.');

	    tests.put(testNumberInteger, new TestImplementation(testNumber,
								description,
								parameters));
	}

	return new HashSet(tests.values());
    }

    private final static String getTestPropertyName(int testNumber,
						    String unqualifiedName)
    {
	return TEST_PREFIX + testNumber + '.' + unqualifiedName;
    }

    /**
     * The application's main loop. This is split from the constructor
     * as theoretically it might be called multiple times. The
     * constructor sets up the static configuration, this does a
     * single execution.
     *
     * @returns exit status to be indicated to parent process.
     **/        
    private final int run() throws GrinderException
    {
	m_context.logMessage("The Grinder version @version@");

	m_context.logMessage(System.getProperty("java.vm.vendor") + " " + 
			     System.getProperty("java.vm.name") + " " +
			     System.getProperty("java.vm.version") +
			     " on " +
			     System.getProperty("os.name") + " " +
			     System.getProperty("os.arch") + " " +
			     System.getProperty("os.version"));

	final String language;

	final GrinderThread runnable[] = new GrinderThread[m_numberOfThreads];

	for (int i=0; i<m_numberOfThreads; i++) {
	    final ThreadCallbacks threadCallbacks =
		m_plugin.createThreadCallbackHandler();

	    runnable[i] =
		new GrinderThread(this, m_context, m_bsfContext, i, 
				  threadCallbacks);
	}

	m_context.getConsoleSender().flush();

	if (!Boolean.getBoolean(DONT_WAIT_FOR_SIGNAL_PROPERTY_NAME)) {
	    m_context.logMessage("waiting for console signal",
				 Logger.LOG | Logger.TERMINAL);

	    waitForMessage();
	}

	if (!received(ConsoleListener.STOP | ConsoleListener.RESET)) {

	    m_context.logMessage("starting threads",
				 Logger.LOG | Logger.TERMINAL);
	    
	    // Start the threads
	    for (int i=0; i<m_numberOfThreads; i++) {
		final Thread t = new Thread(runnable[i],
					    "Grinder thread " + i);
		t.setDaemon(true);
		t.start();
	    }

	    final Timer timer = new Timer();
	    final TimerTask reportToConsoleTimerTask =
		new ReportToConsoleTimerTask();

	    try {
		// Schedule a regular statsitics report to the
		// console. We don't need to schedule this at a fixed
		// rate. Each report contains the work done since the
		// last report.
		timer.schedule(reportToConsoleTimerTask, 0,
			       m_reportToConsoleInterval);

		// Wait for a termination event.
		synchronized (this) {
		    while (GrinderThread.numberOfUncompletedThreads() > 0) {

			m_lastMessagesReceived =
			    m_consoleListener.received(ConsoleListener.RESET |
						       ConsoleListener.STOP);

			if (m_lastMessagesReceived != 0) {
			    break;
			}

			try {
			    wait();
			}
			catch (InterruptedException e) {
			}
		    }
		}
	    }
	    finally {
		timer.cancel();
	    }

	    synchronized (this) {
		if (GrinderThread.numberOfUncompletedThreads() > 0) {
		    
		    m_context.logMessage(
			"waiting for threads to terminate",
			Logger.LOG | Logger.TERMINAL);
			
		    GrinderThread.shutdown();

		    final long time = System.currentTimeMillis();
		    final long maxShutdownTime = 10000;

		    while (GrinderThread.numberOfUncompletedThreads() > 0) {
			try {
			    if (System.currentTimeMillis() - time >
				maxShutdownTime) {
				m_context.logMessage(
				    "threads not terminating, " +
				    "continuing anyway",
				    Logger.LOG | Logger.TERMINAL);
				break;
			    }

			    wait(maxShutdownTime);
			}
			catch (InterruptedException e) {
			}
		    }
		}
	    }

	    // Final report to the console.
	    reportToConsoleTimerTask.run();
	}
	
	m_dataWriter.close();

 	m_context.logMessage("Final statistics for this process:");

	final StatisticsTable statisticsTable =
	    new StatisticsTable(
		CommonStatisticsViews.getSummaryStatisticsView(),
		m_context.getTestRegistry().getTestStatisticsMap());

	statisticsTable.print(m_context.getOutputLogWriter());

	if (m_lastMessagesReceived == 0) {
	    // We've got here naturally, without a console signal.
	    m_context.logMessage("finished, waiting for console signal",
				 Logger.LOG | Logger.TERMINAL);
	    
	    waitForMessage();
	}

	if (received(ConsoleListener.START)) {
	    m_context.logMessage("requesting reset and start");
	    return EXIT_START_SIGNAL;
	}
	else if (received(ConsoleListener.RESET)) {
	    m_context.logMessage("requesting reset");
	    return EXIT_RESET_SIGNAL;
	}
	else if (received(ConsoleListener.STOP)) {
	    m_context.logMessage("requesting stop");
	    return EXIT_STOP_SIGNAL;
	}
	else {
	    m_context.logMessage("finished", Logger.LOG | Logger.TERMINAL);
	    return EXIT_NATURAL_DEATH;
	}
    }

    private final boolean received(int mask)
    {
	return (m_lastMessagesReceived & mask) != 0;
    }

    /**
     * Wait for until a console message matching the requirement
     * arrives.
     * @param mask The mask of constants defined by {@link Listener}
     * which specify the messages to wait for.
     * @returns A mask representing the messages actually received.
     **/
    private final synchronized void waitForMessage()
    {
	while (true) {
	    m_lastMessagesReceived =
		m_consoleListener.received(ConsoleListener.ANY);

	    if (m_lastMessagesReceived != 0) {
		break;
	    }

	    try {
		wait();
	    }
	    catch (InterruptedException e) {
	    }
	}
    }

    private class ReportToConsoleTimerTask extends TimerTask
    {
	private final TestStatisticsMap m_testStatiticsMap;

	public ReportToConsoleTimerTask() 
	{
	    m_testStatiticsMap =
		m_context.getTestRegistry().getTestStatisticsMap();
	    
	}

	public void run() {
	    m_dataWriter.flush();

	    try {
		m_context.getConsoleSender().send(
		    new ReportStatisticsMessage(
			m_testStatiticsMap.getDelta(true)));
	    }
	    catch (CommunicationException e) {
		m_context.logMessage(
		    "Report to console failed: " +e.getMessage(),
		    Logger.LOG | Logger.TERMINAL);

		e.printStackTrace(m_context.getErrorLogWriter());
	    }
	}
    }
}
