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

import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Iterator;
import java.util.List;

import junit.framework.TestCase;


/**
 *  Unit tests for <code>Kernel</code>.
 *
 * @author Philip Aston
 * @version $Revision$
 */
public class TestKernel extends TestCase {

  public TestKernel(String name) {
    super(name);
  }

  private int m_counter = 0;

  private class IncrementCounter implements Runnable {
    
    private final int m_sleep;

    public IncrementCounter(int sleep) {
      m_sleep = sleep;
    }

    public void run() {
      synchronized (TestKernel.this) {
        ++m_counter;
        try {
          Thread.sleep(m_sleep);
        }
        catch (InterruptedException e) {
          // Exit.
        }
      }
    }
  }

  public void testExecuteAndGracefulShutdown() throws Exception {
    final Kernel kernel = new Kernel(5);

    for (int i=0; i<50; ++i) {
      kernel.execute(new IncrementCounter(1));
    }

    kernel.gracefulShutdown();

    assertEquals(50, m_counter);    
  }

  public void testForceShutdown() throws Exception {
    final Kernel kernel = new Kernel(2);

    for (int i=0; i<50; ++i) {
      kernel.execute(new IncrementCounter(10));
    }
    Thread.sleep(20);

    kernel.forceShutdown();

    assertTrue(m_counter != 50);
  }
}
