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

import net.grinder.common.WorkerIdentity;
import net.grinder.communication.StreamSender;
import junit.framework.TestCase;


/**
 * Unit tests for <code>DebugThreadWorker</code>.
 *
 * @author Philip Aston
 * @version $Revision$
 */
public class TestDebugThreadWorker extends TestCase {

  public void testDebugThreadWorker() throws Exception {

    final WorkerIdentity workerIdentity =
      new AgentIdentityImplementation(getClass().getName())
      .createWorkerIdentity();

    final Worker worker = new DebugThreadWorker(workerIdentity);

    assertEquals(workerIdentity, worker.getIdentity());
    assertNotNull(worker.getCommunicationStream());

    final int[] resultHolder = { -1 };

    final Thread waitThread = new Thread() {
      public void run() {
        try {
          resultHolder[0] = worker.waitFor();
        }
        catch (InterruptedException e) {
        }
      }
    };

    waitThread.start();

    assertEquals(-1, resultHolder[0]);
    assertTrue(waitThread.isAlive());

    final ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
    final PrintStream redirectedStderr = new PrintStream(byteStream);
    final PrintStream oldStderr = System.err;

    try {
      System.setErr(redirectedStderr);
      new StreamSender(worker.getCommunicationStream()).shutdown();
      waitThread.join();
    }
    finally {
      System.setErr(oldStderr);
    }

    assertEquals(-2, resultHolder[0]);
    final String output = new String(byteStream.toByteArray());
    assertTrue(output.indexOf("No control stream from agent") > 0);
  }
}
