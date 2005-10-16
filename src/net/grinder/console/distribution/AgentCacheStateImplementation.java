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

package net.grinder.console.distribution;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;


/**
 * {@link AgentCacheState} implementation.
 *
 * @author Philip Aston
 * @version $Revision$
 */
final class AgentCacheStateImplementation
  implements AgentCacheState, UpdateableAgentCacheState {

  private static final int UP_TO_DATE = 0;
  private static final int UPDATING = 1;
  private static final int OUT_OF_DATE = 2;

  private final PropertyChangeSupport m_propertyChangeSupport =
    new PropertyChangeSupport(this);

  // Synchronise on this when changing m_state, m_earliestFileTime,
  // m_updateStartTime.
  private int m_state = OUT_OF_DATE;
  private long m_earliestFileTime = -1;
  private long m_updateStartTime = -1;

  public long getEarliestFileTime() {
    return m_earliestFileTime;
  }

  public boolean getOutOfDate() {
    return UP_TO_DATE != m_state;
  }

  public void setOutOfDate() {
    setOutOfDate(-1);
  }

  public void setOutOfDate(long invalidAfter) {
    synchronized (this) {
      if (m_updateStartTime > invalidAfter) {
        m_updateStartTime = invalidAfter;
      }

      if (m_earliestFileTime > invalidAfter) {
        m_earliestFileTime = invalidAfter;
      }

      setState(OUT_OF_DATE);
    }
  }

  public void updateStarted() {
    synchronized (this) {
      m_updateStartTime = System.currentTimeMillis();
      setState(UPDATING);
    }
  }

  public void updateComplete() {
    synchronized (this) {
      if (m_state == UPDATING) {
        // Only mark clean if we haven't been marked out of date
        // during the update.
        setState(UP_TO_DATE);
      }

      // Even if we're not up to date, we've at least transfered all
      // files older than this m_updateStartTime.
      m_earliestFileTime = m_updateStartTime;
    }
  }

  private void setState(int newState) {
    final boolean oldOutOfDate = getOutOfDate();
    m_state = newState;

    m_propertyChangeSupport.firePropertyChange("outOfDate",
                                               oldOutOfDate,
                                               getOutOfDate());
  }

  public void addListener(PropertyChangeListener listener) {
    m_propertyChangeSupport.addPropertyChangeListener(listener);
  }
}

