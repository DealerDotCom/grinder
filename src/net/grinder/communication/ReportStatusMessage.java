// Copyright (C) 2001, 2002 Dirk Feufel
// Copyright (C) 2001, 2002, 2003 Philip Aston
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

import net.grinder.common.ProcessStatus;


/**
 * Message for informing the Console of worker process status.
 *
 * @author Dirk Feufel
 * @author Philip Aston
 * @version $Revision$
 */
public final class ReportStatusMessage implements Message, ProcessStatus {

  private static final long serialVersionUID = -6396659281675246288L;

  private final String m_identity;
  private final String m_name;
  private final short m_state;
  private final short m_totalNumberOfThreads;
  private final short m_numberOfRunningThreads;

  /**
   * Creates a new <code>ReportStatusMessage</code> instance.
   *
   * @param identity Process identity.
   * @param name Process name.
   * @param state The process state. See {@link
   * net.grinder.common.ProcessStatus}.
   * @param totalThreads The total number of threads.
   * @param runningThreads The number of threads that are still running.
   **/
  public ReportStatusMessage(String identity, String name, short state,
                             short runningThreads, short totalThreads) {
    m_identity = identity;
    m_name = name;
    m_state = state;
    m_numberOfRunningThreads = runningThreads;
    m_totalNumberOfThreads = totalThreads;
  }

  /**
   * Accessor for the process identity.
   *
   * @return The process name.
   */
  public String getIdentity() {
    return m_identity;
  }

  /**
   * Accessor for the process name.
   *
   * @return The process name.
   */
  public String getName() {
    return m_name;
  }

  /**
   * Accessor for the process state.
   *
   * @return The process state.
   */
  public short getState() {
    return m_state;
  }

  /**
   * Accessor for the number of running threads for the process.
   *
   * @return The number of running threads.
   */
  public short getNumberOfRunningThreads() {
    return m_numberOfRunningThreads;
  }

  /**
   * Accessor for the total number of threads for the process.
   *
   * @return The total number of threads for the process.
   */
  public short getTotalNumberOfThreads() {
    return m_totalNumberOfThreads;
  }
}
