// Copyright (C) 2002 Philip Aston
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import net.grinder.common.Logger;
import net.grinder.engine.EngineException;
import net.grinder.plugininterface.GrinderPlugin;
import net.grinder.plugininterface.PluginException;
import net.grinder.plugininterface.PluginThreadCallbacks;


/**
 * Registry of live {@link GrinderPlugin} implementations. Responsible
 * for plugin process and thread initialisation.
 *
 * @author Philip Aston
 * @version $Revision$
 */
final class PluginRegistry
{
    private final ProcessContext m_processContext;
    private final Map m_plugins = new HashMap();

    /**
     * Constructor.
     */
    public PluginRegistry(ProcessContext processContext)
    {
	m_processContext = processContext;
    }

    public RegisteredPlugin register(Class pluginClass) throws EngineException
    {
	if (!GrinderPlugin.class.isAssignableFrom(pluginClass)) {
	    throw new EngineException(
		"The plugin class ('" + pluginClass.getName() +
		"') does not implement the interface '" +
		GrinderPlugin.class.getName() + "'");
	}

	try {
	    synchronized(m_plugins) {
		final RegisteredPlugin existingRegisteredPlugin =
		    (RegisteredPlugin)m_plugins.get(pluginClass);

		if (existingRegisteredPlugin != null) {
		    return existingRegisteredPlugin;
		}
		
		final GrinderPlugin plugin =
		    (GrinderPlugin)pluginClass.newInstance();

		plugin.initialize(m_processContext);
		
		final RegisteredPlugin result = new RegisteredPlugin(plugin);
    
		m_plugins.put(pluginClass, result);

		m_processContext.output(
		    "registered plug-in " + pluginClass.getName());

		return result;
	    }
	}
	catch (Exception e){
	    throw new EngineException(
		"An instance of the plug-in class '" + pluginClass.getName() +
		"' could not be created.", e);
	}
    }

    public static final class RegisteredPlugin 
    {
	private final GrinderPlugin m_plugin;
	private final ThreadLocal m_threadCallbacksThreadLocal = 
	    new ThreadLocal();

	RegisteredPlugin(GrinderPlugin plugin) 
	{
	    m_plugin = plugin;
	}

	public final GrinderPlugin getPlugin()
	{
	    return m_plugin;
	}

	final PluginThreadCallbacks getPluginThreadCallbacks(
	    ThreadContext threadContext) throws EngineException
	{
	    final PluginThreadCallbacks existingPluginThreadCallbacks =
		(PluginThreadCallbacks)m_threadCallbacksThreadLocal.get();
	    
	    if (existingPluginThreadCallbacks != null) {
		return existingPluginThreadCallbacks;
	    }

	    try {
		final PluginThreadCallbacks newPluginThreadCallbacks =
		    m_plugin.createThreadCallbackHandler();

		newPluginThreadCallbacks.initialize(threadContext);
		m_threadCallbacksThreadLocal.set(newPluginThreadCallbacks);

		return newPluginThreadCallbacks;
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

    public final List getPluginThreadCallbacksList(ThreadContext threadContext)
	throws EngineException
    {
	synchronized(m_plugins) {
	    final List result = new ArrayList(m_plugins.size());

	    final Iterator iterator = m_plugins.values().iterator();

	    while (iterator.hasNext()) {
		final RegisteredPlugin registeredPlugin =
		    (RegisteredPlugin)iterator.next();
		
		result.add(
		    registeredPlugin.getPluginThreadCallbacks(threadContext));
	    }

	    return result;
	}
    }
}
