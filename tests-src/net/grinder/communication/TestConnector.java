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
import java.net.Socket;

import junit.framework.TestCase;


/**
 *  Unit test case for <code>Connector</code>.
 *
 * @author Philip Aston
 * @version $Revision$
 */
public class TestConnector extends TestCase {

  public TestConnector(String name) {
    super(name);
  }

  public void testConnnect() throws Exception {

    final SocketAcceptorThread socketAcceptor = new SocketAcceptorThread();

    final Connector connector =
      new Connector(socketAcceptor.getHostName(), socketAcceptor.getPort(),
                    ConnectionType.REPORT);

    final Socket localSocket = connector.connect();

    socketAcceptor.join();

    final Socket serverSocket = socketAcceptor.getAcceptedSocket();
    final InputStream inputStream = serverSocket.getInputStream();

    assertEquals(ConnectionType.REPORT, ConnectionType.read(inputStream));

    final byte[] text = "Hello".getBytes();

    localSocket.getOutputStream().write(text);

    for (int i=0; i<text.length; ++i) {
      assertEquals(text[i], inputStream.read());
    }

    socketAcceptor.close();
    
    try {
      connector.connect();
      fail("Expected CommunicationException");
    }
    catch (CommunicationException e) {
    }
  }
}
