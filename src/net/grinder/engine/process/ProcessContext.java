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

import java.io.PrintWriter;
import java.util.Set;

import net.grinder.common.FilenameFactory;
import net.grinder.common.GrinderException;
import net.grinder.common.GrinderProperties;
import net.grinder.common.Logger;
import net.grinder.common.Test;
import net.grinder.communication.CommunicationDefaults;
import net.grinder.communication.Message;
import net.grinder.communication.RegisterStatisticsViewMessage;
import net.grinder.communication.Sender;
import net.grinder.communication.SenderImplementation;
import net.grinder.engine.EngineException;
import net.grinder.plugininterface.PluginProcessContext;
import net.grinder.statistics.CommonStatisticsViews;
import net.grinder.statistics.ExpressionView;
import net.grinder.statistics.StatisticsView;
import net.grinder.statistics.TestStatisticsFactory;



/**
 * @author Philip Aston
 * @version $Revision$
 **/
class ProcessContext implements PluginProcessContext
{
    private final String m_grinderID;
    private final GrinderProperties m_properties;
    private final GrinderProperties m_pluginParameters;
    private final boolean m_recordTime;
    private final LoggerImplementation m_loggerImplementation;
    private final Logger m_processLogger;
    private final Sender m_consoleSender;
    private final TestRegistry m_testRegistry;

    private boolean m_shouldWriteTitleToDataWriter;

    private final TestStatisticsFactory m_testStatisticsFactory =
	TestStatisticsFactory.getInstance();

    public ProcessContext(String grinderID, GrinderProperties properties)
	throws GrinderException
    {
	m_grinderID = grinderID;
	m_properties = properties;

	m_pluginParameters =
	    properties.getPropertySubset("grinder.plugin.parameter.");

	m_recordTime = properties.getBoolean("grinder.recordTime", true);

	final boolean appendLog =
	    properties.getBoolean("grinder.appendLog", false);

	m_loggerImplementation =
	    new LoggerImplementation(m_grinderID, 
				     properties.getProperty(
					 "grinder.logDirectory", "."),
				     properties.getBoolean(
					 "grinder.logProcessStreams", true),
				     appendLog);

	m_processLogger = m_loggerImplementation.createProcessLogger();

	m_shouldWriteTitleToDataWriter = !appendLog;

	if (properties.getBoolean("grinder.reportToConsole", true)) {
	    // Currently reading of the multicast address is
	    // duplicated here and when we read the console listener
	    // communication settings. Not worried, one day we'll
	    // change this to be unicast and it'll be a different
	    // setting.
	    final String multicastAddress =
		properties.getProperty(
		    "grinder.multicastAddress",
		    CommunicationDefaults.MULTICAST_ADDRESS);

	    final int consolePort =
		properties.getInt("grinder.console.multicastPort",
				  CommunicationDefaults.CONSOLE_PORT);

	    if (multicastAddress != null && consolePort > 0) {
		m_consoleSender =
		    new SenderImplementation(getGrinderID(), multicastAddress,
					     consolePort);
	    }
	    else {
		throw new EngineException(
		    "Unable to report to console: " +
		    "multicast address or console port not specified");
	    }
	}
	else {
	    // Null Sender implementation.
	    m_consoleSender =
		new Sender() {
		    public void send(Message message) {}
		    public void flush() {}
		    public void queue(Message message) {}
		};
	}

	m_testRegistry = new TestRegistry(getConsoleSender());
    }

    void initialiseDataWriter()
    {
	if (m_shouldWriteTitleToDataWriter) {
	    final PrintWriter dataWriter =
		m_loggerImplementation.getDataWriter();

	    dataWriter.print("Thread, Cycle, Test");

	    final ExpressionView[] detailExpressionViews =
		CommonStatisticsViews.getDetailStatisticsView()
		.getExpressionViews();

	    for (int i=0; i<detailExpressionViews.length; ++i) {
		dataWriter.print(", " +
				 detailExpressionViews[i].getDisplayName());
	    }

	    dataWriter.println();
	}
    }

    final Sender getConsoleSender()
    {
	return m_consoleSender;
    }

    final LoggerImplementation getLoggerImplementation()
    {
	return m_loggerImplementation;
    }

    final TestRegistry getTestRegistry()
    {
	return m_testRegistry;
    }

    public final String getGrinderID()
    {
	return m_grinderID;
    }

    public final GrinderProperties getProperties()
    {
	return m_properties;
    }

    public GrinderProperties getPluginParameters()
    {
	return m_pluginParameters;
    }

    public void registerTest(Test test) throws GrinderException
    {
	try {
	    m_testRegistry.registerTest(test);
	}
	catch (GrinderException e) {
	    // Either we map exceptions, or the plugin has to.
	    throw new EngineException("Failed to register test", e);
	}
    }

    public void registerTests(Set tests) throws GrinderException
    {
	try {
	    m_testRegistry.registerTests(tests);
	}
	catch (GrinderException e) {
	    // Either we map exceptions, or the plugin has to.
	    throw new EngineException("Failed to register tests", e);
	}
    }

    public final void logMessage(String message)
    {
	m_processLogger.logMessage(message);
    }

    public final void logMessage(String message, int where)
    {
	m_processLogger.logMessage(message, where);
    }

    public final void logError(String message)
    {
	m_processLogger.logError(message);
    }
    
    public final void logError(String message, int where)
    {
	m_processLogger.logError(message, where);
    }

    public final PrintWriter getOutputLogWriter()
    {
	return m_processLogger.getOutputLogWriter();
    }

    public final PrintWriter getErrorLogWriter()
    {
	return m_processLogger.getErrorLogWriter();
    }

    public String createFilename(String prefix)
    {
	return
	    m_loggerImplementation.getFilenameFactory().createFilename(prefix);
    }

    public String createFilename(String prefix, String suffix)
    {
	return
	    m_loggerImplementation.getFilenameFactory().createFilename(prefix,
								       suffix);
    }

    public final boolean getRecordTime()
    {
	return m_recordTime;
    }

    protected final TestStatisticsFactory getTestStatisticsFactory()
    {
	return m_testStatisticsFactory;
    }

    public void registerSummaryStatisticsView(StatisticsView statisticsView)
	throws GrinderException
    {
	CommonStatisticsViews.getSummaryStatisticsView().add(statisticsView);

	getConsoleSender().queue(
	    new RegisterStatisticsViewMessage(statisticsView));
    }

    public void registerDetailStatisticsView(StatisticsView statisticsView)
	throws GrinderException
    {
	CommonStatisticsViews.getDetailStatisticsView().add(statisticsView);
    }
}
