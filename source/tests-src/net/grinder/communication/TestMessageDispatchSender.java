// Copyright (C) 2005, 2006 Philip Aston
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
 * Unit tests for {@link MessageDispatchSender}.
 *
 * @author Philip Aston
 * @version $Revision$
 */
public class TestMessageDispatchSender extends TestCase {

  public void testSend() throws Exception {
    final MessageDispatchSender messageDispatchSender =
      new MessageDispatchSender();

    messageDispatchSender.send(new SimpleMessage());

    final RandomStubFactory fallbackHandlerStubFactory =
      new RandomStubFactory(Sender.class);
    final Sender fallbackHandler = (Sender)fallbackHandlerStubFactory.getStub();

    messageDispatchSender.addFallback(fallbackHandler);

    final Message m1 = new SimpleMessage();
    final Message m2 = new SimpleMessage();

    messageDispatchSender.send(m1);
    messageDispatchSender.send(m2);

    fallbackHandlerStubFactory.assertSuccess("send", m1);
    fallbackHandlerStubFactory.assertSuccess("send", m2);
    fallbackHandlerStubFactory.assertNoMoreCalls();

    final HandlerSenderStubFactory handlerStubFactory =
      new HandlerSenderStubFactory();

    final Sender previousHandler = messageDispatchSender.set(
      SimpleMessage.class,
      handlerStubFactory.getMessageHandler());
    assertNull(previousHandler);

    final RandomStubFactory otherMessagerHandlerStubFactory =
      new RandomStubFactory(Sender.class);
    final Sender otherMessageHandler =
      (Sender)otherMessagerHandlerStubFactory.getStub();

    Sender previousHandler2 =
      messageDispatchSender.set(OtherMessage.class, otherMessageHandler);
    assertNull(previousHandler2);

    messageDispatchSender.send(m1);
    messageDispatchSender.send(m2);

    handlerStubFactory.assertSuccess("send", m1);
    handlerStubFactory.assertSuccess("send", m2);
    handlerStubFactory.assertNoMoreCalls();
    fallbackHandlerStubFactory.assertNoMoreCalls();
    otherMessagerHandlerStubFactory.assertNoMoreCalls();

    final OtherMessage m3 = new OtherMessage();
    messageDispatchSender.send(m3);

    otherMessagerHandlerStubFactory.assertSuccess("send", m3);
    otherMessagerHandlerStubFactory.assertNoMoreCalls();
    fallbackHandlerStubFactory.assertNoMoreCalls();
    handlerStubFactory.assertNoMoreCalls();

    handlerStubFactory.setShouldThrowException(true);

    try {
      messageDispatchSender.send(m1);
      fail("Expected CommunicationException");
    }
    catch (CommunicationException e) {
    }

    handlerStubFactory.assertException("send",
                                             new Object[] { m1 },
                                             CommunicationException.class);
    handlerStubFactory.assertNoMoreCalls();
    fallbackHandlerStubFactory.assertNoMoreCalls();
    otherMessagerHandlerStubFactory.assertNoMoreCalls();
  }

  public void testWithMessageRequiringResponse() throws Exception {
    final MessageDispatchSender messageDispatchSender =
      new MessageDispatchSender();

    final Message message = new SimpleMessage();
    final MessageRequiringResponse messageRequiringResponse =
      new MessageRequiringResponse(message);

    try {
      messageDispatchSender.send(messageRequiringResponse);
      fail("Expected CommunicationException");
    }
    catch (CommunicationException e) {
    }

    final RandomStubFactory senderStubFactory =
      new RandomStubFactory(Sender.class);
    final Sender sender = (Sender)senderStubFactory.getStub();

    messageRequiringResponse.setResponder(sender);

    messageDispatchSender.send(messageRequiringResponse);
    senderStubFactory.assertSuccess("send", NoResponseMessage.class);
    senderStubFactory.assertNoMoreCalls();

    // Now check a handler can send a response.
    final Message responseMessage = new SimpleMessage();

    messageDispatchSender.set(
      SimpleMessage.class,
      new BlockingSender() {
        public Message blockingSend(Message message)  {
          return responseMessage;
        }

        public void shutdown() { }
      });

    final MessageRequiringResponse messageRequiringResponse2 =
      new MessageRequiringResponse(message);
    messageRequiringResponse2.setResponder(sender);

    messageDispatchSender.send(messageRequiringResponse2);
    senderStubFactory.assertSuccess("send", responseMessage);
    senderStubFactory.assertNoMoreCalls();
  }

  public void testShutdown() throws Exception {
    final MessageDispatchSender messageDispatchSender =
      new MessageDispatchSender();

    messageDispatchSender.shutdown();

    final HandlerSenderStubFactory handlerStubFactory =
      new HandlerSenderStubFactory();

    messageDispatchSender.set(
      SimpleMessage.class,
      handlerStubFactory.getMessageHandler());

    messageDispatchSender.shutdown();

    handlerStubFactory.assertSuccess("shutdown");
    handlerStubFactory.assertNoMoreCalls();

    final RandomStubFactory senderStubFactory =
      new RandomStubFactory(Sender.class);
    final Sender sender = (Sender)senderStubFactory.getStub();

    messageDispatchSender.addFallback(sender);
    messageDispatchSender.addFallback(sender);

    messageDispatchSender.shutdown();

    handlerStubFactory.assertSuccess("shutdown");
    handlerStubFactory.assertNoMoreCalls();
    senderStubFactory.assertSuccess("shutdown");
    senderStubFactory.assertSuccess("shutdown"); // Registered twice.
    senderStubFactory.assertNoMoreCalls();
  }

  public static final class HandlerSenderStubFactory
    extends RandomStubFactory {

    private boolean m_shouldThrowException;

    public HandlerSenderStubFactory() {
      super(Sender.class);
    }

    public Sender getMessageHandler() {
      return (Sender)getStub();
    }
    public void setShouldThrowException(boolean b) {
      m_shouldThrowException = b;
    }

    public void override_send(Object proxy, Message message)
      throws CommunicationException {

      if (m_shouldThrowException) {
        throw new CommunicationException("");
      }
    }
  }

  public static class OtherMessage implements Message {
  }
}
