// Copyright (C) 2003 Philip Aston
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

package net.grinder.communication;

import java.io.InputStream;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;


/**
 *  Unit tests for <code>MessagePump</code>.
 *
 * @author Philip Aston
 * @version $Revision$
 */
public class TestMessagePump extends AbstractSenderAndReceiverTests {

  public TestMessagePump(String name) throws Exception {
    super(name);
  }

  private MessagePump m_messagePump;

  /**
   * Sigh, JUnit treats setUp and tearDown as non-virtual methods -
   * must define in concrete test case class.
   */
  protected void setUp() throws Exception {
    super.setUp();

    final PipedOutputStream outputStream1 = new PipedOutputStream();
    final InputStream inputStream1 =
      new PipedInputStream(outputStream1) {{ buffer = new byte[32768]; }};

    m_receiver = new StreamReceiver(inputStream1);
    final Sender intermediateSender = new StreamSender(outputStream1);

    final PipedOutputStream outputStream2 = new PipedOutputStream();
    final InputStream inputStream2 =
      new PipedInputStream(outputStream2) {{ buffer = new byte[32768]; }};

    final Receiver intermediateReceiver = new StreamReceiver(inputStream2);
    m_sender = new StreamSender(outputStream2);

    m_messagePump =
      new MessagePump(intermediateReceiver, intermediateSender, 1);
  }

  protected void tearDown() throws Exception {
    super.tearDown();

    m_messagePump.shutdown();

    m_receiver.shutdown();
    m_sender.shutdown();
  }

  public void testShutdownOnNullMessage() throws Exception {
    m_sender.send(null);

    try {
      m_sender.send(new SimpleMessage());
      //      fail("Expected CommunicationException");
    }
    catch (CommunicationException e) {
    }
  }
}

