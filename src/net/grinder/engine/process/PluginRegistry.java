// Copyright (C) 2002, 2003 Philip Aston
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

import net.grinder.engine.EngineException;
import net.grinder.plugininterface.GrinderPlugin;


/**
 * Registry of live {@link GrinderPlugin} implementations. Responsible
 * for plugin process and thread initialisation.
 *
 * @author Philip Aston
 * @version $Revision$
 */
public final class PluginRegistry {
  private static PluginRegistry s_instance;

  private final ProcessContext m_processContext;
  private final Map m_plugins = new HashMap();

  /**
   * Singleton accessor.
   * @return The singleton.
   */
  public static final PluginRegistry getInstance() {
    return s_instance;
  }

  /**
   * Constructor.
   */
  PluginRegistry(ProcessContext processContext) throws EngineException {
    if (s_instance != null) {
      throw new EngineException("Already initialised");
    }

    s_instance = this;

    m_processContext = processContext;
  }

  /**
   * Used to register a new plugin.
   *
   * @param pluginClass The plugin's class.
   * @return A handle to the plugin.
   * @exception EngineException if an error occurs
   */
  public RegisteredPlugin register(Class pluginClass) throws EngineException {
    if (!GrinderPlugin.class.isAssignableFrom(pluginClass)) {
      throw new EngineException(
        "The plugin class ('" + pluginClass.getName() +
        "') does not implement the interface '" +
        GrinderPlugin.class.getName() + "'");
    }

    try {
      synchronized (m_plugins) {
        final RegisteredPlugin existingRegisteredPlugin =
          (RegisteredPlugin)m_plugins.get(pluginClass);

        if (existingRegisteredPlugin != null) {
          return existingRegisteredPlugin;
        }

        final GrinderPlugin plugin =
          (GrinderPlugin)pluginClass.newInstance();

        final RegisteredPlugin registeredPlugin =
          new RegisteredPlugin(plugin, m_processContext);

        plugin.initialize(registeredPlugin);

        m_plugins.put(pluginClass, registeredPlugin);

        m_processContext.getLogger().output(
          "registered plug-in " + pluginClass.getName());

        return registeredPlugin;
      }
    }
    catch (Exception e) {
      throw new EngineException(
        "An instance of the plug-in class '" + pluginClass.getName() +
        "' could not be created.", e);
    }
  }

  final List getPluginThreadListenerList(ThreadContext threadContext)
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
