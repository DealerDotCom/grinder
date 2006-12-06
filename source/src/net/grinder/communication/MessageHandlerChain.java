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

package net.grinder.communication;


/**
 * A list of message handlers.
 *
 * @author Philip Aston
 * @version $Revision:$
 */
public interface MessageHandlerChain {

  /**
   * Add a message hander.
   *
   * <p>
   * TODO refactor this and {@link #add(MessageResponder)} to register
   * handlers for a particular type so dispatch can be more efficient.
   * </p>
   *
   * @param messageHandler
   *          The message handler.
   */
  void add(MessageHandler messageHandler);

  /**
   * Add a message responder.
   *
   * @param messageResponder The message responder.
   */
  void add(final MessageResponder messageResponder);

  /**
   * Adapt a {@link Sender} to be a message handler.
   *
   * @param sender The sender.
   */
  void add(final Sender sender);


  /**
   * Handler interface.
   */
  public interface MessageHandler {
    /**
     * The handler implements this to receive a message.
     *
     * @param message
     *          The message.
     * @return <code>true</code>=> The handler processed the message.
     * @throws CommunicationException
     *           If an error occurred. If a handler throws an exception,
     *           subsequent handlers will not be called.
     *
     */
    boolean process(Message message) throws CommunicationException;

    /**
     * Notify the handler that we've been shutdown.
     */
    void shutdown();
  }

  /**
   * Handler interfaces for messages requiring a response.
   *
   * @see MessageRequiringResponse
   */
  public interface MessageResponder {
    /**
     * The handler implements this to receive a message.
     *
     * @param message
     *          The message.
     * @return The response message if the handler processed the message;
     *          <code>null</code> otherwise (in which case subsequent
     *          handlers will be called).
     * @throws CommunicationException
     *           If an error occurred. If a handler throws an exception,
     *           subsequent handlers will not be called.
     *
     */
    Message process(Message message) throws CommunicationException;

    /**
     * Notify the handler that we've been shutdown.
     */
    void shutdown();
  }
}