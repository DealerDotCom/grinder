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

package net.grinder.util;

import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.util.Date;

import net.grinder.common.GrinderException;
import net.grinder.common.Logger;
import net.grinder.plugininterface.PluginProcessContext;
import net.grinder.util.FilenameFactory;
import net.grinder.util.GrinderProperties;


/**
 * Currently each thread owns its own instance of
 * ProcessContextImplementation (or derived class), so we don't need
 * to worry about thread safety.
 *
 * @author Philip Aston
 * @version $Revision$
 */
public class ProcessContextImplementation implements PluginProcessContext
{
    private static final PrintWriter s_stdoutWriter;
    private static final PrintWriter s_stderrWriter;
    private static final String s_lineSeparator =
	System.getProperty("line.separator");
    private static final int s_lineSeparatorLength = s_lineSeparator.length();
    private static final DateFormat s_dateFormat =
	DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM);
    private static long s_nextTime = System.currentTimeMillis();
    private static String s_dateString;

    static
    {
	s_stdoutWriter = new PrintWriter(System.out, true);
	s_stderrWriter = new PrintWriter(System.err, true);
    }

    private synchronized static String getDateString()
    {
	final long now = System.currentTimeMillis();
	
	if (now > s_nextTime) {
	    s_nextTime = now + 1000;
	    s_dateString = s_dateFormat.format(new Date());
	}

	return s_dateString;
    }

    // Each ProcessContextImplementation is used by at most one
    // thread, so we can reuse the following objects.
    private final StringBuffer m_buffer = new StringBuffer();
    private final char[] m_outputLine = new char[512];

    private final GrinderProperties m_pluginParameters;
    private final String m_grinderID;
    private final boolean m_logProcessStreams;
    private final PrintWriter m_outputWriter;
    private final PrintWriter m_errorWriter;
    private final boolean m_appendToLog;

    private final FilenameFactory m_filenameFactory;

    {
	final GrinderProperties properties =
	    GrinderProperties.getProperties();	

	m_logProcessStreams =
	    properties.getBoolean("grinder.logProcessStreams", true);

	m_appendToLog = properties.getBoolean("grinder.appendLog", false);
    }
    
    protected ProcessContextImplementation(PluginProcessContext processContext,
					   String threadID)
    {
	m_pluginParameters = processContext.getPluginParameters();
	m_grinderID = processContext.getGrinderID();
	m_outputWriter = processContext.getOutputLogWriter();
	m_errorWriter = processContext.getErrorLogWriter();

	m_filenameFactory = new FilenameFactory(m_grinderID, threadID);
    }

    public ProcessContextImplementation(String grinderID)
	throws GrinderException
    {
	m_grinderID = grinderID;

	final GrinderProperties properties = GrinderProperties.getProperties();

	m_pluginParameters =
	    properties.getPropertySubset("grinder.plugin.parameter.");

	m_filenameFactory = new FilenameFactory(m_grinderID, null);

	try {
	    m_outputWriter =
		new PrintWriter(
		    new BufferedOutputStream(
			new FileOutputStream(
			    m_filenameFactory.createFilename("out"),
			    m_appendToLog)),
		    true);

	    m_errorWriter =
		new PrintWriter(
		    new BufferedOutputStream(
			new FileOutputStream(
			    m_filenameFactory.createFilename("error"),
			    m_appendToLog)),
		    true);


	}
	catch (FileNotFoundException e) {
	    throw new GrinderException("Could not create output streams", e);
	}
    }

    public String getGrinderID()
    {
	return m_grinderID;
    }

    public FilenameFactory getFilenameFactory()
    {
	return m_filenameFactory;
    }

    public GrinderProperties getPluginParameters()
    {
	return m_pluginParameters;
    }

    public boolean getAppendToLog()
    {
	return m_appendToLog;
    }

    public void logMessage(String message)
    {
	logMessage(message, Logger.LOG);
    }

    public void logMessage(String message, int where)
    {
	if (!m_logProcessStreams) {
	    where &= ~Logger.LOG;
	}

	if (where != 0) {
	    final int lineLength = formatMessage(message);

	    if ((where & Logger.LOG) != 0) {
		m_outputWriter.write(m_outputLine, 0, lineLength);
		m_outputWriter.flush();
	    }

	    if ((where & Logger.TERMINAL) != 0) {
		s_stdoutWriter.write(m_outputLine, 0, lineLength);
		s_stdoutWriter.flush();
	    }
	}
    }

    public void logError(String message)
    {
	logError(message, Logger.LOG);
    }
    
    public void logError(String message, int where) 
    {
	if (!m_logProcessStreams) {
	    where &= ~Logger.LOG;
	}

	if (where != 0) {
	    final int lineLength = formatMessage(message);

	    if ((where & Logger.LOG) != 0) {
		m_errorWriter.write(m_outputLine, 0, lineLength);
		m_errorWriter.flush();
	    }

	    if ((where & Logger.TERMINAL) != 0) {
		s_stderrWriter.write(m_outputLine, 0, lineLength);
		s_stderrWriter.flush();
	    }

	    final int summaryLength = 20;

	    final String summary = 
		message.length() > summaryLength ?
		message.substring(0, summaryLength) + "..." : message;

	    logMessage("ERROR (\"" + summary +
		       "\"), see error log for details",
		       Logger.LOG);
	}
    }

    public PrintWriter getOutputLogWriter()
    {
	return m_outputWriter;
    }

    public PrintWriter getErrorLogWriter()
    {
	return m_errorWriter;
    }

    private int formatMessage(String message)
    {
	m_buffer.setLength(0);

	m_buffer.append(getDateString());
	m_buffer.append(": ");

	appendMessageContext(m_buffer);

	m_buffer.append(message);

	// Sadly this is the most efficient way to get something we
	// can println from the StringBuffer. getString() creates an
	// extra string, getValue() is package scope.
	final int buffferLength = m_buffer.length();
	final int outputLineSpace =
	    m_outputLine.length - s_lineSeparatorLength;
	
	final int lineLength =
	    buffferLength > outputLineSpace ? outputLineSpace : buffferLength;

	m_buffer.getChars(0, lineLength, m_outputLine, 0);
	s_lineSeparator.getChars(0, s_lineSeparatorLength, m_outputLine,
				 lineLength);

	return lineLength + s_lineSeparatorLength;
    }

    protected void appendMessageContext(StringBuffer buffer)
    {
	buffer.append("Grinder Process (");
	buffer.append(m_grinderID);
	buffer.append(") ");
    }
}
