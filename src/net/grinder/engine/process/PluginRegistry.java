// Copyright (C) 2002, 2003, 2004 Philip Aston
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
import net.grinder.script.Grinder.ScriptContext;


/**
 * Registry of live {@link GrinderPlugin} implementations. Responsible
 * for plugin process and thread initialisation.
 *
 * @author Philip Aston
 * @version $Revision$
 */
public final class PluginRegistry {
  private static PluginRegistry s_instance;

  private final Logger m_logger;
  private final ScriptContext m_scriptContext;
  private final ThreadContextLocator m_threadContextLocator;
  private final Map m_plugins = new HashMap();

  /**
   * Singleton accessor.
   *
   * <p>This is called by plug-in implementations. In future I may
   * create an API package to avoid the circular package
   * dependencies.</p>
   *
   * @return The singleton.
   */
  public static PluginRegistry getInstance() {
    return s_instance;
  }

  /**
   * Set the singleton.
   */
  static void setInstance(PluginRegistry pluginRegistry) {
    s_instance = pluginRegistry;
  }

  /**
   * Constructor.
   */
  PluginRegistry(Logger logger, ScriptContext scriptContext,
                 ThreadContextLocator threadContextLocator) {

    s_instance = this;

    m_logger = logger;
    m_scriptContext = scriptContext;
    m_threadContextLocator = threadContextLocator;
  }

  /**
   * Used to register a new plugin.
   *
   * @param plugin The plugin instance.
   * @exception EngineException if an error occurs
   */
  public void register(GrinderPlugin plugin) throws EngineException {

    synchronized (m_plugins) {
      if (!m_plugins.containsKey(plugin)) {

        final RegisteredPlugin registeredPlugin =
          new RegisteredPlugin(plugin, m_scriptContext,
                               m_threadContextLocator);

        try {
          plugin.initialize(registeredPlugin);
        }
        catch (PluginException e) {
          throw new EngineException("An instance of the plug-in class '" +
                                    plugin.getClass().getName() +
                                    "' could not be initialised.", e);
        }
        
        m_plugins.put(plugin, registeredPlugin);
        m_logger.output("registered plug-in " + plugin.getClass().getName());
      }
    }
  }

  List getPluginThreadListenerList(ThreadContext threadContext)
    throws EngineException {
    synchronized (m_plugins) {
      final List result = new ArrayList(m_plugins.size());

      final Iterator iterator = m_plugins.values().iterator();

      while (iterator.hasNext()) {
        final RegisteredPlugin registeredPlugin =
          (RegisteredPlugin)iterator.next();

        result.add(
          registeredPlugin.getPluginThreadListener(threadContext));
      }

      return result;
    }
  }
}
