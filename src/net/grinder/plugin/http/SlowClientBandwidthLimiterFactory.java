// Copyright (C) 2006 Philip Aston
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

package net.grinder.plugin.http;

import net.grinder.util.Sleeper;
import net.grinder.util.Sleeper.ShutdownException;
import HTTPClient.HTTPConnection.BandwidthLimiter;
import HTTPClient.HTTPConnection.BandwidthLimiterFactory;


/**
 * BandwidthLimiterFactory that creates a {@link BandwidthLimiter} that
 * restricts the bandwidth of the data transfered through the buffer in
 * {@link BandwidthLimiter#maximumBytes} by sleeping.
 *
 * @author Philip Aston
 * @version $Revision$
 */
final class SlowClientBandwidthLimiterFactory
  implements BandwidthLimiterFactory {

  private final Sleeper m_sleeper;
  private final int m_targetBPS;

  private long m_lastBufferResizeTime;
  private int m_lastPosition;
  private int m_sleepTime;

  public SlowClientBandwidthLimiterFactory(Sleeper sleeper, int targetBPS) {
    m_sleeper = sleeper;
    m_targetBPS = targetBPS;
  }

  public BandwidthLimiter create() {
    return new SlowClientBandwidthLimiter();
  }

  private final class SlowClientBandwidthLimiter implements BandwidthLimiter {

    private static final float DAMPING_FACTOR = 2 / 3f;

    // I considered adjusting the buffer increment based on the target baud, or
    // dynamically based on the measured performance. I discounted this because
    // there's no obvious algorithm, and its likely to cause non-linear
    // behaviour due to external influences such as the MTU size. Also, having
    // the increment too small will increase the work that we have to do within
    // The Grinder, which might significantly skew timings. The fixed value of
    // 100 will split the average HTTP message up into a few chunks.
    private static final int BUFFER_INCREMENT = 100;

    public int maximumBytes(int position) {

      final long now = m_sleeper.getTimeInMilliseconds();

      // If position is 0, we use the last value for sleep time; otherwise
      // we adjust the sleep time to be closer to the ideal.
      if (position != 0) {
        final int timeSinceLastResize = (int)(now - m_lastBufferResizeTime);

        m_sleepTime +=
          ((position - m_lastPosition) * 8 * 1000 / m_targetBPS -
          timeSinceLastResize) * DAMPING_FACTOR;

        if (m_sleepTime < 0) {
          m_sleepTime = 0;
        }
      }

      m_lastPosition = position;
      m_lastBufferResizeTime = now;

      try {
        m_sleeper.sleepNormal(m_sleepTime, 0);
      }
      catch (ShutdownException e) {
        // Don't propagate exception - the thread will work out its shutdown
        // soon enough.
      }

      // Allow BUFFER_INCREMENT bytes to be read.
      return BUFFER_INCREMENT;
    }
  }
}
