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

import net.grinder.util.ListenerSupport;


/**
 * Passive {@link Sender}class that delegates incoming messages to a chain of
 * handlers until one claims to have handled the message.
 *
 * @author Philip Aston
 * @version $Revision$
 */
public final class HandlerChainSender implements Sender, MessageHandlerChain {

  private final ListenerSupport m_messageHandlers = new ListenerSupport();

  /**
   * Add a message handler.
   *
   * @param messageHandler The message handler.
   */
  public void add(MessageHandler messageHandler) {
    m_messageHandlers.add(messageHandler);
  }

  /**
   * Add a message responder.
   *
   * @param messageResponder The message responder.
   */
  public void add(final MessageResponder messageResponder) {
    m_messageHandlers.add(
      new MessageHandler() {
        public boolean process(Message message) throws CommunicationException {
          if (message instanceof MessageRequiringResponse) {
            final MessageRequiringResponse messageRequringResponse =
              (MessageRequiringResponse)message;

            final Message response =
              messageResponder.process(messageRequringResponse.getMessage());

            if (response != null) {
              messageRequringResponse.sendResponse(response);
              return true;
            }
          }

          return false;
        }

        public void shutdown() {
          messageResponder.shutdown();
        }
      });
  }

  /**
   * Adapt a {@link Sender} to be a message handler.
   *
   * @param sender The sender.
   */
  public void add(final Sender sender) {
    add(
      new MessageHandler() {
        public boolean process(Message message) throws CommunicationException {
          sender.send(message);
          return false;
        }

        public void shutdown() {
          sender.shutdown();
        }
      });
  }

  /**
   * Sends a message to each handler until one claims to have handled the
   * message.
   *
   * @param message The message.
   * @throws CommunicationException If one of the handlers failed.
   */
  public void send(final Message message) throws CommunicationException {

    final CommunicationException[] exception = new CommunicationException[1];

    m_messageHandlers.apply(new ListenerSupport.HandlingInformer() {
      public boolean inform(Object listener) {
        try {
          return ((MessageHandler)listener).process(message);
        }
        catch (CommunicationException e) {
          exception[0] = e;
          return true;
        }
      }
    });

    if (message instanceof MessageRequiringResponse) {
      final MessageRequiringResponse messageRequringResponse =
        (MessageRequiringResponse)message;

      if (!messageRequringResponse.isResponseSent()) {
        // No one responded.
        messageRequringResponse.sendResponse(new NoResponseMessage());
      }
    }

    if (exception[0] != null) {
      throw exception[0];
    }
  }

 /**
  * Shutdown all our handlers.
  */
  public void shutdown() {
    m_messageHandlers.apply(new ListenerSupport.Informer() {
      public void inform(Object listener) {
        ((MessageHandler)listener).shutdown();
      }
    });
  }
}
