// Copyright (C) 2005 - 2011 Philip Aston
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
// COPYRIGHT HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
// INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
// (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
// SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
// HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
// STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
// ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
// OF THE POSSIBILITY OF SUCH DAMAGE.

package net.grinder.engine.agent;

import static net.grinder.util.BlockingClassLoader.isolatingLoader;
import static java.util.Arrays.asList;

import java.io.OutputStream;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

import net.grinder.common.GrinderProperties;
import net.grinder.communication.FanOutStreamSender;
import net.grinder.engine.agent.AgentIdentityImplementation.WorkerIdentityImplementation;
import net.grinder.engine.agent.DebugThreadWorker.IsolateGrinderProcessRunner;
import net.grinder.engine.common.EngineException;
import net.grinder.engine.common.ScriptLocation;
import net.grinder.util.Directory;
import net.grinder.util.weave.agent.ExposeInstrumentation;


/**
 * Class that starts workers in a separate thread and class loader. Used for
 * debugging.
 *
 * @author Philip Aston
 */
final class DebugThreadWorkerFactory extends AbstractWorkerFactory {

  private static Class<?> s_isolatedRunnerClass =
    IsolatedGrinderProcessRunner.class;

  private final List<String> m_blockedClasses = new ArrayList<String>();

  /**
   * Allow unit tests to change the IsolateGrinderProcessRunner.
   */
  static void setIsolatedRunnerClass(Class<?> isolatedRunnerClass) {
    if (isolatedRunnerClass != null) {
      s_isolatedRunnerClass = isolatedRunnerClass;
    }
    else {
      s_isolatedRunnerClass = IsolatedGrinderProcessRunner.class;
    }
  }

  public DebugThreadWorkerFactory(AgentIdentityImplementation agentIdentity,
                                  FanOutStreamSender fanOutStreamSender,
                                  boolean reportToConsole,
                                  ScriptLocation script,
                                  GrinderProperties properties)
    throws EngineException {
    super(agentIdentity,
          fanOutStreamSender,
          reportToConsole,
          script,
          properties);

    final List<String> sharedClasses = new ArrayList<String>();

    sharedClasses.add(IsolateGrinderProcessRunner.class.getName());
    sharedClasses.add(ExposeInstrumentation.class.getName());

    final String additionalSharedClasses =
      properties.getProperty("grinder.debug.singleprocess.sharedclasses");

    if (additionalSharedClasses != null) {
      sharedClasses.addAll(asList(additionalSharedClasses.split(",")));
    }

    //  Block everything...
    m_blockedClasses.add("*");

    // .. except for the shared classes.
    for (String sharedClass : sharedClasses) {
      m_blockedClasses.add("+" + sharedClass);
    }
  }

  @Override
  protected Worker createWorker(WorkerIdentityImplementation workerIdentity,
                                Directory workingDirectory,
                                OutputStream outputStream,
                                OutputStream errorStream)
    throws EngineException {

    // Unfortunately, we can't respect the working directory.

    final ClassLoader classLoader =
      isolatingLoader((URLClassLoader)getClass().getClassLoader(),
                          m_blockedClasses);

    final Class<?> isolatedRunnerClass;

    try {
      isolatedRunnerClass =
        Class.forName(s_isolatedRunnerClass.getName(),
                      true,
                      classLoader);
    }
    catch (ClassNotFoundException e) {
      throw new AssertionError(e);
    }

    final IsolateGrinderProcessRunner runner;

    try {
      runner = (IsolateGrinderProcessRunner)isolatedRunnerClass.newInstance();
    }
    catch (InstantiationException e) {
      throw new EngineException(
        "Failed to create IsolateGrinderProcessRunner", e);
    }
    catch (IllegalAccessException e) {
      throw new EngineException(
        "Failed to create IsolateGrinderProcessRunner", e);
    }

    final Thread currentThread = Thread.currentThread();

    final ClassLoader contextLoader = currentThread.getContextClassLoader();

    try {
      currentThread.setContextClassLoader(classLoader);

      final DebugThreadWorker worker =
        new DebugThreadWorker(workerIdentity, runner);

      worker.start();

      return worker;
    }
    finally {
      currentThread.setContextClassLoader(contextLoader);
    }
  }
}
