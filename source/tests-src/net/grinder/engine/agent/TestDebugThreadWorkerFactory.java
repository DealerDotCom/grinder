// Copyright (C) 2005, 2006, 2007 Philip Aston
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

package net.grinder.engine.agent;

import net.grinder.common.GrinderProperties;
import net.grinder.communication.FanOutStreamSender;
import net.grinder.testutility.AbstractFileTestCase;
import net.grinder.testutility.RedirectStandardStreams;


/**
 * Unit tests for <code>DebugThreadWorkerFactory</code>.
 *
 * @author Philip Aston
 * @version $Revision$
 */
public class TestDebugThreadWorkerFactory extends AbstractFileTestCase {

  public void testFactory() throws Exception {

    final AgentIdentityImplementation agentIdentity =
      new AgentIdentityImplementation(getClass().getName());

    final GrinderProperties properties = new GrinderProperties();
    properties.setProperty("grinder.logDirectory",
                           getDirectory().getAbsolutePath());

    final DebugThreadWorkerFactory factory =
      new DebugThreadWorkerFactory(agentIdentity,
                                   new FanOutStreamSender(0),
                                   false,
                                   null,
                                   properties);

    new RedirectStandardStreams() {
      protected void runWithRedirectedStreams() throws Exception {
        final Worker worker = factory.create(null, null);
        worker.waitFor();
      }
    }.run();

    // Should have output and error files.
    assertEquals(2, getDirectory().list().length);
  }
}
