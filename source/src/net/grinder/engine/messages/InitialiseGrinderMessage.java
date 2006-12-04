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

package net.grinder.engine.messages;

import java.io.File;

import net.grinder.common.GrinderProperties;
import net.grinder.common.WorkerIdentity;
import net.grinder.communication.Message;


/**
 * Message used by the agent to initialise the worker processes.
 *
 * @author Philip Aston
 * @version $Revision$
 */
public final class InitialiseGrinderMessage implements Message {

  private static final long serialVersionUID = 3L;

  private final WorkerIdentity m_workerID;
  private final boolean m_reportToConsole;
  private final File m_scriptFile;
  private final File m_scriptDirectory;
  private final GrinderProperties m_properties;

  /**
   * Constructor.
   *
   * @param workerID Worker process ID.
   * @param reportToConsole Whether or not the worker process should
   * report to the console.
   * @param scriptFile The script file to run.
   * @param scriptDirectory The script root directory.
   * @param properties Properties from the agent.
   */
  public InitialiseGrinderMessage(WorkerIdentity workerID,
                                  boolean reportToConsole,
                                  File scriptFile,
                                  File scriptDirectory,
                                  GrinderProperties properties) {
    m_workerID = workerID;
    m_reportToConsole = reportToConsole;
    m_scriptFile = scriptFile;
    m_scriptDirectory = scriptDirectory;
    m_properties = properties;
  }

  /**
   * Accessor for the worker ID.
   *
   * @return The worker ID.
   */
  public WorkerIdentity getWorkerIdentity() {
    return m_workerID;
  }

  /**
   * Accessor.
   *
   * @return Whether or not the worker process should report to the
   * console.
   */
  public boolean getReportToConsole() {
    return m_reportToConsole;
  }

  /**
   * Accessor.
   *
   * @return The script file to run.
   */
  public File getScriptFile() {
    return m_scriptFile;
  }

  /**
   * Accessor.
   *
   * @return The script root directory.
   */
  public File getScriptDirectory() {
    return m_scriptDirectory;
  }

  /**
   * Accessor.
   *
   * @return Properties from the agent.
   */
  public GrinderProperties getProperties() {
    return m_properties;
  }
}
