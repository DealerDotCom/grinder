// Copyright (C) 2000 Paco Gomez
// Copyright (C) 2000, 2001, 2002, 2003, 2004 Philip Aston
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

package net.grinder.plugininterface;


/**
 * <p>This class is used to share thread information between the
 * Grinder and the plug-in. </p>
 *
 * @author Paco Gomez
 * @author Philip Aston
 * @version $Revision$
 */
public interface PluginThreadContext {

  /**
   * Return the thread ID.
   *
   * @return The thread ID.
   */
  int getThreadID();

  /**
   * Return the current run number.
   *
   * @return The current run number.
   */
  int getRunNumber();

  /**
   * Get the time the current test was started.
   *
   * @return The time in milliseconds since the Epoch (see
   * <code>System.currentTimeMilis</code>).
   */
  long getStartTime();

  /**
   * Allow the plug-in more tightly to mark the critical timing
   * section.
   *
   * <p>Only the first call to {@link #startTimedSection} and the last call
   * to {@link #stopTimedSection} for a given test wrapping have any effect;
   * other calls the plug-in may make are ignored. This means that the
   * plugin can discard its own overhead (and the Jython invocation
   * overhead) for single incovations, but composite wrappings must
   * pay timing penalty for their glue.
   *
   * @see #stopTimedSection
   */
  void startTimedSection();

  /**
   * Allow the plug-in more tightly to mark the critical timing
   * section.
   *
   * @see #startTimedSection
   */
  void stopTimedSection();
}
