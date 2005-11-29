// Copyright (C) 2005 Philip Aston
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

package net.grinder.engine.agent;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import net.grinder.common.GrinderProperties;
import net.grinder.communication.FanOutStreamSender;
import junit.framework.TestCase;


/**
 * Unit tests for <code>DebugThreadWorkerFactory</code>.
 *
 * @author Philip Aston
 * @version $Revision$
 */
public class TestDebugThreadWorkerFactory extends TestCase {

  public void testFactory() throws Exception {

    final AgentIdentityImplementation agentIdentity =
      new AgentIdentityImplementation(getClass().getName());

    final DebugThreadWorkerFactory factory =
      new DebugThreadWorkerFactory(agentIdentity,
                                   new FanOutStreamSender(0),
                                   false,
                                   null,
                                   null,
                                   null);

    final ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
    final PrintStream redirectedStderr = new PrintStream(byteStream);
    final PrintStream oldStderr = System.err;

    System.setErr(redirectedStderr);

    try {
      final Worker worker = factory.create(null, null);
      worker.waitFor();
    }
    finally {
      System.setErr(oldStderr);
    }
  }
}
