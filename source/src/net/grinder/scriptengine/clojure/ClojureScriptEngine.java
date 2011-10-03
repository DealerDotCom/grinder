// Copyright (C) 2011 Philip Aston
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

package net.grinder.scriptengine.clojure;

import java.io.StringReader;

import net.grinder.engine.common.EngineException;
import net.grinder.engine.common.ScriptLocation;
import net.grinder.scriptengine.ScriptExecutionException;
import net.grinder.scriptengine.ScriptEngineService.ScriptEngine;
import net.grinder.scriptengine.ScriptEngineService.WorkerRunnable;
import clojure.lang.Compiler;
import clojure.lang.IFn;


/**
 * Clojure script engine.
 *
 * @author Philip Aston
 */
class ClojureScriptEngine implements ScriptEngine {

  private final IFn m_runnerFactory;

  public ClojureScriptEngine(ScriptLocation script) throws EngineException {
    final Object result;

    try {
      result = Compiler.loadFile(script.getFile().getPath());
    }
    catch (Exception e) {
      throw new ClojureScriptExecutionException("Failed to load " + script, e);
    }

    if (!(result instanceof IFn)) {
      throw new ClojureScriptExecutionException(
        "The script should return a function that creates a test runner " +
        "function " +
        "[It returned " + result.getClass().getName() + "]");
    }

    m_runnerFactory = (IFn)result;
  }

  public WorkerRunnable createWorkerRunnable() throws EngineException {

    final Object result;

    try {
      result = m_runnerFactory.call();
    }
    catch (Exception e) {
      throw new ClojureScriptExecutionException(
        "Failed to create test runner function", e);
    }

    if (!(result instanceof IFn)) {
      throw new ClojureScriptExecutionException(
        "The script should return a function that creates a test runner " +
        "function " +
        "[When called, it returned " + result.getClass().getName() + "]");
    }

    return new ClojureWorkerRunnable((IFn) result);
  }

  public WorkerRunnable createWorkerRunnable(Object testRunner)
    throws EngineException {

    if (testRunner instanceof IFn) {
      return new ClojureWorkerRunnable((IFn) testRunner);
    }

    throw new ClojureScriptExecutionException(
      "supplied testRunner is not a function");
  }

  public String getDescription() {
    final String versionString =
      "(let [v *clojure-version*] " +
      "(format \"Clojure %s.%s.%s\" (v :major) (v :minor) (v :incremental)))";

    return Compiler.load(new StringReader(versionString)).toString();
  }

  public void shutdown() throws EngineException {
    // No-op, until we discover whether Clojure defines an exit hook mechanism.
  }

  private final class ClojureWorkerRunnable implements WorkerRunnable {
    private final IFn m_workerFn;

    private ClojureWorkerRunnable(IFn workerFn) {
      m_workerFn = workerFn;
    }

    public void run() throws ScriptExecutionException {
      try {
        m_workerFn.call();
      }
      catch (Exception e) {
        throw new ClojureScriptExecutionException(
          "Worker thread raised exception", e);
      }
    }

    public void shutdown() throws ScriptExecutionException {
      // No-op.
    }
  }

  private static final class ClojureScriptExecutionException
    extends ScriptExecutionException {

    public ClojureScriptExecutionException(String s) {
      super(s);
    }

    public ClojureScriptExecutionException(String s, Throwable t) {
      super(s, t);
    }
  }
}
