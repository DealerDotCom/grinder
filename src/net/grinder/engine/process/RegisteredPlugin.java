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
import net.grinder.common.Logger;
import net.grinder.communication.RegisterStatisticsViewMessage;
import net.grinder.engine.EngineException;
import net.grinder.plugininterface.GrinderPlugin;
import net.grinder.plugininterface.PluginException;
import net.grinder.plugininterface.PluginProcessContext;
import net.grinder.plugininterface.PluginThreadListener;
import net.grinder.statistics.CommonStatisticsViews;
import net.grinder.statistics.StatisticsView;


/**
 * @author Philip Aston
 * @version $Revision$
 **/
final class RegisteredPlugin implements PluginProcessContext
{
    private final GrinderPlugin m_plugin;
    private final ProcessContext m_processContext;
    private final ThreadLocal m_threadListenerThreadLocal = 
	new ThreadLocal();

    public RegisteredPlugin(GrinderPlugin plugin,
			    ProcessContext processContext)
    {
	m_plugin = plugin;
	m_processContext = processContext;
    }

    public final GrinderPlugin getPlugin()
    {
	return m_plugin;
    }

    public final String getGrinderID()
    {
	return m_processContext.getGrinderID();
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

    public final PluginThreadListener getPluginThreadListener()
	throws EngineException
    {
	final ThreadContext threadContext = ThreadContext.getThreadInstance();

	if (threadContext == null) {
	    throw new EngineException("Must be called from worker thread");
	}

	return getPluginThreadListener(threadContext);
    }

    final PluginThreadListener getPluginThreadListener(
	ThreadContext threadContext) 
	throws EngineException
    {
	final PluginThreadListener existingPluginThreadListener =
	    (PluginThreadListener)m_threadListenerThreadLocal.get();
	    
	if (existingPluginThreadListener != null) {
	    return existingPluginThreadListener;
	}

	try {
	    final PluginThreadListener newPluginThreadListener =
		m_plugin.createThreadListener(threadContext);
	    
	    m_threadListenerThreadLocal.set(newPluginThreadListener);

	    return newPluginThreadListener;
	}
	catch (PluginException e) {
	    final Logger logger = threadContext.getThreadLogger();

	    logger.error("Thread could not initialise plugin" + e);
	    e.printStackTrace(logger.getErrorLogWriter());

	    throw new EngineException(
		"Thread could not initialise plugin", e);
	}
    }
}
