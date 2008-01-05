// Copyright (C) 2005-2007 Philip Aston
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

import java.io.InputStream;

import junit.framework.TestCase;
import net.grinder.common.WorkerIdentity;
import net.grinder.communication.StreamSender;
import net.grinder.engine.common.EngineException;
import net.grinder.testutility.RedirectStandardStreams;


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
        resultHolder[0] = worker.waitFor();
      }
    };

    waitThread.start();

    assertEquals(-1, resultHolder[0]);
    assertTrue(waitThread.isAlive());

    final RedirectStandardStreams streams = new RedirectStandardStreams() {
      protected void runWithRedirectedStreams() throws Exception {
        new StreamSender(worker.getCommunicationStream()).shutdown();
        waitThread.join();
      }
    };

    streams.run();

    assertEquals(-2, resultHolder[0]);
    final String output = new String(streams.getStderrBytes());
    assertTrue(output.indexOf("No control stream from agent") > 0);

    worker.destroy();
  }

  public void testWithBadWorker() throws Exception {

    final WorkerIdentity workerIdentity =
      new AgentIdentityImplementation(getClass().getName())
      .createWorkerIdentity();

    try {
      System.setProperty(IsolatedGrinderProcessRunner.RUNNER_CLASSNAME_PROPERTY,
        "blah");

      try {
        new DebugThreadWorker(workerIdentity);
        fail("Expected EngineException");
      }
      catch (EngineException e) {
        assertTrue(e.getCause() instanceof ClassNotFoundException);
      }

      // Not bothering to test other reflection exceptions. This is partly
      // due to the JDK bug where we get a NoSuchMethodException if the
      // IsolatedGrinderProcessRunner constructor throws it. (We should get
      // a InstantiationException).
    }
    finally {
      System.getProperties().remove(
        IsolatedGrinderProcessRunner.RUNNER_CLASSNAME_PROPERTY);
    }
  }

  public void testIsolatedGrinderProcessRunner() throws Exception {
    try {
      System.setProperty(IsolatedGrinderProcessRunner.RUNNER_CLASSNAME_PROPERTY,
        BadWorker.class.getName());

      final IsolatedGrinderProcessRunner isolatedGrinderProcessRunner =
        new IsolatedGrinderProcessRunner();

      try {
        isolatedGrinderProcessRunner.run(null);
        fail("Expected AssertionError");
      }
      catch (AssertionError e) {
      }
    }
    finally {
      System.getProperties().remove(
        IsolatedGrinderProcessRunner.RUNNER_CLASSNAME_PROPERTY);
    }
  }

  public static class BadWorker {
    public String run(InputStream in) {
      return "wrong return type";
    }
  }
}
