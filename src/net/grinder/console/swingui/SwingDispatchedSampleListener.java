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

package net.grinder.console.swingui;

import javax.swing.SwingUtilities;

import net.grinder.console.model.SampleListener;
import net.grinder.statistics.StatisticsSet;


/**
 * SampleListener Decorator that disptaches the reset() and update()
 * notifications via a Swing thread.
 *
 * @author Philip Aston
 * @version $Revision$
 */
class SwingDispatchedSampleListener implements SampleListener {

  private final SampleListener m_delegate;

  public SwingDispatchedSampleListener(SampleListener delegate) {
    m_delegate = delegate;
  }

  public void update(final StatisticsSet intervalStatistics,
                     final StatisticsSet cumulativeStatistics) {

    SwingUtilities.invokeLater(
      new Runnable() {
        public void run() {
          m_delegate.update(intervalStatistics,
                            cumulativeStatistics);
        }
      }
      );
  }
}
