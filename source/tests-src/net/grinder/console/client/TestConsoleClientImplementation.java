// Copyright (C) 2006, 2007 Philip Aston
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
import net.grinder.communication.CommunicationException;
import net.grinder.console.communication.server.messages.GetNumberOfLifeAgentsMessage;
import net.grinder.console.communication.server.messages.ResetRecordingMessage;
import net.grinder.console.communication.server.messages.ResultMessage;
import net.grinder.console.communication.server.messages.StartRecordingMessage;
import net.grinder.console.communication.server.messages.StopRecordingMessage;
import net.grinder.testutility.RandomStubFactory;
import junit.framework.TestCase;


/**
 * Unit tests for ConsoleClientImplementation.
 *
 * @author Philip Aston
 * @version $Revision:$
 */
public class TestConsoleClientImplementation extends TestCase {

  private final RandomStubFactory m_senderStubFactory =
    new RandomStubFactory(BlockingSender.class);
  private final BlockingSender m_sender =
    (BlockingSender)m_senderStubFactory.getStub();
  private final ConsoleClient m_consoleClient =
    new ConsoleClientImplementation(m_sender);

  public void testRecordingControls() throws Exception {
    m_consoleClient.startRecording();
    m_senderStubFactory.assertSuccess("blockingSend", StartRecordingMessage.class);
    m_senderStubFactory.assertNoMoreCalls();

    m_consoleClient.stopRecording();
    m_senderStubFactory.assertSuccess("blockingSend", StopRecordingMessage.class);
    m_senderStubFactory.assertNoMoreCalls();

    m_consoleClient.resetRecording();
    m_senderStubFactory.assertSuccess("blockingSend", ResetRecordingMessage.class);
    m_senderStubFactory.assertNoMoreCalls();

    final CommunicationException communicationException =
      new CommunicationException("");
    m_senderStubFactory.setThrows("blockingSend", communicationException);

    try {
      m_consoleClient.resetRecording();
      fail("Expected ConsoleClientException");
    }
    catch (ConsoleClientException e) {
      assertSame(communicationException, e.getCause());
    }

    try {
      m_consoleClient.stopRecording();
      fail("Expected ConsoleClientException");
    }
    catch (ConsoleClientException e) {
      assertSame(communicationException, e.getCause());
    }

    try {
      m_consoleClient.startRecording();
      fail("Expected ConsoleClientException");
    }
    catch (ConsoleClientException e) {
      assertSame(communicationException, e.getCause());
    }
  }

  public void testProcessMessages() throws Exception {
    m_senderStubFactory.setResult(
      "blockingSend", new ResultMessage(new Integer(10)));
    assertEquals(10, m_consoleClient.getNumberOfLiveAgents());
    m_senderStubFactory.assertSuccess("blockingSend",
      GetNumberOfLifeAgentsMessage.class);
    m_senderStubFactory.assertNoMoreCalls();

    m_senderStubFactory.setResult("blockingSend", null);

    try {
      m_consoleClient.getNumberOfLiveAgents();
      fail("Expected ConsoleClientException");
    }
    catch (ConsoleClientException e) {
    }

    m_senderStubFactory.setResult(
      "blockingSend", new ResultMessage(new Object()));

    try {
      m_consoleClient.getNumberOfLiveAgents();
      fail("Expected ConsoleClientException");
    }
    catch (ConsoleClientException e) {
    }

    final CommunicationException communicationException =
      new CommunicationException("");
    m_senderStubFactory.setThrows("blockingSend", communicationException);

    try {
      m_consoleClient.getNumberOfLiveAgents();
      fail("Expected ConsoleClientException");
    }
    catch (ConsoleClientException e) {
      assertSame(communicationException, e.getCause());
    }
  }
}
