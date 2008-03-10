// Copyright (C) 2008 Philip Aston
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

package net.grinder.plugin.http;

import junit.framework.TestCase;
import net.grinder.common.GrinderException;
import net.grinder.plugininterface.GrinderPlugin;
import net.grinder.plugininterface.PluginProcessContext;
import net.grinder.plugininterface.PluginRegistry;
import net.grinder.script.Grinder.ScriptContext;
import net.grinder.testutility.RandomStubFactory;
import net.grinder.util.StandardTimeAuthority;


/**
 * Unit tests for {@link HTTPPluginControl}.
 *
 * @author Philip Aston
 * @version $Revision:$
 */
public class TestHTTPPluginControl extends TestCase {

  public void testHTTPPluginControl() throws Exception {
    final RandomStubFactory pluginProcessContextStubFactory =
      new RandomStubFactory(PluginProcessContext.class);
    final PluginProcessContext pluginProcessContext =
      (PluginProcessContext)pluginProcessContextStubFactory.getStub();

    final HTTPPluginThreadState threadState =
      new HTTPPluginThreadState(null,
                                null,
                                null,
                                new StandardTimeAuthority());

    final RandomStubFactory scriptContextStubFactory =
      new RandomStubFactory(ScriptContext.class);
    final ScriptContext scriptContext =
      (ScriptContext)scriptContextStubFactory.getStub();

    pluginProcessContextStubFactory.setResult(
      "getPluginThreadListener", threadState);
    pluginProcessContextStubFactory.setResult(
      "getScriptContext", scriptContext);

    new PluginRegistry() {
      { setInstance(this); }

      public void register(GrinderPlugin plugin) throws GrinderException {
        plugin.initialize(pluginProcessContext);
      }
    };

    final HTTPPluginConnection connectionDefaults =
      HTTPPluginControl.getConnectionDefaults();

    assertNotNull(connectionDefaults);
    assertSame(connectionDefaults, HTTPPluginControl.getConnectionDefaults());

    final HTTPUtilities utilities = HTTPPluginControl.getHTTPUtilities();
    assertNotNull(utilities);
    assertSame(utilities, HTTPPluginControl.getHTTPUtilities());

    final Object threadContext = HTTPPluginControl.getThreadHTTPClientContext();
    assertSame(threadState, threadContext);
    assertSame(threadState, HTTPPluginControl.getThreadHTTPClientContext());

    final HTTPPluginConnection connection =
      HTTPPluginControl.getThreadConnection("http://foo");
    assertSame(connection,
               HTTPPluginControl.getThreadConnection("http://foo/bah"));
    assertNotSame(connection,
                  HTTPPluginControl.getThreadConnection("http://bah"));
  }
}
