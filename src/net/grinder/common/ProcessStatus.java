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

package net.grinder.common;

/**
 * Common interface for enquiring about process status.
 *
 * @author Philip Aston
 * @version $Revision$
 */
public interface ProcessStatus {

  /**
   * Constant representing the "started" state.
   */
  short STATE_STARTED = 1;

  /**
   * Constant representing the "running" state.
   */
  short STATE_RUNNING = 2;

  /**
   * Constant representing the "finished" state.
   */
  short STATE_FINISHED = 3;

  /**
   * Constant representing the "unknown" state.
   */
  short STATE_UNKNOWN = 4;

  /**
   * Return the process name.
   *
   * @return The process name.
   */
  String getName();

  /**
   * Return the process status.
   *
   * @return One of {@link #STATE_STARTED}, {@link #STATE_RUNNING},
   * {@link #STATE_FINISHED}.
   */
  short getState();
}
