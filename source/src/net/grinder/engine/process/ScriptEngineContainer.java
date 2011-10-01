// Copyright (C) 2011 Philip Aston
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
// COPYRIGHT HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
// INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
// (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
// SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
// HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
// STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
// ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
// OF THE POSSIBILITY OF SUCH DAMAGE.

package net.grinder.engine.process;

import java.util.List;

import net.grinder.common.Logger;
import net.grinder.engine.common.EngineException;
import net.grinder.scriptengine.ScriptEngineService;

import org.picocontainer.DefaultPicoContainer;
import org.picocontainer.MutablePicoContainer;
import org.picocontainer.behaviors.Caching;


/**
 * Container for script engines.
 *
 * @author Philip Aston
 */
class ScriptEngineContainer {

  private final MutablePicoContainer m_container =
    new DefaultPicoContainer(new Caching());

  public ScriptEngineContainer(Logger logger,
                               List<String> implementationNames)
    throws EngineException {

    m_container.addComponent(logger);

    for (String implementationName : implementationNames) {
      try {
        m_container.addComponent(Class.forName(implementationName));
      }
      catch (ClassNotFoundException e) {
        throw new EngineException("Could not load '" + implementationName + "'",
                                  e);
      }
    }
  }

  public final List<ScriptEngineService> getServices() {
    return m_container.getComponents(ScriptEngineService.class);
  }
}
