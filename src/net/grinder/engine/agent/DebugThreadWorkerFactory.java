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

package net.grinder.engine.agent;

import java.io.File;
import java.io.OutputStream;

import net.grinder.common.WorkerIdentity;
import net.grinder.communication.FanOutStreamSender;
import net.grinder.engine.common.EngineException;


/**
 * Class that starts workers in a separate thread and class loader. Used for
 * debugging.
 *
 * @author Philip Aston
 * @version $Revision$
 */
final class DebugThreadWorkerFactory extends AbstractWorkerFactory {

  private final File m_alternativePropertiesFile;

  public DebugThreadWorkerFactory(File alternativePropertiesFile,
                                  AgentIdentityImplementation agentIdentity,
                                  FanOutStreamSender fanOutStreamSender,
                                  boolean reportToConsole,
                                  File scriptFile,
                                  File scriptDirectory) {
    super(agentIdentity,
          fanOutStreamSender,
          reportToConsole,
          scriptFile,
          scriptDirectory);

    m_alternativePropertiesFile = alternativePropertiesFile;
  }

  protected Worker createWorker(WorkerIdentity workerIdentity,
                                OutputStream outputStream,
                                OutputStream errorStream)
    throws EngineException {
    return new DebugThreadWorker(workerIdentity, m_alternativePropertiesFile);
  }
}
