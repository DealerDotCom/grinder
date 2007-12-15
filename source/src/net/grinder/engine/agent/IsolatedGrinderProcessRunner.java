// Copyright (C) 2005, 2006, 2007 Philip Aston
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

import java.io.InputStream;
import java.lang.reflect.Method;

import net.grinder.engine.agent.DebugThreadWorker.IsolateGrinderProcessRunner;
import net.grinder.engine.process.WorkerProcessEntryPoint;


/**
 * Implementation of {@link DebugThreadWorker.IsolateGrinderProcessRunner} that
 * is loaded in separate {@link net.grinder.util.IsolatingClassLoader}s by
 * {@link DebugThreadWorker}.
 *
 * @author Philip Aston
 * @version $Revision$
 */
public class IsolatedGrinderProcessRunner
  implements IsolateGrinderProcessRunner {

  /**
   * Key of system property used to override the thread worker class.
   */
  public static final String RUNNER_CLASSNAME_PROPERTY =
    "grinder.debug.singleprocess.runner";

  private final Class m_workerClass;
  private final Method m_runMethod;

  /**
   * Constructor.
   *
   * @throws ClassNotFoundException If the thread worker could not be
   * introspected.
   * @throws SecurityException If the thread worker could not be
   * introspected.
   * @throws NoSuchMethodException If the thread worker does not have a run
   * method.
   */
  public IsolatedGrinderProcessRunner()
    throws ClassNotFoundException, SecurityException, NoSuchMethodException {

    // Allow the unit tests to override the thread runner.
    m_workerClass =
      Class.forName(
        System.getProperty(RUNNER_CLASSNAME_PROPERTY,
                           WorkerProcessEntryPoint.class.getName()));

    m_runMethod =
      m_workerClass.getMethod("run", new Class[] {InputStream.class });
  }

  /**
   * Create and run a
   * {@link net.grinder.engine.process.WorkerProcessEntryPoint}.
   *
   * @param agentInputStream
   *          {@link InputStream} used to listen to the agent.
   * @return Process exit code.
   */
  public int run(final InputStream agentInputStream) {

    try {
      final Object instance = m_workerClass.newInstance();

      final Integer result = (Integer)
        m_runMethod.invoke(instance, new Object[] { agentInputStream });

      return result.intValue();
    }
    catch (Exception e) {
      // We're debug code, so no need to handle this too cleanly.
      throw new AssertionError(e);
    }
  }
}
