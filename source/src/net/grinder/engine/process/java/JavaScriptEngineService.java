package net.grinder.engine.process.java;

import net.grinder.common.GrinderProperties;
import net.grinder.engine.common.EngineException;
import net.grinder.engine.common.ScriptLocation;
import net.grinder.engine.process.Instrumenter;
import net.grinder.engine.process.ScriptEngineService;
import net.grinder.engine.process.instrumenter.NullInstrumenter;
import net.grinder.engine.process.instrumenter.dcr.DCRContext;


/**
 * Java {@link ScriptEngineService} implementation.
 *
 * @author Philip Aston
 */
public final class JavaScriptEngineService implements ScriptEngineService {

  /**
   * {@inheritDoc}
   */
  public Instrumenter createInstrumenter(GrinderProperties properties,
                                         DCRContext dcrContext)
    throws EngineException {

    if (dcrContext != null) {
      return new JavaDCRInstrumenter(dcrContext);
    }

    return new NullInstrumenter();
  }

  /**
   * {@inheritDoc}
   */
  public ScriptEngine getScriptEngine(ScriptLocation script)
    throws EngineException {
    return null;
  }
}
