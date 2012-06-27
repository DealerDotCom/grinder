package net.grinder.scriptengine.groovy;

import net.grinder.engine.common.EngineException;
import net.grinder.engine.common.ScriptLocation;
import net.grinder.scriptengine.Instrumenter;
import net.grinder.scriptengine.ScriptEngineService;
import net.grinder.util.FileExtensionMatcher;

import java.util.List;

/**
 * Groovy implementation of the {@link ScriptEngineService}
 *
 * @author Ryan Gardner
 */
public class GroovyScriptEngineService implements ScriptEngineService {

    private final FileExtensionMatcher m_cljFileMatcher =
            new FileExtensionMatcher(".groovy");

    /**
     * If the script engine service can handle the given script, it should return
     * a suitable implementation.
     * <p/>
     * <p>
     * Implementations typically will execute the script and perform any
     * process level initialisation.
     * </p>
     *
     * @param script The script.
     * @return The script engine, or {@code null}.
     * @throws net.grinder.engine.common.EngineException
     *          If an implementation could not be created.
     */
    /**
     * {@inheritDoc}
     */
    @Override
    public ScriptEngine createScriptEngine(ScriptLocation script) throws EngineException {
        if (m_cljFileMatcher.accept(script.getFile())) {
            try {
                return new GroovyScriptEngine(script);
            } catch (LinkageError e) {
                throw new EngineException("Groovy is not on the classpath", e);
            }
        }

        return null;
    }

    /**
     * Initialises script engine instrumentation.
     * <p/>
     * <p>
     * Each script engine can provide instrumenters, irrespective of the engine
     * used to execute the script. The instrumenters provided by each engine are
     * consulted according to service registration order in the META-INF file.
     * </p>
     *
     * @return Additional instrumenters to use. Engines that do not provide
     *         instrumentation should return an empty list.
     * @throws net.grinder.engine.common.EngineException
     *          If a problem occurred creating instrumenters.
     */
    @Override
    public List<? extends Instrumenter> createInstrumenters() throws EngineException {
        throw new UnsupportedOperationException("createInstrumenters is not implemented in net.grinder.scriptengine.groovy.GroovyScriptEngineService");
    }
}
