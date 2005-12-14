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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.net.URLClassLoader;

import net.grinder.common.WorkerIdentity;
import net.grinder.engine.common.EngineException;
import net.grinder.util.IsolatingClassLoader;
import net.grinder.util.thread.UncheckedInterruptedException;


/**
 * Class that starts a worker in a separate thread and an
 * {@link IsolatingClassLoader}. Used for debugging.
 *
 * @author Philip Aston
 * @version $Revision$
 */
final class DebugThreadWorker implements Worker {

  private static final String[] SHARED_CLASSES = {
    IsolateGrinderProcessRunner.class.getName(),
  };

  private final WorkerIdentity m_workerIdentity;
  private final Thread m_thread;
  private final PipedOutputStream m_communicationStream;
  private int m_result;

  public DebugThreadWorker(WorkerIdentity workerIdentity)
    throws EngineException {
    m_workerIdentity = workerIdentity;

    m_communicationStream = new PipedOutputStream();
    final InputStream inputStream;

    try {
      inputStream = new PipedInputStream(m_communicationStream);
    }
    catch (IOException e) {
      UncheckedInterruptedException.ioException(e);
      throw new EngineException("Assertion failure", e);
    }

    final ClassLoader classLoader =
      new IsolatingClassLoader((URLClassLoader)getClass().getClassLoader(),
                               SHARED_CLASSES,
                               true);

    final IsolateGrinderProcessRunner runner;

    try {
      final Class isolatedRunnerClass =
        Class.forName(IsolatedGrinderProcessRunner.class.getName(),
                      true,
                      classLoader);

      runner = (IsolateGrinderProcessRunner)isolatedRunnerClass.newInstance();
    }
    catch (ClassNotFoundException e) {
      throw new EngineException(
        "Failed to create IsolateGrinderProcessRunner", e);
    }
    catch (InstantiationException e) {
      throw new EngineException(
        "Failed to create IsolateGrinderProcessRunner", e);    }
    catch (IllegalAccessException e) {
      throw new EngineException(
        "Failed to create IsolateGrinderProcessRunner", e);
    }

    m_thread = new Thread(workerIdentity.getName()) {
          public void run() {
            m_result = runner.run(inputStream);
          }
        };
    m_thread.setDaemon(true);
    m_thread.start();
  }

  public WorkerIdentity getIdentity() {
    return m_workerIdentity;
  }

  public OutputStream getCommunicationStream() {
    return m_communicationStream;
  }

  public int waitFor() {
    try {
      m_thread.join();
    }
    catch (InterruptedException e) {
      throw new UncheckedInterruptedException(e);
    }

    return m_result;
  }

  public void destroy() {
    m_thread.interrupt();
  }

  /**
   * Interface to something that can create and run a GrinderProcess. This
   * interface is loaded by both this class's classloader and by our
   * IsolatedClassLoaders.
   */
  public interface IsolateGrinderProcessRunner  {

    /**
     * Create and run a
     * {@link net.grinder.engine.process.WorkerProcessEntryPoint}.
     *
     * @param agentInputStream
     *          {@link InputStream} used to listen to the agent.
     * @return Process exit code.
     */
    int run(InputStream agentInputStream);
  }
}
