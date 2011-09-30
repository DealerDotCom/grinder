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

package net.grinder.engine.process;

import net.grinder.common.GrinderProperties;
import net.grinder.engine.common.EngineException;
import net.grinder.engine.common.ScriptLocation;
import net.grinder.engine.process.instrumenter.dcr.DCRContext;


/**
 * Service interface that script engine implementations should implement and
 * register using the {@link ServiceLocator Java service mechanism}.
 *
 * <p>
 * Script engines will be {@link ScriptEngine#initialise initialised) once and
 * cached.</p>
 *
 * @author Philip Aston
 */
public interface ScriptEngineService {

  /**
   * Initialises script engine instrumentation.
   *
   * <p>
   * Each script engine can provide instrumenters, irrespective of the engine
   * used to execute the script. Multiple instrumenters can be returned using
   * {@link net.grinder.engine.process.instrumenter.CompositeInstrumenter}. The
   * instrumenters provided by each engine are consulted according to service
   * registration order in the META-INF file.
   * </p>
   *
   * @param properties
   *          Properties.
   * @param dcrContext
   *          DCR context; {@code null} if DCR is unavailable.
   * @return The instrumenter to use. Engines that do not provide
   *         instrumentation should return a
   *         {@link net.grinder.engine.process.instrumenter.NullInstrumenter}.
   * @throws EngineException
   *           If process initialisation failed.
   */
  Instrumenter createInstrumenter(GrinderProperties properties,
                                  DCRContext dcrContext)
     throws EngineException;

  /**
   * If the script engine service can handle the given script, it should return
   * a suitable implementation.
   *
   * <p>
   * This method should check it can process the script as cheaply as possible
   * (typically based on the file extension name, or by parsing the first few
   * lines of the script). Execution of the script should be deferred until
   * {@link ScriptEngine#initialise} is called.
   * </p>
   *
   * @param script
   *          The script.
   * @return The script engine, or {@code null}.
   * @throws EngineException
   *           If an implementation could not be created.
   */
  ScriptEngine getScriptEngine(ScriptLocation script) throws EngineException;

  /**
   * Handler for a particular type of script.
   */
  interface ScriptEngine {

    /**
     * Run any process initialisation required by the script. Called once
     * per ScriptEngine instance, before any of the other methods.
     *
     * @param script The script.
     * @param properties Properties.
     * @param logger Logger.
     * @throws EngineException If process initialisation failed.
     */
    void initialise(ScriptLocation script) throws EngineException;

    /**
     * Create a {@link WorkerRunnable} that will be used to run the work
     * for one worker thread. The {@link WorkerRunnable} will forward to
     * a new instance of the script's <code>TestRunner</code> class.
     *
     * of the script's <code>TestRunner</code> class should be used.
     * @return The runnable.
     * @throws EngineException If the runnable could not be created.
     */
    WorkerRunnable createWorkerRunnable() throws EngineException;

    /**
     * Create a {@link WorkerRunnable} that will be used to run the work
     * for one worker thread. The {@link WorkerRunnable} will forward to
     * a the supplied <code>testRunner</code>.
     *
     * @param testRunner An existing script instance that is callable.
     * @return The runnable.
     * @throws EngineException If the runnable could not be created.
     */
    WorkerRunnable createWorkerRunnable(Object testRunner)
      throws EngineException;

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
  }

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
     * @param t Nested {@link Throwable}.
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
}
