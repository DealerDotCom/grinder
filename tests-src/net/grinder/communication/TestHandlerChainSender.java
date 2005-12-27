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

package net.grinder.communication;

import net.grinder.testutility.RandomStubFactory;
import junit.framework.TestCase;


/**
 * TestHandlerChainSender.
 *
 * @author Philip Aston
 * @version $Revision$
 */
public class TestHandlerChainSender extends TestCase {

  public void testSend() throws Exception {
    final HandlerChainSender handlerChainSender = new HandlerChainSender();

    handlerChainSender.send(new SimpleMessage());

    final RandomStubFactory senderStubFactory =
      new RandomStubFactory(Sender.class);
    final Sender sender = (Sender)senderStubFactory.getStub();

    handlerChainSender.add(sender);

    final Message m1 = new SimpleMessage();
    final Message m2 = new SimpleMessage();

    handlerChainSender.send(m1);
    handlerChainSender.send(m2);

    senderStubFactory.assertSuccess("send", m1);
    senderStubFactory.assertSuccess("send", m2);
    senderStubFactory.assertNoMoreCalls();

    final MessageHandlerStubFactory messageHandlerStubFactory =
      new MessageHandlerStubFactory();

    handlerChainSender.add(messageHandlerStubFactory.getMessageHandler());

    final RandomStubFactory senderStubFactory2 =
      new RandomStubFactory(Sender.class);
    final Sender sender2 = (Sender)senderStubFactory2.getStub();

    handlerChainSender.add(sender2);

    handlerChainSender.send(m1);
    handlerChainSender.send(m2);

    senderStubFactory.assertSuccess("send", m1);
    senderStubFactory.assertSuccess("send", m2);
    senderStubFactory.assertNoMoreCalls();
    messageHandlerStubFactory.assertSuccess("process", m1);
    messageHandlerStubFactory.assertSuccess("process", m2);
    messageHandlerStubFactory.assertNoMoreCalls();
    senderStubFactory2.assertSuccess("send", m1);
    senderStubFactory2.assertSuccess("send", m2);
    senderStubFactory2.assertNoMoreCalls();

    messageHandlerStubFactory.setShouldHandle(true);

    handlerChainSender.send(m1);
    handlerChainSender.send(m2);

    senderStubFactory.assertSuccess("send", m1);
    senderStubFactory.assertSuccess("send", m2);
    senderStubFactory.assertNoMoreCalls();
    messageHandlerStubFactory.assertSuccess("process", m1);
    messageHandlerStubFactory.assertSuccess("process", m2);
    messageHandlerStubFactory.assertNoMoreCalls();
    senderStubFactory2.assertNoMoreCalls();

    messageHandlerStubFactory.setShouldHandle(false);
    messageHandlerStubFactory.setShouldThrowException(true);

    try {
      handlerChainSender.send(m1);
      fail("Expected CommunicationException");
    }
    catch (CommunicationException e) {
    }

    senderStubFactory.assertSuccess("send", m1);
    senderStubFactory.assertNoMoreCalls();
    messageHandlerStubFactory.assertException("process",
                                           new Object[] { m1 },
                                           CommunicationException.class);
    messageHandlerStubFactory.assertNoMoreCalls();
    senderStubFactory2.assertNoMoreCalls();
  }

  public void testShutdown() throws Exception {
    final HandlerChainSender handlerChainSender = new HandlerChainSender();

    handlerChainSender.shutdown();

    final MessageHandlerStubFactory messageHandlerStubFactory =
      new MessageHandlerStubFactory();
    messageHandlerStubFactory.setShouldHandle(true);

    handlerChainSender.add(messageHandlerStubFactory.getMessageHandler());

    handlerChainSender.shutdown();

    messageHandlerStubFactory.assertSuccess("shutdown");
    messageHandlerStubFactory.assertNoMoreCalls();

    final RandomStubFactory senderStubFactory =
      new RandomStubFactory(Sender.class);
    final Sender sender = (Sender)senderStubFactory.getStub();

    handlerChainSender.add(sender);

    handlerChainSender.shutdown();

    messageHandlerStubFactory.assertSuccess("shutdown");
    messageHandlerStubFactory.assertNoMoreCalls();
    senderStubFactory.assertSuccess("shutdown");
    senderStubFactory.assertNoMoreCalls();
  }

  public static final class MessageHandlerStubFactory
    extends RandomStubFactory {

    private boolean m_shouldHandle;
    private boolean m_shouldThrowException;

    public MessageHandlerStubFactory() {
      super(HandlerChainSender.MessageHandler.class);
    }

    public HandlerChainSender.MessageHandler getMessageHandler() {
      return (HandlerChainSender.MessageHandler)getStub();
    }

    public void setShouldHandle(boolean b) {
      m_shouldHandle = b;
    }

    public void setShouldThrowException(boolean b) {
      m_shouldThrowException = b;
    }

    public boolean override_process(Object proxy, Message message)
      throws CommunicationException {

      if (m_shouldThrowException) {
        throw new CommunicationException("");
      }

      return m_shouldHandle;
    }
  }
}
