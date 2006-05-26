// Copyright (C) 2004, 2005, 2006 Philip Aston
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

import junit.framework.TestCase;

import net.grinder.common.Logger;
import net.grinder.plugininterface.GrinderPlugin;
import net.grinder.plugininterface.PluginRegistry;
import net.grinder.script.Grinder.ScriptContext;
import net.grinder.statistics.StatisticsServicesImplementation;
import net.grinder.testutility.CallData;
import net.grinder.testutility.RandomStubFactory;


/**
 * Unit test case for <code>PluginRegistry</code>.
 *
 * @author Philip Aston
 * @version $Revision$
 */
public class TestPluginRegistryImplementation extends TestCase {

  public void testConstructorAndSingleton() throws Exception {
    final RandomStubFactory loggerStubFactory =
      new RandomStubFactory(Logger.class);
    final Logger logger = (Logger)loggerStubFactory.getStub();

    final RandomStubFactory scriptContextStubFactory =
      new RandomStubFactory(ScriptContext.class);
    final ScriptContext scriptContext =
      (ScriptContext)scriptContextStubFactory.getStub();

    final ThreadContextLocator threadContextLocator =
      new StubThreadContextLocator();

    final PluginRegistry pluginRegistry =
      new PluginRegistryImplementation(
        logger, scriptContext, threadContextLocator,
        StatisticsServicesImplementation.getInstance());

    assertEquals(pluginRegistry, PluginRegistry.getInstance());
  }

  public void testRegister() throws Exception {
    final RandomStubFactory loggerStubFactory =
      new RandomStubFactory(Logger.class);
    final Logger logger = (Logger)loggerStubFactory.getStub();

    final RandomStubFactory scriptContextStubFactory =
      new RandomStubFactory(ScriptContext.class);
    final ScriptContext scriptContext =
      (ScriptContext)scriptContextStubFactory.getStub();

    final ThreadContextLocator threadContextLocator =
      new StubThreadContextLocator();

    final PluginRegistry pluginRegistry =
      new PluginRegistryImplementation(
        logger, scriptContext, threadContextLocator,
        StatisticsServicesImplementation.getInstance());

    final RandomStubFactory grinderPluginStubFactory =
      new RandomStubFactory(GrinderPlugin.class);
    grinderPluginStubFactory.setIgnoreObjectMethods(true);
    final GrinderPlugin grinderPlugin =
      (GrinderPlugin)grinderPluginStubFactory.getStub();

    pluginRegistry.register(grinderPlugin);

    final CallData callData =
      grinderPluginStubFactory.assertSuccess("initialize",
                                             RegisteredPlugin.class);

    final RegisteredPlugin registeredPlugin =
      (RegisteredPlugin)callData.getParameters()[0];
    assertEquals(scriptContext, registeredPlugin.getScriptContext());

    grinderPluginStubFactory.assertNoMoreCalls();

    loggerStubFactory.assertSuccess("output", new Class[] { String.class });
    loggerStubFactory.assertNoMoreCalls();

    pluginRegistry.register(grinderPlugin);

    grinderPluginStubFactory.assertNoMoreCalls();
    loggerStubFactory.assertNoMoreCalls();
  }
}
