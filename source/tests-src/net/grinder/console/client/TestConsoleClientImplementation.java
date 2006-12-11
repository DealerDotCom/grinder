// Copyright (C) 2006 Philip Aston
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

package net.grinder.console.client;

import net.grinder.communication.BlockingSender;
import net.grinder.communication.ClientSender;
import net.grinder.communication.CommunicationException;
import net.grinder.communication.ConnectionType;
import net.grinder.communication.Connector;
import net.grinder.console.communication.server.messages.ResetRecordingMessage;
import net.grinder.console.communication.server.messages.StartRecordingMessage;
import net.grinder.console.communication.server.messages.StopRecordingMessage;
import net.grinder.testutility.RandomStubFactory;
import junit.framework.TestCase;


/**
 * Unit tetss for ConsoleClientImplementation.
 *
 * @author Philip Aston
 * @version $Revision:$
 */
public class TestConsoleClientImplementation extends TestCase {

  public void testRecordingControls() throws Exception {
    final RandomStubFactory senderStubFactory =
      new RandomStubFactory(BlockingSender.class);
    final BlockingSender sender =
      (BlockingSender)senderStubFactory.getStub();

    final ConsoleClient consoleClient =
      new ConsoleClientImplementation(sender);

    consoleClient.startRecording();
    senderStubFactory.assertSuccess("blockingSend", StartRecordingMessage.class);
    senderStubFactory.assertNoMoreCalls();

    consoleClient.stopRecording();
    senderStubFactory.assertSuccess("blockingSend", StopRecordingMessage.class);
    senderStubFactory.assertNoMoreCalls();

    consoleClient.resetRecording();
    senderStubFactory.assertSuccess("blockingSend", ResetRecordingMessage.class);
    senderStubFactory.assertNoMoreCalls();

    final CommunicationException communicationException =
      new CommunicationException("");
    senderStubFactory.setThrows("blockingSend", communicationException);

    try {
      consoleClient.resetRecording();
      fail("Expected ConsoleClientException");
    }
    catch (ConsoleClientException e) {
      assertSame(communicationException, e.getCause());
    }

    try {
      consoleClient.stopRecording();
      fail("Expected ConsoleClientException");
    }
    catch (ConsoleClientException e) {
      assertSame(communicationException, e.getCause());
    }

    try {
      consoleClient.startRecording();
      fail("Expected ConsoleClientException");
    }
    catch (ConsoleClientException e) {
      assertSame(communicationException, e.getCause());
    }
  }


  /**
   * Temporary test method.
   *
   * @param args Command line arguments.
   * @throws Exception Something went wrong.
   */
  public static void main(String[] args) throws Exception {

    final BlockingSender sender =
      ClientSender.connect(new Connector(args[0],
                                         Integer.parseInt(args[1]),
                                         ConnectionType.CONSOLE_CLIENT));
    final String command = args[2];

    final ConsoleClient consoleControl =
      new ConsoleClientImplementation(sender);

    if (command.equalsIgnoreCase("start")) {
      consoleControl.startRecording();
    }
    else if (command.equalsIgnoreCase("stop")) {
      consoleControl.stopRecording();
    }
    else if (command.equalsIgnoreCase("reset")) {
      consoleControl.resetRecording();
    }
    else {
      throw new IllegalArgumentException(command);
    }
  }

}
