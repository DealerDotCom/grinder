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
import net.grinder.plugininterface.PluginProcessContext;
import net.grinder.plugininterface.PluginException;


/**
 * @author Philip Aston
 * @version $Revision$
 */
class ProcessContext implements PluginProcessContext
{
    private final String m_grinderID;
    private final GrinderProperties m_properties;
    private final GrinderProperties m_pluginParameters;
    private final boolean m_recordTime;
    private final LoggerImplementation m_loggerImplementation;
    private final Logger m_processLogger;

    private TestRegistry m_testRegistry;

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

	if (!appendLog) {
	    final PrintWriter dataWriter =
		m_loggerImplementation.getDataWriter();

	    if (m_recordTime) {
		dataWriter.println("Thread, Cycle, Method, Time");
	    }
	    else {
		dataWriter.println("Thread, Cycle, Method");
	    }
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

    final boolean getRecordTime()
    {
	return m_recordTime;
    }

}
