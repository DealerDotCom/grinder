// Copyright (C) 2007 Philip Aston
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

import java.io.InputStream;
import junit.framework.TestCase;

import net.grinder.communication.SocketAcceptorThread;


/**
 * Unit tests for {@link ConsoleClientFactory}.
*
 * @author Philip Aston
 * @version $Revision:$
 */
public class TestConsoleClientFactory extends TestCase {

  public void testConnection() throws Exception {
    final ConsoleClientFactory consoleClientFactory =
      new ConsoleClientFactory();

    final SocketAcceptorThread socketAcceptor = new SocketAcceptorThread();

    final ConsoleClient consoleClient =
      consoleClientFactory.connect(
        socketAcceptor.getHostName(), socketAcceptor.getPort());

    assertNotNull(consoleClient);

    socketAcceptor.join();

    final InputStream socketInput =
      socketAcceptor.getAcceptedSocket().getInputStream();

    assertEquals(2, socketInput.read()); // ConnectionType.CONSOLE_CLIENT

    assertEquals(0, socketInput.available());

    socketAcceptor.close();

    try {
      consoleClientFactory.connect(
        socketAcceptor.getHostName(), socketAcceptor.getPort());

      fail("Expected ConsoleClientException");
    }
    catch (ConsoleClientException e) {
    }
  }
}
