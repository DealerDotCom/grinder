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

package net.grinder.engine.process;

import net.grinder.engine.process.StopWatch.StopWatchNotRunningException;
import net.grinder.engine.process.StopWatch.StopWatchRunningException;
import junit.framework.TestCase;


/**
 * Unit tests for {@link StopWatchImplementation}.
 *
 * @author Philip Aston
 * @version $Revision$
 */
public class TestStopWatchImplementation extends TestCase {

  public void testStopWatch() throws Exception {
    final StopWatch stopWatch = new StopWatchImplementation();

    try {
      stopWatch.stop();
      fail("Expected StopWatchNotRunningException");
    }
    catch (StopWatchNotRunningException e) {
    }

    stopWatch.start();

    try {
      stopWatch.start();
      fail("Expected StopWatchRunningException");
    }
    catch (StopWatchRunningException e) {
    }

    try {
      stopWatch.reset();
      fail("Expected StopWatchRunningException");
    }
    catch (StopWatchRunningException e) {
    }

    try {
      stopWatch.getTime();
      fail("Expected StopWatchRunningException");
    }
    catch (StopWatchRunningException e) {
    }

    final StopWatch stopWatch2 = new StopWatchImplementation();

    try {
      stopWatch2.add(stopWatch);
      fail("Expected StopWatchRunningException");
    }
    catch (StopWatchRunningException e) {
    }

    Thread.sleep(10);

    stopWatch.stop();

    final long result = stopWatch.getTime();

    assertTrue(result >= 10);
    assertTrue(result < 100);

    stopWatch.reset();

    assertEquals(0, stopWatch.getTime());

    stopWatch.start();
    Thread.sleep(20);
    stopWatch.stop();
    final long result2 = stopWatch.getTime();

    assertTrue(result2 >= 20);
    assertTrue(result2 < 100);

    stopWatch.start();
    Thread.sleep(20);
    stopWatch.stop();
    final long result3 = stopWatch.getTime();

    assertTrue(result3 >= 40);
    assertTrue(result3 < 100);
  }
}
