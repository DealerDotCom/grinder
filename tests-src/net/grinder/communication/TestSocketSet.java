// Copyright (C) 2000, 2001, 2002, 2003 Philip Aston
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

import java.net.Socket;
import java.util.Iterator;
import java.util.List;

import junit.framework.TestCase;


/**
 *  Unit test case for <code>SocketSet</code>.
 *
 * @author Philip Aston
 * @version $Revision$
 */
public class TestSocketSet extends TestCase {

  public TestSocketSet(String name) {
    super(name);
  }

  public void testConstructorAndSentinel() throws Exception {

    final SocketSet socketSet = new SocketSet();

    final SocketSet.Handle handle = socketSet.reserveNextHandle();
    assertTrue(handle.isSentinel());

    // Reserving the SentinelHandle is a no-op, so we can reserve it
    // as often as we like.
    final SocketSet.Handle handle2 = socketSet.reserveNextHandle();
    assertTrue(handle2.isSentinel());

    // free() and close() are also no-ops.
    handle2.free();
    handle2.close();

    assertTrue(!handle2.isClosed());

    final SocketSet.Handle handle3 = socketSet.reserveNextHandle();
    assertTrue(handle3.isSentinel());
    
    try {
      handle3.getInputStream();
      fail("Expected RuntimeException");
    }
    catch (RuntimeException e) {
    }

    try {
      handle3.getOutputStream();
      fail("Expected RuntimeException");
    }
    catch (RuntimeException e) {
    }
  }

  public void testAddAndReserveNextHandle() throws Exception {

    final SocketSet socketSet = new SocketSet();
    final SocketAcceptorThread socketAcceptor = new SocketAcceptorThread(2);

    final Socket socket1 =
      new Socket(socketAcceptor.getHostName(), socketAcceptor.getPort());

    final Socket socket2 =
      new Socket(socketAcceptor.getHostName(), socketAcceptor.getPort());

    socketSet.add(socket1);
    socketSet.add(socket2);

    final SocketSet.Handle handle1 = socketSet.reserveNextHandle();
    final SocketSet.Handle handle2 = socketSet.reserveNextHandle();
    final SocketSet.Handle sentinelHandle = socketSet.reserveNextHandle();

    assertTrue(!handle1.isSentinel());
    assertTrue(!handle2.isSentinel());
    assertTrue(sentinelHandle.isSentinel());
    assertTrue(socketSet.reserveNextHandle().isSentinel());

    assertNotNull(handle1.getInputStream());
    assertNotNull(handle1.getOutputStream());
    assertNotNull(handle2.getInputStream());
    assertNotNull(handle2.getOutputStream());

    handle2.free();
    assertSame(handle2, socketSet.reserveNextHandle());

    assertSame(sentinelHandle, socketSet.reserveNextHandle());

    handle1.free();
    assertSame(handle1, socketSet.reserveNextHandle());

    handle1.free();
    handle2.free();
    handle2.close();
    assertTrue(handle2.isClosed());
    assertSame(sentinelHandle, socketSet.reserveNextHandle());

    socketAcceptor.close();
  }

  public void testReserveAllHandles() throws Exception {

    final SocketSet socketSet = new SocketSet();
    assertEquals(0, socketSet.reserveAllHandles().size());
    assertEquals(0, socketSet.reserveAllHandles().size());

    final SocketAcceptorThread socketAcceptor = new SocketAcceptorThread(2);

    final Socket socket1 =
      new Socket(socketAcceptor.getHostName(), socketAcceptor.getPort());

    final Socket socket2 =
      new Socket(socketAcceptor.getHostName(), socketAcceptor.getPort());

    socketSet.add(socket1);
    socketSet.add(socket2);

    final List handles = socketSet.reserveAllHandles();
    assertEquals(2, handles.size());

    final Iterator i = handles.iterator();

    while (i.hasNext()) {
      final SocketSet.Handle handle = (SocketSet.Handle)i.next();
      assertTrue(!handle.isSentinel());
      assertNotNull(handle.getInputStream());
      assertNotNull(handle.getOutputStream());
      handle.free();
    }

    final List handles2 = socketSet.reserveAllHandles();
    assertEquals(2, handles2.size());

    ((SocketSet.Handle)handles2.get(0)).free();

    assertTrue(new BlockingActionThread() {
        protected void blockingAction() throws InterruptedException {
          socketSet.reserveAllHandles();
        }
      }.checkSuccess(InterruptedException.class));
  }

  public void testClose() throws Exception {

    final SocketSet socketSet = new SocketSet();
    assertEquals(0, socketSet.reserveAllHandles().size());
    assertEquals(0, socketSet.reserveAllHandles().size());

    final SocketAcceptorThread socketAcceptor = new SocketAcceptorThread(2);

    final Socket socket1 =
      new Socket(socketAcceptor.getHostName(), socketAcceptor.getPort());

    final Socket socket2 =
      new Socket(socketAcceptor.getHostName(), socketAcceptor.getPort());

    socketSet.add(socket1);
    socketSet.add(socket2);

    final List handles = socketSet.reserveAllHandles();
    assertEquals(2, handles.size());

    ((SocketSet.Handle)handles.get(1)).free();

    socketSet.close();

    final List handles2 = socketSet.reserveAllHandles();
    assertEquals(0, handles2.size());
  }
}
