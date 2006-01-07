// Copyright (C) 2005 Philip Aston
// All rights reserved.
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

package net.grinder.engine.process;

import java.io.File;

import net.grinder.common.Test;
import net.grinder.engine.common.EngineException;
import net.grinder.script.NotWrappableTypeException;


/**
 * Factory for proxies.
 *
 * @author Philip Aston
 * @version $Revision$
 */
public interface ScriptEngine {

  /**
   * Run any process initialisation required by the script. Called once
   * per ScriptEngine instance, before any of the other methods.
   *
   * @param scriptFile Absolute path to file containing script.
   * @param scriptDirectory Root directory. May not be script's immediate
   * parent.
   * @throws EngineException If process initialisation failed.
   */
  void initialise(File scriptFile, File scriptDirectory)
    throws EngineException;

  /**
   * Create a {@link WorkerRunnable} that will be used to run the work
   * for one worker thread.
   *
   * @return The runnable.
   * @throws EngineException If the runnable could not be created.
   */
  WorkerRunnable createWorkerRunnable() throws EngineException;

  /**
   * Create a proxy object that wraps an target object for a test.
   *
   * @param test The test.
   * @param dispatcher The proxy should use this to dispatch the work.
   * @param o Object to wrap.
   * @return The instrumented proxy.
   * @throws NotWrappableTypeException If the target cannot be wrapped.
   */
  Object createInstrumentedProxy(Test test, Dispatcher dispatcher, Object o)
    throws NotWrappableTypeException;

  /**
   * Shut down the engine.
   *
   * @throws EngineException If the engine could not be shut down.
   */
  void shutdown() throws EngineException;

  /**
   * Returns a description of the script engine for the log.
   *
   * @return The description.
   */
  String getDescription();

  /**
   * Indicates a script execution problem.
   */
  abstract class ScriptExecutionException extends EngineException {
    /**
     * Creates a new <code>ScriptExecutionException</code> instance.
     *
     * @param s Message.
     */
    public ScriptExecutionException(String s) {
      super(s);
    }

    /**
     * Creates a new <code>ScriptExecutionException</code> instance.
     *
     * @param s Message.
     * @param t Nested <code>Throwable</code>.
     */
    public ScriptExecutionException(String s, Throwable t)  {
      super(s, t);
    }

    /**
     * Subclasses abuse getMessage() to include stack trace information in
     * printStackTrace output.
     *
     * @return A short message, without a stack trace.
     */
    public abstract String getShortMessage();
  }

  /**
   * Interface to the runnable script object for a particular worker thread.
   */
  interface WorkerRunnable {
    void run() throws ScriptExecutionException;
    void shutdown() throws ScriptExecutionException;
  }

  /**
   * Call back interface that proxies use to dispatch work.
   */
  interface Dispatcher {

    /*
     * Dispatch method.
     *
     * <p>If called multiple times for the same test and thread, only the outer
     * invocation should be recorded.</p>
     */
    Object dispatch(Callable callable) throws EngineException;

    /**
     * Interface for things that can be called.
     */
    interface Callable {
      Object call();
    }
  }
}
