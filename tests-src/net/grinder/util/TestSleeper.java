// Copyright (C) 2001, 2002, 2003, 2004 Philip Aston
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

package net.grinder.util;

import junit.framework.TestCase;

import net.grinder.common.Logger;
import net.grinder.testutility.RandomStubFactory;
import net.grinder.testutility.Time;


/**
 * JUnit test case for {@link Sleeper}.
 *
 * @author Philip Aston
 * @version $Revision$
 */
public class TestSleeper extends TestCase {

  private final TimeAuthority m_timeAuthority = new StandardTimeAuthority();

  public TestSleeper(String name) {
    super(name);
  }

  public void testConstruction() throws Exception {
    try {
      new Sleeper(m_timeAuthority, null, -1, 1);
      fail("IllegalArgumentException expected");
    }
    catch (IllegalArgumentException e) {
    }

    try {
      new Sleeper(m_timeAuthority, null, 1, -1);
      fail("IllegalArgumentException expected");
    }
    catch (IllegalArgumentException e) {
    }

    new Sleeper(m_timeAuthority, null, 1, 1);
  }

  public void testSleepNormal() throws Exception {
    // Warm up Hot Spot.
    final Sleeper sleep0 = new Sleeper(m_timeAuthority, null, 1, 0);

    Time time0 = new Time(0, 1000) {
        public void doIt() throws Exception { sleep0.sleepNormal(10); }
      };

    for (int i=0; i<10; i++) { time0.run(); }

    // Now do the tests.
    final Sleeper sleep1 = new Sleeper(m_timeAuthority, null, 1, 0);

    assertTrue(
      new Time(50, 70) {
        public void doIt() throws Exception  { sleep1.sleepNormal(50); }
      }.run());

    assertTrue(
      new Time(0, 10) {
        public void doIt() throws Exception  { sleep1.sleepNormal(0); }
      }.run());

    final Sleeper sleep2 = new Sleeper(m_timeAuthority, null, 2, 0);

    assertTrue(
      new Time(100, 120) {
        public void doIt() throws Exception  { sleep2.sleepNormal(50); }
      }.run());

    final Sleeper sleep3 = new Sleeper(m_timeAuthority, null, 1, 0.1);

    final Time time = new Time(40, 60) {
        public void doIt() throws Exception { sleep3.sleepNormal(50);}
      };

    int in = 0;
    for (int i=0; i<30; i++) {
      if (time.run()) {
        ++in;
      }
    }

    assertTrue(in > 20);
  }

  public void testSleepFlat() throws Exception {
    // Warm up Hot Spot.
    final Sleeper sleep0 = new Sleeper(m_timeAuthority, null, 1, 0);

    Time time0 = new Time(0, 1000) {
        public void doIt() throws Exception { sleep0.sleepFlat(10); }
      };

    for (int i=0; i<10; i++) { time0.run(); }

    // Now do the tests.
    final Sleeper sleep1 = new Sleeper(m_timeAuthority, null, 1, 0);

    assertTrue(
      new Time(0, 70) {
        public void doIt() throws Exception  { sleep1.sleepFlat(50); }
      }.run());

    assertTrue(
      new Time(0, 10) {
        public void doIt() throws Exception  { sleep1.sleepFlat(0); }
      }.run());

    final Sleeper sleep2 = new Sleeper(m_timeAuthority, null, 2, 0);

    assertTrue(
      new Time(0, 120) {
        public void doIt() throws Exception  { sleep2.sleepFlat(50); }
      }.run());

    final RandomStubFactory loggerStubFactory =
      new RandomStubFactory(Logger.class);
    final Logger logger = (Logger)loggerStubFactory.getStub();

    final Sleeper sleep3 = new Sleeper(m_timeAuthority, logger, 1, 0);
    sleep3.sleepFlat(10);
    loggerStubFactory.assertSuccess("output", String.class);
    loggerStubFactory.assertNoMoreCalls();
  }

  public void testShutdown() throws Exception {
    final TakeFifty t1 = new TakeFifty();

    assertTrue(
      new Time(500, 1000) {
        public void doIt() throws Exception {
          t1.start();
          Thread.sleep(500);
          t1.getSleeper().shutdown();
          t1.join();
        }
      }.run());

    try {
      t1.getSleeper().sleepFlat(10);
      fail("Expected ShutdownException");
    }
    catch (Sleeper.ShutdownException e) {
    }
  }

  public void testShutdownAllCurrentSleepers() throws Exception {
    final Thread t1 = new TakeFifty();
    final Thread t2 = new TakeFifty();

    assertTrue(
      new Time(500, 1000) {
        public void doIt() throws Exception {
          t1.start();
          t2.start();
          Thread.sleep(500);
          Sleeper.shutdownAllCurrentSleepers();
          t1.join();
          t2.join();
        }
      }.run());

    // No-op.
    Sleeper.shutdownAllCurrentSleepers();
  }

  private final class TakeFifty extends Thread {
    private final Sleeper m_sleeper;

    public TakeFifty() throws Sleeper.ShutdownException {
      m_sleeper = new Sleeper(m_timeAuthority, null, 1, 0);
    }

    public final void run() {
      try {
        m_sleeper.sleepNormal(50000);
      }
      catch (Sleeper.ShutdownException e) {
      }
    }

    public final Sleeper getSleeper() {
      return m_sleeper;
    }
  }
}
