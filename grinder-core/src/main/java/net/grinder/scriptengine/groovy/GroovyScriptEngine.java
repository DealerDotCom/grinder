package net.grinder.scriptengine.groovy;

import groovy.lang.GroovyCallable;
import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovyObject;
import groovy.lang.GroovySystem;
import groovy.lang.MissingPropertyException;
import net.grinder.engine.common.EngineException;
import net.grinder.engine.common.ScriptLocation;
import net.grinder.scriptengine.ScriptEngineService;
import net.grinder.scriptengine.ScriptEngineService.ScriptEngine;
import net.grinder.scriptengine.ScriptExecutionException;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Callable;

/**
 * Groovy implementation of {@link ScriptEngine}
 *
 * @author Ryan Gardner
 */
public class GroovyScriptEngine implements ScriptEngine {
    private static final String TEST_RUNNER_CLOSURE_NAME = "testRunner";

    private final GroovyObject m_groovyObject;

    public GroovyScriptEngine(ScriptLocation script) throws EngineException {
       // Get groovy to compile the script and access the callable closure
        ClassLoader parent = getClass().getClassLoader();
        GroovyClassLoader loader = new GroovyClassLoader(parent);
        try {
            Class testRunnerClass = loader.parseClass(script.getFile());
            GroovyObject groovyObject = (GroovyObject) testRunnerClass.newInstance();
            // test that the method exists - fail fast otherwise
            Callable<?> closure = (Callable<?>) groovyObject.getProperty(TEST_RUNNER_CLOSURE_NAME);

            m_groovyObject = groovyObject;
        }
        catch (IOException io) {
            throw new EngineException("Unable to parse groovy script at: " + script.getFile().getAbsolutePath(), io);
        }
        catch (MissingPropertyException mpe ) {
            throw new EngineException("Unable to locate the closure named \"" + TEST_RUNNER_CLOSURE_NAME + "\" in the script at: " + script.getFile().getAbsolutePath() +
                    ". Make sure that you have your groovy script wrapped in a class, and inside that class that there is a closure named: \"" +  TEST_RUNNER_CLOSURE_NAME + "\"" );
        }
        catch (InstantiationException e) {
            throw new EngineException("Unable to instantiate class from script at: " + script.getFile().getAbsolutePath(), e);
        } catch (IllegalAccessException e) {
            throw new EngineException("Unable to access class from script at: " + script.getFile().getAbsolutePath(), e);
        }
    }

    /**
     * Create a {@link net.grinder.scriptengine.ScriptEngineService.WorkerRunnable} that will be used to run the work
     * for one worker thread. The {@link net.grinder.scriptengine.ScriptEngineService.WorkerRunnable} will forward to
     * a new instance of the script's {@code TestRunner} class.
     *
     * @return The runnable.
     * @throws net.grinder.engine.common.EngineException
     *          If the runnable could not be created.
     */
    @Override
    public ScriptEngineService.WorkerRunnable createWorkerRunnable() throws EngineException {
        return new GroovyWorkerRunnable(m_groovyObject);
    }


    /**
     * Wrapper for groovy's testRunner closure.
     */
    private final class GroovyWorkerRunnable
      implements ScriptEngineService.WorkerRunnable {

        private final GroovyObject groovyObject;

        private GroovyWorkerRunnable(GroovyObject groovyObject) {
            this.groovyObject = groovyObject;
        }

        @Override
        public void run() throws ScriptExecutionException {
            try {
                groovyObject.invokeMethod(TEST_RUNNER_CLOSURE_NAME, new Object[]{});
            }
            catch (Exception e) {
                throw new GroovyScriptExecutionException("Exception raised by worker thread", e);
            }
        }

        @Override
        public void shutdown() throws ScriptExecutionException {
            // nothing to do here
        }
    }
    /**
     * Create a {@link net.grinder.scriptengine.ScriptEngineService.WorkerRunnable} that will be used to run the work
     * for one worker thread. The {@link net.grinder.scriptengine.ScriptEngineService.WorkerRunnable} will forward to
     * a the supplied {@code TestRunner}.
     *
     * @param testRunner An existing script instance that is callable.
     * @return The runnable.
     * @throws net.grinder.engine.common.EngineException
     *          If the runnable could not be created.
     */
    @Override
    public ScriptEngineService.WorkerRunnable createWorkerRunnable(Object testRunner) throws EngineException {
        if (testRunner instanceof GroovyObject) {
           return new GroovyWorkerRunnable((GroovyObject)testRunner);
        }

        throw new GroovyScriptExecutionException("supplied testRunner object is not a groovy object");
    }

    /**
     * Shut down the engine.
     *
     * @throws net.grinder.engine.common.EngineException
     *          If the engine could not be shut down.
     */
    @Override
    public void shutdown() throws EngineException {
        // nothing is necessary
    }

    /**
     * Returns a description of the script engine for the log.
     *
     * @return The description.
     */
    @Override
    public String getDescription() {
        return String.format("GroovyScriptEngine running with groovy version: %s", GroovySystem.getVersion());
    }

    protected static final class GroovyScriptExecutionException
      extends ScriptExecutionException {

      public GroovyScriptExecutionException(String s) {
        super(s);
      }

      public GroovyScriptExecutionException(String s, Throwable t) {
        super(s, t);
      }
    }
}
