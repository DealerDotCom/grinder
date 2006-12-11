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

package net.grinder.console.communication.server;

import net.grinder.communication.BlockingSender;
import net.grinder.communication.BlockingSenderWrapper;
import net.grinder.communication.MessageDispatchSender;
import net.grinder.console.communication.server.messages.ResetRecordingMessage;
import net.grinder.console.communication.server.messages.StartRecordingMessage;
import net.grinder.console.communication.server.messages.StopRecordingMessage;
import net.grinder.console.communication.server.messages.SuccessMessage;
import net.grinder.console.model.ModelInterface;
import net.grinder.testutility.RandomStubFactory;
import junit.framework.TestCase;


/**
 * Unit tests for {@link DispatchClientCommands}.
 *
 * @author Philip Aston
 * @version $Revision:$
 */
public class TestDispatchClientCommands extends TestCase {

  public void testRegisterMessageHandlers() throws Exception {

    final RandomStubFactory modelStubFactory =
      new RandomStubFactory(ModelInterface.class);
    final ModelInterface model =
      (ModelInterface)modelStubFactory.getStub();

    final DispatchClientCommands dispatchClientCommands =
      new DispatchClientCommands(model);

    final MessageDispatchSender messageDispatcher = new MessageDispatchSender();

    dispatchClientCommands.registerMessageHandlers(messageDispatcher);

    final BlockingSender blockingSender =
      new BlockingSenderWrapper(messageDispatcher);

    assertTrue(blockingSender.blockingSend(new ResetRecordingMessage())
               instanceof SuccessMessage);

    modelStubFactory.assertSuccess("reset");
    modelStubFactory.assertNoMoreCalls();

    assertTrue(blockingSender.blockingSend(new StopRecordingMessage())
      instanceof SuccessMessage);

    assertTrue(blockingSender.blockingSend(new StartRecordingMessage())
      instanceof SuccessMessage);

    modelStubFactory.assertSuccess("stop");
    modelStubFactory.assertSuccess("start");
    modelStubFactory.assertNoMoreCalls();
  }
}
