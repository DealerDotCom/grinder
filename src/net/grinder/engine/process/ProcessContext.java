// Copyright (C) 2000 Paco Gomez
// Copyright (C) 2000, 2001, 2002 Philip Aston
// All rights reserved.
//
// This file is part of The Grinder software distribution. Refer to
// the file LICENSE which is part of The Grinder distribution for
// licensing details. The Grinder distribution is available on the
// Internet at http://grinder.sourceforge.net/
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
// "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
// LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
// FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
// REGENTS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
// INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
// (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
// SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
// HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
// STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
// ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
// OF THE POSSIBILITY OF SUCH DAMAGE.

package net.grinder.engine.process;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.Writer;
import java.text.DateFormat;
import java.util.Date;

import net.grinder.common.FilenameFactory;
import net.grinder.common.GrinderException;
import net.grinder.common.GrinderProperties;
import net.grinder.common.Logger;
import net.grinder.communication.Sender;
import net.grinder.communication.RegisterStatisticsViewMessage;
import net.grinder.plugininterface.PluginProcessContext;
import net.grinder.statistics.CommonStatisticsViews;
import net.grinder.statistics.ExpressionView;
import net.grinder.statistics.StatisticsView;
import net.grinder.statistics.TestStatisticsFactory;
import net.grinder.util.DelayedCreationFileWriter;



/**
 * Currently each thread owns its own instance of ProcessContext (or
 * derived class), so we don't need to worry about thread safety.
 *
 * @author Philip Aston
 * @version $Revision$
 */
public class ProcessContext implements PluginProcessContext
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
	s_stdoutWriter = new PrintWriter(System.out);
	s_stderrWriter = new PrintWriter(System.err);
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

    // Each ProcessContext is used by at most one thread, so we can
    // reuse the following objects.
    private final StringBuffer m_buffer = new StringBuffer();
    private final char[] m_outputLine = new char[512];

    private final String m_grinderID;
    private final GrinderProperties m_properties;
    private final boolean m_logProcessStreams;
    private final boolean m_recordTime;
    private final GrinderProperties m_pluginParameters;
    private final PrintWriter m_outputWriter;
    private final PrintWriter m_errorWriter;
    private final PrintWriter m_dataWriter;
    private boolean m_shouldWriteTitleToDataWriter;

    private final FilenameFactoryImplementation m_filenameFactory;
    private final TestStatisticsFactory m_testStatisticsFactory =
	TestStatisticsFactory.getInstance();

    protected ProcessContext(ProcessContext processContext,
			     String contextSuffix)
    {
	m_grinderID = processContext.m_grinderID;
	m_properties = processContext.m_properties;
	m_logProcessStreams = processContext.m_logProcessStreams;
	m_recordTime = processContext.m_recordTime;
	m_pluginParameters = processContext.m_pluginParameters;
	m_outputWriter = processContext.m_outputWriter;
	m_errorWriter = processContext.m_errorWriter;
	m_dataWriter = processContext.m_dataWriter;

	m_filenameFactory =
	    new FilenameFactoryImplementation(
		processContext.m_filenameFactory.getLogDirectory(),
		contextSuffix);
    }

    public ProcessContext(String grinderID, GrinderProperties properties)
	throws GrinderException
    {
	m_grinderID = grinderID;
	m_properties = properties;

	m_logProcessStreams =
	    properties.getBoolean("grinder.logProcessStreams", true);

	m_recordTime = properties.getBoolean("grinder.recordTime", true);

	m_pluginParameters =
	    properties.getPropertySubset("grinder.plugin.parameter.");
	    
	final File logDirectory =
	    new File(properties.getProperty("grinder.logDirectory", "."), "");

	try {
	    logDirectory.mkdirs();
	}
	catch (Exception e) {
	    throw new GrinderException(e.getMessage(), e);
	}

	if (!logDirectory.canWrite()) {
	    throw new GrinderException("Cannot write to log directory '" +
				       logDirectory.getPath() + "'");
	}

	m_filenameFactory = new FilenameFactoryImplementation(logDirectory);

	final boolean appendLog =
	    properties.getBoolean("grinder.appendLog", false);

	// Although we manage the flushing ourselves and don't call
	// printn, we set auto flush on our PrintWriters because
	// clients can get direct access to them.
	m_outputWriter = new PrintWriter(createWriter("out", appendLog), true);
	m_errorWriter =
	    new PrintWriter(createWriter("error", appendLog), true);

	// Don't autoflush, we explictly control flushing of the writer.
	m_dataWriter = new PrintWriter(createWriter("data", appendLog), false);

	m_shouldWriteTitleToDataWriter = appendLog;
    }

    public void initialiseDataWriter()
    {
	if (!m_shouldWriteTitleToDataWriter) {
	    m_dataWriter.print("Thread, Cycle, Test");

	    final ExpressionView[] detailExpressionViews =
		CommonStatisticsViews.getDetailStatisticsView()
		.getExpressionViews();

	    for (int i=0; i<detailExpressionViews.length; ++i) {
		m_dataWriter.print(", " +
				   detailExpressionViews[i].getDisplayName());
	    }

	    m_dataWriter.println();
	}
    }

    private Writer createWriter(String prefix, boolean appendLog)
	throws GrinderException
    {
	final File file = new File(m_filenameFactory.createFilename(prefix));

	// Check we can write to the file and moan now. We won't see
	// the problem later because PrintWriters eat exceptions. If
	// the file doesn't exist, we're pretty sure we can create it
	// because we checked we can write to the log directory.
	if (file.exists() && !file.canWrite()) {
	    throw new GrinderException("Cannot write to '" + file.getPath() +
				       "'");
	}

	return new BufferedWriter(
	    new DelayedCreationFileWriter(file, appendLog));
    }

    public String getGrinderID()
    {
	return m_grinderID;
    }

    public FilenameFactory getFilenameFactory()
    {
	return m_filenameFactory;
    }

    public GrinderProperties getProperties()
    {
	return m_properties;
    }

    public GrinderProperties getPluginParameters()
    {
	return m_pluginParameters;
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

    PrintWriter getDataWriter()
    {
	return m_dataWriter;
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
	final int bufferLength = m_buffer.length();
	final int outputLineSpace =
	    m_outputLine.length - s_lineSeparatorLength;
	
	final int lineLength =
	    bufferLength > outputLineSpace ? outputLineSpace : bufferLength;

	m_buffer.getChars(0, lineLength, m_outputLine, 0);
	s_lineSeparator.getChars(0, s_lineSeparatorLength, m_outputLine,
				 lineLength);

	return lineLength + s_lineSeparatorLength;
    }

    public final boolean getRecordTime()
    {
	return m_recordTime;
    }

    protected void appendMessageContext(StringBuffer buffer)
    {
	buffer.append("Grinder Process (");
	buffer.append(m_grinderID);
	buffer.append(") ");
    }

    private final class FilenameFactoryImplementation
	implements FilenameFactory
    {
	private final File m_logDirectory;
	private final String m_contextString;

	public FilenameFactoryImplementation(File logDirectory)
	{
	    this(logDirectory, null);
	}

	public FilenameFactoryImplementation(File logDirectory,
					     String subContext)
	{
	    m_logDirectory = logDirectory;

	    m_contextString =
		"_" + m_grinderID +
		(subContext != null ? "_" + subContext : "");
	}

	public final File getLogDirectory()
	{
	    return m_logDirectory;
	}

	public final String createFilename(String prefix, String suffix)
	{
	    final StringBuffer result = new StringBuffer();

	    result.append(m_logDirectory);
	    result.append(File.separator);
	    result.append(prefix);
	    result.append(m_contextString);
	    result.append(suffix);

	    return result.toString();
	}

	public final String createFilename(String prefix)
	{
	    return createFilename(prefix, ".log");
	}
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
	    m_consoleSender.send(
		new RegisterStatisticsViewMessage(statisticsView));
	}
    }

    public void registerDetailStatisticsView(StatisticsView statisticsView)
	throws GrinderException
    {
	CommonStatisticsViews.getDetailStatisticsView().add(statisticsView);
    }
}
