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

import java.io.PrintWriter;

import net.grinder.common.GrinderException;
import net.grinder.common.GrinderProperties;
import net.grinder.communication.RegisterStatisticsViewMessage;
import net.grinder.engine.EngineException;
import net.grinder.plugininterface.PluginProcessContext;
import net.grinder.plugininterface.PluginThreadCallbacks;
import net.grinder.statistics.CommonStatisticsViews;
import net.grinder.statistics.StatisticsView;


/**
 * @author Philip Aston
 * @version $Revision$
 **/
final class PluginProcessContextImplementation
    implements PluginProcessContext
{
    private final PluginRegistry.RegisteredPlugin m_registeredPlugin;
    private final ProcessContext m_processContext;

    public PluginProcessContextImplementation(
	PluginRegistry.RegisteredPlugin registeredPlugin,
	ProcessContext processContext)
    {
	m_registeredPlugin = registeredPlugin;
	m_processContext = processContext;
    }

    public final String getGrinderID()
    {
	return m_processContext.getGrinderID();
    }

    public GrinderProperties getPluginParameters()
    {
	return m_processContext.getPluginParameters();
    }

    public final void registerSummaryStatisticsView(
	StatisticsView statisticsView)
	throws GrinderException
    {
	CommonStatisticsViews.getSummaryStatisticsView().add(statisticsView);

	m_processContext.getConsoleSender().queue(
	    new RegisterStatisticsViewMessage(statisticsView));
    }

    public final void registerDetailStatisticsView(
	StatisticsView statisticsView)
	throws GrinderException
    {
	// DetailStatsisticsViews are only for the data logs, so we
	// don't register the view with the console.
	CommonStatisticsViews.getDetailStatisticsView().add(statisticsView);
    }

    public final boolean getRecordTime() 
    {
	return m_processContext.getRecordTime();
    }

    public final PluginThreadCallbacks getPluginThreadCallbacks()
	throws EngineException
    {
	final ThreadContext threadContext = ThreadContext.getThreadInstance();

	if (threadContext == null) {
	    throw new EngineException("Not called from worker thread");
	}

	return m_registeredPlugin.getPluginThreadCallbacks(
	    ThreadContext.getThreadInstance());
    }

    public final void output(String message)
    {
	m_processContext.getLogger().output(message);
    }

    public final void output(String message, int where)
    {
	m_processContext.getLogger().output(message, where);
    }

    public final void error(String message)
    {
	m_processContext.getLogger().error(message);
    }
    
    public final void error(String message, int where)
    {
	m_processContext.getLogger().error(message, where);
    }

    public final PrintWriter getOutputLogWriter()
    {
	return m_processContext.getLogger().getOutputLogWriter();
    }

    public final PrintWriter getErrorLogWriter()
    {
	return m_processContext.getLogger().getErrorLogWriter();
    }

    public String createFilename(String prefix)
    {
	return m_processContext.getLoggerImplementation().
	    getFilenameFactory().createFilename(prefix);
    }

    public String createFilename(String prefix, String suffix)
    {
	return m_processContext.getLoggerImplementation().
	    getFilenameFactory().createFilename(prefix, suffix);
    }
}
