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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import net.grinder.common.GrinderException;
import net.grinder.common.GrinderProperties;
import net.grinder.common.Logger;
import net.grinder.common.Test;
import net.grinder.common.TestImplementation;
import net.grinder.communication.CommunicationDefaults;
import net.grinder.communication.CommunicationException;
import net.grinder.communication.Message;
import net.grinder.communication.Receiver;
import net.grinder.communication.RegisterTestsMessage;
import net.grinder.communication.ReportStatisticsMessage;
import net.grinder.communication.ResetGrinderMessage;
import net.grinder.communication.Sender;
import net.grinder.communication.StartGrinderMessage;
import net.grinder.communication.StopGrinderMessage;
import net.grinder.engine.EngineException;
import net.grinder.plugininterface.GrinderPlugin;
import net.grinder.plugininterface.PluginProcessContext;
import net.grinder.plugininterface.ThreadCallbacks;
import net.grinder.statistics.CommonStatistics;
import net.grinder.statistics.StatisticsTable;
import net.grinder.statistics.StatisticsView;
import net.grinder.statistics.TestStatistics;
import net.grinder.statistics.TestStatisticsMap;


/**
 * The class executed by the main thread of each JVM.
 * The total number of JVM is specified in the property "grinder.jvms".
 * This class is responsible for creating as many threads as configured in the
 * property "grinder.threads". Each thread is an object of class "CycleRunnable".
 * It is responsible for storing the statistical information from the threads
 * and also for send it to the console and print it at the end.
 * 
 * @author Paco Gomez
 * @author Philip Aston
 * @version $Revision$
 * @see net.grinder.engine.process.GrinderThread
 */
public class GrinderProcess
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
     * 
     */    
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
    private final int m_numberOfThreads;

    private final Listener m_listener;
    private final Sender m_consoleSender;
    private int m_reportToConsoleInterval = 0;

    private final GrinderPlugin m_plugin;

    /**
     * A map of Tests to TestData. (TestData is the class this
     * package uses to store information about Tests).
     **/
    private final Map m_testSet;

    /** A map of Tests to Statistics for passing elsewhere. */
    private final TestStatisticsMap m_testStatisticsMap;
    private final CommonStatistics m_commonStatistics;

    public GrinderProcess(String grinderID, File propertiesFile)
	throws GrinderException
    {
	final GrinderProperties properties =
	    new GrinderProperties(propertiesFile);

	m_context = new ProcessContext(grinderID, properties);

	m_numberOfThreads = properties.getInt("grinder.threads", 1);

	m_commonStatistics = CommonStatistics.getInstance();

	// Parse plugin class.
	m_plugin = instantiatePlugin();

	// Get defined tests.
	final Set tests = m_plugin.getTests();

	// Parse console configuration.
	final String multicastAddress = 
	    properties.getProperty("grinder.multicastAddress",
				   CommunicationDefaults.MULTICAST_ADDRESS);

	if (properties.getBoolean("grinder.receiveConsoleSignals", true)) {
	    final int grinderPort =
		properties.getInt("grinder.multicastPort",
				  CommunicationDefaults.GRINDER_PORT);

	    if (multicastAddress != null && grinderPort > 0) {
		final ConsoleListener listener =
		    new ConsoleListener(m_context, multicastAddress,
					grinderPort);

		final Thread t = new Thread(listener, "Console Listener");
		t.setDaemon(true);
		t.start();

		m_listener = listener;
	    }
	    else {
		throw new EngineException(
		    "Unable to receive console signals: " +
		    "multicast address or port not specified");
	    }
	}
	else {
	    m_listener =
		new Listener() {
		    public boolean shouldWait() { return false; }
		    public void reset() {}
		    public boolean messageReceived(int mask) { return false; }
		};
	}

	if (properties.getBoolean("grinder.reportToConsole", true)) {
	    final int consolePort =
		properties.getInt("grinder.console.multicastPort",
				  CommunicationDefaults.CONSOLE_PORT);

	    if (multicastAddress != null && consolePort > 0) {
		m_consoleSender = new Sender(m_context.getGrinderID(),
					     multicastAddress, consolePort);

		m_reportToConsoleInterval =
		    properties.getInt("grinder.reportToConsole.interval", 500);

		m_consoleSender.send(new RegisterTestsMessage(tests));
	    }
	    else {
		throw new EngineException(
		    "Unable to report to console: " +
		    "multicast address or console port not specified");
	    }
	}
	else {
	    m_consoleSender = null;
	}

	// Wrap tests with our information.
	m_testSet = new TreeMap();
	m_testStatisticsMap = new TestStatisticsMap();
	
	final Iterator testSetIterator = tests.iterator();

	while (testSetIterator.hasNext())
	{
	    final Test test = (Test)testSetIterator.next();

	    final String sleepTimePropertyName =
		getTestPropertyName(test.getNumber(), "sleepTime");

	    final long sleepTime =
		properties.getInt(sleepTimePropertyName, -1);

	    final TestStatistics statistics =
		m_commonStatistics.new TestStatisticsImplementation();

	    m_testSet.put(test, new TestData(test, sleepTime, statistics));
	    m_testStatisticsMap.put(test, statistics);
	}
    }

    public GrinderPlugin instantiatePlugin()
	throws GrinderException
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

    private Set getPropertiesTestSet() throws GrinderException
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

    private static String getTestPropertyName(int testNumber,
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
     */        
    protected int run() throws GrinderException
    {
	m_context.logMessage(System.getProperty("java.vm.vendor") + " " + 
			     System.getProperty("java.vm.name") + " " +
			     System.getProperty("java.vm.version"));

	m_context.logMessage(System.getProperty("os.name") + " " +
			     System.getProperty("os.arch") + " " +
			     System.getProperty("os.version"));

	final GrinderThread runnable[] = new GrinderThread[m_numberOfThreads];

	for (int i=0; i<m_numberOfThreads; i++) {
	    final ThreadContext threadContext =
		new ThreadContext(m_context, i);

	    final ThreadCallbacks threadCallbackHandler =
		m_plugin.createThreadCallbackHandler();

	    runnable[i] = new GrinderThread(this, threadCallbackHandler,
					    threadContext, m_testSet);
	}

	if (m_listener.shouldWait() &&
	    !Boolean.getBoolean(DONT_WAIT_FOR_SIGNAL_PROPERTY_NAME)) {
	    m_context.logMessage("waiting for console signal",
				 Logger.LOG | Logger.TERMINAL);

	    waitForMessage();
	}

	if (!m_listener.messageReceived(Listener.STOP | Listener.RESET)) {

	    m_context.logMessage("starting threads",
				 Logger.LOG | Logger.TERMINAL);
	    
	    //   Start the threads
	    for (int i=0; i<m_numberOfThreads; i++) {
		final Thread t = new Thread(runnable[i],
					    "Grinder thread " + i);
		t.setDaemon(true);
		t.start();
	    }
	    
	    do {		// We want at least one report.
		m_context.getDataWriter().flush();

		waitForMessage(m_reportToConsoleInterval,
			       Listener.RESET | Listener.STOP);
		
		if (m_consoleSender != null) {
		    m_consoleSender.send(
			new ReportStatisticsMessage(
			    m_testStatisticsMap.getDelta(true)));
		}
	    }
	    while (GrinderThread.numberOfUncompletedThreads() > 0 &&
		   !m_listener.messageReceived(Listener.RESET |
					       Listener.STOP));
	    
	    if (GrinderThread.numberOfUncompletedThreads() > 0) {

		m_context.logMessage("waiting for threads to terminate",
				     Logger.LOG | Logger.TERMINAL);

		GrinderThread.shutdown();

		final long time = System.currentTimeMillis();
		final long maxShutdownTime = 10000;

		while (GrinderThread.numberOfUncompletedThreads() > 0) {
		    synchronized (this) {
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
	}

	m_context.getDataWriter().close();

 	m_context.logMessage("Final statistics for this process:");

	final StatisticsTable statisticsTable =
	    new StatisticsTable(m_commonStatistics.getStatisticsView(),
				m_testStatisticsMap);

	statisticsTable.print(m_context.getOutputLogWriter());

	if (m_listener.shouldWait() &&
	    !m_listener.messageReceived(Listener.ANY)) {
	    // We've got here naturally, without a console signal.
	    m_context.logMessage("finished, waiting for console signal",
				 Logger.LOG | Logger.TERMINAL);
	    waitForMessage();
	}

	if (m_listener.messageReceived(Listener.START)) {
	    m_context.logMessage("requesting reset and start");
	    return EXIT_START_SIGNAL;
	}
	else if (m_listener.messageReceived(Listener.RESET)) {
	    m_context.logMessage("requesting reset");
	    return EXIT_RESET_SIGNAL;
	}
	else if (m_listener.messageReceived(Listener.STOP)) {
	    m_context.logMessage("requesting stop");
	    return EXIT_STOP_SIGNAL;
	}
	else {
	    m_context.logMessage("finished", Logger.LOG | Logger.TERMINAL);
	    return EXIT_NATURAL_DEATH;
	}
    }

    /**
     * Wait for the given time, or until a console message arrives.
     *
     * @param period How long to wait. 0 => forever.
     * @throws IllegalArgumentException if period is negative.
     **/
    private void waitForMessage(long period, int mask)
    {
	m_listener.reset();

	long currentTime = System.currentTimeMillis();
	final long wakeUpTime = currentTime + period;

	final boolean forever = period == 0;

	while (forever || currentTime < wakeUpTime) {
	    try {
		synchronized(this) {
		    wait(forever ? 0 : wakeUpTime - currentTime);
		}
			
		break;
	    }
	    catch (InterruptedException e) {
		if (m_listener.messageReceived(mask)) {
		    break;
		}
		else {
		    currentTime = System.currentTimeMillis();
		}
	    }
	}
    }

    /**
     * Equivalent to waitForMessage(0, 0);
     **/
    private void waitForMessage()
    {
	waitForMessage(0, 0);
    }

    private interface Listener
    {
	public final static int START = 1 << 0;
	public final static int RESET = 1 << 1;
	public final static int STOP =  1 << 2;
	public final static int ANY = START | RESET | STOP;

	boolean shouldWait();
	void reset();
	boolean messageReceived(int mask);
    }

    /**
     * Runnable that receives event messages from the Console.
     * Currently no point in synchronising access.
     */
    private class ConsoleListener implements Runnable, Listener
    {
	private final PluginProcessContext m_context;
	private final Receiver m_receiver;
	private int m_message = 0;

	public ConsoleListener(PluginProcessContext context, String address,
			       int port)
	    throws CommunicationException
	{
	    m_context = context;
	    m_receiver = new Receiver(address, port);
	}
	
	public void run()
	{
	    while (true) {
		final Message message;
		
		try {
		    message = m_receiver.waitForMessage();
		}
		catch (CommunicationException e) {
		    m_context.logError("error receiving console signal: " + e,
				       Logger.LOG | Logger.TERMINAL);
		    continue;
		}

		if (message instanceof StartGrinderMessage) {
		    m_context.logMessage("got a start message from console");
		    m_message |= START;
		}
		else if (message instanceof StopGrinderMessage) {
		    m_context.logMessage("got a stop message from console");
		    m_message |= STOP;
		}
		else if (message instanceof ResetGrinderMessage) {
		    m_context.logMessage("got a reset message from console");
		    m_message |= RESET;
		}
		else {
		    m_context.logMessage(
			"got an unknown message from console");
		}

		synchronized(GrinderProcess.this) {
		    GrinderProcess.this.notifyAll();
		}
	    }
	}

	public boolean shouldWait()
	{
	    return true;
	}

	public boolean messageReceived(int mask)
	{
	    return (m_message & mask) != 0;
	}

	public void reset()
	{
	    m_message = 0;
	}
    }
}


