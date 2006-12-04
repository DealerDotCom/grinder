// Copyright (C) 2005 Philip Aston
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

package net.grinder.util.thread;

import java.util.Timer;
import java.util.TimerTask;

import junit.framework.TestCase;

public class TestWakeableCondition extends TestCase {

  public void testWithSingleThread() throws Exception {
    final WakeableCondition wakeableCondition = new WakeableCondition();

    // State is initially false, so we shouldn't block here.
    assertFalse(wakeableCondition.await(false));
    assertFalse(wakeableCondition.await(false));

    wakeableCondition.set(true);
    assertTrue(wakeableCondition.await(true));

    wakeableCondition.set(false);
    assertFalse(wakeableCondition.await(false));
  }

  public void testWithMultipleThreads() throws Exception {
    final Timer timer = new Timer();

    final WakeableCondition wakeableCondition = new WakeableCondition();

    final TimerTask setTrueTask = new TimerTask() {
      public void run() { wakeableCondition.set(true); }
    };

    timer.schedule(setTrueTask, 0, 1);

    assertTrue(wakeableCondition.await(true));
    assertTrue(wakeableCondition.await(true));

    setTrueTask.cancel();

    final TimerTask wakeUpAllWaitersTask = new TimerTask() {
      public void run() { wakeableCondition.wakeUpAllWaiters(); }
    };

    timer.schedule(wakeUpAllWaitersTask, 0, 1);

    // Will return true, because state is false but interrupted.
    assertTrue(wakeableCondition.await(false));

    // State should still be true.
    assertTrue(wakeableCondition.await(true));

    wakeUpAllWaitersTask.cancel();

    timer.cancel();
  }
}
