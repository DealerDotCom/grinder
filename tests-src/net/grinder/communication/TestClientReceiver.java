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
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

import junit.framework.TestCase;


/**
 *  Unit test case for <code>ClientReceiver</code>.
 *
 * @author Philip Aston
 * @version $Revision$
 */
public class TestClientReceiver extends TestCase {

  public TestClientReceiver(String name) {
    super(name);
  }

  public void testReceive() throws Exception {

    final SocketAcceptorThread socketAcceptor = new SocketAcceptorThread();

    final Connector connector =
      new Connector(socketAcceptor.getHostName(), socketAcceptor.getPort(),
                    ConnectionType.CONTROL);

    final Receiver clientReceiver = ClientReceiver.connect(connector);

    socketAcceptor.join();

    final OutputStream socketOutput =
      socketAcceptor.getAcceptedSocket().getOutputStream();

    final SimpleMessage message1 = new SimpleMessage();
    
    final ObjectOutputStream objectStream1 =
      new ObjectOutputStream(socketOutput);
    objectStream1.writeObject(message1);
    objectStream1.flush();

    final SimpleMessage message2 = new SimpleMessage();

    final ObjectOutputStream objectStream2 =
      new ObjectOutputStream(socketOutput);
    objectStream2.writeObject(message2);
    objectStream2.flush();

    final Message receivedMessage1 = clientReceiver.waitForMessage();
    final Message receivedMessage2 = clientReceiver.waitForMessage();

    assertEquals(message1, receivedMessage1);
    assertEquals(message2, receivedMessage2);

    socketAcceptor.close();

    try {
      ClientReceiver.connect(connector);
      fail("Expected CommunicationException");
    }
    catch (CommunicationException e) {
    }
  }

  public void testShutdown() throws Exception {

    final SocketAcceptorThread socketAcceptor = new SocketAcceptorThread();

    final Connector connector =
      new Connector(socketAcceptor.getHostName(), socketAcceptor.getPort(),
                    ConnectionType.CONTROL);

    final Receiver clientReceiver = ClientReceiver.connect(connector);

    socketAcceptor.join();

    final OutputStream socketOutput =
      socketAcceptor.getAcceptedSocket().getOutputStream();

    final SimpleMessage message1 = new SimpleMessage();
    
    final ObjectOutputStream objectStream1 =
      new ObjectOutputStream(socketOutput);
    objectStream1.writeObject(message1);
    objectStream1.flush();

    final Message receivedMessage = clientReceiver.waitForMessage();

    clientReceiver.shutdown();

    assertNull(clientReceiver.waitForMessage());

    socketAcceptor.close();
  }

  public void testCloseCommunicationMessage() throws Exception {

    final SocketAcceptorThread socketAcceptor = new SocketAcceptorThread();

    final Connector connector =
      new Connector(socketAcceptor.getHostName(), socketAcceptor.getPort(),
                    ConnectionType.CONTROL);

    final Receiver clientReceiver = ClientReceiver.connect(connector);

    socketAcceptor.join();

    final OutputStream socketOutput =
      socketAcceptor.getAcceptedSocket().getOutputStream();

    final SimpleMessage message1 = new SimpleMessage();
    
    final ObjectOutputStream objectStream1 =
      new ObjectOutputStream(socketOutput);
    objectStream1.writeObject(message1);
    objectStream1.flush();

    final Message receivedMessage = clientReceiver.waitForMessage();

    final Message closeCommunicationMessage = new CloseCommunicationMessage();

    final ObjectOutputStream objectStream2 =
      new ObjectOutputStream(socketOutput);
    objectStream2.writeObject(closeCommunicationMessage);
    objectStream2.flush();

    assertNull(clientReceiver.waitForMessage());

    socketAcceptor.close();
  }
}
  
