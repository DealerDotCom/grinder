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
import net.grinder.communication.RegisterStatisticsViewMessage;
import net.grinder.communication.Sender;
import net.grinder.plugininterface.PluginException;
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

    private TestRegistry m_testRegistry;

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
    }

    public void initialiseDataWriter()
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

    public String getGrinderID()
    {
	return m_grinderID;
    }

    public GrinderProperties getProperties()
    {
	return m_properties;
    }

    public GrinderProperties getPluginParameters()
    {
	return m_pluginParameters;
    }

    public void registerTest(Test test) throws PluginException
    {
	try {
	    m_testRegistry.registerTest(test);
	}
	catch (GrinderException e) {
	    // Either we map exceptions, or the plugin has to.
	    throw new PluginException("Failed to register test", e);
	}
    }

    public void registerTests(Set tests) throws PluginException
    {
	try {
	    m_testRegistry.registerTests(tests);
	}
	catch (GrinderException e) {
	    // Either we map exceptions, or the plugin has to.
	    throw new PluginException("Failed to register tests", e);
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

    final LoggerImplementation getLoggerImplementation()
    {
	return m_loggerImplementation;
    }

    final void setTestRegistry(TestRegistry testRegistry)
    {
	m_testRegistry = testRegistry;
    }

    final TestRegistry getTestRegistry()
    {
	return m_testRegistry;
    }

    public final boolean getRecordTime()
    {
	return m_recordTime;
    }

    protected final TestStatisticsFactory getTestStatisticsFactory()
    {
	return m_testStatisticsFactory;
    }

    /** A quick and dirty hack. We do the right thing in G3. **/
    Sender m_consoleSender;

    public void registerSummaryStatisticsView(StatisticsView statisticsView)
	throws GrinderException
    {
	CommonStatisticsViews.getSummaryStatisticsView().add(statisticsView);

	if (m_consoleSender != null) {
	    m_consoleSender.queue(
		new RegisterStatisticsViewMessage(statisticsView));
	}
    }

    public void registerDetailStatisticsView(StatisticsView statisticsView)
	throws GrinderException
    {
	CommonStatisticsViews.getDetailStatisticsView().add(statisticsView);
    }
}
