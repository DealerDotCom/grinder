// Copyright (C) 2005 Philip Aston
// All rights reserved.
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

import net.grinder.common.Test;
import net.grinder.statistics.StatisticsSet;


/**
 * Access state related to a particular dispatch of a test by a worker thread.
 *
 * @author Philip Aston
 * @version $Revision$
 */
interface DispatchContext {

  /**
   * The test.
   *
   * @return The test that this dispatch is for.
   */
  Test getTest();

  /**
   * The statistics, or <code>null</code> if the statistics have been
   * reported. Can be updated.
   *
   * @return The statistics, or <code>null</code>.
   */
  StatisticsSet getStatistics();

  /**
   * Report any pending dispatch.
   */
  void report();

  StopWatch getPauseTimer();

  long getElapsedTime();
}
