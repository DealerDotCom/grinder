// Copyright (C) 2000 - 2007 Philip Aston
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

package net.grinder.engine.messages;

import java.io.File;

import net.grinder.common.GrinderProperties;
import net.grinder.common.WorkerIdentity;
import net.grinder.communication.Message;
import net.grinder.engine.agent.PublicAgentIdentityImplementation;
import net.grinder.engine.common.ScriptLocation;
import net.grinder.testutility.AbstractFileTestCase;
import net.grinder.testutility.Serializer;
import net.grinder.util.FileContents;


/**
 *  Unit test case for messages that are sent to the agent and worker
 *  processes.
 *
 * @author Philip Aston
 * @version $Revision$
 */
public class TestEngineMessages extends AbstractFileTestCase {

  private static Message serialise(Message original) throws Exception {
    return (Message) Serializer.serialize(original);
  }

  public void testInitialiseGrinderMessage() throws Exception {

    final ScriptLocation script =
      new ScriptLocation(new File("d:/foo/bah"), new File("/foo"));

    final PublicAgentIdentityImplementation agentIdentity =
      new PublicAgentIdentityImplementation("Agent");
    final WorkerIdentity workerIdentity =
      agentIdentity.createWorkerIdentity();

    final GrinderProperties properties = new GrinderProperties();

    final InitialiseGrinderMessage original =
      new InitialiseGrinderMessage(workerIdentity, false, script, properties);

    final InitialiseGrinderMessage received =
      (InitialiseGrinderMessage) serialise(original);

    assertEquals(workerIdentity, received.getWorkerIdentity());
    assertTrue(!received.getReportToConsole());
    assertEquals(script, received.getScript());
    assertEquals(properties, received.getProperties());

    final InitialiseGrinderMessage another =
      new InitialiseGrinderMessage(workerIdentity, true, script, properties);

    assertEquals(workerIdentity, another.getWorkerIdentity());
    assertTrue(another.getReportToConsole());
    assertEquals(script, another.getScript());
  }

  public void testResetGrinderMessage() throws Exception {
    serialise(new ResetGrinderMessage());
  }

  public void testStartGrinderMessage() throws Exception {
    final File file = new File("blah/blah");

    final StartGrinderMessage received =
      (StartGrinderMessage)serialise(new StartGrinderMessage(file));

    assertEquals(file, received.getScriptFile());
  }

  public void testStopGrinderMessage() throws Exception {
    serialise(new StopGrinderMessage());
  }

  public void testDistributeFileMessage() throws Exception {
    final File file = new File("test");
    new File(getDirectory(), file.getPath()).createNewFile();

    final FileContents fileContents = new FileContents(getDirectory(), file);

    final DistributeFileMessage received =
      (DistributeFileMessage)
      serialise(new DistributeFileMessage(fileContents));

    assertEquals(fileContents.toString(),
                 received.getFileContents().toString());
  }

  public void testClearCacheMessage() throws Exception {
    serialise(new ClearCacheMessage());
  }
}
