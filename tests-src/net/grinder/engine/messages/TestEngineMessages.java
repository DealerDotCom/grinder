// Copyright (C) 2000, 2001, 2002, 2003, 2004 Philip Aston
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

import net.grinder.communication.Message;
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

    final String string1 = "someID";
    final String string2 = "anotherID";
    final File file0 = new File("/foo");
    final File file1 = new File("d:/foo/bah");

    final InitialiseGrinderMessage original =
      new InitialiseGrinderMessage(string1, string2, false, file0, file1);

    final InitialiseGrinderMessage received =
      (InitialiseGrinderMessage) serialise(original);

    assertEquals(string1, original.getAgentID());
    assertEquals(string2, original.getWorkerID());
    assertTrue(!original.getReportToConsole());
    assertEquals(file0, original.getScriptFile());
    assertEquals(file1, original.getScriptDirectory());

    final InitialiseGrinderMessage another =
      new InitialiseGrinderMessage(string2, string1, true, file1, file0);

    assertEquals(string2, another.getAgentID());
    assertEquals(string1, another.getWorkerID());
    assertTrue(another.getReportToConsole());
    assertEquals(file1, another.getScriptFile());
    assertEquals(file0, another.getScriptDirectory());
  }

  public void testResetGrinderMessage() throws Exception {
    serialise(new ResetGrinderMessage());
  }

  public void testStartGrinderMessage() throws Exception {
    final File file = new File("blah/blah");

    final StartGrinderMessage startGrinderMessage =
      new StartGrinderMessage(file);

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
