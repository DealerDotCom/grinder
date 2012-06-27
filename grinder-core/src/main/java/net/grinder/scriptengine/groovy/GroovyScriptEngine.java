package net.grinder.scriptengine.groovy;

import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovyObject;
import groovy.lang.MissingPropertyException;
import net.grinder.engine.common.EngineException;
import net.grinder.engine.common.ScriptLocation;
import net.grinder.scriptengine.ScriptEngineService;
import net.grinder.scriptengine.ScriptEngineService.ScriptEngine;

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

    private final Callable<?> m_runnerFactory;

    public GroovyScriptEngine(ScriptLocation script) throws EngineException {
       // Get groovy to compile the script and access the callable closure

        ClassLoader parent = getClass().getClassLoader();
        GroovyClassLoader loader = new GroovyClassLoader(parent);
        try {
            Class testRunnerClass = loader.parseClass(script.getFile());

            GroovyObject groovyObject = (GroovyObject) testRunnerClass.newInstance();
            Callable<?> closure = (Callable<?>) groovyObject.getProperty(TEST_RUNNER_CLOSURE_NAME);
            m_runnerFactory = closure;
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
        throw new UnsupportedOperationException("createWorkerRunnable is not implemented in net.grinder.scriptengine.groovy.GroovyScriptEngine");
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
        throw new UnsupportedOperationException("createWorkerRunnable is not implemented in net.grinder.scriptengine.groovy.GroovyScriptEngine");
    }

    /**
     * Shut down the engine.
     *
     * @throws net.grinder.engine.common.EngineException
     *          If the engine could not be shut down.
     */
    @Override
    public void shutdown() throws EngineException {
        throw new UnsupportedOperationException("shutdown is not implemented in net.grinder.scriptengine.groovy.GroovyScriptEngine");
    }

    /**
     * Returns a description of the script engine for the log.
     *
     * @return The description.
     */
    @Override
    public String getDescription() {
        throw new UnsupportedOperationException("getDescription is not implemented in net.grinder.scriptengine.groovy.GroovyScriptEngine");
    }
}
